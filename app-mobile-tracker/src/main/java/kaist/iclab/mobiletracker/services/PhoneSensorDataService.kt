package kaist.iclab.mobiletracker.services

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.helpers.BLEHelper
import kaist.iclab.mobiletracker.helpers.LanguageHelper
import kaist.iclab.mobiletracker.repository.CampaignSensorRepository
import kaist.iclab.mobiletracker.repository.PhoneSensorRepository
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.utils.NotificationHelper
import kaist.iclab.mobiletracker.utils.toCampaignSensorName
import kaist.iclab.tracker.sensor.common.ActivityRecognitionSensor
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.galaxywatch.AudioSensor
import kaist.iclab.tracker.sensor.phone.PhoneVadRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named
import kotlin.time.Duration.Companion.milliseconds

/**
 * Foreground service for receiving and storing phone sensor data locally in Room database.
 * Replicates the wearable tracker's batching mechanism using Channels.
 *
 * Uses LifecycleService for automatic coroutine lifecycle management.
 */
class PhoneSensorDataService : LifecycleService(), KoinComponent {
    companion object {
        private const val TAG = "PhoneSensorDataService"

        fun start(context: Context) {
            val intent = Intent(context, PhoneSensorDataService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PhoneSensorDataService::class.java)
            context.stopService(intent)
        }
    }

    private val sensors by inject<List<Sensor<*, *>>>(qualifier = named("phoneSensors"))
    private val phoneSensorRepository by inject<PhoneSensorRepository>()
    private val serviceNotification by inject<BackgroundController.ServiceNotification>()
    private val timestampService by inject<SyncTimestampService>()
    private val bleHelper by inject<BLEHelper>()

