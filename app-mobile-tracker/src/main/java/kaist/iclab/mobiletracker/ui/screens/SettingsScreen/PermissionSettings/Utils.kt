package kaist.iclab.mobiletracker.ui.screens.SettingsScreen.PermissionSettings

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.ui.graphics.vector.ImageVector
import com.samsung.android.sdk.health.data.request.DataTypes
import kaist.iclab.mobiletracker.R
import kaist.iclab.tracker.permission.PermissionState

/**
 * Data class representing permission icon mapping
 */
private data class PermissionConfig(
    val permissionId: String,
    val icon: ImageVector
)

/**
 * Permission icon mappings - maps permission IDs to their icons
 */
private val permissionConfigs = buildList {
    // Add conditional permissions
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(PermissionConfig(
            permissionId = Manifest.permission.POST_NOTIFICATIONS,
            icon = Icons.Filled.Notifications
        ))
    }
    add(PermissionConfig(
        permissionId = Manifest.permission.ACCESS_FINE_LOCATION,
        icon = Icons.Filled.Place
    ))
    add(PermissionConfig(
        permissionId = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        icon = Icons.Filled.Place
    ))
    add(PermissionConfig(
        permissionId = Manifest.permission.ACTIVITY_RECOGNITION,
        icon = Icons.AutoMirrored.Filled.DirectionsWalk
    ))
    add(PermissionConfig(
        permissionId = Manifest.permission.READ_CONTACTS,
        icon = Icons.Filled.Contacts
    ))
    add(PermissionConfig(
        permissionId = Manifest.permission.CAMERA,
        icon = Icons.Filled.Camera
    ))
    add(PermissionConfig(
        permissionId = Manifest.permission.RECORD_AUDIO,
        icon = Icons.Filled.Mic
    ))
    // Android 13+ uses granular media permissions, Android 12 and below uses READ_EXTERNAL_STORAGE
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(PermissionConfig(
            permissionId = Manifest.permission.READ_MEDIA_IMAGES,
            icon = Icons.Filled.Storage
        ))
        add(PermissionConfig(
            permissionId = Manifest.permission.READ_MEDIA_VIDEO,
            icon = Icons.Filled.Storage
        ))
        add(PermissionConfig(
            permissionId = Manifest.permission.READ_MEDIA_AUDIO,
            icon = Icons.Filled.Storage
        ))
    } else {
        add(PermissionConfig(
            permissionId = Manifest.permission.READ_EXTERNAL_STORAGE,
            icon = Icons.Filled.Storage
        ))
    }
    add(PermissionConfig(
        permissionId = Manifest.permission.READ_CALENDAR,
        icon = Icons.Filled.CalendarMonth
    ))
    add(PermissionConfig(
        permissionId = Manifest.permission.BODY_SENSORS,
        icon = Icons.Filled.FitnessCenter
    ))
    add(PermissionConfig(
        permissionId = Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
        icon = Icons.Filled.Accessibility
    ))
    add(PermissionConfig(
        permissionId = Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
        icon = Icons.Filled.Notifications
    ))
    add(PermissionConfig(
        permissionId = Manifest.permission.PACKAGE_USAGE_STATS,
        icon = Icons.Filled.GridView
    ))
    // Samsung Health Steps
    add(PermissionConfig(
        permissionId = DataTypes.STEPS.name,
        icon = Icons.AutoMirrored.Filled.DirectionsWalk
    ))
}

/**
 * Gets permission icon by permission ID
 */
fun getPermissionIcon(permissionId: String): ImageVector {
    return permissionConfigs.find { it.permissionId == permissionId }?.icon
        ?: Icons.Filled.Settings
}

/**
 * Gets localized permission status text by permission state
 */
