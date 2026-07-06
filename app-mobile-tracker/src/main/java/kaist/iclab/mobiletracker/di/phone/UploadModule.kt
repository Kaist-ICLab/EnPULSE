package kaist.iclab.mobiletracker.di.phone

import com.google.gson.Gson
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.entity.phone.AmbientLightEntity
import kaist.iclab.mobiletracker.db.entity.phone.AppListChangeEntity
import kaist.iclab.mobiletracker.db.entity.phone.AppUsageLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.BatteryEntity
import kaist.iclab.mobiletracker.db.entity.phone.BluetoothScanEntity
import kaist.iclab.mobiletracker.db.entity.phone.CallLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.ConnectivityEntity
import kaist.iclab.mobiletracker.db.entity.phone.DataTrafficEntity
import kaist.iclab.mobiletracker.db.entity.phone.DeviceModeEntity
import kaist.iclab.mobiletracker.db.entity.phone.MediaEntity
import kaist.iclab.mobiletracker.db.entity.phone.MessageLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.NotificationEntity
import kaist.iclab.mobiletracker.db.entity.phone.ScreenEntity
import kaist.iclab.mobiletracker.db.entity.phone.StepEntity
import kaist.iclab.mobiletracker.db.entity.phone.UserInteractionEntity
import kaist.iclab.mobiletracker.db.entity.phone.WifiScanEntity
import kaist.iclab.mobiletracker.db.obx.PhoneSensorStore
import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.repository.PhoneSensorRepository
import kaist.iclab.mobiletracker.repository.PhoneSensorRepositoryImpl
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.supabase.SupabaseUploadService
import kaist.iclab.mobiletracker.services.upload.SensorUploadService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry
import kaist.iclab.mobiletracker.services.upload.handlers.buildPhoneUploadHandlers
import kaist.iclab.mobiletracker.services.upload.handlers.buildWatchUploadHandlers
import kaist.iclab.tracker.sensor.common.BatterySensor
import kaist.iclab.tracker.sensor.common.LocationSensor
import kaist.iclab.tracker.sensor.phone.AmbientLightSensor
import kaist.iclab.tracker.sensor.phone.AppListChangeSensor
import kaist.iclab.tracker.sensor.phone.AppUsageLogSensor
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

