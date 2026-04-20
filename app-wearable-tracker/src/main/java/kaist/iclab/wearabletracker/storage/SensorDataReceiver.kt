package kaist.iclab.wearabletracker.storage

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.wearabletracker.Constants.DB.BATCH_SIZE
import kaist.iclab.wearabletracker.Constants.DB.BUFFER_SIZE
import kaist.iclab.wearabletracker.Constants.DB.FLUSH_INTERVAL_MS
import kaist.iclab.wearabletracker.data.AutoSyncManager
import kaist.iclab.wearabletracker.db.dao.BaseDao
import kaist.iclab.wearabletracker.repository.ErrorClassifier.runClassified
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class SensorDataReceiver(
    private val context: Context,
) {
    private val serviceIntent = Intent(context, SensorDataReceiverService::class.java)
    fun startBackgroundCollection() {
        context.startForegroundService(serviceIntent)
    }

    fun stopBackgroundCollection() {
        context.stopService(serviceIntent)
    }

    class SensorDataReceiverService : Service() {
        private val sensors by inject<List<Sensor<*, *>>>(qualifier = named("sensors"))
        private val sensorDataStorages by inject<Map<String, BaseDao<SensorEntity>>>(
            qualifier = named(
                "sensorDataStorages"
            )
        )
        private val serviceNotification by inject<BackgroundController.ServiceNotification>()

        // Injected CoroutineScope for lifecycle management
        private val coroutineScope by inject<CoroutineScope>()

        // Inject AutoSyncManager to piggyback on hardware wakeups during Doze mode
        private val autoSyncManager by inject<AutoSyncManager>()

        // Channel to receive sensor events
        private val eventChannel = Channel<Pair<String, SensorEntity>>(
            capacity = BUFFER_SIZE,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        private var batchJob: Job? = null

        // Guards against duplicate registration on repeated onStartCommand
        private var listenersRegistered = false

        private val listener: Map<String, (SensorEntity) -> Unit> = sensors.associate {
            it.id to
                    { e: SensorEntity ->
                        // Send to channel instead of immediate insert
                        eventChannel.trySend(it.id to e)
                        Unit
                    }
        }

        override fun onBind(p0: Intent?): IBinder? = null

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            val postNotification = NotificationCompat.Builder(
                this,
                serviceNotification.channelId
            )
                .setSmallIcon(serviceNotification.icon)
                .setContentTitle(serviceNotification.title)
                .setContentText(serviceNotification.description)
                .setOngoing(true)
                .build()

            val serviceType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0

            this.startForeground(
                serviceNotification.notificationId,
                postNotification,
                serviceType
            )

            // Start batch processing
            startBatchProcessing()

            // Register listeners only once to prevent duplicates
            if (!listenersRegistered) {
                listenersRegistered = true
                for (sensor in sensors) {
                    sensor.addListener(listener[sensor.id]!!)
                }
            }

            return START_STICKY
        }

        private fun startBatchProcessing() {
            if (batchJob?.isActive == true) return

            batchJob = coroutineScope.launch {
                val buffer = mutableMapOf<String, MutableList<SensorEntity>>()
                var lastFlushTime = System.currentTimeMillis()

                try {
                    while (isActive) {
                        // Calculate remaining time until next scheduled flush
                        val nextFlushDelay = maxOf(
                            0L,
                            FLUSH_INTERVAL_MS - (System.currentTimeMillis() - lastFlushTime)
                        )

                        // Wait for data OR for the flush interval to hit
                        val result = withTimeoutOrNull(nextFlushDelay) {
                            eventChannel.receive()
                        }

                        if (result != null) {
                            val (sensorId, entity) = result
                            val sensorBuffer = buffer.getOrPut(sensorId) { mutableListOf() }
                            sensorBuffer.add(entity)

                            // Flush immediately if this sensor hit the batch limit
                            if (sensorBuffer.size >= BATCH_SIZE) {
                                flushBuffer(buffer)
                                lastFlushTime = System.currentTimeMillis()
                                autoSyncManager.evalSync()
                            }
                        } else {
                            // Timeout reached: periodic flush of all sensors
                            if (buffer.isNotEmpty()) {
                                flushBuffer(buffer)
                            }
                            lastFlushTime = System.currentTimeMillis()
                            autoSyncManager.evalSync()
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e("SensorDataReceiver", "Batch processing error: ${e.message}", e)
                    }
                }
            }
        }

        private suspend fun flushBuffer(buffer: MutableMap<String, MutableList<SensorEntity>>) {
            buffer.forEach { (sensorId, entities) ->
                if (entities.isNotEmpty()) {
                    // Make a copy to insert and clear original list
                    val batchToInsert = entities.toList()
                    entities.clear()
                    runClassified(
                        "SensorDataReceiver",
                        "flush batch for $sensorId"
                    ) {
                        sensorDataStorages[sensorId]?.insert(batchToInsert)
                    }
                }
            }
        }

        override fun onDestroy() {
            // Unregister listeners
            if (listenersRegistered) {
                for (sensor in sensors) {
                    sensor.removeListener(listener[sensor.id]!!)
                }
                listenersRegistered = false
            }

            // Cancel incoming data processing job
            batchJob?.cancel()

            // Drain remaining data and flush synchronously with a timeout
            // to ensure data is persisted before the process dies
            val buffer = mutableMapOf<String, MutableList<SensorEntity>>()
            while (true) {
                val result = eventChannel.tryReceive()
                if (result.isSuccess) {
                    val (sensorId, entity) = result.getOrThrow()
                    buffer.getOrPut(sensorId) { mutableListOf() }.add(entity)
                } else {
                    break
                }
            }
            if (buffer.isNotEmpty()) {
                runBlocking {
                    withTimeoutOrNull(3000L) {
                        flushBuffer(buffer)
                    } ?: Log.w("SensorDataReceiver", "Flush timed out during onDestroy")
                }
            }
        }
    }
}