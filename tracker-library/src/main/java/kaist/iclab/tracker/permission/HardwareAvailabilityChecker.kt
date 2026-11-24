package kaist.iclab.tracker.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

/**
 * Utility class for checking hardware availability for permissions that require specific hardware.
 *
 * This class provides methods to check if the device has the necessary hardware components
 * (camera, microphone, body sensors, location) before requesting permissions, and whether
 * Samsung Health is installed.
 */
object HardwareAvailabilityChecker {

    /**
     * Checks if the required hardware is available for a given permission.
     *
     * @param context The application context
     * @param permission The permission ID to check hardware for
     * @return true if hardware is available, false otherwise
     */
    fun isHardwareAvailable(context: Context, permission: String): Boolean {
        return when (permission) {
            Manifest.permission.BODY_SENSORS -> isBodySensorHardwareAvailable(context)
            Manifest.permission.CAMERA -> isCameraHardwareAvailable(context)
            Manifest.permission.RECORD_AUDIO -> isMicrophoneHardwareAvailable(context)
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> isLocationHardwareAvailable(context)

            else -> true // Default to true for permissions that don't require hardware checks
        }
    }

    /** Package of the Samsung Health app, which the Samsung Health Data SDK reads from. */
    const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth"

    /**
     * Whether Samsung Health is installed. The Data SDK works on any Android 10+ phone with
     * Samsung Health installed, not only Samsung phones; a too-old version is reported by the SDK.
     */
    fun isSamsungHealthInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(SAMSUNG_HEALTH_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /**
     * Checks if body sensor hardware is available on the device.
     * BODY_SENSORS permission uses Android's body sensor API (like Heart Rate).
     * Many modern phones (e.g. newer Samsung Galaxy devices) do not have this built-in.
     *
     * @param context The application context
     * @return true if body sensor hardware is available, false otherwise
     */
    private fun isBodySensorHardwareAvailable(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_HEART_RATE)
    }

    /**
     * Checks if camera hardware is available on the device.
     *
     * @param context The application context
     * @return true if camera hardware is available, false otherwise
     */
    private fun isCameraHardwareAvailable(context: Context): Boolean {
        val packageManager = context.packageManager
        // Check for any camera (front or back)
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
                packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    /**
     * Checks if microphone hardware is available on the device.
     *
     * @param context The application context
     * @return true if microphone hardware is available, false otherwise
     */
    private fun isMicrophoneHardwareAvailable(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
    }

    /**
     * Checks if location hardware (GPS) is available on the device.
     * Note: Most devices have location services, but some tablets or emulators might not have GPS.
     *
     * @param context The application context
     * @return true if location hardware is available, false otherwise
     */
    private fun isLocationHardwareAvailable(context: Context): Boolean {
        val packageManager = context.packageManager
        // Check for GPS hardware
        // Note: Network-based location is usually always available, but GPS might not be
        // We return true if either GPS or network location is available
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION) ||
                packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
    }
}

