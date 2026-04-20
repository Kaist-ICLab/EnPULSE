package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.R
import kaist.iclab.tracker.sensor.common.LocationSensor
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.phone.AmbientLightSensor
import kaist.iclab.tracker.sensor.phone.AppListChangeSensor
import kaist.iclab.tracker.sensor.phone.AppUsageLogSensor
import kaist.iclab.tracker.sensor.phone.BatterySensor
import kaist.iclab.tracker.sensor.phone.BluetoothScanSensor
import kaist.iclab.tracker.sensor.phone.CallLogSensor
import kaist.iclab.tracker.sensor.phone.ConnectivitySensor
import kaist.iclab.tracker.sensor.phone.DataTrafficSensor
import kaist.iclab.tracker.sensor.phone.DeviceModeSensor
import kaist.iclab.tracker.sensor.phone.MediaSensor
import kaist.iclab.tracker.sensor.phone.MessageLogSensor
import kaist.iclab.tracker.sensor.phone.NotificationSensor
import kaist.iclab.tracker.sensor.phone.ScreenSensor
import kaist.iclab.tracker.sensor.phone.StepSensor
import kaist.iclab.tracker.sensor.phone.UserInteractionSensor
import kaist.iclab.tracker.sensor.phone.WifiScanSensor
import kaist.iclab.tracker.sensor.survey.SurveySensor
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Controller module - BackgroundController and sensors list
 */
val controllerModule = module {
    // Sensors list
    single(named("phoneSensors")) {
        listOf(
            get<AmbientLightSensor>(),
            get<AppListChangeSensor>(),
            get<AppUsageLogSensor>(),
            get<BatterySensor>(),
            get<BluetoothScanSensor>(),
            get<CallLogSensor>(),
            get<DataTrafficSensor>(),
            get<DeviceModeSensor>(),
            get<LocationSensor>(),
            get<MediaSensor>(),
            get<MessageLogSensor>(),
            get<ConnectivitySensor>(),
            get<NotificationSensor>(),
            get<ScreenSensor>(),
            get<StepSensor>(),
            get<UserInteractionSensor>(),
            get<WifiScanSensor>(),
            get<SurveySensor>(),
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

    // BackgroundController
    single {
        BackgroundController(
            context = androidContext(),
            controllerStateStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = ControllerState(ControllerState.FLAG.DISABLED),
                clazz = ControllerState::class.java,
                collectionName = BackgroundController::class.simpleName ?: ""
            ),
            sensors = get(named("phoneSensors")),
            serviceNotification = get<BackgroundController.ServiceNotification>(),
            allowPartialSensing = true,
        )
    }
}
