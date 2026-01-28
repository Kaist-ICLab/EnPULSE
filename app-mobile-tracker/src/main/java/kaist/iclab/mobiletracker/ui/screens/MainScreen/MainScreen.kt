package kaist.iclab.mobiletracker.ui.screens.MainScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kaist.iclab.mobiletracker.navigation.NavGraph
import kaist.iclab.mobiletracker.navigation.Screen
import kaist.iclab.mobiletracker.ui.BottomNavigationBar
import kaist.iclab.mobiletracker.ui.components.LoadingOverlay.LoadingOverlay
import kaist.iclab.mobiletracker.ui.theme.AppColors
import kaist.iclab.mobiletracker.utils.SupabaseLoadingInterceptor
import kaist.iclab.mobiletracker.viewmodels.auth.AuthViewModel
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main screen with bottom navigation bar.
 * Contains the main content area and bottom navigation.
 */
@Composable
fun MainScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    startDestination: String,
    permissionManager: AndroidPermissionManager
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Loading state for Supabase operations
    var isLoading by remember { mutableStateOf(false) }

    // Set up loading interceptor callback
    DisposableEffect(Unit) {
        val callback: (Boolean) -> Unit = { loading ->
            CoroutineScope(Dispatchers.Main).launch {
                isLoading = loading
            }
        }
        SupabaseLoadingInterceptor.onLoadingStateChanged = callback

        onDispose {
            SupabaseLoadingInterceptor.onLoadingStateChanged = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = AppColors.Background,
            bottomBar = {
                // Show bottom navigation for all routes except Login and Onboarding
                if (currentRoute != Screen.Login.route && currentRoute != Screen.Onboarding.route) {
                    BottomNavigationBar(navController = navController)
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                NavGraph(
                    navController = navController,
                    authViewModel = authViewModel,
                    startDestination = startDestination,
                    permissionManager = permissionManager
                )
            }
        }

        // Loading overlay - placed after Scaffold so it has higher z-index (appears on top of navbar)
        // Shows automatically when Supabase operations are active
        LoadingOverlay(
            isLoading = isLoading,
            showOverlay = true,
            blockNavigation = true
        )
    }
}

