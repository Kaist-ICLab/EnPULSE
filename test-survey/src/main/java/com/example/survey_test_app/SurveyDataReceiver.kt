package com.example.survey_test_app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.survey.SurveySensor
import org.koin.android.ext.android.inject

class SurveyDataReceiver(
    private val context: Context,
) {
    private val serviceIntent = Intent(context, SurveyDataReceiverService::class.java)
    fun startBackgroundCollection() { context.startForegroundService(serviceIntent) }
    fun stopBackgroundCollection() { context.stopService(serviceIntent) }

    class SurveyDataReceiverService: Service() {
        companion object {
            private val TAG = SurveyDataReceiverService::class.simpleName
        }

        private val sensor by inject<SurveySensor>()
        private val sensors = listOf(sensor)
        private val serviceNotification by inject<MyBackgroundController.ServiceNotification>()
        private val listener = sensors.associate {
            it.name to { e: SensorEntity -> Log.d(it.name, e.toString()); Unit }
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

            val serviceType = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            this.startForeground(
                serviceNotification.notificationId,
                postNotification,
                serviceType
            )

            for (sensor in sensors) {
                sensor.addListener(listener[sensor.name]!!)
            }

            Log.d(TAG, "SurveyDataReceiver started")

            return START_STICKY
        }

        override fun onDestroy() {
            for (sensor in sensors) {
                sensor.removeListener(listener[sensor.name]!!)
            }
        }
    }
}