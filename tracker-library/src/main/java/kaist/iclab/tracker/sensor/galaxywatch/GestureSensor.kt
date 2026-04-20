package kaist.iclab.tracker.sensor.galaxywatch

import android.Manifest
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.watchhar.FloatRingBuffer
import kaist.iclab.tracker.sensor.watchhar.WatchHarEventDetector
import kaist.iclab.tracker.sensor.watchhar.ShortRingBuffer
import kaist.iclab.tracker.sensor.watchhar.loadModelFile
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GestureSensor(
    private val context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
    private val imuSensor: IMUSensor,
    private val audioSensor: AudioSensor,
) : BaseSensor<GestureSensor.Config, GestureSensor.Entity>(
    permissionManager,
    configStorage,
    stateStorage,
    Config::class,
    Entity::class
) {
    companion object {
        private const val CHANNELS = 6
        private const val EVENT_WINDOW_FRAMES = 150
        private const val CLASSIFIER_WINDOW_FRAMES = 50
        private const val AUDIO_WINDOW_SIZE = 950
        private const val PROBABILITY_SCALE = 1000

        private const val IMU_ENCODER_MODEL = "float32/imu_encoder.tflite"
        private const val MEL_PREPROCESS_MODEL = "float32/mel_preprocess.tflite"
        private const val AUDIO_ENCODER_MODEL = "float32/audio_encoder.tflite"
        private const val FEATURE_FUSION_MODEL = "float32/feature_fusion.tflite"

        private const val IMU_INPUT_BYTES = 1 * CLASSIFIER_WINDOW_FRAMES * 1 * CHANNELS * 4
        private const val IMU_OUTPUT_BYTES = 1 * 128 * 4
        private const val MEL_INPUT_BYTES = 1 * AUDIO_WINDOW_SIZE * 1 * 4
        private const val MEL_OUTPUT_BYTES = 1 * 96 * 64 * 4
        private const val AUDIO_OUTPUT_BYTES = 1 * 640 * 4
        private const val NUM_CLASSES = 27
        private const val FUSION_OUTPUT_BYTES = 1 * NUM_CLASSES * 4

        private val LABELS = listOf(
            "Alarm Clock",
            "Blender In Use",
            "Brushing Hair",
            "Chopping",
            "Clapping",
            "Coughing",
            "Drill In Use",
            "Drinking",
            "Grating",
            "Hair Dryer In Use",
            "Hammering",
            "Knocking",
            "Laughing",
            "Microwave",
            "Pouring Pitcher",
            "Sanding",
            "Scratching",
            "Screwing",
            "Shaver In Use",
            "Toilet Flushing",
            "Toothbrushing",
            "Twisting Jar",
            "Vacuum In Use",
            "Washing Utensils",
            "Washing Hands",
            "Wiping With Rag",
            "Other",
        )

        private val NORM_P_MAX = floatArrayOf(5.679449f, -2.147220f, 7.097475f, 0.524282f, 0.322827f, 0.323170f)
        private val NORM_P_MIN = floatArrayOf(-5.338194f, -8.850697f, -1.813361f, -0.526405f, -0.307250f, -0.342115f)
        private val NORM_P_MEAN = floatArrayOf(0.425053f, -5.287797f, 2.476576f, -0.017834f, -0.004427f, -0.020158f)
        private val NORM_P_STD = floatArrayOf(5.883963f, 4.776786f, 5.469956f, 1.504210f, 0.813269f, 0.856229f)
    }

    data class Config(
        val thresholdHigh: Float = 0.7f,
        val thresholdLow: Float = 0.5f,
        val interpreterThreads: Int = 2,
        val classificationIntervalMillis: Long = 200L,
    ) : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val classIndex: Int?,
        val label: String?,
        val score: Int?,
        val probabilities: List<Int>?,
    ) : SensorEntity()

    override val id: String = "Gesture"
    override val permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            null
        }
    ).toTypedArray()

    private val imuWindow = ArrayDeque<IMUSensor.Entity>(CLASSIFIER_WINDOW_FRAMES)
    private val eventWindow = ArrayDeque<IMUSensor.Entity>(EVENT_WINDOW_FRAMES)
    private val audioBuffer = ShortRingBuffer(AUDIO_WINDOW_SIZE)
    private val movingAvgWindow = FloatRingBuffer(100)
    private val dataLock = Any()

    private var modelRunner: WatchHarClassifier? = null
    private var eventDetector: WatchHarEventDetector? = null
    private var eventActive = false
    private var lastClassificationTimestamp = Long.MIN_VALUE
    private val classVoteCounts = IntArray(NUM_CLASSES)
    private val probabilitySums = FloatArray(NUM_CLASSES)
    private var predictionCount = 0

    private var ownsImuSensor = false
    private var ownsAudioSensor = false

    private val imuListener: (IMUSensor.Entity) -> Unit = { entity ->
        handleImuEntity(entity)
    }

    private val audioListener: (AudioSensor.Entity) -> Unit = { entity ->
        handleAudioEntity(entity)
    }

    override fun init() {
        super.init()
        imuSensor.init()
        audioSensor.init()
    }

    override fun onStart() {
        modelRunner = WatchHarClassifier(context, configStateFlow.value.interpreterThreads)
        eventDetector = WatchHarEventDetector(context, configStateFlow.value.interpreterThreads)
        eventActive = false
        synchronized(dataLock) {
            imuWindow.clear()
            eventWindow.clear()
            audioBuffer.clear()
            movingAvgWindow.clear()
            lastClassificationTimestamp = Long.MIN_VALUE
        }
        resetEventAggregation()

        imuSensor.addListener(imuListener)
        audioSensor.addListener(audioListener)

        if (imuSensor.sensorStateFlow.value.flag == SensorState.FLAG.DISABLED) {
            imuSensor.enable()
        }
        if (imuSensor.sensorStateFlow.value.flag != SensorState.FLAG.RUNNING) {
            ownsImuSensor = true
            imuSensor.start()
        } else {
            ownsImuSensor = false
        }

        if (audioSensor.sensorStateFlow.value.flag == SensorState.FLAG.DISABLED) {
            audioSensor.enable()
        }
    }

    override fun onStop() {
        imuSensor.removeListener(imuListener)
        audioSensor.removeListener(audioListener)

        if (ownsImuSensor && imuSensor.sensorStateFlow.value.flag == SensorState.FLAG.RUNNING) {
            imuSensor.stop()
        }
        if (ownsAudioSensor && audioSensor.sensorStateFlow.value.flag == SensorState.FLAG.RUNNING) {
            audioSensor.stop()
        }
        ownsImuSensor = false
        ownsAudioSensor = false

        synchronized(dataLock) {
            imuWindow.clear()
            eventWindow.clear()
            audioBuffer.clear()
            movingAvgWindow.clear()
            lastClassificationTimestamp = Long.MIN_VALUE
        }
        eventActive = false
        resetEventAggregation()
        eventDetector?.close()
        eventDetector = null
        modelRunner?.close()
        modelRunner = null
    }

    private fun handleImuEntity(entity: IMUSensor.Entity) {
        val detectionSnapshot = synchronized(dataLock) {
            if (eventWindow.size == EVENT_WINDOW_FRAMES) {
                eventWindow.removeFirst()
            }
            eventWindow.addLast(entity)

            if (imuWindow.size == CLASSIFIER_WINDOW_FRAMES) {
                imuWindow.removeFirst()
            }
            imuWindow.addLast(entity)

            if (eventWindow.size < EVENT_WINDOW_FRAMES) {
                return
            }

            val snapshot = FloatArray(EVENT_WINDOW_FRAMES * CHANNELS)
            var offset = 0
            eventWindow.forEach { frame ->
                snapshot[offset++] = frame.accX
                snapshot[offset++] = frame.accY
                snapshot[offset++] = frame.accZ
                snapshot[offset++] = frame.gyroX
                snapshot[offset++] = frame.gyroY
                snapshot[offset++] = frame.gyroZ
            }
            normalize(snapshot)
            snapshot
        }

        val detectedNow = synchronized(dataLock) {
            val probability = eventDetector?.run(detectionSnapshot) ?: return
            movingAvgWindow.write(probability)
            val smoothedProbability = movingAvgWindow.getAvg()
            when {
                smoothedProbability >= configStateFlow.value.thresholdHigh -> true
                smoothedProbability < configStateFlow.value.thresholdLow -> false
                else -> eventActive
            }
        }

        if (detectedNow != eventActive) {
            eventActive = detectedNow
            if (eventActive) {
                resetEventAggregation()
                synchronized(dataLock) {
                    audioBuffer.clear()
                    lastClassificationTimestamp = Long.MIN_VALUE
                }
                if (audioSensor.sensorStateFlow.value.flag != SensorState.FLAG.RUNNING) {
                    ownsAudioSensor = true
                    audioSensor.start()
                } else {
                    ownsAudioSensor = false
                }
            } else {
                if (ownsAudioSensor && audioSensor.sensorStateFlow.value.flag == SensorState.FLAG.RUNNING) {
                    audioSensor.stop()
                }
                ownsAudioSensor = false
                synchronized(dataLock) {
                    audioBuffer.clear()
                    lastClassificationTimestamp = Long.MIN_VALUE
                }
                emitAggregatedEvent(entity.timestamp)
                resetEventAggregation()
            }
        }
    }

    private fun handleAudioEntity(entity: AudioSensor.Entity) {
        if (!eventActive) return

        val shouldClassify = synchronized(dataLock) {
            audioBuffer.write(entity.samples.toShortArray())
            if (!audioBuffer.isFull() || imuWindow.size < CLASSIFIER_WINDOW_FRAMES) {
                return
            }

            val intervalMillis = configStateFlow.value.classificationIntervalMillis.coerceAtLeast(0L)
            if (intervalMillis > 0L &&
                lastClassificationTimestamp != Long.MIN_VALUE &&
                entity.timestamp - lastClassificationTimestamp < intervalMillis
            ) {
                return
            }

            lastClassificationTimestamp = entity.timestamp
            true
        }

        if (shouldClassify) {
            classify(entity.timestamp)
        }
    }

    private fun classify(timestamp: Long) {
        val imuInput = synchronized(dataLock) {
            val result = FloatArray(CLASSIFIER_WINDOW_FRAMES * CHANNELS)
            var offset = 0
            imuWindow.forEach { frame ->
                result[offset++] = frame.accX
                result[offset++] = frame.accY
                result[offset++] = frame.accZ
                result[offset++] = frame.gyroX
                result[offset++] = frame.gyroY
                result[offset++] = frame.gyroZ
            }
            normalize(result)
            result
        }

        val audioInput = synchronized(dataLock) {
            audioBuffer.getLastData(AUDIO_WINDOW_SIZE)
        }

        val result = modelRunner?.runClassifier(imuInput, audioInput) ?: return
        val classIndex = result.indices.maxByOrNull { result[it] } ?: return
        synchronized(dataLock) {
            classVoteCounts[classIndex] += 1
            for (index in result.indices) {
                probabilitySums[index] += result[index]
            }
            predictionCount += 1
        }
    }

    private fun emitEntity(
        timestamp: Long,
        classIndex: Int?,
        probabilities: List<Int>?,
    ) {
        val received = System.currentTimeMillis()
        val entity = Entity(
            received = received,
            timestamp = timestamp,
            classIndex = classIndex,
            label = classIndex?.let { LABELS.getOrNull(it) },
            score = classIndex?.let { probabilities?.getOrNull(it) },
            probabilities = probabilities
        )
        listeners.forEach { it.invoke(entity) }
    }

    private fun emitAggregatedEvent(timestamp: Long) {
        val averagedProbabilities = synchronized(dataLock) {
            if (predictionCount == 0) {
                return
            }

            FloatArray(NUM_CLASSES) { index ->
                probabilitySums[index] / predictionCount.toFloat()
            }
        }

        val classIndex = classVoteCounts.indices.maxByOrNull { classVoteCounts[it] } ?: return
        val scaledProbabilities = scaleProbabilitiesToThousand(averagedProbabilities)
        emitEntity(
            timestamp = timestamp,
            classIndex = classIndex,
            probabilities = scaledProbabilities
        )
    }

    private fun resetEventAggregation() {
        classVoteCounts.fill(0)
        probabilitySums.fill(0f)
        predictionCount = 0
    }

    private fun scaleProbabilitiesToThousand(probabilities: FloatArray): List<Int> {
        val total = probabilities.sum().takeIf { it > 0f } ?: return List(probabilities.size) { 0 }
        val scaled = probabilities.map { (it / total) * PROBABILITY_SCALE }
        val base = scaled.map { kotlin.math.floor(it).toInt() }.toMutableList()
        var remaining = PROBABILITY_SCALE - base.sum()

        scaled.withIndex()
            .sortedByDescending { it.value - kotlin.math.floor(it.value) }
            .forEach { indexedValue ->
                if (remaining <= 0) return@forEach
                base[indexedValue.index] += 1
                remaining -= 1
            }

        return base
    }

    private fun normalize(buffer: FloatArray) {
        for (index in buffer.indices) {
            val channel = index % CHANNELS
            val step1 = 1f + (buffer[index] - NORM_P_MAX[channel]) * 2f / (NORM_P_MAX[channel] - NORM_P_MIN[channel])
            buffer[index] = (step1 - NORM_P_MEAN[channel]) / NORM_P_STD[channel]
        }
    }

    private class WatchHarClassifier(
        context: Context,
        numThreads: Int,
    ) {
        private val interpreterOptions = Interpreter.Options().apply {
            setNumThreads(numThreads)
            setUseXNNPACK(true)
        }

        private val imuInterpreter = Interpreter(loadModelFile(context, IMU_ENCODER_MODEL), interpreterOptions)
        private val melInterpreter = Interpreter(loadModelFile(context, MEL_PREPROCESS_MODEL), interpreterOptions)
        private val audioInterpreter = Interpreter(loadModelFile(context, AUDIO_ENCODER_MODEL), interpreterOptions)
        private val fusionInterpreter = Interpreter(loadModelFile(context, FEATURE_FUSION_MODEL), interpreterOptions)

        private val imuInputBuffer = ByteBuffer.allocateDirect(IMU_INPUT_BYTES).order(ByteOrder.nativeOrder())
        private val imuInputFloats = imuInputBuffer.asFloatBuffer()
        private val imuOutputBuffer = ByteBuffer.allocateDirect(IMU_OUTPUT_BYTES).order(ByteOrder.nativeOrder())
        private val melInputBuffer = ByteBuffer.allocateDirect(MEL_INPUT_BYTES).order(ByteOrder.nativeOrder())
        private val melOutputBuffer = ByteBuffer.allocateDirect(MEL_OUTPUT_BYTES).order(ByteOrder.nativeOrder())
        private val audioOutputBuffer = ByteBuffer.allocateDirect(AUDIO_OUTPUT_BYTES).order(ByteOrder.nativeOrder())
        private val fusionOutputBuffer = ByteBuffer.allocateDirect(FUSION_OUTPUT_BYTES).order(ByteOrder.nativeOrder())
        private val resultBuffer = FloatArray(NUM_CLASSES)

        @Synchronized
        fun runClassifier(imuData: FloatArray, audioData: ShortArray): FloatArray {
            imuInputBuffer.rewind()
            imuInputFloats.rewind()
            imuOutputBuffer.rewind()
            imuInputFloats.put(imuData)
            imuInterpreter.run(imuInputBuffer, imuOutputBuffer)

            melInputBuffer.rewind()
            for (sample in audioData) {
                melInputBuffer.putFloat(sample.toFloat() / 32768f)
            }
            melInputBuffer.rewind()
            melOutputBuffer.rewind()
            melInterpreter.run(melInputBuffer, melOutputBuffer)

            melOutputBuffer.rewind()
            audioOutputBuffer.rewind()
            audioInterpreter.run(melOutputBuffer, audioOutputBuffer)

            imuOutputBuffer.rewind()
            audioOutputBuffer.rewind()
            fusionOutputBuffer.rewind()
            val outputs: MutableMap<Int, Any> = hashMapOf(0 to fusionOutputBuffer)
            fusionInterpreter.runForMultipleInputsOutputs(
                arrayOf(imuOutputBuffer, audioOutputBuffer),
                outputs
            )

            fusionOutputBuffer.rewind()
            fusionOutputBuffer.asFloatBuffer().get(resultBuffer)
            softmax(resultBuffer)
            return resultBuffer.copyOf()
        }

        fun close() {
            imuInterpreter.close()
            melInterpreter.close()
            audioInterpreter.close()
            fusionInterpreter.close()
        }

        private fun softmax(logits: FloatArray) {
            val maxValue = logits.max()
            var sum = 0f
            for (index in logits.indices) {
                logits[index] = kotlin.math.exp((logits[index] - maxValue).toDouble()).toFloat()
                sum += logits[index]
            }
            for (index in logits.indices) {
                logits[index] /= sum
            }
        }
    }
}
