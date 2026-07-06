package kaist.iclab.mobiletracker.repository.handlers

import kaist.iclab.mobiletracker.config.AppConfig
import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.repository.SensorRecord

/**
 * Builds the full list of [SensorDataHandler]s, one [GenericSensorDataHandler] per sensor. Each
 * entry supplies only the sensor's metadata and its entity -> [SensorRecord] display projection,
 * copied verbatim from the retired per-sensor `*DataHandler` classes.
 */
fun buildSensorDataHandlers(s: SensorStores): List<SensorDataHandler> = listOf(
    GenericSensorDataHandler(
        sensorId = "Battery",
        displayName = "Battery",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.BATTERY_SENSOR,
        store = s.battery
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Level" to "${e.level}%",
                "Status" to e.status.toString()
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "AmbientLight",
        displayName = "Ambient Light",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.AMBIENT_LIGHT_SENSOR,
        store = s.ambientLight
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Value" to String.format("%.1f lux", e.value)
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "AppListChange",
        displayName = "App List Change",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.APP_LIST_CHANGE_SENSOR,
        store = s.appListChange
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Changed" to (e.changedAppJson?.take(50) ?: "N/A")
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "AppUsage",
        displayName = "App Usage",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.APP_USAGE_LOG_SENSOR,
        store = s.appUsageLog
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Package" to e.packageName,
                "Event" to e.eventType.toString()
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "BluetoothScan",
        displayName = "Bluetooth Scan",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.BLUETOOTH_SCAN_SENSOR,
        store = s.bluetoothScan
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Name" to e.name,
                "Address" to e.address
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "CallLog",
        displayName = "Call Log",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.CALL_LOG_SENSOR,
        store = s.callLog
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Type" to e.type.toString(),
                "Duration" to "${e.duration}s"
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "Connectivity",
        displayName = "Connectivity",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.CONNECTIVITY_SENSOR,
        store = s.connectivity
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Network" to e.networkType,
                "Connected" to e.isConnected.toString()
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "DataTraffic",
        displayName = "Data Traffic",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.DATA_TRAFFIC_SENSOR,
        store = s.dataTraffic
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Total Rx" to "${e.totalRx / 1024} KB",
                "Total Tx" to "${e.totalTx / 1024} KB"
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "DeviceMode",
        displayName = "Device Mode",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.DEVICE_MODE_SENSOR,
        store = s.deviceMode
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Event" to e.eventType,
                "Value" to e.value
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "Location",
        displayName = "Location",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.LOCATION_SENSOR,
        store = s.location
    ) { e ->
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
    GenericSensorDataHandler(
        sensorId = "Media",
        displayName = "Media",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.MEDIA_SENSOR,
        store = s.media
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Type" to (e.mimeType ?: "Unknown"),
                "Name" to (e.fileName?.take(30) ?: "Unknown")
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "MessageLog",
        displayName = "Message Log",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.MESSAGE_LOG_SENSOR,
        store = s.messageLog
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Type" to e.messageType,
                "Number" to e.number.take(10)
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "Notification",
        displayName = "Notification",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.NOTIFICATION_SENSOR,
        store = s.notification
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Package" to e.packageName,
                "Title" to e.title
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "Screen",
        displayName = "Screen",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.SCREEN_SENSOR,
        store = s.screen
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Type" to e.type
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "Step",
        displayName = "Step",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.STEP_SENSOR,
        store = s.step
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Steps" to e.steps.toString()
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "UserInteraction",
        displayName = "User Interaction",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.USER_INTERACTION_SENSOR,
        store = s.userInteraction
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Event" to e.eventType.toString()
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "WifiScan",
        displayName = "WiFi Scan",
        isWatchSensor = false,
        supabaseTableName = AppConfig.SupabaseTables.WIFI_SCAN_SENSOR,
        store = s.wifiScan
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "SSID" to e.ssid,
                "BSSID" to e.bssid
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "WatchHeartRate",
        displayName = "Heart Rate",
        isWatchSensor = true,
        supabaseTableName = AppConfig.SupabaseTables.HEART_RATE_SENSOR,
        store = s.watchHeartRate
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Heart Rate" to "${e.hr} BPM",
                "Status" to e.hrStatus.toString()
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "WatchAccelerometer",
        displayName = "Accelerometer",
        isWatchSensor = true,
        supabaseTableName = AppConfig.SupabaseTables.ACCELEROMETER_SENSOR,
        store = s.watchAccelerometer
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "X" to String.format("%.3f", e.x),
                "Y" to String.format("%.3f", e.y),
                "Z" to String.format("%.3f", e.z)
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "WatchEDA",
        displayName = "EDA",
        isWatchSensor = true,
        supabaseTableName = AppConfig.SupabaseTables.EDA_SENSOR,
        store = s.watchEDA
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "EDA" to String.format("%.3f μS", e.skinConductance)
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "WatchPPG",
        displayName = "PPG",
        isWatchSensor = true,
        supabaseTableName = AppConfig.SupabaseTables.PPG_SENSOR,
        store = s.watchPPG
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Green" to e.green.toString(),
                "IR" to e.ir.toString()
            )
        )
    },
    GenericSensorDataHandler(
        sensorId = "WatchSkinTemperature",
        displayName = "Skin Temperature",
        isWatchSensor = true,
        supabaseTableName = AppConfig.SupabaseTables.SKIN_TEMPERATURE_SENSOR,
        store = s.watchSkinTemperature
    ) { e ->
        SensorRecord(
            id = e.id,
            timestamp = e.timestamp,
            fields = mapOf(
                "Skin Temp" to String.format("%.1f°C", e.objectTemp)
            )
        )
    }
)
