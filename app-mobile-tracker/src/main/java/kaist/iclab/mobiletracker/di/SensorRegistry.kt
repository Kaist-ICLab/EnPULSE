package kaist.iclab.mobiletracker.di

import com.google.gson.Gson
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
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
import kaist.iclab.mobiletracker.db.entity.watch.WatchAccelerometerEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchEDAEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchHeartRateEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchPPGEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchSkinTemperatureEntity
import kaist.iclab.mobiletracker.db.obx.PhoneSensorStore
import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.repository.SensorRecord
import kaist.iclab.mobiletracker.repository.handlers.GenericSensorDataHandler
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandler
import kaist.iclab.mobiletracker.services.supabase.SupabaseUploadService
import kaist.iclab.mobiletracker.services.upload.handlers.GenericSensorUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler
import kaist.iclab.tracker.sensor.common.BatterySensor
import kaist.iclab.tracker.sensor.common.LocationSensor
import kaist.iclab.tracker.sensor.core.SensorEntity
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
import kotlinx.serialization.KSerializer

/**
 * Single source of truth for every sensor's metadata. Adding a new sensor requires exactly one
 * entry in [buildAllSensorDescriptors] — no more parallel builder lists to keep in sync.
 */
class SensorDescriptor<T>(
    val sensorId: String,
    val displayName: String,
    val isWatchSensor: Boolean,
    val store: SensorStore<T>,
    val supabaseTable: String,
    val serializer: KSerializer<T>,
    val toRecord: (T) -> SensorRecord,
    val fromSensorEntity: ((SensorEntity, String?) -> T)? = null
) where T : BaseEntity, T : CsvSerializable {
    fun toDataHandler(): SensorDataHandler = GenericSensorDataHandler(
        sensorId = sensorId,
        displayName = displayName,
        isWatchSensor = isWatchSensor,
        supabaseTableName = supabaseTable,
        store = store,
        toRecord = toRecord
    )

    fun toUploadHandler(supabase: SupabaseUploadService): SensorUploadHandler = GenericSensorUploadHandler(
        sensorId = sensorId,
        store = store,
        serializer = serializer,
        tableName = supabaseTable,
        sensorName = displayName,
        supabase = supabase
    )

    fun toPhoneSensorStore(): PhoneSensorStore<T>? = fromSensorEntity?.let { PhoneSensorStore(store, it) }
}

