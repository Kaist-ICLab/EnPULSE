package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.entity.phone.ActivityRecognitionEntity
import kaist.iclab.mobiletracker.db.entity.phone.AmbientLightEntity
import kaist.iclab.mobiletracker.db.entity.phone.AppListChangeEntity
import kaist.iclab.mobiletracker.db.entity.phone.AppUsageLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.BatteryEntity
import kaist.iclab.mobiletracker.db.entity.phone.BluetoothScanEntity
import kaist.iclab.mobiletracker.db.entity.phone.CallLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.ConnectivityEntity
import kaist.iclab.mobiletracker.db.entity.phone.DataTrafficEntity
import kaist.iclab.mobiletracker.db.entity.phone.DeviceModeEntity
import kaist.iclab.mobiletracker.db.entity.phone.ExerciseEntity
import kaist.iclab.mobiletracker.db.entity.phone.MediaEntity
import kaist.iclab.mobiletracker.db.entity.phone.MessageLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.NotificationEntity
import kaist.iclab.mobiletracker.db.entity.phone.ScreenEntity
import kaist.iclab.mobiletracker.db.entity.phone.SleepEntity
import kaist.iclab.mobiletracker.db.entity.phone.StepEntity
import kaist.iclab.mobiletracker.db.entity.phone.UserInteractionEntity
import kaist.iclab.mobiletracker.db.entity.phone.WebAppLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.WifiScanEntity
import kaist.iclab.mobiletracker.db.entity.watch.AccelerometerEntity
import kaist.iclab.mobiletracker.db.entity.watch.ECGEntity
import kaist.iclab.mobiletracker.db.entity.watch.EDAEntity
import kaist.iclab.mobiletracker.db.entity.watch.HeartRateEntity
import kaist.iclab.mobiletracker.db.entity.watch.PPGEntity
import kaist.iclab.mobiletracker.db.entity.watch.SkinTemperatureEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchGestureEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchIMUEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchStressEntity
import kaist.iclab.mobiletracker.db.obx.PhoneSensorStore
import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.db.obx.SupabaseJson
import kaist.iclab.mobiletracker.repository.handlers.GenericSensorDataHandler
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandler
import kaist.iclab.mobiletracker.services.supabase.SupabaseUploadService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerImpl
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandler
import kaist.iclab.tracker.sensor.common.ActivityRecognitionSensor
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
import kaist.iclab.tracker.sensor.phone.ExerciseSensor
import kaist.iclab.tracker.sensor.phone.MediaSensor
import kaist.iclab.tracker.sensor.phone.MessageLogSensor
import kaist.iclab.tracker.sensor.phone.NotificationSensor
import kaist.iclab.tracker.sensor.phone.ScreenSensor
import kaist.iclab.tracker.sensor.phone.SleepSensor
import kaist.iclab.tracker.sensor.phone.StepSensor
import kaist.iclab.tracker.sensor.phone.UserInteractionSensor
import kaist.iclab.tracker.sensor.phone.WifiScanSensor
import kaist.iclab.mobiletracker.webapp.bridge.WebAppLogRecord
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
    val fromSensorEntity: ((SensorEntity, String?) -> T)? = null
) where T : BaseEntity, T : CsvSerializable, T : RecordSerializable {
    fun toDataHandler(): SensorDataHandler = GenericSensorDataHandler(
        sensorId = sensorId,
        displayName = displayName,
        isWatchSensor = isWatchSensor,
        supabaseTableName = supabaseTable,
        store = store,
        serializer = serializer
    )

    fun toUploadHandler(supabase: SupabaseUploadService): SensorUploadHandler = SensorUploadHandlerImpl(
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
    return listOf(
        SensorDescriptor(
            sensorId = "ActivityRecognition",
            displayName = "Activity Recognition",
            isWatchSensor = false,
            store = s.activityRecognition,
            supabaseTable = AppConfig.SupabaseTables.ACTIVITY_RECOGNITION_SENSOR,
            serializer = ActivityRecognitionEntity.serializer(),
            fromSensorEntity = { e, uuid ->
                e as ActivityRecognitionSensor.Entity
                ActivityRecognitionEntity(
                    uuid = uuid ?: "",
                    received = e.received,
                    timestamp = e.timestamp,
                    elapsedRealtimeMillis = e.elapsedRealtimeMillis,
                    activityType = e.activityType,
                    score = e.score,
                    probabilities = e.probabilities.toIntArray()
                )
            }
        ),
        SensorDescriptor(
            sensorId = "AmbientLight",
            displayName = "Ambient Light",
            isWatchSensor = false,
            store = s.ambientLight,
            supabaseTable = AppConfig.SupabaseTables.AMBIENT_LIGHT_SENSOR,
            serializer = AmbientLightEntity.serializer(),
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
            fromSensorEntity = { e, uuid ->
                e as AppListChangeSensor.Entity
                AppListChangeEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, changedAppJson = e.changedApp?.let { SupabaseJson.encodeToString(it) }, appListJson = e.appList?.let { SupabaseJson.encodeToString(it) })
            }
        ),
        SensorDescriptor(
            sensorId = "AppUsage",
            displayName = "App Usage",
            isWatchSensor = false,
            store = s.appUsageLog,
            supabaseTable = AppConfig.SupabaseTables.APP_USAGE_LOG_SENSOR,
            serializer = AppUsageLogEntity.serializer(),
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
            fromSensorEntity = { e, uuid ->
                e as DeviceModeSensor.Entity
                DeviceModeEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, eventType = e.eventType, value = e.value)
            }
        ),
        SensorDescriptor(
            sensorId = "Exercise",
            displayName = "Exercise",
            isWatchSensor = false,
            store = s.exercise,
            supabaseTable = AppConfig.SupabaseTables.EXERCISE_SENSOR,
            serializer = ExerciseEntity.serializer(),
            fromSensorEntity = { e, uuid ->
                e as ExerciseSensor.Entity
                ExerciseEntity(
                    uuid = uuid ?: "",
                    received = e.received,
                    timestamp = e.timestamp,
                    duration = e.duration,
                    exerciseType = e.exerciseType,
                    customTitle = e.customTitle,
                    calories = e.calories,
                    distance = e.distance,
                    count = e.count,
                    meanHeartRate = e.meanHeartRate,
                    maxHeartRate = e.maxHeartRate,
                    minHeartRate = e.minHeartRate,
                    altitudeGain = e.altitudeGain,
                    altitudeLoss = e.altitudeLoss,
                    meanCadence = e.meanCadence,
                    maxCadence = e.maxCadence,
                    meanPower = e.meanPower,
                    maxPower = e.maxPower,
                    meanSpeed = e.meanSpeed,
                    maxSpeed = e.maxSpeed,
                    meanRpm = e.meanRpm,
                    maxRpm = e.maxRpm
                )
            }
        ),
        SensorDescriptor(
            sensorId = "Location",
            displayName = "Location",
            isWatchSensor = false,
            store = s.location,
            supabaseTable = AppConfig.SupabaseTables.LOCATION_SENSOR,
            serializer = LocationEntity.serializer(),
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
            fromSensorEntity = { e, uuid ->
                e as ScreenSensor.Entity
                ScreenEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, type = e.type)
            }
        ),
        SensorDescriptor(
            sensorId = "Sleep",
            displayName = "Sleep",
            isWatchSensor = false,
            store = s.sleep,
            supabaseTable = AppConfig.SupabaseTables.SLEEP_SENSOR,
            serializer = SleepEntity.serializer(),
            fromSensorEntity = { e, uuid ->
                e as SleepSensor.Entity
                SleepEntity(
                    uuid = uuid ?: "",
                    received = e.received,
                    timestamp = e.timestamp,
                    duration = e.duration,
                    sleepScore = e.sleepScore,
                    stagesJson = SupabaseJson.encodeToString(e.stages)
                )
            }
        ),
        SensorDescriptor(
            sensorId = "Step",
            displayName = "Step",
            isWatchSensor = false,
            store = s.step,
            supabaseTable = AppConfig.SupabaseTables.STEP_SENSOR,
            serializer = StepEntity.serializer(),
            fromSensorEntity = { e, uuid ->
                e as StepSensor.Entity
                StepEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, duration = e.duration, steps = e.steps)
            }
        ),
        SensorDescriptor(
            sensorId = "UserInteraction",
            displayName = "User Interaction",
            isWatchSensor = false,
            store = s.userInteraction,
            supabaseTable = AppConfig.SupabaseTables.USER_INTERACTION_SENSOR,
            serializer = UserInteractionEntity.serializer(),
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
            fromSensorEntity = { e, uuid ->
                e as WifiScanSensor.Entity
                WifiScanEntity(uuid = uuid ?: "", received = e.received, timestamp = e.timestamp, ssid = e.ssid, bssid = e.bssid, frequency = e.frequency, level = e.level)
            }
        ),
        // Not a real sensor — a generic event logged by a webapp via the `logEvent` bridge action
        // (LogBridgeHandler). fromSensorEntity converts the WebAppLogRecord the handler builds
        // straight from the JS payload, same as every other sensor's entity conversion above.
        SensorDescriptor(
            sensorId = "WebAppLog",
            displayName = "WebApp Log",
            isWatchSensor = false,
            store = s.webAppLog,
            supabaseTable = AppConfig.SupabaseTables.WEB_APP_LOG_SENSOR,
            serializer = WebAppLogEntity.serializer(),
            fromSensorEntity = { e, uuid ->
                e as WebAppLogRecord
                WebAppLogEntity(uuid = uuid ?: "", received = System.currentTimeMillis(), timestamp = e.timestamp, webAppId = e.webAppId, eventName = e.eventName, propertiesJson = e.propertiesJson)
            }
        ),
        // Watch sensors — written via WatchSensorRepository (BLE), no phone-side store needed
        SensorDescriptor(
            sensorId = "HeartRate",
            displayName = "Heart Rate",
            isWatchSensor = true,
            store = s.watchHeartRate,
            supabaseTable = AppConfig.SupabaseTables.HEART_RATE_SENSOR,
            serializer = HeartRateEntity.serializer()
        ),
        SensorDescriptor(
            sensorId = "Accelerometer",
            displayName = "Accelerometer",
            isWatchSensor = true,
            store = s.watchAccelerometer,
            supabaseTable = AppConfig.SupabaseTables.ACCELEROMETER_SENSOR,
            serializer = AccelerometerEntity.serializer()
        ),
        SensorDescriptor(
            sensorId = "EDA",
            displayName = "EDA",
            isWatchSensor = true,
            store = s.watchEDA,
            supabaseTable = AppConfig.SupabaseTables.EDA_SENSOR,
            serializer = EDAEntity.serializer()
        ),
        SensorDescriptor(
            sensorId = "PPG",
            displayName = "PPG",
            isWatchSensor = true,
            store = s.watchPPG,
            supabaseTable = AppConfig.SupabaseTables.PPG_SENSOR,
            serializer = PPGEntity.serializer()
        ),
        SensorDescriptor(
            sensorId = "SkinTemperature",
            displayName = "Skin Temperature",
            isWatchSensor = true,
            store = s.watchSkinTemperature,
            supabaseTable = AppConfig.SupabaseTables.SKIN_TEMPERATURE_SENSOR,
            serializer = SkinTemperatureEntity.serializer()
        ),
        SensorDescriptor(
            sensorId = "ECG",
            displayName = "ECG",
            isWatchSensor = true,
            store = s.ecg,
            supabaseTable = AppConfig.SupabaseTables.ECG_SENSOR,
            serializer = ECGEntity.serializer()
        ),
        SensorDescriptor(
            sensorId = "IMU",
            displayName = "IMU",
            isWatchSensor = true,
            store = s.watchIMU,
            supabaseTable = AppConfig.SupabaseTables.IMU_SENSOR,
            serializer = WatchIMUEntity.serializer()
        ),
        SensorDescriptor(
            sensorId = "Gesture",
            displayName = "Gesture",
            isWatchSensor = true,
            store = s.watchGesture,
            supabaseTable = AppConfig.SupabaseTables.GESTURE_SENSOR,
            serializer = WatchGestureEntity.serializer()
        ),
        SensorDescriptor(
            sensorId = "Stress",
            displayName = "Stress",
            isWatchSensor = true,
            store = s.watchStress,
            supabaseTable = AppConfig.SupabaseTables.STRESS_SENSOR,
            serializer = WatchStressEntity.serializer()
        ),
    )
}
