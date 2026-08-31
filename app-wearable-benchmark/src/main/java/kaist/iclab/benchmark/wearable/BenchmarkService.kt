package kaist.iclab.benchmark.wearable

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.launch

/**
 * Foreground service that periodically collects device metrics and writes them to a CSV.
 * Adapted for Wear OS.
 *
 * Launched by [BenchmarkActivity] with extras:
 *   - EXTRA_INTERVAL_MS: sampling interval in milliseconds
 *   - EXTRA_SCENARIO_NAME: label for the CSV filename
 */
class BenchmarkService : Service() {

    companion object {
        const val TAG = "WatchBenchmarkService"
        const val CHANNEL_ID = "watch_benchmark_channel"
        const val NOTIFICATION_ID = 9002

        const val EXTRA_INTERVAL_MS = "extra_interval_ms"
        const val EXTRA_SCENARIO_NAME = "extra_scenario_name"

        const val ACTION_STOP = "kaist.iclab.benchmark.wearable.STOP"
        const val ACTION_PAUSE = "kaist.iclab.benchmark.wearable.PAUSE"
        const val ACTION_RESUME = "kaist.iclab.benchmark.wearable.RESUME"

        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"

        var isRunning = false
            private set

        var isPaused = false
            private set

        var currentFolderPath: String? = null
            private set

        var startTimeMs: Long = 0
            private set

        var totalPausedMs: Long = 0
            private set

        var initialBatteryLevel: Int = -1
            private set

        var latestBatteryLevel: Int = -1
            private set

        var initialBatteryChargeUah: Long = -1L
            private set

        var latestBatteryChargeUah: Long = -1L
            private set

        var maxCpuTemperature: Float = -1f
            private set

        var maxNativeHeapBytes: Long = -1L
            private set

        var maxAppMemoryMb: Float = -1f
            private set

        /**
         * Checks if there are any existing benchmark data folders stored on the device.
         * 
         * @param context The application context.
         * @return `true` if at least one benchmark data folder exists, `false` otherwise.
         */
        fun hasStoredData(context: android.content.Context): Boolean {
            val baseDir = context.getExternalFilesDir("Benchmarks") ?: return false
            return baseDir.exists() && baseDir.isDirectory && (baseDir.listFiles { file ->
                file.isDirectory && file.name.startsWith(
                    "watch-"
                )
            }?.isNotEmpty() == true)
        }

        /**
         * Deletes all benchmark data folders stored on the device.
         * 
         * @param context The application context.
         */
        fun deleteAllData(context: android.content.Context) {
            val baseDir = context.getExternalFilesDir("Benchmarks") ?: return
            if (baseDir.exists() && baseDir.isDirectory) {
                baseDir.listFiles()?.forEach { it.deleteRecursively() }
            }
        }
    }

    private lateinit var metricsCollector: MetricsCollector
    private lateinit var csvWriter: CsvWriter
    private lateinit var handler: Handler
    private var wakeLock: PowerManager.WakeLock? = null
    private var intervalMs: Long = 60_000L
    private var targetDurationMs: Long = 0L
    private var pauseStartTimeMs: Long = 0L

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            val now = System.currentTimeMillis()

            if (isPaused) {
                handler.postDelayed(this, 1000)
                return
            }

            val elapsedMs = now - startTimeMs - totalPausedMs
            if (targetDurationMs > 0 && elapsedMs >= targetDurationMs) {
                Log.i(TAG, "Auto-stop duration reached: ${targetDurationMs / 60000} mins")
                playAutoStopSound()
                performStop()
                return
            }

