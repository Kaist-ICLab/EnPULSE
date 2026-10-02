package kaist.iclab.wearabletracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.wearabletracker.helpers.PermissionHelper
import kaist.iclab.wearabletracker.theme.WearableTrackerTheme
import kaist.iclab.wearabletracker.ui.SettingsScreen
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    companion object {
        /** Set by the post-reboot "tap to resume" notification. */
        const val EXTRA_RESUME_COLLECTION = "resume_collection"
    }

    val permissionManager by inject<AndroidPermissionManager>()
    private val sensorController by inject<BackgroundController>()
    private var resumeCollectionPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        resumeCollectionPending = intent?.getBooleanExtra(EXTRA_RESUME_COLLECTION, false) == true

        permissionManager.bind(this)

        // Check notification permission at app start
        PermissionHelper.checkNotificationPermission(permissionManager)
        setContent {
            WearableTrackerTheme {
                SettingsScreen(androidPermissionManager = permissionManager)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_RESUME_COLLECTION, false)) {
            resumeCollectionPending = true
        }
    }

    // Restart from onResume, once the app is visibly in the foreground: that is what allows the
    // microphone foreground service the boot broadcast was refused.
    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        if (!resumeCollectionPending) return
        resumeCollectionPending = false
        NotificationManagerCompat.from(this).cancel(Constants.NotificationId.RESUME_COLLECTION)
        if (sensorController.controllerStateFlow.value.flag == ControllerState.FLAG.READY) {
            try {
                sensorController.start()
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to resume collection: ${e.message}", e)
            }
        }
    }
}
