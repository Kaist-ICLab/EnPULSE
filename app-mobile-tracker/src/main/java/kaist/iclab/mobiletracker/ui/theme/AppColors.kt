package kaist.iclab.mobiletracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * App color palette
 * Centralized color definitions for the app
 */
object AppColors {
    // Background colors
    val White = Color.White
    val Transparent = Color.Transparent
    val Background = Color(0xFFF1F1F3) // Neutral gray background
    
    // Primary color
    val PrimaryColor = Color(0xFF3B82F6) // Primary blue color
    val SecondaryColor = Color(0xFF10B981) // Secondary green color (Emerald 500)
    
    // Navigation bar colors
    val NavigationBarUnselected = Color(0xFF6B7280) // Gray for unselected
    
    // Switch/Toggle colors
    val SwitchOff = Color(0xFF696969) // Gray for toggle off (enabled but unchecked)
    val SwitchDisabled = Color(0xFFD1D5DB) // Light gray for disabled toggle (lighter/more faded)
    
    // Text colors
    val TextPrimary = Color.Black // Black text color
    val TextSecondary = Color(0xFF9CA3AF) // Light gray text color
    
    // Border colors
    val BorderLight = Color(0xFFE0E0E0) // Light gray border
    val BorderDark = Color(0xFFB0B0B0) // Darker gray border
    
    // Google Sign-In button colors
    val GoogleBlue = Color(0xFF4285F4)
    val GoogleRed = Color(0xFFEA4335)
    val GoogleYellow = Color(0xFFFBBC04)
    val GoogleGreen = Color(0xFF34A853)
    
    // Permission status colors
    val ErrorColor = Color(0xFFDC2626) // Red color for error/denied states

    // Settings Icon Colors
    val IconAccount = Color(0xFF2196F3) // Blue
    val IconSync = Color(0xFF4CAF50)    // Green
    val IconLanguage = Color(0xFF9C27B0) // Purple
    val IconSecurity = Color(0xFFF44336) // Red
    val IconPhone = Color(0xFF009688)    // Teal
    val IconInfo = Color(0xFF607D8B)     // Blue Grey

    // Sensor Colors
    val ColorLocation = Color(0xFF4285F4)
    val ColorAppUsage = Color(0xFF9C27B0)
    val ColorActivity = Color(0xFF34A853)
    val ColorBattery = Color(0xFFFBBC04)
    val ColorNotification = Color(0xFFEA4335)
    val ColorScreen = Color(0xFF607D8B)
    val ColorConnectivity = Color(0xFF00ACC1)
    val ColorBluetooth = Color(0xFF3F51B5)
    val ColorAmbientLight = Color(0xFFFF9800)
    val ColorAppListChange = Color(0xFFE91E63)
    val ColorCallLog = Color(0xFF8BC34A)
    val ColorDataTraffic = Color(0xFF009688)
    val ColorDeviceMode = Color(0xFF795548)
    val ColorMedia = Color(0xFFFF5722)
    val ColorMessage = Color(0xFFCDDC39)
    val ColorUserInteraction = Color(0xFF673AB7)
    val ColorWifi = Color(0xFF00BCD4)
    val ColorDefault = Color(0xFF9E9E9E)

    fun getSensorColor(sensorId: String): Color {
        // Normalize by removing spaces to handle both "AmbientLight" and "Ambient Light"
        val normalizedId = sensorId.replace(" ", "")
        
        return when (normalizedId) {
            // Phone Sensors
            "AmbientLight" -> ColorAmbientLight
            "AppListChange" -> ColorAppListChange
            "AppUsage", "AppUsageLog" -> ColorAppUsage
            "Battery" -> ColorBattery
            "BluetoothScan", "Bluetooth" -> ColorBluetooth
            "CallLog" -> ColorCallLog
            "Connectivity" -> ColorConnectivity
            "DataTraffic" -> ColorDataTraffic
            "DeviceMode" -> ColorDeviceMode
            "Location" -> ColorLocation
            "Media" -> ColorMedia
            "MessageLog", "Message" -> ColorMessage
            "Notification" -> ColorNotification
            "Screen" -> ColorScreen
            "Step" -> ColorActivity
            "UserInteraction" -> ColorUserInteraction
            "WifiScan", "Wifi", "WiFi" -> ColorWifi

            // Watch sensors
            "WatchAccelerometer" -> ColorBluetooth
            "WatchEDA" -> ColorWifi
            "WatchHeartRate" -> ColorAppListChange
            "WatchPPG" -> ColorMedia
            "WatchSkinTemperature" -> ColorAmbientLight
            else -> ColorDefault
        }
    }

    fun getPermissionColor(permissionName: String): Color {
        return when (permissionName) {
            "Post Notifications", "Notification Listener" -> ColorNotification
            "Access Location", "Background Location", "Location" -> ColorLocation
            "Read Contacts", "Contacts" -> ColorCallLog
            "Camera", "Media Images", "Media Video", "Media Audio", "Storage", "Microphone" -> ColorMedia
            "Read Calendar", "Calendar" -> ColorAppUsage
            "Activity Recognition", "Read Steps (Samsung Health)", "Body Sensors" -> ColorActivity
            "Accessibility Service" -> ColorUserInteraction
            "Usage Stats" -> ColorAppUsage
            else -> ColorDefault
        }
    }
}


