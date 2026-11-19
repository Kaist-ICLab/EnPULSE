package kaist.iclab.wearabletracker.health

import android.content.Context
import android.content.Intent
import kaist.iclab.tracker.sensor.core.Sensor
import kotlinx.coroutines.flow.StateFlow

/**
 * The parts of the watch app that depend on the Samsung Health Sensor SDK. The samsung flavor
 * implements it with the SDK; the open flavor, built without the SDK, has no health sensors.
 */
interface HealthSensorPlatform {
    /** Whether the health platform is connected. Collection can't start while this is false. */
    val isConnected: StateFlow<Boolean>

    /** True when the health platform rejected this app (developer mode is off). */
    val sdkPolicyError: StateFlow<Boolean>

    fun clearSdkPolicyError()

    /**
     * The on-demand ECG sensor, or null when this build has none. It is not managed by
     * BackgroundController, so the caller must init() it.
     */
    val ecgSensor: Sensor<*, *>?

    /** Opens the ECG measurement screen, or null when this build has no ECG. */
    fun ecgMeasurementIntent(context: Context): Intent?

    /** Starts the trigger condition sources that read health sensors (stress detection). */
    fun startTriggerSources()
}

/** Koin qualifier for the [kaist.iclab.wearabletracker.WatchSensorDescriptor]s of health sensors. */
const val HEALTH_SENSOR_DESCRIPTORS = "healthSensorDescriptors"

/**
 * Koin qualifier for stores of health sensors outside BackgroundController (ECG), keyed by sensor
 * ID. They still take part in phone sync, record counts and flushing.
 */
const val HEALTH_EXTRA_STORES = "healthExtraStores"
