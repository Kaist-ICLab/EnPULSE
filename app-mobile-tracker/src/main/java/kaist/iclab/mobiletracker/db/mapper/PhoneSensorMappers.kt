package kaist.iclab.mobiletracker.db.mapper

import java.time.Instant
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.data.sensors.phone.AmbientLightSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.BatterySensorData
import kaist.iclab.mobiletracker.data.sensors.phone.BluetoothScanSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.CallLogSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.MessageLogSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.UserInteractionSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.DataTrafficSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.MediaSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.StepSensorData
import kaist.iclab.mobiletracker.data.sensors.common.LocationSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.DeviceModeSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.ScreenSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.AppListChangeSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.AppUsageLogSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.NotificationSensorData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kaist.iclab.mobiletracker.data.sensors.phone.WifiScanSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.ConnectivitySensorData
import kaist.iclab.mobiletracker.db.entity.phone.AmbientLightEntity
import kaist.iclab.mobiletracker.db.entity.phone.AppListChangeEntity
import kaist.iclab.mobiletracker.db.entity.phone.AppUsageLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.BatteryEntity
import kaist.iclab.mobiletracker.db.entity.phone.BluetoothScanEntity
import kaist.iclab.mobiletracker.db.entity.phone.CallLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.MessageLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.UserInteractionEntity
import kaist.iclab.mobiletracker.db.entity.phone.DataTrafficEntity
import kaist.iclab.mobiletracker.db.entity.phone.MediaEntity
import kaist.iclab.mobiletracker.db.entity.phone.StepEntity
import kaist.iclab.mobiletracker.db.entity.phone.DeviceModeEntity
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.entity.phone.ScreenEntity
import kaist.iclab.mobiletracker.db.entity.phone.WifiScanEntity
import kaist.iclab.mobiletracker.db.entity.phone.ConnectivityEntity
import kaist.iclab.mobiletracker.db.entity.phone.NotificationEntity

object AmbientLightMapper : EntityToSupabaseMapper<AmbientLightEntity, AmbientLightSensorData> {
    override fun map(entity: AmbientLightEntity, userUuid: String?): AmbientLightSensorData {
        return AmbientLightSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = DeviceType.PHONE.value,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            value = entity.value,
            received = Instant.ofEpochMilli(entity.received).toString(),
            accuracy = entity.accuracy
        )
    }
}

object BatteryMapper : EntityToSupabaseMapper<BatteryEntity, BatterySensorData> {
    override fun map(entity: BatteryEntity, userUuid: String?): BatterySensorData {
        return BatterySensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = DeviceType.PHONE.value,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            level = entity.level,
            connectedType = entity.connectedType,
            status = entity.status,
            temperature = entity.temperature,
            received = Instant.ofEpochMilli(entity.received).toString()
        )
    }
}

object CallLogMapper : EntityToSupabaseMapper<CallLogEntity, CallLogSensorData> {
    override fun map(entity: CallLogEntity, userUuid: String?): CallLogSensorData {
        return CallLogSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            received = Instant.ofEpochMilli(entity.received).toString(),
            deviceType = DeviceType.PHONE.value,
            duration = entity.duration,
            number = entity.number,
            type = entity.type
        )
    }
}

object MessageLogMapper : EntityToSupabaseMapper<MessageLogEntity, MessageLogSensorData> {
    override fun map(entity: MessageLogEntity, userUuid: String?): MessageLogSensorData {
        return MessageLogSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            received = Instant.ofEpochMilli(entity.received).toString(),
            deviceType = DeviceType.PHONE.value,
            number = entity.number,
            messageType = entity.messageType,
            contactType = entity.contactType
        )
    }
}

object UserInteractionMapper : EntityToSupabaseMapper<UserInteractionEntity, UserInteractionSensorData> {
    override fun map(entity: UserInteractionEntity, userUuid: String?): UserInteractionSensorData {
        return UserInteractionSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            received = Instant.ofEpochMilli(entity.received).toString(),
            deviceType = DeviceType.PHONE.value,
            packageName = entity.packageName,
            className = entity.className,
            eventType = entity.eventType,
            text = entity.text
        )
    }
}

object BluetoothScanMapper : EntityToSupabaseMapper<BluetoothScanEntity, BluetoothScanSensorData> {
    override fun map(entity: BluetoothScanEntity, userUuid: String?): BluetoothScanSensorData {
        return BluetoothScanSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = DeviceType.PHONE.value,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            name = entity.name,
            alias = entity.alias,
            address = entity.address,
            bondState = entity.bondState,
            connectionType = entity.connectionType,
            classType = entity.classType,
            rssi = entity.rssi,
            isLE = entity.isLE,
            received = Instant.ofEpochMilli(entity.received).toString()
        )
    }
}

object DataTrafficMapper : EntityToSupabaseMapper<DataTrafficEntity, DataTrafficSensorData> {
    override fun map(entity: DataTrafficEntity, userUuid: String?): DataTrafficSensorData {
        return DataTrafficSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            received = Instant.ofEpochMilli(entity.received).toString(),
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            deviceType = DeviceType.PHONE.value,
            totalRx = entity.totalRx,
            totalTx = entity.totalTx,
            mobileRx = entity.mobileRx,
            mobileTx = entity.mobileTx
        )
    }
}

