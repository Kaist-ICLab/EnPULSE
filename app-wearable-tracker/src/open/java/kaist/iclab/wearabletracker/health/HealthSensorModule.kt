package kaist.iclab.wearabletracker.health

import android.content.Context
import android.content.Intent
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.wearabletracker.WatchSensorDescriptor
import kaist.iclab.wearabletracker.db.obx.WatchSensorStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * The open flavor is built without the Samsung Health Sensor SDK, so it has no health sensors,
 * no ECG and nothing to connect to.
 */
private class OpenHealthSensorPlatform : HealthSensorPlatform {
    override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)
    override val sdkPolicyError: StateFlow<Boolean> = MutableStateFlow(false)
    override fun clearSdkPolicyError() = Unit
    override val ecgSensor: Sensor<*, *>? = null
    override fun ecgMeasurementIntent(context: Context): Intent? = null
    override fun startTriggerSources() = Unit
}

val healthSensorModule = module {
    single<HealthSensorPlatform> { OpenHealthSensorPlatform() }

    single<List<WatchSensorDescriptor>>(named(HEALTH_SENSOR_DESCRIPTORS)) { emptyList() }

    single<Map<String, WatchSensorStore<*>>>(named(HEALTH_EXTRA_STORES)) { emptyMap() }
}
