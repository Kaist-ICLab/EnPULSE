package kaist.iclab.mobiletracker.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.utils.AppToast
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import kaist.iclab.mobiletracker.ui.screens.DataScreen.DataScreen
import kaist.iclab.mobiletracker.ui.screens.HomeScreen.HomeScreen
import kaist.iclab.mobiletracker.ui.screens.LoginScreen.LoginScreen
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.AboutSettings.AboutSettingsScreen
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.AccountSettings.AccountSettingsScreen
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.AccountSettings.CampaignSettings.CampaignSettingsScreen
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.DataSyncSettings.AutomaticSyncSettings.AutomaticSyncSettingsScreen
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.LanguageSettings.LanguageScreen
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.PermissionSettings.PermissionSettingsScreen
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.PhoneSensorConfigSettings.PhoneSensorConfigSettingsScreen
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.DataSyncSettings.ServerSyncSettingsScreen
import kaist.iclab.mobiletracker.ui.screens.SettingsScreen.SettingsScreen
import kaist.iclab.mobiletracker.ui.screens.SensorDetailScreen.SensorDetailScreen
import kaist.iclab.mobiletracker.viewmodels.auth.AuthViewModel
import kaist.iclab.mobiletracker.viewmodels.data.SensorDetailViewModel
import kaist.iclab.tracker.permission.AndroidPermissionManager

/**
 * Navigation graph for the app.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    startDestination: String = Screen.Login.route,
    permissionManager: AndroidPermissionManager
) {
    val userState by authViewModel.userState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Track previous login state to show toast on successful login
    var previousLoginState by remember { mutableStateOf(userState.isLoggedIn) }
    var previousErrorMessage by remember { mutableStateOf<String?>(userState.message) }
    
    // Show toast when user successfully logs in
    LaunchedEffect(userState.isLoggedIn) {
        if (userState.isLoggedIn && !previousLoginState) {
            AppToast.show(context, R.string.toast_login_success)
        }
        previousLoginState = userState.isLoggedIn
    }
    
    // Show toast when authentication error occurs
    LaunchedEffect(userState.message) {
        val currentMessage = userState.message
        // Only show toast if there's a new error message and user is not logged in
        if (currentMessage != null && 
            currentMessage != previousErrorMessage && 
            !userState.isLoggedIn) {
            AppToast.show(context, currentMessage, AppToast.Duration.LONG)
        }
        previousErrorMessage = currentMessage
    }
    
    // Get system animation duration (respects user's animation speed settings)
    // Fallback to 300ms if system value is unavailable or invalid
    val animationDuration = try {
        val systemDuration = context.resources.getInteger(android.R.integer.config_mediumAnimTime)
        if (systemDuration > 0) systemDuration else 400
    } catch (e: Exception) {
        400 // Fallback to 300ms if system resource is unavailable
    }

    // Handle language change by recreating activity
    val onLanguageChanged: () -> Unit = {
        if (activity != null) {
            activity.recreate()
        }
    }

    // Navigate based on authentication state
    LaunchedEffect(userState.isLoggedIn) {
        val mainTabs = listOf(Screen.Home.route, Screen.Data.route, Screen.Setting.route)
        val currentRoute = navController.currentDestination?.route
        
        if (userState.isLoggedIn) {
            // Navigate to Home screen (main tab) when user logs in
            // Only navigate if we are currently on the Login screen, preventing
            // forced navigation when restoring state on sub-screens (e.g. Language change)
            if (currentRoute == Screen.Login.route) {
                navController.navigate(Screen.Home.route) {
                    // Clear back stack to prevent going back to login
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        } else {
            // Navigate to Login when user logs out
            if (currentRoute !in mainTabs && currentRoute != Screen.Login.route) {
                navController.navigate(Screen.Login.route) {
                    // Clear back stack to prevent going back to main tabs
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }


    NavHost(
        navController = navController,
        startDestination = startDestination,
        // Use system default slide transitions with system animation duration
        // Respects user's animation speed settings (Developer Options > Animation duration scale)
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(animationDuration)
            ) + fadeIn(animationSpec = tween(animationDuration))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(animationDuration)
            ) + fadeOut(animationSpec = tween(animationDuration))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(animationDuration)
            ) + fadeIn(animationSpec = tween(animationDuration))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(animationDuration)
            ) + fadeOut(animationSpec = tween(animationDuration))
        }
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onSignInWithGoogle = {
                    if (activity != null) {
                        authViewModel.login(activity)
                    }
                },
                onLanguageChanged = onLanguageChanged
            )
        }

        // Main tabs - each has BackHandler to close app when back is pressed
        composable(route = Screen.Home.route) {
            BackHandler {
                activity?.finish()
            }
            HomeScreen()
        }

        composable(route = Screen.Data.route) {
            BackHandler {
                activity?.finish()
            }
            DataScreen(navController = navController)
        }


        composable(route = Screen.Setting.route) {
            BackHandler {
                activity?.finish()
            }
            SettingsScreen(navController = navController)
        }

        // Settings sub-screens
        composable(route = Screen.PhoneSensor.route) {
            PhoneSensorConfigSettingsScreen(navController = navController)
        }

        composable(route = Screen.Language.route) {
            LanguageScreen(
                navController = navController,
                onLanguageChanged = onLanguageChanged
            )
        }

        composable(route = Screen.Account.route) {
            AccountSettingsScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(route = Screen.Campaign.route) {
            CampaignSettingsScreen(navController = navController)
        }

        composable(route = Screen.Permission.route) {
            PermissionSettingsScreen(
                navController = navController,
                permissionManager = permissionManager
            )
        }

        composable(route = Screen.ServerSync.route) {
            ServerSyncSettingsScreen(navController = navController)
        }


        composable(route = Screen.AutomaticSync.route) {
            AutomaticSyncSettingsScreen(navController = navController)
        }

        composable(route = Screen.About.route) {
            AboutSettingsScreen(navController = navController)
        }

        composable(
            route = Screen.SensorDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("sensorId") { 
                    type = androidx.navigation.NavType.StringType 
                }
            )
        ) { backStackEntry ->
            val sensorId = backStackEntry.arguments?.getString("sensorId") ?: ""
            val viewModel: SensorDetailViewModel = org.koin.androidx.compose.koinViewModel(
                parameters = { org.koin.core.parameter.parametersOf(sensorId) }
            )
            SensorDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

