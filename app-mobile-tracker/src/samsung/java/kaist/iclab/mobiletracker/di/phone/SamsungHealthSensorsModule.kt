package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.storage.CouchbaseSensorStateStorage
import kaist.iclab.mobiletracker.storage.SimpleStateStorage
import kaist.iclab.tracker.listener.SamsungHealthDataInitializer
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.permission.HealthDataPermissionHandler
import kaist.iclab.tracker.permission.SamsungHealthDataPermissionHandler
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.phone.ExerciseSensor
import kaist.iclab.tracker.sensor.phone.SleepSensor
import kaist.iclab.tracker.sensor.phone.StepSensor
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

/**
 * Samsung Health Data SDK sensors (step, exercise, sleep). Only the samsung flavor has them; the
 * open flavor's copy of this module provides an empty list.
 */
val samsungHealthSensorsModule = module {
    single {
        SamsungHealthDataInitializer(context = androidContext())
    }

    single<HealthDataPermissionHandler> {
        SamsungHealthDataPermissionHandler(context = androidContext())
    }

    // Step Sensor
    single {
        StepSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                StepSensor.Config(
                    syncPastLimitSeconds = TimeUnit.DAYS.toSeconds(7),
                    timeMarginSeconds = TimeUnit.HOURS.toSeconds(1),
                    bucketSizeMinutes = 10,
                    readIntervalMillis = TimeUnit.SECONDS.toMillis(10)
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = StepSensor::class.simpleName ?: ""
            ),
            samsungHealthDataInitializer = get()
        )
    }

    // Exercise Sensor
    single {
        ExerciseSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                ExerciseSensor.Config(
                    syncPastLimitSeconds = TimeUnit.DAYS.toSeconds(7),
                    timeMarginSeconds = TimeUnit.HOURS.toSeconds(1),
                    readIntervalMillis = TimeUnit.SECONDS.toMillis(10)
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = ExerciseSensor::class.simpleName ?: ""
            ),
            samsungHealthDataInitializer = get()
        )
    }

    // Sleep Sensor
    single {
        SleepSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                SleepSensor.Config(
                    syncPastLimitSeconds = TimeUnit.DAYS.toSeconds(7),
                    timeMarginSeconds = TimeUnit.HOURS.toSeconds(1),
                    readIntervalMillis = TimeUnit.SECONDS.toMillis(10)
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = SleepSensor::class.simpleName ?: ""
            ),
            samsungHealthDataInitializer = get()
        )
    }

    single<List<Sensor<*, *>>>(named(SAMSUNG_HEALTH_SENSORS)) {
        listOf(get<ExerciseSensor>(), get<SleepSensor>(), get<StepSensor>())
    }
}
