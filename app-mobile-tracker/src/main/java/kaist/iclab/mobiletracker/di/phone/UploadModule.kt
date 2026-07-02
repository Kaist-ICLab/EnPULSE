package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.db.obx.PhoneEntityMappers
import kaist.iclab.mobiletracker.db.obx.PhoneSensorStore
import kaist.iclab.mobiletracker.db.obx.SensorStores
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
import kaist.iclab.tracker.sensor.common.BatterySensor
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

    // Map of sensor IDs to phone stores (store + library-entity→entity mapping) for ObjectBox
    single<Map<String, PhoneSensorStore<*>>>(named("sensorDataStorages")) {
        val s = get<SensorStores>()
        mapOf(
            get<AmbientLightSensor>().id to PhoneSensorStore(s.ambientLight) { e, uuid -> PhoneEntityMappers.ambientLight(e as AmbientLightSensor.Entity, uuid) },
            get<AppListChangeSensor>().id to PhoneSensorStore(s.appListChange) { e, uuid -> PhoneEntityMappers.appListChange(e as AppListChangeSensor.Entity, uuid) },
            get<AppUsageLogSensor>().id to PhoneSensorStore(s.appUsageLog) { e, uuid -> PhoneEntityMappers.appUsageLog(e as AppUsageLogSensor.Entity, uuid) },
            get<BatterySensor>().id to PhoneSensorStore(s.battery) { e, uuid -> PhoneEntityMappers.battery(e as BatterySensor.Entity, uuid) },
            get<BluetoothScanSensor>().id to PhoneSensorStore(s.bluetoothScan) { e, uuid -> PhoneEntityMappers.bluetoothScan(e as BluetoothScanSensor.Entity, uuid) },
            get<ConnectivitySensor>().id to PhoneSensorStore(s.connectivity) { e, uuid -> PhoneEntityMappers.connectivity(e as ConnectivitySensor.Entity, uuid) },
            get<CallLogSensor>().id to PhoneSensorStore(s.callLog) { e, uuid -> PhoneEntityMappers.callLog(e as CallLogSensor.Entity, uuid) },
            get<MessageLogSensor>().id to PhoneSensorStore(s.messageLog) { e, uuid -> PhoneEntityMappers.messageLog(e as MessageLogSensor.Entity, uuid) },
            get<UserInteractionSensor>().id to PhoneSensorStore(s.userInteraction) { e, uuid -> PhoneEntityMappers.userInteraction(e as UserInteractionSensor.Entity, uuid) },
            get<DataTrafficSensor>().id to PhoneSensorStore(s.dataTraffic) { e, uuid -> PhoneEntityMappers.dataTraffic(e as DataTrafficSensor.Entity, uuid) },
            get<DeviceModeSensor>().id to PhoneSensorStore(s.deviceMode) { e, uuid -> PhoneEntityMappers.deviceMode(e as DeviceModeSensor.Entity, uuid) },
            get<LocationSensor>().id to PhoneSensorStore(s.location) { e, uuid -> PhoneEntityMappers.location(e as LocationSensor.Entity, uuid) },
            get<ScreenSensor>().id to PhoneSensorStore(s.screen) { e, uuid -> PhoneEntityMappers.screen(e as ScreenSensor.Entity, uuid) },
            get<MediaSensor>().id to PhoneSensorStore(s.media) { e, uuid -> PhoneEntityMappers.media(e as MediaSensor.Entity, uuid) },
            get<NotificationSensor>().id to PhoneSensorStore(s.notification) { e, uuid -> PhoneEntityMappers.notification(e as NotificationSensor.Entity, uuid) },
            get<StepSensor>().id to PhoneSensorStore(s.step) { e, uuid -> PhoneEntityMappers.step(e as StepSensor.Entity, uuid) },
            get<WifiScanSensor>().id to PhoneSensorStore(s.wifiScan) { e, uuid -> PhoneEntityMappers.wifiScan(e as WifiScanSensor.Entity, uuid) },
        )
    }

    // PhoneSensorRepository - bind interface to implementation
    single<PhoneSensorRepository> {
        PhoneSensorRepositoryImpl(
            sensorDataStorages = get<Map<String, PhoneSensorStore<*>>>(named("sensorDataStorages")),
            locationStore = get<SensorStores>().location,
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
        val s = get<SensorStores>()
        val handlers = listOf(
            LocationUploadHandler(store = s.location, service = get<LocationSensorService>()),
            BatteryUploadHandler(store = s.battery, service = get<BatterySensorService>()),
            ScreenUploadHandler(store = s.screen, service = get<ScreenSensorService>()),
            WifiScanUploadHandler(store = s.wifiScan, service = get<WifiSensorService>()),
            StepUploadHandler(store = s.step, service = get<StepSensorService>()),
            AmbientLightUploadHandler(
                store = s.ambientLight,
                service = get<AmbientLightSensorService>()
            ),
            AppListChangeUploadHandler(
                store = s.appListChange,
                service = get<AppListChangeSensorService>()
            ),
            AppUsageLogUploadHandler(
                store = s.appUsageLog,
                service = get<AppUsageLogSensorService>()
            ),
            BluetoothScanUploadHandler(
                store = s.bluetoothScan,
                service = get<BluetoothScanSensorService>()
            ),
            CallLogUploadHandler(store = s.callLog, service = get<CallLogSensorService>()),
            ConnectivityUploadHandler(
                store = s.connectivity,
                service = get<ConnectivitySensorService>()
            ),
            DataTrafficUploadHandler(
                store = s.dataTraffic,
                service = get<DataTrafficSensorService>()
            ),
            DeviceModeUploadHandler(
                store = s.deviceMode,
                service = get<DeviceModeSensorService>()
            ),
            MediaUploadHandler(store = s.media, service = get<MediaSensorService>()),
            MessageLogUploadHandler(
                store = s.messageLog,
                service = get<MessageLogSensorService>()
            ),
            NotificationUploadHandler(
                store = s.notification,
                service = get<NotificationSensorService>()
            ),
            UserInteractionUploadHandler(
                store = s.userInteraction,
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
