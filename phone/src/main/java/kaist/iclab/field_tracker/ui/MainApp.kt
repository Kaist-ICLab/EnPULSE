package kaist.iclab.field_tracker.ui

import android.icu.text.NumberFormat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import kaist.iclab.field_tracker.convertUnixToFormatted
import kaist.iclab.field_tracker.toSensorUIModel
import kaist.iclab.field_tracker.ui.screens.DataConfigScreen
import kaist.iclab.field_tracker.ui.screens.LoginScreen
import kaist.iclab.field_tracker.ui.screens.PermissionListScreen
import kaist.iclab.field_tracker.ui.screens.SettingScreen
import kaist.iclab.field_tracker.ui.screens.UserProfileScreen
import kaist.iclab.field_tracker.viewmodel.MainViewModel
import java.util.Locale

enum class AppScreens(name: String) {
    Login("Login"),
    Setting("Setting"),
    PermissionList("PermissionList"),
    UserProfile("UserProfile"),
    DataConfig("DataConfig"),
}

@Composable
fun MainApp(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    // Set the initial screen
    //  TODO:  val initialScreen = AppScreens.Login.name
    val initialScreen = AppScreens.Setting.name

    val userState = viewModel.userStateFlow.collectAsState()
    val permissionState = viewModel.permissionStateFlow.collectAsState()

    NavHost(
        navController = navController,
        startDestination = initialScreen,
        modifier = Modifier
            .fillMaxSize()
    ) {
        composable(route = AppScreens.Login.name) {
            LoginScreen(
                onLogin = { viewModel.login() },
                onTestWithoutLogin = {
                    navController.navigate(AppScreens.Setting.name) {
                        popUpTo(0) { inclusive = true } // Remove all backstack
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(route = AppScreens.Setting.name) {
            val controllerState = viewModel.controllerStateFlow.collectAsState()
            SettingScreen(
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateBack = { navController.popBackStack() },
                onNavigateToPermissionList = { navController.navigate(AppScreens.PermissionList.name) },
                onNavigateToUserProfile = { navController.navigate(AppScreens.UserProfile.name) },
                onNavigateToDataConfig = { navController.navigate("${AppScreens.DataConfig.name}/${it}") },
                controllerState = controllerState.value,
                onControllerStateChange = { if (it) viewModel.start() else viewModel.stop() },
                sensors = viewModel.sensors.map { it.toSensorUIModel() },
                permissionMap = permissionState.value,
                userState = userState.value,
                deviceInfo = viewModel.getDeviceInfo(),
                appVersion = viewModel.getAppVersion()
            )
        }
        composable(route = AppScreens.PermissionList.name) {
            PermissionListScreen(
                permissionMap = permissionState.value,
                onPermissionRequest = { names -> viewModel.requestPermissions(names) },
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateBack = { navController.popBackStack() }
            )
        }
        composable(route = AppScreens.UserProfile.name) {
            UserProfileScreen(
                user = userState.value.user ?: error("User is null"),
                logout = { viewModel.logout() },
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "${AppScreens.DataConfig.name}/{data}", arguments = listOf(
            navArgument("data") { type = NavType.StringType }
        )) { backStackEntry ->
            val sensorName = backStackEntry.arguments?.getString("data") ?: error("Name is null")
            val sensor = viewModel.sensors.find { it.NAME == sensorName }
                ?: error("No sensor with the name $sensorName found")
            val dataStorage = viewModel.sensorDataStorages.find { it.ID == sensor.ID }
                ?: error("No data storage with the name $sensorName found")
            val stat = dataStorage.statFlow.collectAsState().value
            DataConfigScreen(
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateBack = { navController.popBackStack() },
                sensor = sensor.toSensorUIModel(),
                permissionMap = permissionState.value.filter { it.key in sensor.permissions },
                onPermissionRequest = { names, onResult ->
                    viewModel.requestPermissions(
                        names,
                        onResult
                    )
                },
                recordCount = NumberFormat.getNumberInstance(Locale.US).format(stat.count),
                lastUpdated = convertUnixToFormatted(stat.timestamp),
            )
        }

    }
}




