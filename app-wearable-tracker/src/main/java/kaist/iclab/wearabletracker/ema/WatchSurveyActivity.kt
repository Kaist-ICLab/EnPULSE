package kaist.iclab.wearabletracker.ema

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kaist.iclab.wearabletracker.theme.WearableTrackerTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Activity launched when a microEMA survey needs to be displayed on the watch.
 * Triggered by a button press on the SettingsScreen.
 *
 * Handles the full lifecycle: load config → show question → persist response → finish.
 * The ViewModel handles expiry, dismissal, and compliance tracking.
 */
class WatchSurveyActivity : ComponentActivity() {
    private val microEmaViewModel: MicroEmaViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure the screen wakes up and shows the activity even if locked
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        setContent {
            WearableTrackerTheme {
                MicroEmaScreen(
                    viewModel = microEmaViewModel,
                    onFinish = { finish() }
                )
            }
        }
    }

    /**
     * Handle back press / swipe-to-dismiss as a DISMISSED event.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        microEmaViewModel.dismiss()
        // onFinish callback in MicroEmaScreen will call finish() after the brief delay
    }
}
