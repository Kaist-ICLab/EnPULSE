package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.storage.CouchbaseSensorStateStorage
import kaist.iclab.mobiletracker.storage.CouchbaseSurveyConfigStorage
import kaist.iclab.mobiletracker.storage.SimpleStateStorage
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.microema.MicroEmaBuilder
import kaist.iclab.tracker.sensor.galaxywatch.MicroEmaSensor
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.tracker.trigger.ActionConfig
import kaist.iclab.tracker.trigger.CalculationConfig
import kaist.iclab.tracker.trigger.ConditionConfig
import kaist.iclab.tracker.trigger.DefaultTriggerEngine
import kaist.iclab.tracker.trigger.TriggerEngine
import kaist.iclab.tracker.trigger.TriggerRule
import kaist.iclab.tracker.trigger.WindowConfig
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val microEmaSensorModule = module {

    single<TriggerEngine> {
        DefaultTriggerEngine()
    }

    single<StateStorage<MicroEmaSensor.Config>>(named("microEmaConfigStorage")) {
        val persistentStorage = get<CouchbaseSurveyConfigStorage>()
        val savedConfigs = persistentStorage.get().configs
        val watchConfigs = savedConfigs.filter { it.deviceType == 1 && it.expireAfterMs != null }

        val initialConfig = if (watchConfigs.isNotEmpty()) {
            val watchEmaConfigs = watchConfigs.associate { survey ->
                val watchConfig = MicroEmaBuilder.build(survey)
                watchConfig.surveyId to watchConfig
            }

            // Create Simple Periodic Rules for each survey (every 60 mins)
            val rules = watchEmaConfigs.keys.map { surveyId ->
                TriggerRule(
                    id = "simple_periodic_$surveyId",
                    sourceSensorId = "HeartRate",
                    window = WindowConfig(type = "TUMBLING", sizeMs = 60 * 60 * 1000L),
                    calculation = CalculationConfig(type = "MEAN", dataField = "bpm"),
                    condition = ConditionConfig(operator = "GREATER_THAN", threshold = 0.0),
                    action = ActionConfig(type = "MICRO_EMA", payload = surveyId.toString())
                )
            }

            MicroEmaSensor.Config(
                rules = rules,
                watchSurveyConfigs = watchEmaConfigs
            )
        } else {
            MicroEmaSensor.Config()
        }

        SimpleStateStorage(initialConfig)
    }

    single<StateStorage<SensorState>>(named("microEmaStateStorage")) {
        CouchbaseSensorStateStorage(
            couchbase = get(),
            collectionName = MicroEmaSensor::class.simpleName ?: "MicroEmaSensor"
        )
    }

    single {
        MicroEmaSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = get(named("microEmaConfigStorage")),
            stateStorage = get(named("microEmaStateStorage")),
            triggerEngine = get(),
            bleChannel = BLEDataChannel(androidContext())
        )
    }
}
