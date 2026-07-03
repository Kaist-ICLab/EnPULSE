package kaist.iclab.mobiletracker.services.upload.handlers

import kaist.iclab.mobiletracker.db.mapper.AccelerometerMapper
import kaist.iclab.mobiletracker.db.mapper.AmbientLightMapper
import kaist.iclab.mobiletracker.db.mapper.AppListChangeMapper
import kaist.iclab.mobiletracker.db.mapper.AppUsageLogMapper
import kaist.iclab.mobiletracker.db.mapper.BatteryMapper
import kaist.iclab.mobiletracker.db.mapper.BluetoothScanMapper
import kaist.iclab.mobiletracker.db.mapper.CallLogMapper
import kaist.iclab.mobiletracker.db.mapper.ConnectivityMapper
import kaist.iclab.mobiletracker.db.mapper.DataTrafficMapper
import kaist.iclab.mobiletracker.db.mapper.DeviceModeMapper
import kaist.iclab.mobiletracker.db.mapper.EDAMapper
import kaist.iclab.mobiletracker.db.mapper.HeartRateMapper
import kaist.iclab.mobiletracker.db.mapper.MediaMapper
import kaist.iclab.mobiletracker.db.mapper.MessageLogMapper
import kaist.iclab.mobiletracker.db.mapper.NotificationMapper
import kaist.iclab.mobiletracker.db.mapper.PPGMapper
import kaist.iclab.mobiletracker.db.mapper.ScreenMapper
import kaist.iclab.mobiletracker.db.mapper.SkinTemperatureMapper
import kaist.iclab.mobiletracker.db.mapper.StepMapper
import kaist.iclab.mobiletracker.db.mapper.UserInteractionMapper
import kaist.iclab.mobiletracker.db.mapper.WifiMapper
import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.services.supabase.SupabaseUploadService
import kaist.iclab.mobiletracker.services.supabase.LocationSensorService
import kaist.iclab.mobiletracker.services.upload.handlers.phone.LocationUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.watch.WatchLocationUploadHandler
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
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> AmbientLightMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.AMBIENT_LIGHT_SENSOR, "Ambient Light", it) },
        csvHeader = "eventId,uuid,received,timestamp,accuracy,value",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.accuracy},${e.value}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "AppListChange",
        store = s.appListChange,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> AppListChangeMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.APP_LIST_CHANGE_SENSOR, "App List Change", it) },
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
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> AppUsageLogMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.APP_USAGE_LOG_SENSOR, "App Usage Log", it) },
        csvHeader = "eventId,uuid,received,timestamp,packageName,installedBy,eventType",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.packageName},${e.installedBy},${e.eventType}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "Battery",
        store = s.battery,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> BatteryMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.BATTERY_SENSOR, "Battery", it) },
        csvHeader = "eventId,uuid,received,timestamp,connectedType,status,level,temperature",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.connectedType},${e.status},${e.level},${e.temperature}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "BluetoothScan",
        store = s.bluetoothScan,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> BluetoothScanMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.BLUETOOTH_SCAN_SENSOR, "Bluetooth Scan", it) },
        csvHeader = "eventId,uuid,received,timestamp,name,alias,address,bondState,connectionType,classType,rssi,isLE",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.name},${e.alias},${e.address},${e.bondState},${e.connectionType},${e.classType},${e.rssi},${e.isLE}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "CallLog",
        store = s.callLog,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> CallLogMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.CALL_LOG_SENSOR, "Call Log", it) },
        csvHeader = "eventId,uuid,received,timestamp,duration,number,type",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.duration},${e.number},${e.type}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "Connectivity",
        store = s.connectivity,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> ConnectivityMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.CONNECTIVITY_SENSOR, "Connectivity", it) },
        csvHeader = "eventId,uuid,received,timestamp,networkType,isConnected,hasInternet,transportTypes",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.networkType},${e.isConnected},${e.hasInternet},${e.transportTypes}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "DataTraffic",
        store = s.dataTraffic,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> DataTrafficMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.DATA_TRAFFIC_SENSOR, "Data Traffic", it) },
        csvHeader = "eventId,uuid,received,timestamp,totalRx,totalTx,mobileRx,mobileTx",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.totalRx},${e.totalTx},${e.mobileRx},${e.mobileTx}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "DeviceMode",
        store = s.deviceMode,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> DeviceModeMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.DEVICE_MODE_SENSOR, "Device Mode", it) },
        csvHeader = "eventId,uuid,received,timestamp,eventType,value",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.eventType},${e.value}" }
    ),
    LocationUploadHandler(store = s.location, service = get<LocationSensorService>()),
    GenericSensorUploadHandler(
        sensorId = "Media",
        store = s.media,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> MediaMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.MEDIA_SENSOR, "Media", it) },
        csvHeader = "eventId,uuid,received,timestamp,operation,mediaType,storageType,uri,fileName,mimeType,size,dateAdded,dateModified",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.operation},${e.mediaType},${e.storageType},${e.uri},${e.fileName},${e.mimeType},${e.size},${e.dateAdded},${e.dateModified}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "MessageLog",
        store = s.messageLog,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> MessageLogMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.MESSAGE_LOG_SENSOR, "Message Log", it) },
        csvHeader = "eventId,uuid,received,timestamp,number,messageType,contactType",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.number},${e.messageType},${e.contactType}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "Notification",
        store = s.notification,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> NotificationMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.NOTIFICATION_SENSOR, "Notification", it) },
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
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> ScreenMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.SCREEN_SENSOR, "Screen", it) },
        csvHeader = "eventId,uuid,received,timestamp,type",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.type}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "Step",
        store = s.step,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> StepMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.STEP_SENSOR, "Step", it) },
        csvHeader = "eventId,uuid,received,timestamp,startTime,endTime,steps",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.startTime},${e.endTime},${e.steps}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "UserInteraction",
        store = s.userInteraction,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> UserInteractionMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.USER_INTERACTION_SENSOR, "User Interaction", it) },
        csvHeader = "eventId,uuid,received,timestamp,packageName,className,eventType,text",
        toCsvRow = { e ->
            val escapedText = e.text.replace("\"", "\"\"")
            "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.packageName},${e.className},${e.eventType},\"$escapedText\""
        }
    ),
    GenericSensorUploadHandler(
        sensorId = "WifiScan",
        store = s.wifiScan,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> WifiMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.WIFI_SCAN_SENSOR, "Wifi", it) },
        csvHeader = "eventId,uuid,received,timestamp,ssid,bssid,frequency,level",
        toCsvRow = { e ->
            val escapedSsid = e.ssid.replace("\"", "\"\"")
            "${e.eventId},${e.uuid},${e.received},${e.timestamp},\"$escapedSsid\",${e.bssid},${e.frequency},${e.level}"
        }
    )
)

