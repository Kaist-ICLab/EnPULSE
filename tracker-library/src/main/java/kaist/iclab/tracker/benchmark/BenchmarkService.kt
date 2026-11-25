package kaist.iclab.tracker.benchmark

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kaist.iclab.tracker.R

/**
 * Keeps EnPULSE's process alive during an independent benchmark run (pradipta/testing only). With
 * collection stopped, nothing else does, and Android freezes or kills a background process, which
 * would stop the recording. All the recording work stays in [BenchmarkRecorder].
 *
 * Sticky, so if the process is killed anyway Android restarts the service, and the run continues.
 */
class BenchmarkService : Service() {
    companion object {
        private const val CHANNEL_ID = "enpulse_benchmark"
        private const val NOTIFICATION_ID = 0x42454e43 // "BENC"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForeground must come first: Android stops the app if it is late.
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.benchmark_notification_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val scenario = BenchmarkRecorder.activeRun.value?.scenario.orEmpty()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(getString(R.string.benchmark_notification_title))
            .setContentText(getString(R.string.benchmark_notification_text, scenario))
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
        )
        BenchmarkRecorder.onServiceStarted()
        return START_STICKY
    }
}