fun buildAllSensorDescriptors(s: SensorStores): List<SensorDescriptor<*>> {
    val gson = Gson()
    return listOf(
        SensorDescriptor(
            sensorId = "AmbientLight",
            displayName = "Ambient Light",
            isWatchSensor = false,
            store = s.ambientLight,
            supabaseTable = AppConfig.SupabaseTables.AMBIENT_LIGHT_SENSOR,
            serializer = AmbientLightEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Value" to String.format("%.1f lux", e.value))) },
            fromSensorEntity = { e, uuid ->
                e as AmbientLightSensor.Entity
                AmbientLightEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, accuracy = e.accuracy, value = e.value)
            }
        ),
        SensorDescriptor(
            sensorId = "AppListChange",
            displayName = "App List Change",
            isWatchSensor = false,
            store = s.appListChange,
            supabaseTable = AppConfig.SupabaseTables.APP_LIST_CHANGE_SENSOR,
            serializer = AppListChangeEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Changed" to (e.changedAppJson?.take(50) ?: "N/A"))) },
            fromSensorEntity = { e, uuid ->
                e as AppListChangeSensor.Entity
                AppListChangeEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, changedAppJson = e.changedApp?.let { gson.toJson(it) }, appListJson = e.appList?.let { gson.toJson(it) })
            }
        ),
        SensorDescriptor(
            sensorId = "AppUsage",
            displayName = "App Usage",
            isWatchSensor = false,
            store = s.appUsageLog,
            supabaseTable = AppConfig.SupabaseTables.APP_USAGE_LOG_SENSOR,
            serializer = AppUsageLogEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Package" to e.packageName, "Event" to e.eventType.toString())) },
            fromSensorEntity = { e, uuid ->
                e as AppUsageLogSensor.Entity
                AppUsageLogEntity(uuid = uuid ?: "", eventId = e.eventId, received = e.received, timestamp = e.timestamp, packageName = e.packageName, installedBy = e.installedBy, eventType = e.eventType)
            }
        ),
        SensorDescriptor(
            sensorId = "Battery",
            displayName = "Battery",
            isWatchSensor = false,
            store = s.battery,
            supabaseTable = AppConfig.SupabaseTables.BATTERY_SENSOR,
            serializer = BatteryEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Level" to "${e.level}%", "Status" to e.status.toString())) },
            fromSensorEntity = { e, uuid ->
                e as BatterySensor.Entity
                BatteryEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, connectedType = e.connectedType, status = e.status, level = e.level, temperature = e.temperature)
            }
        ),
        SensorDescriptor(
            sensorId = "BluetoothScan",
            displayName = "Bluetooth Scan",
            isWatchSensor = false,
            store = s.bluetoothScan,
            supabaseTable = AppConfig.SupabaseTables.BLUETOOTH_SCAN_SENSOR,
            serializer = BluetoothScanEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Name" to e.name, "Address" to e.address)) },
            fromSensorEntity = { e, uuid ->
                e as BluetoothScanSensor.Entity
                BluetoothScanEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, name = e.name, alias = e.alias, address = e.address, bondState = e.bondState, connectionType = e.connectionType, classType = e.classType, rssi = e.rssi, isLE = e.isLE)
            }
        ),
        SensorDescriptor(
            sensorId = "CallLog",
            displayName = "Call Log",
            isWatchSensor = false,
            store = s.callLog,
            supabaseTable = AppConfig.SupabaseTables.CALL_LOG_SENSOR,
            serializer = CallLogEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Type" to e.type.toString(), "Duration" to "${e.duration}s")) },
            fromSensorEntity = { e, uuid ->
                e as CallLogSensor.Entity
                CallLogEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, duration = e.duration, number = e.number, type = e.type)
            }
        ),
        SensorDescriptor(
            sensorId = "Connectivity",
            displayName = "Connectivity",
            isWatchSensor = false,
            store = s.connectivity,
            supabaseTable = AppConfig.SupabaseTables.CONNECTIVITY_SENSOR,
            serializer = ConnectivityEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Network" to e.networkType, "Connected" to e.isConnected.toString())) },
            fromSensorEntity = { e, uuid ->
                e as ConnectivitySensor.Entity
                ConnectivityEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, networkType = e.networkType, isConnected = e.isConnected, hasInternet = e.hasInternet, transportTypes = e.transportTypes.joinToString(","))
            }
        ),
        SensorDescriptor(
            sensorId = "DataTraffic",
            displayName = "Data Traffic",
            isWatchSensor = false,
            store = s.dataTraffic,
            supabaseTable = AppConfig.SupabaseTables.DATA_TRAFFIC_SENSOR,
            serializer = DataTrafficEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Total Rx" to "${e.totalRx / 1024} KB", "Total Tx" to "${e.totalTx / 1024} KB")) },
            fromSensorEntity = { e, uuid ->
                e as DataTrafficSensor.Entity
                DataTrafficEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, totalRx = e.totalRx, totalTx = e.totalTx, mobileRx = e.mobileRx, mobileTx = e.mobileTx)
            }
        ),
        SensorDescriptor(
            sensorId = "DeviceMode",
            displayName = "Device Mode",
            isWatchSensor = false,
            store = s.deviceMode,
            supabaseTable = AppConfig.SupabaseTables.DEVICE_MODE_SENSOR,
            serializer = DeviceModeEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Event" to e.eventType, "Value" to e.value)) },
            fromSensorEntity = { e, uuid ->
                e as DeviceModeSensor.Entity
                DeviceModeEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, eventType = e.eventType, value = e.value)
            }
        ),
        SensorDescriptor(
            sensorId = "Location",
            displayName = "Location",
            isWatchSensor = false,
            store = s.location,
            supabaseTable = AppConfig.SupabaseTables.LOCATION_SENSOR,
            serializer = LocationEntity.serializer(),
            toRecord = { e ->
                SensorRecord(
                    id = e.id,
                    timestamp = e.timestamp,
                    fields = mapOf(
                        "Latitude" to String.format("%.6f°", e.latitude),
                        "Longitude" to String.format("%.6f°", e.longitude),
                        "Accuracy" to String.format("%.1f m", e.accuracy)
                    )
                )
            },
            fromSensorEntity = { e, uuid ->
                e as LocationSensor.Entity
                LocationEntity(uuid = uuid ?: "", deviceType = DeviceType.PHONE.value, received = e.received, timestamp = e.timestamp, latitude = e.latitude, longitude = e.longitude, altitude = e.altitude, speed = e.speed, accuracy = e.accuracy)
            }
        ),
        SensorDescriptor(
            sensorId = "Media",
            displayName = "Media",
            isWatchSensor = false,
            store = s.media,
            supabaseTable = AppConfig.SupabaseTables.MEDIA_SENSOR,
            serializer = MediaEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Type" to (e.mimeType ?: "Unknown"), "Name" to (e.fileName?.take(30) ?: "Unknown"))) },
            fromSensorEntity = { e, uuid ->
                e as MediaSensor.Entity
                MediaEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, operation = e.operation, mediaType = e.mediaType, storageType = e.storageType, uri = e.uri, fileName = e.fileName, mimeType = e.mimeType, size = e.size, dateAdded = e.dateAdded, dateModified = e.dateModified)
            }
        ),
        SensorDescriptor(
            sensorId = "MessageLog",
            displayName = "Message Log",
            isWatchSensor = false,
            store = s.messageLog,
            supabaseTable = AppConfig.SupabaseTables.MESSAGE_LOG_SENSOR,
            serializer = MessageLogEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Type" to e.messageType, "Number" to e.number.take(10))) },
            fromSensorEntity = { e, uuid ->
                e as MessageLogSensor.Entity
                MessageLogEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, number = e.number, messageType = e.messageType, contactType = e.contactType)
            }
        ),
        SensorDescriptor(
            sensorId = "Notification",
            displayName = "Notification",
            isWatchSensor = false,
            store = s.notification,
            supabaseTable = AppConfig.SupabaseTables.NOTIFICATION_SENSOR,
            serializer = NotificationEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Package" to e.packageName, "Title" to e.title)) },
            fromSensorEntity = { e, uuid ->
                e as NotificationSensor.Entity
                NotificationEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, packageName = e.packageName, eventType = e.eventType, title = e.title, text = e.text, visibility = e.visibility, category = e.category)
            }
        ),
        SensorDescriptor(
            sensorId = "Screen",
            displayName = "Screen",
            isWatchSensor = false,
            store = s.screen,
            supabaseTable = AppConfig.SupabaseTables.SCREEN_SENSOR,
            serializer = ScreenEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Type" to e.type)) },
            fromSensorEntity = { e, uuid ->
                e as ScreenSensor.Entity
                ScreenEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, type = e.type)
            }
        ),
        SensorDescriptor(
            sensorId = "Step",
            displayName = "Step",
            isWatchSensor = false,
            store = s.step,
            supabaseTable = AppConfig.SupabaseTables.STEP_SENSOR,
            serializer = StepEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Steps" to e.steps.toString())) },
            fromSensorEntity = { e, uuid ->
                e as StepSensor.Entity
                StepEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, startTime = e.startTime, endTime = e.endTime, steps = e.steps)
            }
        ),
        SensorDescriptor(
            sensorId = "UserInteraction",
            displayName = "User Interaction",
            isWatchSensor = false,
            store = s.userInteraction,
            supabaseTable = AppConfig.SupabaseTables.USER_INTERACTION_SENSOR,
            serializer = UserInteractionEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Event" to e.eventType.toString())) },
            fromSensorEntity = { e, uuid ->
                e as UserInteractionSensor.Entity
                UserInteractionEntity(uuid = uuid ?: "", eventId = e.eventId, received = e.received, timestamp = e.timestamp, packageName = e.packageName, className = e.className, eventType = e.eventType, text = e.text)
            }
        ),
        SensorDescriptor(
            sensorId = "WifiScan",
            displayName = "WiFi Scan",
            isWatchSensor = false,
            store = s.wifiScan,
            supabaseTable = AppConfig.SupabaseTables.WIFI_SCAN_SENSOR,
            serializer = WifiScanEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("SSID" to e.ssid, "BSSID" to e.bssid)) },
            fromSensorEntity = { e, uuid ->
                e as WifiScanSensor.Entity
                WifiScanEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, ssid = e.ssid, bssid = e.bssid, frequency = e.frequency, level = e.level)
            }
        ),
        // Watch sensors — written via WatchSensorRepository (BLE), no phone-side store needed
        SensorDescriptor(
            sensorId = "WatchHeartRate",
            displayName = "Heart Rate",
            isWatchSensor = true,
            store = s.watchHeartRate,
            supabaseTable = AppConfig.SupabaseTables.HEART_RATE_SENSOR,
            serializer = WatchHeartRateEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Heart Rate" to "${e.hr} BPM", "Status" to e.hrStatus.toString())) }
        ),
        SensorDescriptor(
            sensorId = "WatchAccelerometer",
            displayName = "Accelerometer",
            isWatchSensor = true,
            store = s.watchAccelerometer,
            supabaseTable = AppConfig.SupabaseTables.ACCELEROMETER_SENSOR,
            serializer = WatchAccelerometerEntity.serializer(),
            toRecord = { e ->
                SensorRecord(
                    id = e.id,
                    timestamp = e.timestamp,
                    fields = mapOf("X" to String.format("%.3f", e.x), "Y" to String.format("%.3f", e.y), "Z" to String.format("%.3f", e.z))
                )
            }
        ),
        SensorDescriptor(
            sensorId = "WatchEDA",
            displayName = "EDA",
            isWatchSensor = true,
            store = s.watchEDA,
            supabaseTable = AppConfig.SupabaseTables.EDA_SENSOR,
            serializer = WatchEDAEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("EDA" to String.format("%.3f μS", e.skinConductance))) }
        ),
        SensorDescriptor(
            sensorId = "WatchPPG",
            displayName = "PPG",
            isWatchSensor = true,
            store = s.watchPPG,
            supabaseTable = AppConfig.SupabaseTables.PPG_SENSOR,
            serializer = WatchPPGEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Green" to e.green.toString(), "IR" to e.ir.toString())) }
        ),
        SensorDescriptor(
            sensorId = "WatchSkinTemperature",
            displayName = "Skin Temperature",
            isWatchSensor = true,
            store = s.watchSkinTemperature,
            supabaseTable = AppConfig.SupabaseTables.SKIN_TEMPERATURE_SENSOR,
            serializer = WatchSkinTemperatureEntity.serializer(),
            toRecord = { e -> SensorRecord(id = e.id, timestamp = e.timestamp, fields = mapOf("Skin Temp" to String.format("%.1f°C", e.objectTemp))) }
        ),
    )
}
