package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.db.TrackerRoomDB
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.repository.PhoneSensorRepository
import kaist.iclab.mobiletracker.repository.PhoneSensorRepositoryImpl
import kaist.iclab.mobiletracker.services.SensorServiceRegistry
import kaist.iclab.mobiletracker.services.SensorServiceRegistryImpl
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.supabase.AmbientLightSensorService
import kaist.iclab.mobiletracker.services.supabase.AppListChangeSensorService
import kaist.iclab.mobiletracker.services.supabase.AppUsageLogSensorService
import kaist.iclab.mobiletracker.services.supabase.BatterySensorService
import kaist.iclab.mobiletracker.services.supabase.BluetoothScanSensorService
import kaist.iclab.mobiletracker.services.supabase.CallLogSensorService
import kaist.iclab.mobiletracker.services.supabase.ConnectivitySensorService
import kaist.iclab.mobiletracker.services.supabase.DataTrafficSensorService
import kaist.iclab.mobiletracker.services.supabase.DeviceModeSensorService
import kaist.iclab.mobiletracker.services.supabase.LocationSensorService
import kaist.iclab.mobiletracker.services.supabase.MediaSensorService
import kaist.iclab.mobiletracker.services.supabase.MessageLogSensorService
import kaist.iclab.mobiletracker.services.supabase.NotificationSensorService
import kaist.iclab.mobiletracker.services.supabase.ScreenSensorService
import kaist.iclab.mobiletracker.services.supabase.StepSensorService
import kaist.iclab.mobiletracker.services.supabase.UserInteractionSensorService
import kaist.iclab.mobiletracker.services.supabase.WifiSensorService
import kaist.iclab.mobiletracker.services.upload.PhoneSensorUploadService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry
import kaist.iclab.mobiletracker.services.upload.handlers.phone.AmbientLightUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.AppListChangeUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.AppUsageLogUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.BatteryUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.BluetoothScanUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.CallLogUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.ConnectivityUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.DataTrafficUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.DeviceModeUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.LocationUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.MediaUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.MessageLogUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.NotificationUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.ScreenUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.StepUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.UserInteractionUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.phone.WifiScanUploadHandler
import kaist.iclab.tracker.sensor.common.LocationSensor
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
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Upload module - sensor services, upload handlers, and upload service
 */
