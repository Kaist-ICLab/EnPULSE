package kaist.iclab.wearabletracker.health

import android.content.Context
import android.content.Intent
import kaist.iclab.tracker.listener.SamsungHealthSensorInitializer
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.galaxywatch.AccelerometerSensor
import kaist.iclab.tracker.sensor.galaxywatch.ECGSensor
import kaist.iclab.tracker.sensor.galaxywatch.EDASensor
import kaist.iclab.tracker.sensor.galaxywatch.HeartRateSensor
import kaist.iclab.tracker.sensor.galaxywatch.PPGSensor
import kaist.iclab.tracker.sensor.galaxywatch.SkinTemperatureSensor
import kaist.iclab.tracker.sensor.galaxywatch.StressSensor
import kaist.iclab.tracker.trigger.adapter.galaxywatch.StressDetectionAdapter
import kaist.iclab.wearabletracker.WatchSensorDescriptor
import kaist.iclab.wearabletracker.db.obx.SamsungWatchSensorStores
import kaist.iclab.wearabletracker.db.obx.WatchSensorStore
import kaist.iclab.wearabletracker.ecg.EcgMeasurementActivity
import kaist.iclab.wearabletracker.ecg.EcgViewModel
import kaist.iclab.wearabletracker.sensorConfig
import kaist.iclab.wearabletracker.sensorState
import kotlinx.coroutines.flow.StateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

private class SamsungHealthSensorPlatform(
    private val initializer: SamsungHealthSensorInitializer,
    override val ecgSensor: ECGSensor,
    private val stressDetectionAdapter: StressDetectionAdapter
) : HealthSensorPlatform {
    override val isConnected: StateFlow<Boolean> = initializer.connectionStateFlow
    override val sdkPolicyError: StateFlow<Boolean> = initializer.sdkPolicyErrorStateFlow
    override fun clearSdkPolicyError() = initializer.clearSdkPolicyError()
    override fun ecgMeasurementIntent(context: Context): Intent =
        Intent(context, EcgMeasurementActivity::class.java)
    override fun startTriggerSources() = stressDetectionAdapter.start()
}

/** Samsung Health Sensor SDK sensors, their stores, on-demand ECG and stress detection. */
val healthSensorModule = module {
    single {
        SamsungHealthSensorInitializer(context = androidContext())
    }

    single {
        SamsungWatchSensorStores(boxStore = get())
    }

    single {
        AccelerometerSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(AccelerometerSensor::class, AccelerometerSensor.Config()),
            stateStorage = sensorState(AccelerometerSensor::class),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        PPGSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(PPGSensor::class, PPGSensor.Config()),
            stateStorage = sensorState(PPGSensor::class),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        HeartRateSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(HeartRateSensor::class, HeartRateSensor.Config()),
            stateStorage = sensorState(HeartRateSensor::class),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        SkinTemperatureSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(
                SkinTemperatureSensor::class,
                SkinTemperatureSensor.Config()
            ),
            stateStorage = sensorState(SkinTemperatureSensor::class),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        EDASensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(EDASensor::class, EDASensor.Config()),
            stateStorage = sensorState(EDASensor::class),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        StressSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(StressSensor::class, StressSensor.Config()),
            stateStorage = sensorState(StressSensor::class),
            heartRateSensor = get(),
            rmssdHistory = get()
        )
    }

    // ECG is on-demand only (Samsung HealthTrackerType.ECG_ON_DEMAND), triggered manually via
    // EcgViewModel — intentionally NOT added to watchSensorDescriptors so it is excluded from
    // BackgroundController's auto start/stop loop and SensorDataReceiver's auto-listener wiring.
    single {
        ECGSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(ECGSensor::class, ECGSensor.Config()),
            stateStorage = sensorState(ECGSensor::class),
            samsungHealthSensorInitializer = get()
        )
    }

    single<List<WatchSensorDescriptor>>(named(HEALTH_SENSOR_DESCRIPTORS)) {
        val stores = get<SamsungWatchSensorStores>()
        listOf(
            WatchSensorDescriptor(get<AccelerometerSensor>(), stores.accelerometer),
            WatchSensorDescriptor(get<PPGSensor>(), stores.ppg),
            WatchSensorDescriptor(get<HeartRateSensor>(), stores.heartRate),
            WatchSensorDescriptor(get<SkinTemperatureSensor>(), stores.skinTemperature),
            WatchSensorDescriptor(get<EDASensor>(), stores.eda),
            WatchSensorDescriptor(get<StressSensor>(), stores.watchStress),
        )
    }

    // ECG's store still participates in phone-sync/record-counts/flush even though
    // the sensor itself isn't part of watchSensorDescriptors (see ECGSensor single above).
    single<Map<String, WatchSensorStore<*>>>(named(HEALTH_EXTRA_STORES)) {
        mapOf(get<ECGSensor>().id to get<SamsungWatchSensorStores>().ecg)
    }

    single {
        StressDetectionAdapter(
            stressSensor = get(),
            tracker = get()
        )
    }

    single<HealthSensorPlatform> {
        SamsungHealthSensorPlatform(
            initializer = get(),
            ecgSensor = get(),
            stressDetectionAdapter = get()
        )
    }

    viewModel {
        EcgViewModel(
            ecgSensor = get(),
            watchSensorStores = get(),
            permissionManager = get<AndroidPermissionManager>()
        )
    }
}
