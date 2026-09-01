package kaist.iclab.tracker.sensor.phone

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.galaxywatch.AudioSensor
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class VADSensor(
    private val context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
    private val audioSensor: AudioSensor,
) : BaseSensor<VADSensor.Config, VADSensor.Entity>(
    permissionManager,
    configStorage,
    stateStorage,
    Config::class,
    Entity::class,
    titleResId = R.string.sensor_vad,
    descriptionResId = R.string.sensor_desc_vad,
    icon = Icons.Default.RecordVoiceOver
) {
    companion object {
        private const val MODEL_FILE = "audio/yamnet_classification.tflite"
        private const val INPUT_SAMPLES = 15_600
        private const val OUTPUT_CLASSES = 521
        private const val PCM_NORMALIZER = 32_768f
        private const val SPEECH_CLASS_INDEX = 0 // "Speech" is typically index 0 in YAMNet
    }

    @Serializable
    data class Config(
        val speechThreshold: Float = 0.5f
    ) : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val isSpeech: Boolean,
        val speechProbability: Float,
        val inferenceTimeMs: Float
    ) : SensorEntity()

    override val id: String = "VAD"
    override val permissions: Array<String> get() = audioSensor.permissions

    override val foregroundServiceTypes: Array<Int> get() = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            null
        }
    ).toTypedArray()

    private var interpreter: Interpreter? = null
    private var input: ByteBuffer? = null
    private var output: ByteBuffer? = null
    private var inputFloats: java.nio.FloatBuffer? = null
    private var outputFloats: java.nio.FloatBuffer? = null
    private val scores = FloatArray(OUTPUT_CLASSES)

    private val audioBuffer = ShortArray(INPUT_SAMPLES)
    private var audioBufferIndex = 0
    private var ownsAudioSensor = false

    private val audioListener: (AudioSensor.Entity) -> Unit = listener@{ entity ->
        val config = configStateFlow.value
        
        var srcPos = 0
        var remainingSrc = entity.samples.size

        while (remainingSrc > 0) {
            val spaceLeft = INPUT_SAMPLES - audioBufferIndex
            val toCopy = minOf(remainingSrc, spaceLeft)
            
            System.arraycopy(entity.samples, srcPos, audioBuffer, audioBufferIndex, toCopy)
            audioBufferIndex += toCopy
            srcPos += toCopy
            remainingSrc -= toCopy

            if (audioBufferIndex == INPUT_SAMPLES) {
                runInference(audioBuffer, entity.timestamp, config.speechThreshold)
                audioBufferIndex = 0
            }
        }
    }

    override fun init() {
        super.init()
        audioSensor.init()
    }

    override fun onStart() {
        val modelBuffer = loadModelFile(context, MODEL_FILE)
        interpreter = Interpreter(
            modelBuffer,
            Interpreter.Options().apply {
                setNumThreads(2)
                setUseXNNPACK(true)
            }
        )
        
        input = ByteBuffer.allocateDirect(INPUT_SAMPLES * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
        output = ByteBuffer.allocateDirect(OUTPUT_CLASSES * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
        inputFloats = input?.asFloatBuffer()
        outputFloats = output?.asFloatBuffer()
        
        validateContract()
        
        audioBufferIndex = 0
        audioSensor.addListener(audioListener)

        if (audioSensor.sensorStateFlow.value.flag == SensorState.FLAG.DISABLED) {
            audioSensor.enable()
        }
        if (audioSensor.sensorStateFlow.value.flag != SensorState.FLAG.RUNNING) {
            ownsAudioSensor = true
            audioSensor.start()
        } else {
            ownsAudioSensor = false
        }
    }

    override fun onStop() {
        audioSensor.removeListener(audioListener)
        
        if (ownsAudioSensor && audioSensor.sensorStateFlow.value.flag == SensorState.FLAG.RUNNING) {
            audioSensor.stop()
        }
        ownsAudioSensor = false
        
        interpreter?.close()
        interpreter = null
        input = null
        output = null
        inputFloats = null
        outputFloats = null
    }
    
    @Synchronized
    private fun runInference(pcm16: ShortArray, timestamp: Long, speechThreshold: Float) {
        val startedNs = SystemClock.elapsedRealtimeNanos()
        
        inputFloats?.rewind()
        for (sample in pcm16) {
            inputFloats?.put(sample / PCM_NORMALIZER)
        }
        input?.rewind()
        output?.rewind()
        
        interpreter?.run(input, output)
        
        outputFloats?.rewind()
        outputFloats?.get(scores)
        
        val elapsedNs = SystemClock.elapsedRealtimeNanos() - startedNs
        val inferenceTimeMs = elapsedNs / 1_000_000f
        
        val speechProb = scores[SPEECH_CLASS_INDEX]
        val isSpeech = speechProb > speechThreshold
        
        if (isSpeech) {
            android.util.Log.d("VADSensor", "Speech detected! probability=${speechProb}")
        }
        
        val entity = Entity(
            received = System.currentTimeMillis(),
            timestamp = timestamp,
            isSpeech = isSpeech,
            speechProbability = speechProb,
            inferenceTimeMs = inferenceTimeMs
        )
        
        listeners.forEach { it.invoke(entity) }
    }

    private fun validateContract() {
        val interp = interpreter ?: return
        val inputTensor = interp.getInputTensor(0)
        val outputTensor = interp.getOutputTensor(0)
        require(inputTensor.shape().contentEquals(intArrayOf(INPUT_SAMPLES))) {
            "Unexpected YAMNet input shape: ${inputTensor.shape().contentToString()}"
        }
        require(inputTensor.dataType() == DataType.FLOAT32)
        require(outputTensor.shape().contentEquals(intArrayOf(1, OUTPUT_CLASSES))) {
            "Unexpected YAMNet output shape: ${outputTensor.shape().contentToString()}"
        }
        require(outputTensor.dataType() == DataType.FLOAT32)
    }

    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength).also {
            fileInputStream.close()
            assetFileDescriptor.close()
        }
    }
}
