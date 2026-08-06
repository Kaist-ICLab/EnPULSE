package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.di.AppCoroutineScope
import kaist.iclab.mobiletracker.repository.CampaignSensorRepository
import kaist.iclab.mobiletracker.storage.CouchbaseSensorStateStorage
import kaist.iclab.mobiletracker.storage.SimpleStateStorage
import kaist.iclab.mobiletracker.trigger.PhoneTriggerActionHandler
import kaist.iclab.mobiletracker.webapp.WebAppTriggerHandler
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.phone.TimingSensor
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.storage.core.TimingScheduleStorage
import kaist.iclab.tracker.storage.couchbase.CouchbaseTimingScheduleStorage
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.tracker.trigger.DefaultTriggerEngine
import kaist.iclab.tracker.trigger.adapter.TimingDetectionAdapter
import kaist.iclab.tracker.trigger.engine.TriggerEngine
import kaist.iclab.tracker.trigger.state.DetectionStateTracker
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Trigger engine module — the trigger engine now lives entirely on the phone (previously
 * watch-only). Wires [DetectionStateTracker], [TimingSensor] + its [TimingDetectionAdapter]
 * bridge, [DefaultTriggerEngine], and [PhoneTriggerActionHandler].
 *
 * [DetectionStateTracker] is also injected into [kaist.iclab.mobiletracker.helpers.BLEHelper],
 * which forwards sensor detection states the watch streams over BLE
 * (`AppConfig.BLEKeys.DETECTION_STATE_UPDATE`) into it.
 *
 * [TimingSensor.Config] is sourced from `campaign_table.config` (the same generic per-sensor
 * config column every sensor will eventually read from — see
 * [kaist.iclab.mobiletracker.repository.CampaignSensorRepository.getSensorConfig]) for the row
 * whose `name = "timing_sensor"`, not a dedicated table.
 */
val triggerModule = module {
    // Single DetectionStateTracker shared by TimingDetectionAdapter, BLEHelper (watch-forwarded
    // sensor states), and the engine itself.
    single { DetectionStateTracker() }

    // Fire-instant bookkeeping storage for TimingSensor (next/last schedule per named value).
    // Unrelated to where the schedule *definitions* come from (see timingSensorConfigStorage
    // below) — this only tracks alarms already computed from them.
    single<TimingScheduleStorage> {
        CouchbaseTimingScheduleStorage(
            couchbase = get(),
            collectionName = "TimingScheduleStorage"
        )
    }

    // TimingSensor config storage - primed from CampaignSensorRepository's own persistent cache
    // (CampaignSensorConfigStorage, Couchbase-backed) at startup, same "prime a SimpleStateStorage
    // from an already-durable cache" pattern surveySensorConfigStorage uses in
    // SurveySensorModule.kt. CampaignSensorRepositoryImpl primes its in-memory flow from that same
    // persistent storage synchronously in its constructor, so this sees the last-synced config
    // immediately, before any network call.
    single<StateStorage<TimingSensor.Config>>(named("timingSensorConfigStorage")) {
        val configJson = get<CampaignSensorRepository>().getSensorConfig(TimingSensor.CAMPAIGN_TABLE_NAME)
        val initialConfig = if (configJson != null) {
            try {
                TimingSensor.Config.fromJson(configJson.toString())
            } catch (e: Exception) {
                TimingSensor.Config()
            }
        } else {
            TimingSensor.Config()
        }

        SimpleStateStorage(initialConfig)
    }

    single {
        TimingSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = get(named("timingSensorConfigStorage")),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = TimingSensor::class.simpleName ?: ""
            ),
            scheduleStorage = get<TimingScheduleStorage>(),
        )
    }

    single {
        TimingDetectionAdapter(
            timingSensor = get<TimingSensor>(),
            tracker = get<DetectionStateTracker>()
        )
    }

    // Dedicated BLE channel for outbound trigger actions (WatchEma), mirroring the pattern
    // WatchSurveyConfigPusher already uses for its own send-only channel.
    single<PhoneTriggerActionHandler> {
        PhoneTriggerActionHandler(
            context = androidContext(),
            surveySensor = get(),
            webAppTriggerHandler = get<WebAppTriggerHandler>(),
            bleChannel = BLEDataChannel(androidContext())
        )
    }

    single<TriggerEngine> {
        DefaultTriggerEngine(
            context = androidContext(),
            detectionStateTracker = get<DetectionStateTracker>(),
            coroutineScope = get<AppCoroutineScope>().io
        ).apply {
            setActionHandler(get<PhoneTriggerActionHandler>())
        }
    }
}
