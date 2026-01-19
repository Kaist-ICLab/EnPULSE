package kaist.iclab.tracker.permission

import android.Manifest
import android.os.Build
import com.samsung.android.sdk.health.data.request.DataTypes

/**
 * Data class for managing app permissions.
 *
 * @property name The user-friendly name of the permission (e.g., "Camera Permission", "Location Permission").
 * @property ids  A list of permission IDs to request (e.g., [Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO]).
 *                Some permissions must be requested together (e.g., [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]).
 */
data class Permission(
    val name: String,
    val ids: Array<String> /*Some permission required to requested together*/
) {
    companion object {
        val supportedPermissions: Array<Permission> = listOfNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Permission(
                name = "Post Notifications",
                ids = arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            ) else null,
            Permission(
                name = "Access Location",
                ids = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            ),
            Permission(
                name = "Background Location",
                ids = arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ),
            Permission(
                name = "Read Contacts",
                ids = arrayOf(Manifest.permission.READ_CONTACTS)
            ),
            Permission(
                name = "Camera",
                ids = arrayOf(Manifest.permission.CAMERA)
            ),
            Permission(
                name = "Microphone",
                ids = arrayOf(Manifest.permission.RECORD_AUDIO)
            ),
            // Android 13+ uses granular media permissions, Android 12 and below uses READ_EXTERNAL_STORAGE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Permission(
                    name = "Media Images",
                    ids = arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                )
            } else {
                Permission(
                    name = "Storage",
                    ids = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                )
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Permission(
                name = "Media Video",
                ids = arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            ) else null,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Permission(
                name = "Media Audio",
                ids = arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
            ) else null,
            Permission(
                name = "Read Calendar",
                ids = arrayOf(Manifest.permission.READ_CALENDAR)
            ),
            Permission(
                name = "Activity Recognition",
                ids = arrayOf(Manifest.permission.ACTIVITY_RECOGNITION)
            ),
            Permission(
                name = "Body Sensors",
                ids = listOfNotNull(
                    Manifest.permission.BODY_SENSORS,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.BODY_SENSORS_BACKGROUND else null
                ).toTypedArray()
            ),
            Permission(
                name = "Accessibility Service",
                ids = arrayOf(Manifest.permission.BIND_ACCESSIBILITY_SERVICE)
            ),
            Permission(
                name = "Notification Listener",
                ids = arrayOf(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)
            ),
            Permission(
                name = "Usage Stats",
                ids = arrayOf(Manifest.permission.PACKAGE_USAGE_STATS)
            ),
            Permission(
                name = "Read Steps (Samsung Health)",
                ids = arrayOf(DataTypes.STEPS.name)
            )
        ).toTypedArray()
    }

    /**
     * Compares two Permission objects for equality.
     * Two Permission objects are considered equal if they have the same name and ids array content.
     *
     * @param other The object to compare with this Permission
     * @return true if the objects are equal, false otherwise
     *
     * Note: Uses contentEquals() for the ids array to compare array contents rather than array references.
     */
    override fun equals(other: Any?): Boolean {
        // Same reference - objects are identical
        if (this === other) return true
        // Different class types - not equal
        if (javaClass != other?.javaClass) return false

        // Safe cast after type check
        other as Permission

        // Compare all fields: name and ids array content
        if (name != other.name) return false
        if (!ids.contentEquals(other.ids)) return false

        return true
    }

    /**
     * Generates a hash code for this Permission object.
     * Used by hash-based collections (HashSet, HashMap) for efficient storage and lookup.
     *
     * @return A hash code value for this object
     *
     * Note:
     * - Must be consistent with equals(): if two objects are equal, they must have the same hashCode
     * - Uses contentHashCode() for the ids array to hash based on array contents
     * - Uses prime number 31 for combining hash codes (common practice in Java/Kotlin)
     */
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + ids.contentHashCode()
        return result
    }
}

/**
 * Gets the aggregated permission state for a Permission object with multiple IDs.
 * Returns GRANTED only if all IDs are GRANTED.
 * Otherwise, returns the "worst" state (PERMANENTLY_DENIED > RATIONALE_REQUIRED > NOT_REQUESTED > UNSUPPORTED)
 *
 * @param permissionStateMap A map of permission IDs to their current PermissionState
 * @return The aggregated PermissionState for this Permission
 */
fun Permission.getPermissionState(permissionStateMap: Map<String, PermissionState>): PermissionState {
    val states = ids.map { id ->
        permissionStateMap[id] ?: PermissionState.NOT_REQUESTED
    }

    // If all are GRANTED, return GRANTED
    if (states.all { it == PermissionState.GRANTED }) {
        return PermissionState.GRANTED
    }

    // If any is UNSUPPORTED, return UNSUPPORTED
    if (states.any { it == PermissionState.UNSUPPORTED }) {
        return PermissionState.UNSUPPORTED
    }

    // If any is PERMANENTLY_DENIED, return PERMANENTLY_DENIED
    if (states.any { it == PermissionState.PERMANENTLY_DENIED }) {
        return PermissionState.PERMANENTLY_DENIED
    }

    // If any requires rationale, return RATIONALE_REQUIRED
    if (states.any { it == PermissionState.RATIONALE_REQUIRED }) {
        return PermissionState.RATIONALE_REQUIRED
    }

    // Otherwise, return NOT_REQUESTED
    return PermissionState.NOT_REQUESTED
}