            try {
                val snapshot = metricsCollector.collect()
                csvWriter.write(snapshot)

                if (initialBatteryLevel == -1) {
                    initialBatteryLevel = snapshot.batteryLevel
                }
                latestBatteryLevel = snapshot.batteryLevel

                if (initialBatteryChargeUah == -1L) {
                    initialBatteryChargeUah = snapshot.batteryChargeUah
                }
                latestBatteryChargeUah = snapshot.batteryChargeUah

                if (snapshot.cpuTemperature > maxCpuTemperature) {
                    maxCpuTemperature = snapshot.cpuTemperature
                }
                if (snapshot.nativeHeapBytes > maxNativeHeapBytes) {
                    maxNativeHeapBytes = snapshot.nativeHeapBytes
                }
                if (snapshot.appMemoryMb > maxAppMemoryMb) {
                    maxAppMemoryMb = snapshot.appMemoryMb
                }

                updateNotification(snapshot, elapsedMs)
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting metrics", e)
            }
            handler.postDelayed(this, intervalMs)
        }
    }

    private fun playAutoStopSound() {
        try {
            val uri =
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play sound", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        metricsCollector = MetricsCollector(applicationContext)
        csvWriter = CsvWriter(applicationContext)
        handler = Handler(Looper.getMainLooper())
        createNotificationChannel()

        // Start foreground immediately in onCreate to prevent RemoteServiceException
        val notification = buildNotification("Starting...", 0)
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                performStop()
                return START_NOT_STICKY
            }

            ACTION_PAUSE -> {
                if (!isPaused) {
                    isPaused = true
                    pauseStartTimeMs = System.currentTimeMillis()
                    updateNotificationForPause()
                }
                return START_STICKY
            }

            ACTION_RESUME -> {
                if (isPaused) {
                    isPaused = false
                    totalPausedMs += (System.currentTimeMillis() - pauseStartTimeMs)
                    handler.removeCallbacks(tickRunnable)
                    handler.post(tickRunnable)
                }
                return START_STICKY
            }
        }

        intervalMs = intent?.getLongExtra(EXTRA_INTERVAL_MS, 60_000L) ?: 60_000L
        val durationMins = intent?.getLongExtra(EXTRA_DURATION_MINUTES, 0L) ?: 0L
        targetDurationMs = durationMins * 60_000L
        val scenarioName = intent?.getStringExtra(EXTRA_SCENARIO_NAME) ?: "unnamed"

        // Acquire partial wake lock with a 24-hour timeout safety net
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "EnPULSE-WatchBenchmark::MetricsLogger"
        ).apply { acquire(24 * 60 * 60 * 1000L) }

        // Use a coroutine to prevent main thread blocking for File IO
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val folder = csvWriter.open(scenarioName)
                currentFolderPath = folder
                Log.i(TAG, "CSV folder created: $folder")

                startTimeMs = System.currentTimeMillis()
                totalPausedMs = 0L
                isPaused = false
                initialBatteryLevel = -1
                latestBatteryLevel = -1
                initialBatteryChargeUah = -1L
                latestBatteryChargeUah = -1L
                maxCpuTemperature = -1f
                maxNativeHeapBytes = -1L
                maxAppMemoryMb = -1f
                isRunning = true

                // Take an initial measurement immediately, then schedule recurring
                handler.post(tickRunnable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create CSV file", e)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private var isStopping = false

    /**
     * Gracefully stops the benchmark recording, calculates the summary statistics,
     * writes the summary to the CSV folder, and initiates data transfer to the phone.
     */
    private fun performStop() {
        if (isStopping) return
        isStopping = true
        isRunning = false
        handler.removeCallbacks(tickRunnable)

        // Write summary before closing
        if (startTimeMs > 0 && initialBatteryLevel != -1) {
            val endTimeMs = System.currentTimeMillis()
            if (isPaused) {
                totalPausedMs += (endTimeMs - pauseStartTimeMs)
            }
            val dropped = initialBatteryLevel - latestBatteryLevel
            val activeDurationHrs = (endTimeMs - startTimeMs - totalPausedMs) / (1000.0 * 3600.0)
            val drainRate = if (activeDurationHrs > 0) dropped / activeDurationHrs else 0.0

            val droppedCharge =
                if (initialBatteryChargeUah != -1L && latestBatteryChargeUah != -1L) {
                    initialBatteryChargeUah - latestBatteryChargeUah
                } else 0L

            val summaryText = buildString {
                appendLine("EnPULSE Watch Benchmark Summary")
                appendLine("================================")
                appendLine("Start Time:      ${java.util.Date(startTimeMs)}")
                appendLine("End Time:        ${java.util.Date(endTimeMs)}")
                appendLine("Active Dur:      ${String.format("%.2f", activeDurationHrs)} hours")
                appendLine(
                    "Paused Dur:      ${
                        String.format(
                            "%.2f",
                            totalPausedMs / (1000.0 * 3600.0)
                        )
                    } hours"
                )
                appendLine("Initial Battery: $initialBatteryLevel%")
                appendLine("Final Battery:   $latestBatteryLevel%")
                appendLine("Total Dropped:   $dropped%")
                appendLine("Initial Charge:  ${initialBatteryChargeUah} uAh")
                appendLine("Final Charge:    ${latestBatteryChargeUah} uAh")
                appendLine("Charge Dropped:  ${droppedCharge} uAh")
                appendLine("Drain Rate:      ${String.format("%.2f", drainRate)} %/hr")
                appendLine(
                    "Max CPU Temp:    ${
                        if (maxCpuTemperature >= 0) String.format(
                            "%.1f",
                            maxCpuTemperature
                        ) + " C" else "N/A"
                    }"
                )
                appendLine("Max App Memory:  ${String.format("%.1f", maxAppMemoryMb)} MB")
                appendLine(
                    "Max Native Heap: ${
                        String.format(
                            "%.1f",
                            maxNativeHeapBytes / (1024f * 1024f)
                        )
                    } MB"
                )
                appendLine("Interval:        ${intervalMs / 1000} seconds")
            }
            csvWriter.writeSummary(summaryText)
        }
        csvWriter.close()

        val finalFolder = currentFolderPath
        if (finalFolder != null) {
            val notification = buildNotification("Stopping & sending data...", latestBatteryLevel)
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID, notification)

            val ctx = applicationContext
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                DataTransferManager.sendFolderToPhone(ctx, finalFolder)
                stopSelf()
            }
        } else {
            stopSelf()
        }
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(tickRunnable)
        csvWriter.close()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }

        Log.i(TAG, "Service stopped. Folder: $currentFolderPath")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Watch benchmark resource logger"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String, batteryLevel: Int): Notification {
        val stopIntent = Intent(this, BenchmarkService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, BenchmarkActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(
                Notification.Action.Builder(
                    null, "Stop", stopPending
                ).build()
            )
            .build()
    }

    private fun updateNotificationForPause() {
        val elapsedMs = System.currentTimeMillis() - startTimeMs - totalPausedMs
        val hours = elapsedMs / 3600_000
        val minutes = (elapsedMs % 3600_000) / 60_000
        val seconds = (elapsedMs % 60_000) / 1000
        val text = "⏸ PAUSED | ⏱ ${hours}h ${minutes}m ${seconds}s"
        val notification = buildNotification(text, latestBatteryLevel)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(snapshot: MetricsSnapshot, elapsedMs: Long) {
        val hours = elapsedMs / 3600_000
        val minutes = (elapsedMs % 3600_000) / 60_000
        val seconds = (elapsedMs % 60_000) / 1000
        val text = "⏱ ${hours}h ${minutes}m ${seconds}s"

        val notification = buildNotification(text, snapshot.batteryLevel)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }
}
