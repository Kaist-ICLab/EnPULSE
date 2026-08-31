package kaist.iclab.mobiletracker.di.phone

import com.google.android.gms.location.Priority
import kaist.iclab.mobiletracker.storage.CouchbaseSensorStateStorage
import kaist.iclab.mobiletracker.storage.SimpleStateStorage
import kaist.iclab.tracker.listener.SamsungHealthDataInitializer
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.common.BatterySensor
import kaist.iclab.tracker.sensor.common.LocationSensor
import kaist.iclab.tracker.sensor.galaxywatch.AudioSensor
import kaist.iclab.tracker.sensor.phone.ConnectivitySensor
import kaist.iclab.tracker.sensor.phone.DeviceModeSensor
import kaist.iclab.tracker.sensor.phone.ScreenSensor
import kaist.iclab.tracker.sensor.phone.WifiScanSensor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

/**
 * Core sensors module - fundamental device sensors
 */
val coreSensorsModule = module {
    // Sensor Management Dependencies
    single {
        SamsungHealthDataInitializer(context = androidContext())
    }

    single {
        AndroidPermissionManager(
            context = androidContext(),
            scope = get<kaist.iclab.mobiletracker.di.AppCoroutineScope>().io
        )
    }

    // Location Sensor
    single {
        LocationSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                LocationSensor.Config(
                    interval = TimeUnit.SECONDS.toMillis(1),
                    maxUpdateAge = 0,
                    maxUpdateDelay = 0,
                    minUpdateDistance = 0.0f,
                    minUpdateInterval = 0,
                    priority = Priority.PRIORITY_HIGH_ACCURACY,
                    waitForAccurateLocation = false,
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = LocationSensor::class.simpleName ?: ""
            )
        )
    }

    // Battery Sensor
    single {
        BatterySensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(BatterySensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = BatterySensor::class.simpleName ?: ""
            )
        )
    }

    // Screen Sensor
    single {
        ScreenSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(ScreenSensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = ScreenSensor::class.simpleName ?: ""
            )
        )
    }

    // Device Mode Sensor
    single {
        DeviceModeSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(DeviceModeSensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = DeviceModeSensor::class.simpleName ?: ""
            )
        )
    }

    // Connectivity Sensor
    single {
        ConnectivitySensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(ConnectivitySensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = ConnectivitySensor::class.simpleName ?: ""
            )
        )
    }

    // WiFi Scan Sensor
    single {
        WifiScanSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(WifiScanSensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = WifiScanSensor::class.simpleName ?: ""
            )
        )
    }

    // Audio Sensor (VAD)
    single {
        AudioSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(AudioSensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = AudioSensor::class.simpleName ?: ""
            )
        )
    }
}
