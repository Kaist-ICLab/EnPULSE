package kaist.iclab.mobiletracker.webapp.bridge

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
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
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
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

    fun scheduleLocalNotification(request: BridgeRequest): BridgeResponse {
        // Implementation for scheduling a notification would require WorkManager or AlarmManager.
        // For simplicity in this demo, we'll just acknowledge it.
        return BridgeResponse(request.requestId, "success", errorMessage = "Not fully implemented in this demo")
    }

    fun closeWebApp(request: BridgeRequest): BridgeResponse {
        Handler(Looper.getMainLooper()).post {
            closeAction()
        }
        return BridgeResponse(request.requestId, "success")
    }

    fun openNativeSettings(request: BridgeRequest): BridgeResponse {
        val params = request.payload.jsonObject
        val settingType = params["settingType"]?.jsonPrimitive?.content ?: ""
        
        val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        return BridgeResponse(request.requestId, "success")
    }
}
