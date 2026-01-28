package kaist.iclab.mobiletracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import kaist.iclab.mobiletracker.helpers.BLEHelper
import kaist.iclab.mobiletracker.helpers.LanguageHelper
import kaist.iclab.mobiletracker.navigation.Screen
import kaist.iclab.mobiletracker.ui.screens.MainScreen.MainScreen
import kaist.iclab.mobiletracker.viewmodels.auth.AuthViewModel
import kaist.iclab.tracker.permission.AndroidPermissionManager
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    private val bleHelper by inject<BLEHelper>()
    private val permissionManager by inject<AndroidPermissionManager>()

    override fun attachBaseContext(newBase: Context) {
        val context = LanguageHelper(newBase).attachBaseContextWithLanguage(newBase)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind permission manager to activity
        permissionManager.bind(this)

        // Initialize BLEHelper - dependencies are injected by Koin
        bleHelper.initialize()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    AppContent()
                }
            }
        }
    }

    @Composable
    private fun AppContent() {
        val serverClientId = remember { getString(R.string.default_web_client_id) }
        val authViewModel: AuthViewModel = koinViewModel(
            parameters = { parametersOf(this@MainActivity, serverClientId) }
        )

        val userState by authViewModel.userState.collectAsState()
        val userProfile by authViewModel.userProfile.collectAsState()

        // Determine start destination based on full profile state
        // Note: If logged in but profile not loaded yet, start at Login and NavGraph will redirect
        val startDestination = when {
            !userState.isLoggedIn -> Screen.Login.route
            userProfile == null -> Screen.Login.route  // Profile loading, start at Login
            userProfile?.campaign_id == null -> Screen.Onboarding.route  // No campaign -> onboarding
            else -> Screen.Home.route  // Has campaign -> home
        }

        val navController = rememberNavController()

        MainScreen(
            navController = navController,
            authViewModel = authViewModel,
            startDestination = startDestination,
            permissionManager = permissionManager
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        bleHelper.cleanup()
    }
}
