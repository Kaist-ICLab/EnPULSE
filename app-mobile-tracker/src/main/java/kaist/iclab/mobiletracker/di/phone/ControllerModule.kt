package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.R
import kaist.iclab.tracker.sensor.common.ActivityRecognitionSensor
import kaist.iclab.tracker.sensor.common.BatterySensor
import kaist.iclab.tracker.sensor.common.LocationSensor
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.controller.OffBodyDetector
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.galaxywatch.MicroEmaSensor
import kaist.iclab.tracker.sensor.phone.AmbientLightSensor
import kaist.iclab.tracker.sensor.phone.AppListChangeSensor
import kaist.iclab.tracker.sensor.phone.AppUsageLogSensor
import kaist.iclab.tracker.sensor.phone.BluetoothScanSensor
import kaist.iclab.tracker.sensor.phone.ConnectivitySensor
import kaist.iclab.tracker.sensor.phone.DataTrafficSensor
import kaist.iclab.tracker.sensor.phone.DeviceModeSensor
import kaist.iclab.tracker.sensor.phone.MediaSensor
import kaist.iclab.tracker.sensor.phone.ScreenSensor
import kaist.iclab.tracker.sensor.phone.TimingSensor
import kaist.iclab.tracker.sensor.phone.VADSensor
import kaist.iclab.tracker.sensor.phone.WifiScanSensor
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Koin qualifier for the list of Samsung Health sensors this flavor provides. */
const val SAMSUNG_HEALTH_SENSORS = "samsungHealthSensors"

/**
 * Controller module - BackgroundController and sensors list
 */
val controllerModule = module {
    // Sensors list. SurveySensor is deliberately excluded — it must always capture responses
    // regardless of "Start Logging"/campaign_table, so BackgroundController never manages its
    // start/stop and it has no Settings toggle. See SurveyResponseCapture's doc comment.
    single(named("phoneSensors")) {
        // Samsung Health sensors exist only in the samsung flavor; see SamsungHealthSensorsModule.
        val samsungHealth = get<List<Sensor<*, *>>>(named(SAMSUNG_HEALTH_SENSORS)).associateBy { it.id }
        listOfNotNull(
            get<ActivityRecognitionSensor>(),
            get<AmbientLightSensor>(),
            get<AppListChangeSensor>(),
            get<AppUsageLogSensor>(),
            get<BatterySensor>(),
            get<BluetoothScanSensor>(),
            get<DataTrafficSensor>(),
            get<DeviceModeSensor>(),
            samsungHealth["Exercise"],
            get<LocationSensor>(),
            get<MediaSensor>(),
            get<ConnectivitySensor>(),
            get<ScreenSensor>(),
            samsungHealth["Sleep"],
            samsungHealth["Step"],
            get<WifiScanSensor>(),
            get<TimingSensor>(),
            get<MicroEmaSensor>(),
            get<VADSensor>(),
        )
    }

    // BackgroundController ServiceNotification
    single {
        val context = androidContext()
        BackgroundController.ServiceNotification(
            channelId = "BackgroundControllerService",
            channelName = "MobileTracker",
            notificationId = 1,
            title = context.getString(R.string.notification_title),
            description = context.getString(R.string.notification_description),
            icon = R.drawable.ic_launcher_foreground
        )
    }

    single<StateStorage<ControllerState>>(named("phoneControllerStateStorage")) {
        CouchbaseStateStorage(
            couchbase = get(),
            defaultVal = ControllerState(ControllerState.FLAG.DISABLED),
            serializer = ControllerState.serializer(),
            collectionName = BackgroundController::class.simpleName ?: ""
        )
    }

    single {
        OffBodyDetector(context = androidContext())
    }

    // BackgroundController
    single {
        BackgroundController(
            context = androidContext(),
            controllerStateStorage = get(named("phoneControllerStateStorage")),
            sensors = get(named("phoneSensors")),
            serviceNotification = get<BackgroundController.ServiceNotification>(),
            allowPartialSensing = true,
        )
    }
}
