package kaist.iclab.mobiletracker.webapp.bridge

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.widget.Toast
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.utils.NotificationHelper
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AppBridgeHandler(
    private val context: Context,
    private val closeAction: () -> Unit
) {
    fun showNativeToast(request: BridgeRequest): BridgeResponse {
        val params = request.payload.jsonObject
        val message = params["message"]?.jsonPrimitive?.content ?: ""
        val durationStr = params["duration"]?.jsonPrimitive?.content ?: "short"

        val duration = if (durationStr == "long") Toast.LENGTH_LONG else Toast.LENGTH_SHORT

        // Must run on UI thread
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, duration).show()
        }

        return BridgeResponse(request.requestId, "success")
    }

    fun vibrate(request: BridgeRequest): BridgeResponse {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            val effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        }

        return BridgeResponse(request.requestId, "success")
    }

    fun showNativeNotification(request: BridgeRequest): BridgeResponse {
        val params = request.payload.jsonObject
        val title = params["title"]?.jsonPrimitive?.content ?: "EnPULSE WebApp"
        val message = params["message"]?.jsonPrimitive?.content ?: ""

        val channelId = "${Constants.Notification.CHANNEL_ID_SURVEY_TRIGGER}_webapp_notify"
        NotificationHelper.ensureNotificationChannel(
            context = context,
            channelId = channelId,
            channelName = "WebApp Notifications"
        )

        val notification = NotificationHelper.buildNotification(
            context = context,
            channelId = channelId,
            title = title,
            text = message,
            smallIcon = R.drawable.ic_launcher_foreground
        ).build()

        val notificationId = message.hashCode()
        NotificationHelper.showNotification(context, notificationId, notification)

        return BridgeResponse(request.requestId, "success")
    }

    fun closeWebApp(request: BridgeRequest): BridgeResponse {
        Handler(Looper.getMainLooper()).post {
            closeAction()
        }
        return BridgeResponse(request.requestId, "success")
    }

    fun openNativeSettings(request: BridgeRequest): BridgeResponse {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        return BridgeResponse(request.requestId, "success")
    }
}
