package kaist.iclab.tracker.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.samsung.android.sdk.health.data.request.DataTypes

/**
 * Utility class for checking hardware availability for permissions that require specific hardware.
 *
 * This class provides methods to check if the device has the necessary hardware components
 * (camera, microphone, body sensors, location) or device manufacturer requirements
 * (Samsung for Samsung Health) before requesting permissions.
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

            DataTypes.STEPS.name -> isSamsungDevice() // Samsung Health requires Samsung device
            else -> true // Default to true for permissions that don't require hardware checks
        }
    }

    /**
     * Checks if the device is a Samsung device.
     * Samsung Health DataTypes (like STEPS) are only supported on Samsung devices.
     *
     * @return true if the device is a Samsung device, false otherwise
     */
    fun isSamsungDevice(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }

    /**
     * Checks if body sensor hardware is available on the device.
     * BODY_SENSORS permission uses Android's body sensor API, which is available
     * on devices running Android 4.4W (API 20) and later.
     *
     * @param context The application context
     * @return true if body sensor API is supported, false otherwise
     */
    private fun isBodySensorHardwareAvailable(context: Context): Boolean {
        // BODY_SENSORS permission was introduced in Android 4.4W (API 20)
        // If the device supports this API level, it supports body sensors
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH
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
                packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
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

