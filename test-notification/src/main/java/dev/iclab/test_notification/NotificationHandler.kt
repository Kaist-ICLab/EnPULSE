package dev.iclab.test_notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificationHandler(private val context: Context) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private val serviceChannelId = "TEST_SERVICE_CHANNEL"
        private val serviceChannelName = "Test Service Channel"
    }


    fun runService() {
        val intent = Intent(context, TestService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopService() {
        val intent = Intent(context, TestService::class.java)
        context.stopService(intent)
    }

    private val serviceChannel = NotificationChannel(
        serviceChannelId,
        serviceChannelName,
        NotificationManager.IMPORTANCE_DEFAULT
    )


    private val postChannelId = "POST_CHANNEL"
    private val postChannelName = "Post Channel"
    private val postNotificationId = 1
    private val postChannel = NotificationChannel(
        postChannelId,
        postChannelName,
        NotificationManager.IMPORTANCE_DEFAULT
    )
    private val postNotification = NotificationCompat.Builder(context, postChannelId)
        .setSmallIcon(R.drawable.ic_notification_post)
        .setContentTitle("Test Posting")
        .setContentText("Testing...")
        .setOngoing(true)
        .build()

    fun post() {
        notificationManager.notify(postNotificationId, postNotification)
    }

    fun remove() {
        notificationManager.cancel(postNotificationId)
    }

    fun initNotification() {
        /*Register channels*/
        notificationManager.createNotificationChannel(serviceChannel)
        notificationManager.createNotificationChannel(postChannel)
    }

    class TestService : Service() {
        private var job: Job? = null
        override fun onBind(p0: Intent?): IBinder? = null

        private val serviceNotificationId = 1


        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            job = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    delay(5000)
                    Log.d("TestService", "Service is running...")
                }
            }
            val serviceNotification =
                NotificationCompat.Builder(this.applicationContext, serviceChannelId)
                    .setSmallIcon(R.drawable.ic_notification_service)
                    .setContentTitle("Test Service Notification")
                    .setContentText("Testing service...")
                    .setOngoing(true)
                    .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    serviceNotificationId,
                    serviceNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                ServiceCompat.startForeground(this, serviceNotificationId, serviceNotification, 0)
            }
            return super.onStartCommand(intent, flags, startId)
        }

        override fun onDestroy() {
            super.onDestroy()
            job?.cancel()
        }
    }
}