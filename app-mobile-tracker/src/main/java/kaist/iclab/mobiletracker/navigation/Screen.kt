package kaist.iclab.mobiletracker.navigation

/**
 * Sealed class representing all screens in the app.
 * Used for type-safe navigation.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    
    // Main tabs
    object Home : Screen("home")
    object Data : Screen("data")
    object Setting : Screen("setting")
    
    // Settings sub-screens
    object Account : Screen("account")
    object Campaign : Screen("campaign")
    object Devices : Screen("devices")
    object Language : Screen("language")
    object Permission : Screen("permission")
    object PhoneSensor : Screen("phone_sensor")
    object ServerSync : Screen("server_sync")
    object AutomaticSync : Screen("automatic_sync")
    object About : Screen("about")
    
    // Data screen sub-screens
    object SensorDetail : Screen("sensor_detail/{sensorId}") {
        fun createRoute(sensorId: String) = "sensor_detail/$sensorId"
    }
}