fun getPermissionStatusText(context: Context, permissionState: PermissionState): String {
    return when (permissionState) {
        PermissionState.PERMANENTLY_DENIED -> context.getString(R.string.permission_status_denied)
        PermissionState.UNSUPPORTED -> context.getString(R.string.permission_status_unsupported)
        PermissionState.NOT_REQUESTED -> context.getString(R.string.permission_status_waiting)
        PermissionState.GRANTED -> context.getString(R.string.permission_status_granted)
        PermissionState.RATIONALE_REQUIRED -> context.getString(R.string.permission_status_not_fully_granted)
    }
}

/**
 * Gets localized permission description by permission ID
 */
fun getPermissionDescription(context: Context, permissionId: String): String {
    val stringResId = when (permissionId) {
        Manifest.permission.POST_NOTIFICATIONS -> R.string.permission_desc_notifications
        Manifest.permission.ACCESS_FINE_LOCATION -> R.string.permission_desc_location
        Manifest.permission.ACCESS_BACKGROUND_LOCATION -> R.string.permission_desc_background_location
        Manifest.permission.READ_CONTACTS -> R.string.permission_desc_contacts
        Manifest.permission.CAMERA -> R.string.permission_desc_camera
        Manifest.permission.RECORD_AUDIO -> R.string.permission_desc_microphone
        Manifest.permission.READ_EXTERNAL_STORAGE -> R.string.permission_desc_storage
        Manifest.permission.READ_MEDIA_IMAGES -> R.string.permission_desc_media_images
        Manifest.permission.READ_MEDIA_VIDEO -> R.string.permission_desc_media_video
        Manifest.permission.READ_MEDIA_AUDIO -> R.string.permission_desc_media_audio
        Manifest.permission.READ_CALENDAR -> R.string.permission_desc_calendar
        Manifest.permission.ACTIVITY_RECOGNITION -> R.string.permission_desc_activity_recognition
        Manifest.permission.BODY_SENSORS -> R.string.permission_desc_body_sensors
        Manifest.permission.BIND_ACCESSIBILITY_SERVICE -> R.string.permission_desc_accessibility
        Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE -> R.string.permission_desc_notification_listener
        Manifest.permission.PACKAGE_USAGE_STATS -> R.string.permission_desc_usage_stats
        DataTypes.STEPS.name -> R.string.permission_desc_steps
        else -> null
    }
    
    return stringResId?.let { context.getString(it) } ?: ""
}

/**
 * Gets localized permission name by permission ID
 */
fun getPermissionName(context: Context, permissionId: String): String {
    val stringResId = when (permissionId) {
        Manifest.permission.POST_NOTIFICATIONS -> R.string.permission_name_notifications
        Manifest.permission.ACCESS_FINE_LOCATION -> R.string.permission_name_location
        Manifest.permission.ACCESS_BACKGROUND_LOCATION -> R.string.permission_name_background_location
        Manifest.permission.READ_CONTACTS -> R.string.permission_name_contacts
        Manifest.permission.CAMERA -> R.string.permission_name_camera
        Manifest.permission.RECORD_AUDIO -> R.string.permission_name_microphone
        Manifest.permission.READ_EXTERNAL_STORAGE -> R.string.permission_name_storage
        Manifest.permission.READ_MEDIA_IMAGES -> R.string.permission_name_media_images
        Manifest.permission.READ_MEDIA_VIDEO -> R.string.permission_name_media_video
        Manifest.permission.READ_MEDIA_AUDIO -> R.string.permission_name_media_audio
        Manifest.permission.READ_CALENDAR -> R.string.permission_name_calendar
        Manifest.permission.ACTIVITY_RECOGNITION -> R.string.permission_name_activity_recognition
        Manifest.permission.BODY_SENSORS -> R.string.permission_name_body_sensors
        Manifest.permission.BIND_ACCESSIBILITY_SERVICE -> R.string.permission_name_accessibility
        Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE -> R.string.permission_name_notification_listener
        Manifest.permission.PACKAGE_USAGE_STATS -> R.string.permission_name_usage_stats
        DataTypes.STEPS.name -> R.string.permission_name_steps
        else -> null
    }
    
    return stringResId?.let { context.getString(it) } ?: ""
}

