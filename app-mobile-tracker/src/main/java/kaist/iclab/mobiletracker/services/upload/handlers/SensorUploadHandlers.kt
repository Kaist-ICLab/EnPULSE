package kaist.iclab.mobiletracker.services.upload.handlers

import kaist.iclab.mobiletracker.config.AppConfig
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
import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.services.supabase.SupabaseUploadService
import kaist.iclab.mobiletracker.services.upload.handlers.phone.LocationUploadHandler
import org.koin.core.scope.Scope
import org.koin.core.scope.get

/**
 * Builder functions that assemble the sensor upload handler lists from a [SensorStores] instance.
 * Every homogeneous per-sensor handler is expressed as a [GenericSensorUploadHandler] entry
 * (mapper + service batch call + CSV shape), while the two deviceType-special Location handlers
 * stay as concrete classes.
 *
 * Declared as extensions on Koin's [Scope] so services can be resolved via [get].
 */
fun Scope.buildPhoneUploadHandlers(s: SensorStores): List<SensorUploadHandler> = listOf(
    GenericSensorUploadHandler(
        sensorId = "AmbientLight",
        store = s.ambientLight,
        serializer = AmbientLightEntity.serializer(),
        tableName = AppConfig.SupabaseTables.AMBIENT_LIGHT_SENSOR,
        sensorName = "Ambient Light",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,accuracy,value",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.accuracy},${e.value}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "AppListChange",
        store = s.appListChange,
        serializer = AppListChangeEntity.serializer(),
        tableName = AppConfig.SupabaseTables.APP_LIST_CHANGE_SENSOR,
        sensorName = "App List Change",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,changedAppJson,appListJson",
        toCsvRow = { e ->
            val escapedChangedApp = e.changedAppJson?.replace("\"", "\"\"") ?: ""
            val escapedAppList = e.appListJson?.replace("\"", "\"\"") ?: ""
            "${e.eventId},${e.uuid},${e.received},${e.timestamp},\"$escapedChangedApp\",\"$escapedAppList\""
        }
    ),
    GenericSensorUploadHandler(
        sensorId = "AppUsage",
        store = s.appUsageLog,
        serializer = AppUsageLogEntity.serializer(),
        tableName = AppConfig.SupabaseTables.APP_USAGE_LOG_SENSOR,
        sensorName = "App Usage Log",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,packageName,installedBy,eventType",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.packageName},${e.installedBy},${e.eventType}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "Battery",
        store = s.battery,
        serializer = BatteryEntity.serializer(),
        tableName = AppConfig.SupabaseTables.BATTERY_SENSOR,
        sensorName = "Battery",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,connectedType,status,level,temperature",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.connectedType},${e.status},${e.level},${e.temperature}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "BluetoothScan",
        store = s.bluetoothScan,
        serializer = BluetoothScanEntity.serializer(),
        tableName = AppConfig.SupabaseTables.BLUETOOTH_SCAN_SENSOR,
        sensorName = "Bluetooth Scan",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,name,alias,address,bondState,connectionType,classType,rssi,isLE",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.name},${e.alias},${e.address},${e.bondState},${e.connectionType},${e.classType},${e.rssi},${e.isLE}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "CallLog",
        store = s.callLog,
        serializer = CallLogEntity.serializer(),
        tableName = AppConfig.SupabaseTables.CALL_LOG_SENSOR,
        sensorName = "Call Log",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,duration,number,type",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.duration},${e.number},${e.type}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "Connectivity",
        store = s.connectivity,
        serializer = ConnectivityEntity.serializer(),
        tableName = AppConfig.SupabaseTables.CONNECTIVITY_SENSOR,
        sensorName = "Connectivity",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,networkType,isConnected,hasInternet,transportTypes",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.networkType},${e.isConnected},${e.hasInternet},${e.transportTypes}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "DataTraffic",
        store = s.dataTraffic,
        serializer = DataTrafficEntity.serializer(),
        tableName = AppConfig.SupabaseTables.DATA_TRAFFIC_SENSOR,
        sensorName = "Data Traffic",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,totalRx,totalTx,mobileRx,mobileTx",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.totalRx},${e.totalTx},${e.mobileRx},${e.mobileTx}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "DeviceMode",
        store = s.deviceMode,
        serializer = DeviceModeEntity.serializer(),
        tableName = AppConfig.SupabaseTables.DEVICE_MODE_SENSOR,
        sensorName = "Device Mode",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,eventType,value",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.eventType},${e.value}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "Media",
        store = s.media,
        serializer = MediaEntity.serializer(),
        tableName = AppConfig.SupabaseTables.MEDIA_SENSOR,
        sensorName = "Media",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,operation,mediaType,storageType,uri,fileName,mimeType,size,dateAdded,dateModified",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.operation},${e.mediaType},${e.storageType},${e.uri},${e.fileName},${e.mimeType},${e.size},${e.dateAdded},${e.dateModified}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "MessageLog",
        store = s.messageLog,
        serializer = MessageLogEntity.serializer(),
        tableName = AppConfig.SupabaseTables.MESSAGE_LOG_SENSOR,
        sensorName = "Message Log",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,number,messageType,contactType",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.number},${e.messageType},${e.contactType}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "Notification",
        store = s.notification,
        serializer = NotificationEntity.serializer(),
        tableName = AppConfig.SupabaseTables.NOTIFICATION_SENSOR,
        sensorName = "Notification",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,packageName,eventType,title,text,visibility,category",
        toCsvRow = { e ->
            val escapedTitle = e.title.replace("\"", "\"\"")
            val escapedText = e.text.replace("\"", "\"\"")
            "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.packageName},${e.eventType},\"$escapedTitle\",\"$escapedText\",${e.visibility},${e.category}"
        }
    ),
    GenericSensorUploadHandler(
        sensorId = "Screen",
        store = s.screen,
        serializer = ScreenEntity.serializer(),
        tableName = AppConfig.SupabaseTables.SCREEN_SENSOR,
        sensorName = "Screen",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,type",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.type}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "Step",
        store = s.step,
        serializer = StepEntity.serializer(),
        tableName = AppConfig.SupabaseTables.STEP_SENSOR,
        sensorName = "Step",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,startTime,endTime,steps",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.startTime},${e.endTime},${e.steps}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "UserInteraction",
        store = s.userInteraction,
        serializer = UserInteractionEntity.serializer(),
        tableName = AppConfig.SupabaseTables.USER_INTERACTION_SENSOR,
        sensorName = "User Interaction",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,packageName,className,eventType,text",
        toCsvRow = { e ->
            val escapedText = e.text.replace("\"", "\"\"")
            "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.packageName},${e.className},${e.eventType},\"$escapedText\""
        }
    ),
    GenericSensorUploadHandler(
        sensorId = "WifiScan",
        store = s.wifiScan,
        serializer = WifiScanEntity.serializer(),
        tableName = AppConfig.SupabaseTables.WIFI_SCAN_SENSOR,
        sensorName = "Wifi",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,ssid,bssid,frequency,level",
        toCsvRow = { e ->
            val escapedSsid = e.ssid.replace("\"", "\"\"")
            "${e.eventId},${e.uuid},${e.received},${e.timestamp},\"$escapedSsid\",${e.bssid},${e.frequency},${e.level}"
        }
    ),
    GenericSensorUploadHandler(
        sensorId = "Location",
        store = s.location,
        serializer = LocationEntity.serializer(),
        tableName = AppConfig.SupabaseTables.LOCATION_SENSOR,
        sensorName = "location",
        supabase = get(),
        csvHeader = "eventId,uuid,deviceType,received,timestamp,latitude,longitude,altitude,speed,accuracy",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.deviceType},${e.received},${e.timestamp},${e.latitude},${e.longitude},${e.altitude},${e.speed},${e.accuracy}" }
    )
)

