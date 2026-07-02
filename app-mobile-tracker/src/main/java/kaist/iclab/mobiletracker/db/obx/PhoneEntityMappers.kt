package kaist.iclab.mobiletracker.db.obx

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
import com.google.gson.Gson

/**
 * Collects the "sensor-library Entity -> stored Entity" mapping logic that previously
 * lived inside each phone/common DAO's insert() method, so it can be reused after the
 * Room DAOs are removed. Each function copies the field-by-field mapping verbatim from
 * the corresponding DAO. The stored entity's `id` is intentionally left unset because
 * ObjectBox assigns it on put().
 */
object PhoneEntityMappers {
    private val gson = Gson()

    fun ambientLight(e: AmbientLightSensor.Entity, userUuid: String?): AmbientLightEntity =
        AmbientLightEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            accuracy = e.accuracy,
            value = e.value
        )

    fun appListChange(e: AppListChangeSensor.Entity, userUuid: String?): AppListChangeEntity =
        AppListChangeEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            changedAppJson = e.changedApp?.let { gson.toJson(it) },
            appListJson = e.appList?.let { gson.toJson(it) }
        )

    fun appUsageLog(e: AppUsageLogSensor.Entity, userUuid: String?): AppUsageLogEntity =
        AppUsageLogEntity(
            uuid = userUuid ?: "",
            eventId = e.eventId,
            received = e.received,
            timestamp = e.timestamp,
            packageName = e.packageName,
            installedBy = e.installedBy,
            eventType = e.eventType
        )

    fun battery(e: BatterySensor.Entity, userUuid: String?): BatteryEntity =
        BatteryEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            connectedType = e.connectedType,
            status = e.status,
            level = e.level,
            temperature = e.temperature
        )

    fun bluetoothScan(e: BluetoothScanSensor.Entity, userUuid: String?): BluetoothScanEntity =
        BluetoothScanEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            name = e.name,
            alias = e.alias,
            address = e.address,
            bondState = e.bondState,
            connectionType = e.connectionType,
            classType = e.classType,
            rssi = e.rssi,
            isLE = e.isLE
        )

    fun callLog(e: CallLogSensor.Entity, userUuid: String?): CallLogEntity =
        CallLogEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            duration = e.duration,
            number = e.number,
            type = e.type
        )

    fun connectivity(e: ConnectivitySensor.Entity, userUuid: String?): ConnectivityEntity =
        ConnectivityEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            networkType = e.networkType,
            isConnected = e.isConnected,
            hasInternet = e.hasInternet,
            transportTypes = e.transportTypes.joinToString(",")
        )

    fun dataTraffic(e: DataTrafficSensor.Entity, userUuid: String?): DataTrafficEntity =
        DataTrafficEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            totalRx = e.totalRx,
            totalTx = e.totalTx,
            mobileRx = e.mobileRx,
            mobileTx = e.mobileTx
        )

    fun deviceMode(e: DeviceModeSensor.Entity, userUuid: String?): DeviceModeEntity =
        DeviceModeEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            eventType = e.eventType,
            value = e.value
        )

    fun media(e: MediaSensor.Entity, userUuid: String?): MediaEntity =
        MediaEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            operation = e.operation,
            mediaType = e.mediaType,
            storageType = e.storageType,
            uri = e.uri,
            fileName = e.fileName,
            mimeType = e.mimeType,
            size = e.size,
            dateAdded = e.dateAdded,
            dateModified = e.dateModified
        )

    fun messageLog(e: MessageLogSensor.Entity, userUuid: String?): MessageLogEntity =
        MessageLogEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            number = e.number,
            messageType = e.messageType,
            contactType = e.contactType
        )

    fun notification(e: NotificationSensor.Entity, userUuid: String?): NotificationEntity =
        NotificationEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            packageName = e.packageName,
            eventType = e.eventType,
            title = e.title,
            text = e.text,
            visibility = e.visibility,
            category = e.category
        )

    fun screen(e: ScreenSensor.Entity, userUuid: String?): ScreenEntity =
        ScreenEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            type = e.type
        )

    fun step(e: StepSensor.Entity, userUuid: String?): StepEntity =
        StepEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            startTime = e.startTime,
            endTime = e.endTime,
            steps = e.steps
        )

    fun userInteraction(e: UserInteractionSensor.Entity, userUuid: String?): UserInteractionEntity =
        UserInteractionEntity(
            uuid = userUuid ?: "",
            eventId = e.eventId,
            received = e.received,
            timestamp = e.timestamp,
            packageName = e.packageName,
            className = e.className,
            eventType = e.eventType,
            text = e.text
        )

    fun wifiScan(e: WifiScanSensor.Entity, userUuid: String?): WifiScanEntity =
        WifiScanEntity(
            uuid = userUuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            ssid = e.ssid,
            bssid = e.bssid,
            frequency = e.frequency,
            level = e.level
        )

    fun location(e: LocationSensor.Entity, userUuid: String?): LocationEntity =
        LocationEntity(
            uuid = userUuid ?: "",
            deviceType = DeviceType.PHONE.value,
            received = e.received,
            timestamp = e.timestamp,
            latitude = e.latitude,
            longitude = e.longitude,
            altitude = e.altitude,
            speed = e.speed,
            accuracy = e.accuracy
        )
}
