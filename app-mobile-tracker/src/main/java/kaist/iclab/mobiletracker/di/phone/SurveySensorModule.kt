package kaist.iclab.mobiletracker.di.phone

import android.util.Log
import kaist.iclab.mobiletracker.storage.CouchbaseSensorStateStorage
import kaist.iclab.mobiletracker.storage.CouchbaseSurveyConfigStorage
import kaist.iclab.mobiletracker.storage.SimpleStateStorage
import kaist.iclab.mobiletracker.utils.SurveyConfigConverter
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.phone.SurveySensor
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.storage.core.SurveyScheduleStorage
import kaist.iclab.tracker.storage.couchbase.CouchbaseSurveyScheduleStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Survey sensor module - survey storage and sensor
 */
val surveySensorModule = module {
    // Survey Schedule Storage
    single<SurveyScheduleStorage> {
        CouchbaseSurveyScheduleStorage(
            couchbase = get(),
            collectionName = "SurveyScheduleStorage"
        )
    }

    // Persistent storage for raw SurveyConfigList from Supabase
    single {
        CouchbaseSurveyConfigStorage(
            couchbase = get()
        )
    }

    // SurveySensor config storage - primed from persistent storage at startup
    single<StateStorage<SurveySensor.Config>>(named("surveySensorConfigStorage")) {
        val persistentStorage = get<CouchbaseSurveyConfigStorage>()
        val savedConfigs = persistentStorage.get().configs

        val initialConfig = if (savedConfigs.isNotEmpty()) {
            try {
                SurveyConfigConverter.toSurveySensorConfig(savedConfigs.filter { it.deviceType == 0 })
            } catch (e: Exception) {
                Log.e(
                    "SurveySensorModule",
                    "Error priming survey config: ${e.message}"
                )
                SurveySensor.Config(survey = emptyMap())
            }
        } else {
            SurveySensor.Config(survey = emptyMap())
        }

        SimpleStateStorage(initialConfig)
    }

    // Survey Sensor
    single {
        SurveySensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = get(named("surveySensorConfigStorage")),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = SurveySensor::class.simpleName ?: ""
            ),
            scheduleStorage = get<SurveyScheduleStorage>(),
        )
    }
}