    // Channel for batching
    private val eventChannel = Channel<Pair<String, SensorEntity>>(
        capacity = Constants.DB.BUFFER_SIZE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Guards against duplicate registration / batch processing on repeated onStartCommand
    private var listenersRegistered = false
    private var batchProcessingJob: Job? = null
    private var vadRuntime: PhoneVadRuntime? = null
    
    private val audioBuffer = ShortArray(PhoneVadRuntime.INPUT_SAMPLES)
    private var audioBufferIndex = 0

    // Listener just sends to channel
    private val listener: Map<String, (SensorEntity) -> Unit> = sensors.associate { sensor ->
        sensor.id to { e: SensorEntity ->
            if (phoneSensorRepository.hasStorageForSensor(sensor.id)) {
                eventChannel.trySend(sensor.id to e)
            } else {
                Log.w(TAG, "[PHONE] - No storage found for sensor ${sensor.name} (${sensor.id})")
            }
        }
    }

    private val activityRecognitionListener: (SensorEntity) -> Unit = listener@{ entity ->
        val activityEntity = entity as? ActivityRecognitionSensor.Entity ?: return@listener

        // Process the data locally (now successfully saves to Room DB and syncs to Supabase)
        listener["ActivityRecognition"]?.invoke(entity)

        val label = when (activityEntity.activityType) {
            com.google.android.gms.location.DetectedActivity.STILL -> "Still"
            com.google.android.gms.location.DetectedActivity.WALKING -> "Walking"
            com.google.android.gms.location.DetectedActivity.RUNNING -> "Running"
            com.google.android.gms.location.DetectedActivity.ON_FOOT -> "On Foot"
            com.google.android.gms.location.DetectedActivity.ON_BICYCLE -> "On Bicycle"
            com.google.android.gms.location.DetectedActivity.IN_VEHICLE -> "In Vehicle"
            else -> "Unknown"
        }

        if (label != "Unknown") {
            Log.d(TAG, "Phone activity detected: $label. Syncing to watch...")
            bleHelper.sendDetectionStateUpdates(mapOf("physical_activity" to label))
        }
    }

    private val audioListener: (SensorEntity) -> Unit = listener@{ entity ->
        val audioEntity = entity as? AudioSensor.Entity ?: return@listener

        var srcPos = 0
        var remainingSrc = audioEntity.samples.size

        while (remainingSrc > 0) {
            val spaceLeft = PhoneVadRuntime.INPUT_SAMPLES - audioBufferIndex
            val toCopy = minOf(remainingSrc, spaceLeft)
            
            System.arraycopy(audioEntity.samples, srcPos, audioBuffer, audioBufferIndex, toCopy)
            audioBufferIndex += toCopy
            srcPos += toCopy
            remainingSrc -= toCopy

            if (audioBufferIndex == PhoneVadRuntime.INPUT_SAMPLES) {
                vadRuntime?.let { runtime ->
                    val chunkToProcess = audioBuffer.copyOf()
                    val prediction = runtime.run(chunkToProcess)
                    if (prediction.isSpeech) {
                        Log.d(TAG, "Phone YAMNet VAD detected speech! p=${prediction.speechProbability}")
                        bleHelper.sendDetectionStateUpdates(mapOf("voice_activity" to "Speech"))
                    }
                }
                audioBufferIndex = 0
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (vadRuntime == null) {
            vadRuntime = PhoneVadRuntime(this)
        }
        startForegroundService()
        registerListeners()
        startBatchProcessing()
        return START_STICKY
    }

    private fun startForegroundService() {
        val pendingIntent = NotificationHelper.createMainActivityPendingIntent(this, 0)
        val localizedContext = LanguageHelper(this).applyLanguage(this)

        val postNotification = NotificationHelper.buildNotification(
            context = this,
            channelId = serviceNotification.channelId,
            title = localizedContext.getString(R.string.notification_title),
            text = localizedContext.getString(R.string.notification_description),
            smallIcon = serviceNotification.icon,
            ongoing = true,
            pendingIntent = pendingIntent
        ).build()

        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        startForeground(serviceNotification.notificationId, postNotification, serviceType)
    }

    private val campaignSensorRepository by inject<CampaignSensorRepository>()

    private fun registerListeners() {
        if (listenersRegistered) return
        listenersRegistered = true

        val activeSensors = campaignSensorRepository.getActiveSensors().map { it.name }

        sensors.forEach { sensor ->
            // Convert library sensor ID (e.g., "Location", "AppUsageLog") to campaign table name format
            val campaignSensorName = sensor.id.toCampaignSensorName()

            if (activeSensors.contains(campaignSensorName)) {
                if (sensor is ActivityRecognitionSensor) {
                    sensor.addListener(activityRecognitionListener)
                } else if (sensor is AudioSensor) {
                    sensor.addListener(audioListener)
                } else {
                    sensor.addListener(listener[sensor.id]!!)
                }
            }
        }
        // Survey is handled entirely by SurveyResponseCapture, not this service — see its doc
        // comment for why SurveySensor isn't in the "phoneSensors" list this loop iterates.
    }

    /**
     * Starts batch processing using lifecycleScope for automatic cancellation.
     */
    private fun startBatchProcessing() {
        if (batchProcessingJob?.isActive == true) return
        batchProcessingJob = lifecycleScope.launch(Dispatchers.IO) {
            val buffer = mutableMapOf<String, MutableList<SensorEntity>>()
            var lastFlushTime = System.currentTimeMillis()

            while (isActive) {
                val result = eventChannel.tryReceive()
                if (result.isSuccess) {
                    val (sensorId, entity) = result.getOrThrow()
                    buffer.getOrPut(sensorId) { mutableListOf() }.add(entity)
                } else {
                    delay(100.milliseconds)
                }

                val currentTime = System.currentTimeMillis()
                val shouldFlush = buffer.values.any { it.size >= Constants.DB.BATCH_SIZE } ||
                        (currentTime - lastFlushTime >= Constants.DB.FLUSH_INTERVAL_MS && buffer.isNotEmpty())

                if (shouldFlush) {
                    flushBuffer(buffer)
                    lastFlushTime = currentTime
                }
            }
        }
    }

    private suspend fun flushBuffer(buffer: MutableMap<String, MutableList<SensorEntity>>) {
        buffer.forEach { (sensorId, entities) ->
            if (entities.isNotEmpty()) {
                // Copy and insert
                val batchToInsert = entities.toList()
                entities.clear()

                when (val result =
                    phoneSensorRepository.insertSensorDataBatch(sensorId, batchToInsert)) {
                    is Result.Success -> timestampService.updateLastPhoneSensorData()
                    is Result.Error -> Log.e(
                        TAG,
                        "Failed to insert batch for $sensorId: ${result.message}"
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        // Only remove if we registered
        if (listenersRegistered) {
            sensors.forEach { it.removeListener(listener[it.id]!!) }
            listenersRegistered = false
        }

        // Flush remaining data before destruction with a timeout to avoid ANR
        runBlocking {
            val flushed = withTimeoutOrNull(3000L.milliseconds) {
                val buffer = mutableMapOf<String, MutableList<SensorEntity>>()
                while (true) {
                    val result = eventChannel.tryReceive()
                    if (result.isSuccess) {
                        val (id, entity) = result.getOrThrow()
                        buffer.getOrPut(id) { mutableListOf() }.add(entity)
                    } else break
                }
                flushBuffer(buffer)
            }
            if (flushed == null) {
                Log.w(TAG, "onDestroy flush timed out after 3s — some buffered data may be lost")
            }
        }

        vadRuntime?.close()
        vadRuntime = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
