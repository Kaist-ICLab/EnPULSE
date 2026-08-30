package kaist.iclab.mobiletracker.webapp.bridge

import android.content.Context
import android.content.ContextWrapper
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
import android.app.Activity
import android.view.WindowManager
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.intOrNull

class AppBridgeHandler(
    private val context: Context,
    private val closeAction: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var screenTimeoutRunnable: Runnable? = null

    /**
     * Resolves the underlying Activity by unwrapping ContextWrapper classes.
     */
    private fun getActivityContext(): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }
    /**
     * Shows a native Toast message on the Android UI thread.
     *
     * Payload Parameters:
     * - `message` (String): The text message to display in the toast.
     * - `duration` (String, Optional): Duration of the toast, either "short" (default) or "long".
     */
    fun showNativeToast(request: BridgeRequest): BridgeResponse {
        val params = request.payload.jsonObject
        val message = params["message"]?.jsonPrimitive?.content ?: ""
        val durationStr = params["duration"]?.jsonPrimitive?.content ?: "short"

        val duration = if (durationStr == "long") Toast.LENGTH_LONG else Toast.LENGTH_SHORT

        // Must run on UI thread
        mainHandler.post {
            Toast.makeText(context, message, duration).show()
        }

        return BridgeResponse(request.requestId, "success")
    }

    /**
     * Resolves and returns the system Vibrator service based on the running SDK level.
     */
    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    }

    /**
     * Triggers a simple one-shot haptic feedback (100ms vibration at default amplitude).
     */
    fun vibrate(request: BridgeRequest): BridgeResponse {
        val vibrator = getVibrator()
        if (vibrator.hasVibrator()) {
            val effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        }

        return BridgeResponse(request.requestId, "success")
    }

    /**
     * Builds and immediately displays a local native push notification on the device.
     *
     * Payload Parameters:
     * - `title` (String): The title text of the notification.
     * - `message` (String): The body/message text of the notification.
     */
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

    /**
     * Closes the active WebApp activity and returns the user to the native application.
     */
    fun closeWebApp(request: BridgeRequest): BridgeResponse {
        mainHandler.post {
            closeAction()
        }
        return BridgeResponse(request.requestId, "success")
    }

    /**
     * Opens the native Android System Settings screen.
     */
    fun openNativeSettings(request: BridgeRequest): BridgeResponse {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        return BridgeResponse(request.requestId, "success")
    }

    /**
     * Controls the screen keep-awake flag (`FLAG_KEEP_SCREEN_ON`) on the active WebApp activity.
     * Optionally schedules an automatic timeout to clear the flag after `timeoutMs` expires.
     *
     * Payload Parameters:
     * - `enabled` (Boolean): True to keep the screen on, False to allow it to turn off.
     * - `timeoutMs` (Long, Optional): Duration in milliseconds to keep the screen on before reverting.
     *
     * Example JS Request payload:
     * ```json
     * {
     *   "enabled": true,
     *   "timeoutMs": 10000
     * }
     * ```
     */
    fun setKeepScreenOn(request: BridgeRequest): BridgeResponse {
        val params = request.payload.jsonObject
        val enabled = params["enabled"]?.jsonPrimitive?.booleanOrNull ?: false
        val timeoutMs = params["timeoutMs"]?.jsonPrimitive?.longOrNull

        mainHandler.post {
            val activity = getActivityContext()
            if (activity != null) {
                // Cancel any pending timeout task to prevent conflicts
                screenTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                screenTimeoutRunnable = null

                try {
                    if (enabled) {
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        
                        // If a custom timeout is specified, schedule clearing the flag
                        if (timeoutMs != null && timeoutMs > 0) {
                            val runnable = Runnable {
                                try {
                                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                } catch (e: Exception) {
                                    // Safeguard window lifecycle updates
                                }
                                screenTimeoutRunnable = null
                            }
                            screenTimeoutRunnable = runnable
                            mainHandler.postDelayed(runnable, timeoutMs)
                        }
                    } else {
                        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                } catch (e: Exception) {
                    // Safeguard window lifecycle updates
                }
            }
        }
        return BridgeResponse(request.requestId, "success")
    }

    /**
     * Triggers advanced vibration/haptics using a custom timing pattern and optional amplitudes.
     *
     * Payload Parameters:
     * - `pattern` (JsonArray of Long): Timings array representing vibration and pause durations.
     * - `amplitudes` (JsonArray of Int, Optional): Amplitude strength values (0-255) for each timing step.
     *
     * Example JS Request payload:
     * ```json
     * {
     *   "pattern": [500, 1000, 500],
     *   "amplitudes": [128, 0, 255]
     * }
     * ```
     */
    fun vibratePattern(request: BridgeRequest): BridgeResponse {
        val params = request.payload.jsonObject
        val patternArray = params["pattern"]?.jsonArray
        val amplitudesArray = params["amplitudes"]?.jsonArray
        
        if (patternArray != null && patternArray.isNotEmpty()) {
            try {
                // Ensure timings are non-negative to avoid native IllegalArgumentExceptions
                val timings = LongArray(patternArray.size) { i ->
                    val t = patternArray[i].jsonPrimitive.longOrNull ?: 0L
                    if (t < 0) 0L else t
                }
                val vibrator = getVibrator()
                if (vibrator.hasVibrator()) {
                    // If custom amplitudes are provided and match the pattern length, use them
                    val effect = if (amplitudesArray != null && amplitudesArray.size == timings.size) {
                        val amplitudes = IntArray(amplitudesArray.size) { i ->
                            val amp = amplitudesArray[i].jsonPrimitive.intOrNull ?: 0
                            // Android amplitudes must be in 0..255, or -1 for default
                            if (amp == -1) -1 else amp.coerceIn(0, 255)
                        }
                        VibrationEffect.createWaveform(timings, amplitudes, -1)
                    } else {
                        VibrationEffect.createWaveform(timings, -1)
                    }
                    vibrator.vibrate(effect)
                }
            } catch (e: Exception) {
                // Safeguard against runtime platform/hardware constraints or parameter checks
            }
        }
        return BridgeResponse(request.requestId, "success")
    }
}