val uploadModule = module {
    // SyncTimestampService for tracking upload timestamps
    single {
        SyncTimestampService(context = androidContext())
    }

    // Map of sensor IDs to DAOs for storing phone sensor data in Room database
    single<Map<String, BaseDao<*, *>>>(named("sensorDataStorages")) {
        val db = get<TrackerRoomDB>()
        mapOf(
            get<AmbientLightSensor>().id to db.ambientLightDao(),
            get<AppListChangeSensor>().id to db.appListChangeDao(),
            get<AppUsageLogSensor>().id to db.appUsageLogDao(),
            get<BatterySensor>().id to db.batteryDao(),
            get<BluetoothScanSensor>().id to db.bluetoothScanDao(),
            get<ConnectivitySensor>().id to db.connectivityDao(),
            get<CallLogSensor>().id to db.callLogDao(),
            get<MessageLogSensor>().id to db.messageLogDao(),
            get<UserInteractionSensor>().id to db.userInteractionDao(),
            get<DataTrafficSensor>().id to db.dataTrafficDao(),
            get<DeviceModeSensor>().id to db.deviceModeDao(),
            get<LocationSensor>().id to db.locationDao(),
            get<ScreenSensor>().id to db.screenDao(),
            get<MediaSensor>().id to db.mediaDao(),
            get<NotificationSensor>().id to db.notificationDao(),
            get<StepSensor>().id to db.stepDao(),
            get<WifiScanSensor>().id to db.wifiDao(),
        )
    }

    // PhoneSensorRepository - bind interface to implementation
    single<PhoneSensorRepository> {
        PhoneSensorRepositoryImpl(
            sensorDataStorages = get<Map<String, BaseDao<*, *>>>(named("sensorDataStorages")),
            supabaseHelper = get(),
            appScope = get()
        )
    }

    // Sensor Services for uploading to Supabase
    single { AmbientLightSensorService(supabaseHelper = get()) }
    single { AppListChangeSensorService(supabaseHelper = get()) }
    single { AppUsageLogSensorService(supabaseHelper = get()) }
    single { BatterySensorService(supabaseHelper = get()) }
    single { BluetoothScanSensorService(supabaseHelper = get()) }
    single { CallLogSensorService(supabaseHelper = get()) }
    single { ConnectivitySensorService(supabaseHelper = get()) }
    single { DataTrafficSensorService(supabaseHelper = get()) }
    single { DeviceModeSensorService(supabaseHelper = get()) }
    single { LocationSensorService(supabaseHelper = get()) }
    single { MediaSensorService(supabaseHelper = get()) }
    single { MessageLogSensorService(supabaseHelper = get()) }
    single { NotificationSensorService(supabaseHelper = get()) }
    single { ScreenSensorService(supabaseHelper = get()) }
    single { StepSensorService(supabaseHelper = get()) }
    single { UserInteractionSensorService(supabaseHelper = get()) }
    single { WifiSensorService(supabaseHelper = get()) }

    // Phone sensor service registry
    single<SensorServiceRegistry>(named("phoneSensorServiceRegistry")) {
        SensorServiceRegistryImpl(
            mapOf(
                get<AmbientLightSensor>().id to get<AmbientLightSensorService>(),
                get<AppListChangeSensor>().id to get<AppListChangeSensorService>(),
                get<AppUsageLogSensor>().id to get<AppUsageLogSensorService>(),
                get<BatterySensor>().id to get<BatterySensorService>(),
                get<BluetoothScanSensor>().id to get<BluetoothScanSensorService>(),
                get<ConnectivitySensor>().id to get<ConnectivitySensorService>(),
                get<MediaSensor>().id to get<MediaSensorService>(),
                get<MessageLogSensor>().id to get<MessageLogSensorService>(),
                get<NotificationSensor>().id to get<NotificationSensorService>(),
                get<StepSensor>().id to get<StepSensorService>(),
                get<UserInteractionSensor>().id to get<UserInteractionSensorService>(),
                get<CallLogSensor>().id to get<CallLogSensorService>(),
                get<DataTrafficSensor>().id to get<DataTrafficSensorService>(),
                get<DeviceModeSensor>().id to get<DeviceModeSensorService>(),
                get<LocationSensor>().id to get<LocationSensorService>(),
                get<ScreenSensor>().id to get<ScreenSensorService>(),
                get<WifiScanSensor>().id to get<WifiSensorService>(),
            )
        )
    }

    // SensorUploadHandlerRegistry for phone sensors
    single<SensorUploadHandlerRegistry> {
        val db = get<TrackerRoomDB>()
        val handlers = listOf(
            LocationUploadHandler(dao = db.locationDao(), service = get<LocationSensorService>()),
            BatteryUploadHandler(dao = db.batteryDao(), service = get<BatterySensorService>()),
            ScreenUploadHandler(dao = db.screenDao(), service = get<ScreenSensorService>()),
            WifiScanUploadHandler(dao = db.wifiDao(), service = get<WifiSensorService>()),
            StepUploadHandler(dao = db.stepDao(), service = get<StepSensorService>()),
            AmbientLightUploadHandler(
                dao = db.ambientLightDao(),
                service = get<AmbientLightSensorService>()
            ),
            AppListChangeUploadHandler(
                dao = db.appListChangeDao(),
                service = get<AppListChangeSensorService>()
            ),
            AppUsageLogUploadHandler(
                dao = db.appUsageLogDao(),
                service = get<AppUsageLogSensorService>()
            ),
            BluetoothScanUploadHandler(
                dao = db.bluetoothScanDao(),
                service = get<BluetoothScanSensorService>()
            ),
            CallLogUploadHandler(dao = db.callLogDao(), service = get<CallLogSensorService>()),
            ConnectivityUploadHandler(
                dao = db.connectivityDao(),
                service = get<ConnectivitySensorService>()
            ),
            DataTrafficUploadHandler(
                dao = db.dataTrafficDao(),
                service = get<DataTrafficSensorService>()
            ),
            DeviceModeUploadHandler(
                dao = db.deviceModeDao(),
                service = get<DeviceModeSensorService>()
            ),
            MediaUploadHandler(dao = db.mediaDao(), service = get<MediaSensorService>()),
            MessageLogUploadHandler(
                dao = db.messageLogDao(),
                service = get<MessageLogSensorService>()
            ),
            NotificationUploadHandler(
                dao = db.notificationDao(),
                service = get<NotificationSensorService>()
            ),
            UserInteractionUploadHandler(
                dao = db.userInteractionDao(),
                service = get<UserInteractionSensorService>()
            )
        )
        SensorUploadHandlerRegistry(handlers)
    }

    // PhoneSensorUploadService for handling phone sensor data uploads
    single {
        PhoneSensorUploadService(
            handlerRegistry = get(),
            supabaseHelper = get(),
            syncTimestampService = get(),
            campaignSensorRepository = get()
        )
    }
}