fun Scope.buildWatchUploadHandlers(s: SensorStores): List<SensorUploadHandler> = listOf(
    GenericSensorUploadHandler(
        sensorId = "WatchHeartRate",
        store = s.watchHeartRate,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> HeartRateMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.HEART_RATE_SENSOR, "heart rate", it) },
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
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> AccelerometerMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.ACCELEROMETER_SENSOR, "accelerometer", it) },
        csvHeader = "eventId,uuid,received,timestamp,x,y,z",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.x},${e.y},${e.z}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "WatchEDA",
        store = s.watchEDA,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> EDAMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.EDA_SENSOR, "EDA", it) },
        csvHeader = "eventId,uuid,received,timestamp,skinConductance,status",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.skinConductance},${e.status}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "WatchPPG",
        store = s.watchPPG,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> PPGMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.PPG_SENSOR, "PPG", it) },
        csvHeader = "eventId,uuid,received,timestamp,green,greenStatus,red,redStatus,ir,irStatus",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.green},${e.greenStatus},${e.red},${e.redStatus},${e.ir},${e.irStatus}" }
    ),
    GenericSensorUploadHandler(
        sensorId = "WatchSkinTemperature",
        store = s.watchSkinTemperature,
        timestampOf = { it.timestamp },
        toSupabase = { e, uuid -> SkinTemperatureMapper.map(e, uuid) },
        uploadBatch = { get<SupabaseUploadService>().upsertBatch(AppConfig.SupabaseTables.SKIN_TEMPERATURE_SENSOR, "skin temperature", it) },
        csvHeader = "eventId,uuid,received,timestamp,ambientTemp,objectTemp,status",
        toCsvRow = { e -> "${e.eventId},${e.uuid},${e.received},${e.timestamp},${e.ambientTemp},${e.objectTemp},${e.status}" }
    ),
    WatchLocationUploadHandler(store = s.location, service = get<LocationSensorService>())
)
