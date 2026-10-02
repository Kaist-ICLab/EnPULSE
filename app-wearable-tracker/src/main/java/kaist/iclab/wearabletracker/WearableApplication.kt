package kaist.iclab.wearabletracker

import android.app.Application
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependencies
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependenciesProvider
import androidx.core.app.NotificationManagerCompat
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.wearabletracker.data.PhoneCommunicationManager
import kaist.iclab.wearabletracker.helpers.NotificationHelper
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.wearabletracker.storage.SensorDataReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
        // Triggers and detections are one-shot; delete them once handled so a queued one is never
        // delivered again (e.g. to the next wearer after the watch reconnects).
        get<PhoneCommunicationManager>().getBleChannel().deleteAfterDelivery(
            setOf(
                Constants.BLE.KEY_WATCH_EMA_TRIGGER,
                Constants.BLE.KEY_WATCH_NOTIFICATION_TRIGGER,
                Constants.BLE.KEY_DETECTION_STATE_UPDATE
            )
        )

        markCollectionStoppedAfterRestart()
        observeCollectionForDataWriter()
    }

    /**
     * A fresh process means nothing is collecting yet: Android does not restart the sensor
     * service on its own (restartAfterProcessDeath = false), and a reboot doesn't resume it
     * either. A saved RUNNING or PAUSED is left over from before the restart, so report it as
     * stopped and ask the wearer to start collection again from the app.
     */
    private fun markCollectionStoppedAfterRestart() {
        if (BackgroundController.ControllerService.isServiceRunning) return
        val controllerState = get<StateStorage<ControllerState>>(named("watchControllerStateStorage"))
        val flag = controllerState.get().flag
        if (flag == ControllerState.FLAG.RUNNING || flag == ControllerState.FLAG.PAUSED) {
            controllerState.set(ControllerState(ControllerState.FLAG.READY))
            NotificationHelper.showCollectionStoppedNotification(this)
        }
    }

    /**
     * Keep the database writer in step with data collection for the whole process, not only while
     * the settings screen is open; otherwise nothing is saved after a reboot or a restart until
     * someone opens the app. PAUSED (watch not worn) keeps it alive, since no data arrives anyway
     * and restarting it from the background on resume could be refused.
     */
    private fun observeCollectionForDataWriter() {
        val controllerState = get<StateStorage<ControllerState>>(named("watchControllerStateStorage"))
        val dataWriter = get<SensorDataReceiver>()
        get<CoroutineScope>().launch {
            controllerState.stateFlow.collect { state ->
                when (state.flag) {
                    ControllerState.FLAG.RUNNING -> {
                        dataWriter.startBackgroundCollection()
                        NotificationManagerCompat.from(this@WearableApplication)
                            .cancel(Constants.NotificationId.COLLECTION_STOPPED)
                    }
                    ControllerState.FLAG.PAUSED -> Unit
                    else -> dataWriter.stopBackgroundCollection()
                }
            }
        }
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
            restartAfterProcessDeath = false,
            sensors = sensors,
            serviceNotification = koin.get(),
            allowPartialSensing = true,
            offBodyDetector = koin.get()
        )
    }
}
