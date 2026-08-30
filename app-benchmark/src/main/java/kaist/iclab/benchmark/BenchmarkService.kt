package kaist.iclab.benchmark

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

/**
 * Foreground service that periodically collects device metrics and writes them to a CSV.
 *
 * Launched by [BenchmarkActivity] with extras:
 *   - EXTRA_INTERVAL_MS: sampling interval in milliseconds
 *   - EXTRA_SCENARIO_NAME: label for the CSV filename
 */
class BenchmarkService : Service() {

    companion object {
        const val TAG = "BenchmarkService"
        const val CHANNEL_ID = "benchmark_channel"
        const val NOTIFICATION_ID = 9001

        const val EXTRA_INTERVAL_MS = "extra_interval_ms"
        const val EXTRA_SCENARIO_NAME = "extra_scenario_name"

        const val ACTION_STOP = "kaist.iclab.benchmark.STOP"

        var isRunning = false
            private set

        var currentFolderPath: String? = null
            private set

        var startTimeMs: Long = 0
            private set

        var initialBatteryLevel: Int = -1
            private set

        var latestBatteryLevel: Int = -1
            private set
    }

    private lateinit var metricsCollector: MetricsCollector
    private lateinit var csvWriter: CsvWriter
    private lateinit var handler: Handler
    private var wakeLock: PowerManager.WakeLock? = null
    private var intervalMs: Long = 60_000L

    private val tickRunnable = object : Runnable {
        override fun run() {
            try {
                val snapshot = metricsCollector.collect()
                csvWriter.write(snapshot)
                
                if (initialBatteryLevel == -1) {
                    initialBatteryLevel = snapshot.batteryLevel
                }
                latestBatteryLevel = snapshot.batteryLevel
                
                updateNotification(snapshot)
                Log.d(TAG, "Tick: battery=${snapshot.batteryLevel}%, cpu=${snapshot.cpuUsagePercent}%")
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting metrics", e)
            }
            handler.postDelayed(this, intervalMs)
        }
    }

    override fun onCreate() {
        super.onCreate()
        metricsCollector = MetricsCollector(applicationContext)
        csvWriter = CsvWriter(applicationContext)
        handler = Handler(Looper.getMainLooper())
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        intervalMs = intent?.getLongExtra(EXTRA_INTERVAL_MS, 60_000L) ?: 60_000L
        val scenarioName = intent?.getStringExtra(EXTRA_SCENARIO_NAME) ?: "unnamed"

        // Start foreground immediately
        val notification = buildNotification("Starting...", 0)
        startForeground(NOTIFICATION_ID, notification)

        // Acquire partial wake lock
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "EnPULSE-Benchmark::MetricsLogger"
        ).apply { acquire() }

        // Open CSV file
        try {
            currentFolderPath = csvWriter.open(scenarioName)
            Log.i(TAG, "CSV folder created: $currentFolderPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create CSV file", e)
            stopSelf()
            return START_NOT_STICKY
        }

        startTimeMs = System.currentTimeMillis()
        initialBatteryLevel = -1
        latestBatteryLevel = -1
        isRunning = true

        // Take an initial measurement immediately, then schedule recurring
        handler.post(tickRunnable)

        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(tickRunnable)
        
        // Write summary before closing
        if (startTimeMs > 0 && initialBatteryLevel != -1) {
            val endTimeMs = System.currentTimeMillis()
            val durationHrs = (endTimeMs - startTimeMs) / (1000.0 * 3600.0)
            val dropped = initialBatteryLevel - latestBatteryLevel
            val drainRate = if (durationHrs > 0) dropped / durationHrs else 0.0
            
            val summaryText = buildString {
                appendLine("EnPULSE Benchmark Summary")
                appendLine("=========================")
                appendLine("Start Time: ${java.util.Date(startTimeMs)}")
                appendLine("End Time:   ${java.util.Date(endTimeMs)}")
                appendLine("Duration:   ${String.format("%.2f", durationHrs)} hours")
                appendLine("Initial Battery: $initialBatteryLevel%")
                appendLine("Final Battery:   $latestBatteryLevel%")
                appendLine("Total Dropped:   $dropped%")
                appendLine("Drain Rate:      ${String.format("%.2f", drainRate)} %/hr")
                appendLine("Interval:        ${intervalMs / 1000} seconds")
            }
            csvWriter.writeSummary(summaryText)
        }
        
        csvWriter.close()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}

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
            description = "Benchmark resource logger"
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

    private fun updateNotification(snapshot: MetricsSnapshot) {
        val elapsed = (System.currentTimeMillis() - startTimeMs) / 1000
        val hours = elapsed / 3600
        val minutes = (elapsed % 3600) / 60
        val text = "🔋 ${snapshot.batteryLevel}% | ⏱ ${hours}h ${minutes}m | CPU ${String.format("%.0f", snapshot.cpuUsagePercent)}%"

        val notification = buildNotification(text, snapshot.batteryLevel)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }
}