fun Scope.buildWatchUploadHandlers(s: SensorStores): List<SensorUploadHandler> = listOf(
    GenericSensorUploadHandler(
        sensorId = "WatchHeartRate",
        store = s.watchHeartRate,
        serializer = WatchHeartRateEntity.serializer(),
        tableName = AppConfig.SupabaseTables.HEART_RATE_SENSOR,
        sensorName = "heart rate",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,hr,hrStatus,ibi,ibiStatus",
        toCsvRow = { e ->
            val escapedIbi = e.ibi.joinToString(",").replace("\"", "\"\"")
            val escapedIbiStatus = e.ibiStatus.joinToString(",").replace("\"", "\"\"")
            "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.hr},${e.hrStatus},\"[$escapedIbi]\",\"[$escapedIbiStatus]\""
        }
    ),
    GenericSensorUploadHandler(
        sensorId = "WatchAccelerometer",
        store = s.watchAccelerometer,
        serializer = WatchAccelerometerEntity.serializer(),
        tableName = AppConfig.SupabaseTables.ACCELEROMETER_SENSOR,
        sensorName = "accelerometer",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,x,y,z",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.x},${e.y},${e.z}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "WatchEDA",
        store = s.watchEDA,
        serializer = WatchEDAEntity.serializer(),
        tableName = AppConfig.SupabaseTables.EDA_SENSOR,
        sensorName = "EDA",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,skinConductance,status",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.skinConductance},${e.status}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "WatchPPG",
        store = s.watchPPG,
        serializer = WatchPPGEntity.serializer(),
        tableName = AppConfig.SupabaseTables.PPG_SENSOR,
        sensorName = "PPG",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,green,greenStatus,red,redStatus,ir,irStatus",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.green},${e.greenStatus},${e.red},${e.redStatus},${e.ir},${e.irStatus}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "WatchSkinTemperature",
        store = s.watchSkinTemperature,
        serializer = WatchSkinTemperatureEntity.serializer(),
        tableName = AppConfig.SupabaseTables.SKIN_TEMPERATURE_SENSOR,
        sensorName = "skin temperature",
        supabase = get(),
        csvHeader = "eventId,uuid,received,timestamp,ambientTemp,objectTemp,status",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.ambientTemp},${e.objectTemp},${e.status}" }
    ),
)
