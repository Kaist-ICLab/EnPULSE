package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.db.obx.PhoneEntityMappers
import kaist.iclab.mobiletracker.db.obx.PhoneSensorStore
import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.repository.PhoneSensorRepository
import kaist.iclab.mobiletracker.repository.PhoneSensorRepositoryImpl
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.supabase.SupabaseUploadService
import kaist.iclab.mobiletracker.services.upload.PhoneSensorUploadService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry
import kaist.iclab.mobiletracker.services.upload.handlers.buildPhoneUploadHandlers
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

    // Single generic upload service replacing the 23 per-sensor *SensorService classes.
    // Every sensor (incl. the concrete Location handlers) uploads through this.
    single { SupabaseUploadService(supabaseHelper = get()) }

    // SensorUploadHandlerRegistry for phone sensors (registered in buildPhoneUploadHandlers)
    single<SensorUploadHandlerRegistry> {
        SensorUploadHandlerRegistry(buildPhoneUploadHandlers(get<SensorStores>()))
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