object MediaMapper : EntityToSupabaseMapper<MediaEntity, MediaSensorData> {
    override fun map(entity: MediaEntity, userUuid: String?): MediaSensorData {
        return MediaSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            received = Instant.ofEpochMilli(entity.received).toString(),
            deviceType = DeviceType.PHONE.value,
            operation = entity.operation,
            mediaType = entity.mediaType,
            storageType = entity.storageType,
            uri = entity.uri,
            fileName = entity.fileName,
            mimeType = entity.mimeType,
            size = entity.size,
            dateAdded = entity.dateAdded?.let { Instant.ofEpochMilli(it).toString() },
            dateModified = entity.dateModified?.let { Instant.ofEpochMilli(it).toString() }
        )
    }
}

object StepMapper : EntityToSupabaseMapper<StepEntity, StepSensorData> {
    override fun map(entity: StepEntity, userUuid: String?): StepSensorData {
        return StepSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            received = Instant.ofEpochMilli(entity.received).toString(),
            deviceType = DeviceType.PHONE.value,
            startTime = entity.startTime,
            endTime = entity.endTime,
            steps = entity.steps
        )
    }
}

object DeviceModeMapper : EntityToSupabaseMapper<DeviceModeEntity, DeviceModeSensorData> {
    override fun map(entity: DeviceModeEntity, userUuid: String?): DeviceModeSensorData {
        return DeviceModeSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            received = Instant.ofEpochMilli(entity.received).toString(),
            deviceType = DeviceType.PHONE.value,
            eventType = entity.eventType,
            value = entity.value
        )
    }
}

object PhoneLocationMapper : EntityToSupabaseMapper<LocationEntity, LocationSensorData> {
    override fun map(entity: LocationEntity, userUuid: String?): LocationSensorData {
        return LocationSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = entity.deviceType, // Should be 0 for phone
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            received = Instant.ofEpochMilli(entity.received).toString(),
            accuracy = entity.accuracy,
            altitude = entity.altitude,
            latitude = entity.latitude,
            longitude = entity.longitude,
            speed = entity.speed
        )
    }
}

object ScreenMapper : EntityToSupabaseMapper<ScreenEntity, ScreenSensorData> {
    override fun map(entity: ScreenEntity, userUuid: String?): ScreenSensorData {
        return ScreenSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            received = Instant.ofEpochMilli(entity.received).toString(),
            deviceType = DeviceType.PHONE.value,
            type = entity.type
        )
    }
}

object WifiMapper : EntityToSupabaseMapper<WifiScanEntity, WifiScanSensorData> {
    override fun map(entity: WifiScanEntity, userUuid: String?): WifiScanSensorData {
        return WifiScanSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            deviceType = DeviceType.PHONE.value,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            bssid = entity.bssid,
            frequency = entity.frequency,
            level = entity.level,
            ssid = entity.ssid,
            received = Instant.ofEpochMilli(entity.received).toString()
        )
    }
}

object ConnectivityMapper : EntityToSupabaseMapper<ConnectivityEntity, ConnectivitySensorData> {
    override fun map(entity: ConnectivityEntity, userUuid: String?): ConnectivitySensorData {
        val transportList = entity.transportTypes.split(",").filter { it.isNotBlank() }
        return ConnectivitySensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            received = Instant.ofEpochMilli(entity.received).toString(),
            deviceType = DeviceType.PHONE.value,
            isConnected = entity.isConnected,
            hasInternet = entity.hasInternet,
            networkType = entity.networkType,
            transportTypes = transportList
        )
    }
}

object AppListChangeMapper : EntityToSupabaseMapper<AppListChangeEntity, AppListChangeSensorData> {
    private val json = Json { ignoreUnknownKeys = true }
    
    override fun map(entity: AppListChangeEntity, userUuid: String?): AppListChangeSensorData {
        // Parse JSON strings to JsonElement so Supabase stores them as native JSON (not stringified)
        val changedApp: JsonElement? = entity.changedAppJson?.let { jsonString ->
            try {
                json.parseToJsonElement(jsonString)
            } catch (e: Exception) {
                null
            }
        }
        
        val appList: JsonElement? = entity.appListJson?.let { jsonString ->
            try {
                json.parseToJsonElement(jsonString)
            } catch (e: Exception) {
                null
            }
        }
        
        return AppListChangeSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            received = Instant.ofEpochMilli(entity.received).toString(),
            deviceType = DeviceType.PHONE.value,
            changedApp = changedApp,
            appList = appList
        )
    }
}

object AppUsageLogMapper : EntityToSupabaseMapper<AppUsageLogEntity, AppUsageLogSensorData> {
    override fun map(entity: AppUsageLogEntity, userUuid: String?): AppUsageLogSensorData {
        return AppUsageLogSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            received = Instant.ofEpochMilli(entity.received).toString(),
            deviceType = DeviceType.PHONE.value,
            packageName = entity.packageName,
            installedBy = entity.installedBy,
            eventType = entity.eventType
        )
    }
}

object NotificationMapper : EntityToSupabaseMapper<NotificationEntity, NotificationSensorData> {
    override fun map(entity: NotificationEntity, userUuid: String?): NotificationSensorData {
        return NotificationSensorData(
            eventId = entity.eventId,
            uuid = userUuid,
            timestamp = Instant.ofEpochMilli(entity.timestamp).toString(),
            received = Instant.ofEpochMilli(entity.received).toString(),
            deviceType = DeviceType.PHONE.value,
            packageName = entity.packageName,
            eventType = entity.eventType,
            title = entity.title,
            text = entity.text,
            visibility = entity.visibility,
            category = entity.category
        )
    }
}
