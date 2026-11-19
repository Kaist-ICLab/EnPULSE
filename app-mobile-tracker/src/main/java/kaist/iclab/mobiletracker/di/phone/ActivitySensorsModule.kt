package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.storage.CouchbaseSensorStateStorage
import kaist.iclab.mobiletracker.storage.SimpleStateStorage
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.common.ActivityRecognitionSensor
import kaist.iclab.tracker.sensor.phone.AmbientLightSensor
import kaist.iclab.tracker.sensor.phone.AppListChangeSensor
import kaist.iclab.tracker.sensor.phone.AppUsageLogSensor
import kaist.iclab.tracker.sensor.phone.DataTrafficSensor
import kaist.iclab.tracker.sensor.phone.MediaSensor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

/**
 * Activity sensors module - app usage, steps, user interaction, ambient light
 */
val activitySensorsModule = module {
    // Activity Recognition Sensor
    single {
        ActivityRecognitionSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                ActivityRecognitionSensor.Config(intervalMillis = 10000L)
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = ActivityRecognitionSensor::class.simpleName ?: ""
            )
        )
    }

    // Ambient Light Sensor
    single {
        AmbientLightSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                AmbientLightSensor.Config(interval = 100L)
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = AmbientLightSensor::class.simpleName ?: ""
            )
        )
    }

    // App Usage Log Sensor
    single {
        AppUsageLogSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                AppUsageLogSensor.Config(interval = 60000L)
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = AppUsageLogSensor::class.simpleName ?: ""
            )
        )
    }

    // App List Change Sensor
    single {
        AppListChangeSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                AppListChangeSensor.Config(
                    periodicIntervalMillis = TimeUnit.SECONDS.toMillis(10),
                    includeSystemApps = false,
                    includeDisabledApps = false
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = AppListChangeSensor::class.simpleName ?: ""
            )
        )
    }

    // Media Sensor
    single {
        MediaSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(MediaSensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = MediaSensor::class.simpleName ?: ""
            )
        )
    }

    // Data Traffic Sensor
    single {
        DataTrafficSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                DataTrafficSensor.Config(interval = TimeUnit.MINUTES.toMillis(1))
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = DataTrafficSensor::class.simpleName ?: ""
            )
        )
    }
}
