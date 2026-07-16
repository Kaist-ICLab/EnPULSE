package kaist.iclab.wearabletracker.ecg

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import kaist.iclab.wearabletracker.theme.WearableTrackerTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Activity for a single on-demand ECG measurement. Launched from a button on SettingsScreen
 * after the elbow-placement instruction dialog is confirmed.
 */
class EcgMeasurementActivity : ComponentActivity() {
    private val ecgViewModel: EcgViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                ecgViewModel.cancelMeasurement()
                finish()
            }
        })

        setContent {
            WearableTrackerTheme {
                EcgMeasurementScreen(
                    viewModel = ecgViewModel,
                    onFinish = { finish() }
                )
            }
        }
    }
}