val uploadModule = module {
    single {
        SyncTimestampService(context = androidContext())
    }

    single<Map<String, PhoneSensorStore<*>>>(named("sensorDataStorages")) {
        val s = get<SensorStores>()
        val gson = Gson()
        mapOf(
            get<AmbientLightSensor>().id to PhoneSensorStore(s.ambientLight) { e, uuid ->
                e as AmbientLightSensor.Entity
                AmbientLightEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, accuracy = e.accuracy, value = e.value)
            },
            get<AppListChangeSensor>().id to PhoneSensorStore(s.appListChange) { e, uuid ->
                e as AppListChangeSensor.Entity
                AppListChangeEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, changedAppJson = e.changedApp?.let { gson.toJson(it) }, appListJson = e.appList?.let { gson.toJson(it) })
            },
            get<AppUsageLogSensor>().id to PhoneSensorStore(s.appUsageLog) { e, uuid ->
                e as AppUsageLogSensor.Entity
                AppUsageLogEntity(uuid = uuid ?: "", eventId = e.eventId, received = e.received, timestamp = e.timestamp, packageName = e.packageName, installedBy = e.installedBy, eventType = e.eventType)
            },
            get<BatterySensor>().id to PhoneSensorStore(s.battery) { e, uuid ->
                e as BatterySensor.Entity
                BatteryEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, connectedType = e.connectedType, status = e.status, level = e.level, temperature = e.temperature)
            },
            get<BluetoothScanSensor>().id to PhoneSensorStore(s.bluetoothScan) { e, uuid ->
                e as BluetoothScanSensor.Entity
                BluetoothScanEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, name = e.name, alias = e.alias, address = e.address, bondState = e.bondState, connectionType = e.connectionType, classType = e.classType, rssi = e.rssi, isLE = e.isLE)
            },
            get<CallLogSensor>().id to PhoneSensorStore(s.callLog) { e, uuid ->
                e as CallLogSensor.Entity
                CallLogEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, duration = e.duration, number = e.number, type = e.type)
            },
            get<ConnectivitySensor>().id to PhoneSensorStore(s.connectivity) { e, uuid ->
                e as ConnectivitySensor.Entity
                ConnectivityEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, networkType = e.networkType, isConnected = e.isConnected, hasInternet = e.hasInternet, transportTypes = e.transportTypes.joinToString(","))
            },
            get<DataTrafficSensor>().id to PhoneSensorStore(s.dataTraffic) { e, uuid ->
                e as DataTrafficSensor.Entity
                DataTrafficEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, totalRx = e.totalRx, totalTx = e.totalTx, mobileRx = e.mobileRx, mobileTx = e.mobileTx)
            },
            get<DeviceModeSensor>().id to PhoneSensorStore(s.deviceMode) { e, uuid ->
                e as DeviceModeSensor.Entity
                DeviceModeEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, eventType = e.eventType, value = e.value)
            },
            get<LocationSensor>().id to PhoneSensorStore(s.location) { e, uuid ->
                e as LocationSensor.Entity
                LocationEntity(uuid = uuid ?: "", deviceType = DeviceType.PHONE.value, received = e.received, timestamp = e.timestamp, latitude = e.latitude, longitude = e.longitude, altitude = e.altitude, speed = e.speed, accuracy = e.accuracy)
            },
            get<MediaSensor>().id to PhoneSensorStore(s.media) { e, uuid ->
                e as MediaSensor.Entity
                MediaEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, operation = e.operation, mediaType = e.mediaType, storageType = e.storageType, uri = e.uri, fileName = e.fileName, mimeType = e.mimeType, size = e.size, dateAdded = e.dateAdded, dateModified = e.dateModified)
            },
            get<MessageLogSensor>().id to PhoneSensorStore(s.messageLog) { e, uuid ->
                e as MessageLogSensor.Entity
                MessageLogEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, number = e.number, messageType = e.messageType, contactType = e.contactType)
            },
            get<NotificationSensor>().id to PhoneSensorStore(s.notification) { e, uuid ->
                e as NotificationSensor.Entity
                NotificationEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, packageName = e.packageName, eventType = e.eventType, title = e.title, text = e.text, visibility = e.visibility, category = e.category)
            },
            get<ScreenSensor>().id to PhoneSensorStore(s.screen) { e, uuid ->
                e as ScreenSensor.Entity
                ScreenEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, type = e.type)
            },
            get<StepSensor>().id to PhoneSensorStore(s.step) { e, uuid ->
                e as StepSensor.Entity
                StepEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, startTime = e.startTime, endTime = e.endTime, steps = e.steps)
            },
            get<UserInteractionSensor>().id to PhoneSensorStore(s.userInteraction) { e, uuid ->
                e as UserInteractionSensor.Entity
                UserInteractionEntity(uuid = uuid ?: "", eventId = e.eventId, received = e.received, timestamp = e.timestamp, packageName = e.packageName, className = e.className, eventType = e.eventType, text = e.text)
            },
            get<WifiScanSensor>().id to PhoneSensorStore(s.wifiScan) { e, uuid ->
                e as WifiScanSensor.Entity
                WifiScanEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, ssid = e.ssid, bssid = e.bssid, frequency = e.frequency, level = e.level)
            },
        )
    }

    single<PhoneSensorRepository> {
        PhoneSensorRepositoryImpl(
            sensorDataStorages = get<Map<String, PhoneSensorStore<*>>>(named("sensorDataStorages")),
            supabaseHelper = get(),
            appScope = get()
        )
    }

    single { SupabaseUploadService(supabaseHelper = get()) }

    single<SensorUploadHandlerRegistry> {
        val s = get<SensorStores>()
        SensorUploadHandlerRegistry(buildPhoneUploadHandlers(s) + buildWatchUploadHandlers(s))
    }

    single {
        SensorUploadService(
            handlerRegistry = get(),
            supabaseHelper = get(),
            syncTimestampService = get(),
            campaignSensorRepository = get()
        )
    }
}
