package kaist.iclab.tracker.permission

import android.content.Intent
import androidx.activity.ComponentActivity

/**
 * Permission IDs for reading Samsung Health data (steps, sleep, exercise).
 *
 * Each value equals the Samsung Health Data SDK's `DataType.name`, so permission states saved by
 * earlier versions keep their keys. The IDs are plain strings so that this library builds without
 * the SDK; the SDK side lives in the tracker-samsung-data module.
 */
object HealthDataPermissions {
    const val STEPS = "steps"
    const val SLEEP = "sleep"
    const val EXERCISE = "exercise"

    val ALL: Set<String> = setOf(STEPS, SLEEP, EXERCISE)
}

/**
 * Checks and requests [HealthDataPermissions] for [AndroidPermissionManager].
 *
 * Without one, the manager reports these permissions as [PermissionState.UNSUPPORTED]. Results
 * map each permission ID to [PermissionState.GRANTED] or [PermissionState.NOT_REQUESTED].
 */
interface HealthDataPermissionHandler {
    /**
     * Looks up which of [permissions] are granted. Calls [onUnsupported] instead when the health
     * platform can't be used on this device. Other failures leave the states unchanged.
     */
    fun queryGranted(
        permissions: Set<String>,
        onResult: (Map<String, PermissionState>) -> Unit,
        onUnsupported: () -> Unit
    )

    /** Asks the user for [permissions], then reports their states through [onResult]. */
    fun request(
        activity: ComponentActivity,
        permissions: Set<String>,
        onResult: (Map<String, PermissionState>) -> Unit
    )

    /** Opens the app where the user manages these permissions, or null if it isn't installed. */
    fun settingsIntent(): Intent?
}
