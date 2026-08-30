package kaist.iclab.wearabletracker

import android.app.Application
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependencies
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependenciesProvider
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.trigger.adapter.galaxywatch.GestureDetectionAdapter
import kaist.iclab.tracker.trigger.adapter.galaxywatch.StressDetectionAdapter
import kaist.iclab.wearabletracker.data.CampaignSensorConfigRepository
import kaist.iclab.wearabletracker.data.SyncAckListener
import kaist.iclab.wearabletracker.ema.MicroEmaResponseManager
import kaist.iclab.wearabletracker.trigger.DetectionStateForwarder
import kaist.iclab.wearabletracker.trigger.WatchEmaTriggerReceiver
import kaist.iclab.wearabletracker.trigger.WatchNotificationTriggerReceiver
import kaist.iclab.wearabletracker.trigger.WatchSurveyConfigReceiver
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import org.koin.core.qualifier.named

class WearableApplication : Application(), KoinComponent, BackgroundControllerDependenciesProvider {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@WearableApplication)
            androidLogger(level = Level.NONE)
            modules(koinModule)
        }

        // Start listening for sync ACKs from the phone
        get<SyncAckListener>().startListening()
        get<MicroEmaResponseManager>().startListening()
        get<CampaignSensorConfigRepository>().startListening()

        // Trigger condition sources: feed the local DetectionStateTracker, then forward it to
        // the phone-hosted trigger engine. WatchSurveyConfigReceiver caches survey question
        // content; WatchEmaTriggerReceiver executes the phone's "launch this survey" commands.
        get<StressDetectionAdapter>().start()
        get<GestureDetectionAdapter>().start()
        get<DetectionStateForwarder>().start()
        get<WatchSurveyConfigReceiver>().startListening()
        get<WatchEmaTriggerReceiver>().startListening()
        get<WatchNotificationTriggerReceiver>().startListening()
    }

    override fun provideBackgroundControllerDependencies(): BackgroundControllerDependencies {
        val koin = getKoin()
        val allSensors = koin.get<List<Sensor<*, *>>>(named("sensors"))
        // Campaign membership gates collection; a never-synced (null) config fails open so a
        // fresh install isn't bricked before its first phone sync.
        val activeSensorIds =
            koin.get<CampaignSensorConfigRepository>().configFlow.value.activeSensorIds
        val sensors =
            if (activeSensorIds == null) allSensors else allSensors.filter { it.id in activeSensorIds }
        return BackgroundControllerDependencies(
            controllerStateStorage = koin.get(named("watchControllerStateStorage")),
            sensors = sensors,
            serviceNotification = koin.get(),
            allowPartialSensing = true
        )
    }
}
