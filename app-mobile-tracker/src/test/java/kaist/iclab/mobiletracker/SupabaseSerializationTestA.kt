package kaist.iclab.mobiletracker

import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.data.sensors.phone.AmbientLightSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.AppListChangeSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.AppUsageLogSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.BluetoothScanSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.CallLogSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.ConnectivitySensorData
import kaist.iclab.mobiletracker.data.sensors.phone.DataTrafficSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.DeviceModeSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.MediaSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.MessageLogSensorData
import kaist.iclab.mobiletracker.db.entity.phone.AmbientLightEntity
import kaist.iclab.mobiletracker.db.entity.phone.AppListChangeEntity
import kaist.iclab.mobiletracker.db.entity.phone.AppUsageLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.BluetoothScanEntity
import kaist.iclab.mobiletracker.db.entity.phone.CallLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.ConnectivityEntity
import kaist.iclab.mobiletracker.db.entity.phone.DataTrafficEntity
import kaist.iclab.mobiletracker.db.entity.phone.DeviceModeEntity
import kaist.iclab.mobiletracker.db.entity.phone.MediaEntity
import kaist.iclab.mobiletracker.db.entity.phone.MessageLogEntity
import kaist.iclab.mobiletracker.db.mapper.AmbientLightMapper
import kaist.iclab.mobiletracker.db.mapper.AppListChangeMapper
import kaist.iclab.mobiletracker.db.mapper.AppUsageLogMapper
import kaist.iclab.mobiletracker.db.mapper.BluetoothScanMapper
import kaist.iclab.mobiletracker.db.mapper.CallLogMapper
import kaist.iclab.mobiletracker.db.mapper.ConnectivityMapper
import kaist.iclab.mobiletracker.db.mapper.DataTrafficMapper
import kaist.iclab.mobiletracker.db.mapper.DeviceModeMapper
import kaist.iclab.mobiletracker.db.mapper.MediaMapper
import kaist.iclab.mobiletracker.db.mapper.MessageLogMapper
import kaist.iclab.mobiletracker.db.obx.SupabaseJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Safety net for the entity↔DTO merge (batch A): proves that serializing each merged `@Serializable`
 * entity produces JSON identical to the (proven, production) DTO path. Mirrors
 * [SupabaseSerializationTest]. Runs on the JVM; no device or Supabase needed.
 */
class SupabaseSerializationTestA {

    // --- AmbientLight ---
    private fun AmbientLightEntity.toSupabaseRowAmbientLight(u: String): JsonObject {
        val o = SupabaseJson.encodeToJsonElement(AmbientLightEntity.serializer(), this).jsonObject
        return JsonObject(o + ("uuid" to JsonPrimitive(u)))
    }

    @Test
    fun ambientLightEntitySerializesIdenticallyToDto() {
        val u = "user-123"
        val e = AmbientLightEntity(
            id = 1L,
            eventId = "event-ambient",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            accuracy = 3,
            value = 123.5f
        )
        val dtoJson = SupabaseJson
            .encodeToJsonElement(AmbientLightSensorData.serializer(), AmbientLightMapper.map(e, u))
            .jsonObject
        assertEquals(dtoJson, e.toSupabaseRowAmbientLight(u))
    }

    // --- AppUsageLog ---
    private fun AppUsageLogEntity.toSupabaseRowAppUsageLog(u: String): JsonObject {
        val o = SupabaseJson.encodeToJsonElement(AppUsageLogEntity.serializer(), this).jsonObject
        return JsonObject(o + ("uuid" to JsonPrimitive(u)))
    }

    @Test
    fun appUsageLogEntitySerializesIdenticallyToDto() {
        val u = "user-123"
        val e = AppUsageLogEntity(
            id = 2L,
            uuid = "OLD",
            eventId = "event-appusage",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            packageName = "com.example.app",
            installedBy = "USER",
            eventType = 7
        )
        val dtoJson = SupabaseJson
            .encodeToJsonElement(AppUsageLogSensorData.serializer(), AppUsageLogMapper.map(e, u))
            .jsonObject
        assertEquals(dtoJson, e.toSupabaseRowAppUsageLog(u))
    }

    // --- BluetoothScan ---
    private fun BluetoothScanEntity.toSupabaseRowBluetoothScan(u: String): JsonObject {
        val o = SupabaseJson.encodeToJsonElement(BluetoothScanEntity.serializer(), this).jsonObject
        return JsonObject(o + ("uuid" to JsonPrimitive(u)))
    }

    @Test
    fun bluetoothScanEntitySerializesIdenticallyToDto() {
        val u = "user-123"
        val e = BluetoothScanEntity(
            id = 3L,
            eventId = "event-bt",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            name = "MyHeadset",
            alias = "Headset Alias",
            address = "00:11:22:33:44:55",
            bondState = 12,
            connectionType = 2,
            classType = 1032,
            rssi = -67,
            isLE = true
        )
        val dtoJson = SupabaseJson
            .encodeToJsonElement(BluetoothScanSensorData.serializer(), BluetoothScanMapper.map(e, u))
            .jsonObject
        assertEquals(dtoJson, e.toSupabaseRowBluetoothScan(u))
    }

    // --- CallLog ---
    private fun CallLogEntity.toSupabaseRowCallLog(u: String): JsonObject {
        val o = SupabaseJson.encodeToJsonElement(CallLogEntity.serializer(), this).jsonObject
        return JsonObject(o + ("uuid" to JsonPrimitive(u)))
    }

    @Test
    fun callLogEntitySerializesIdenticallyToDto() {
        val u = "user-123"
        val e = CallLogEntity(
            id = 4L,
            eventId = "event-call",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            duration = 125_000L,
            number = "+821012345678",
            type = 1
        )
        val dtoJson = SupabaseJson
            .encodeToJsonElement(CallLogSensorData.serializer(), CallLogMapper.map(e, u))
            .jsonObject
        assertEquals(dtoJson, e.toSupabaseRowCallLog(u))
    }

    // --- Connectivity ---
    private fun ConnectivityEntity.toSupabaseRowConnectivity(u: String): JsonObject {
        val o = SupabaseJson.encodeToJsonElement(ConnectivityEntity.serializer(), this).jsonObject
        return JsonObject(o + ("uuid" to JsonPrimitive(u)))
    }

    @Test
    fun connectivityEntitySerializesIdenticallyToDto() {
        val u = "user-123"
        val e = ConnectivityEntity(
            id = 5L,
            eventId = "event-conn",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            networkType = "WIFI",
            isConnected = true,
            hasInternet = true,
            transportTypes = "WIFI,VPN"
        )
        val dtoJson = SupabaseJson
            .encodeToJsonElement(ConnectivitySensorData.serializer(), ConnectivityMapper.map(e, u))
            .jsonObject
        assertEquals(dtoJson, e.toSupabaseRowConnectivity(u))
    }

    @Test
    fun connectivityEntitySerializesIdenticallyToDtoWithEmptyTransportTypes() {
        val u = "user-123"
        // Empty comma-joined string must serialize to an empty JSON array on both paths.
        val e = ConnectivityEntity(
            id = 6L,
            eventId = "event-conn-empty",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            networkType = "CELLULAR",
            isConnected = true,
            hasInternet = false,
            transportTypes = ""
        )
        val dtoJson = SupabaseJson
            .encodeToJsonElement(ConnectivitySensorData.serializer(), ConnectivityMapper.map(e, u))
            .jsonObject
        assertEquals(dtoJson, e.toSupabaseRowConnectivity(u))
    }

    // --- DataTraffic ---
    private fun DataTrafficEntity.toSupabaseRowDataTraffic(u: String): JsonObject {
        val o = SupabaseJson.encodeToJsonElement(DataTrafficEntity.serializer(), this).jsonObject
        return JsonObject(o + ("uuid" to JsonPrimitive(u)))
    }

    @Test
    fun dataTrafficEntitySerializesIdenticallyToDto() {
        val u = "user-123"
        val e = DataTrafficEntity(
            id = 7L,
            eventId = "event-traffic",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            totalRx = 10_000L,
            totalTx = 20_000L,
            mobileRx = 3_000L,
            mobileTx = 4_000L
        )
        val dtoJson = SupabaseJson
            .encodeToJsonElement(DataTrafficSensorData.serializer(), DataTrafficMapper.map(e, u))
            .jsonObject
        assertEquals(dtoJson, e.toSupabaseRowDataTraffic(u))
    }

    // --- DeviceMode ---
    private fun DeviceModeEntity.toSupabaseRowDeviceMode(u: String): JsonObject {
        val o = SupabaseJson.encodeToJsonElement(DeviceModeEntity.serializer(), this).jsonObject
        return JsonObject(o + ("uuid" to JsonPrimitive(u)))
    }

    @Test
    fun deviceModeEntitySerializesIdenticallyToDto() {
        val u = "user-123"
        val e = DeviceModeEntity(
            id = 8L,
            eventId = "event-mode",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            eventType = "POWER_SAVE_MODE_EVENT",
            value = "POWER_SAVE_MODE_ON"
        )
        val dtoJson = SupabaseJson
            .encodeToJsonElement(DeviceModeSensorData.serializer(), DeviceModeMapper.map(e, u))
            .jsonObject
        assertEquals(dtoJson, e.toSupabaseRowDeviceMode(u))
    }

    // --- Media ---
    private fun MediaEntity.toSupabaseRowMedia(u: String): JsonObject {
        val o = SupabaseJson.encodeToJsonElement(MediaEntity.serializer(), this).jsonObject
        return JsonObject(o + ("uuid" to JsonPrimitive(u)))
    }

    @Test
    fun mediaEntitySerializesIdenticallyToDto() {
        val u = "user-123"
        val e = MediaEntity(
            id = 9L,
            eventId = "event-media",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            operation = "CREATE",
            mediaType = "IMAGE",
            storageType = "EXTERNAL",
            uri = "content://media/external/images/media/42",
            fileName = "photo.jpg",
            mimeType = "image/jpeg",
            size = 204_800L,
            dateAdded = 1_699_999_000_000L,
            dateModified = 1_699_999_500_000L
        )
        val dtoJson = SupabaseJson
            .encodeToJsonElement(MediaSensorData.serializer(), MediaMapper.map(e, u))
            .jsonObject
        assertEquals(dtoJson, e.toSupabaseRowMedia(u))
    }

    @Test
    fun mediaEntitySerializesIdenticallyToDtoWithNullOptionals() {
        val u = "user-123"
        // Leave all nullable fields null to prove null propagates identically on both paths.
        val e = MediaEntity(
            id = 10L,
            eventId = "event-media-null",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            operation = "DELETE",
            mediaType = "VIDEO",
            storageType = "INTERNAL",
            uri = "content://media/internal/video/media/7",
            fileName = null,
            mimeType = null,
            size = null,
            dateAdded = null,
            dateModified = null
        )
        val dtoJson = SupabaseJson
            .encodeToJsonElement(MediaSensorData.serializer(), MediaMapper.map(e, u))
            .jsonObject
        assertEquals(dtoJson, e.toSupabaseRowMedia(u))
    }

    // --- MessageLog ---
    private fun MessageLogEntity.toSupabaseRowMessageLog(u: String): JsonObject {
        val o = SupabaseJson.encodeToJsonElement(MessageLogEntity.serializer(), this).jsonObject
        return JsonObject(o + ("uuid" to JsonPrimitive(u)))
    }

    @Test
    fun messageLogEntitySerializesIdenticallyToDto() {
        val u = "user-123"
        val e = MessageLogEntity(
            id = 11L,
            eventId = "event-msg",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            number = "+821098765432",
            messageType = "SMS",
            contactType = 1
        )
        val dtoJson = SupabaseJson
            .encodeToJsonElement(MessageLogSensorData.serializer(), MessageLogMapper.map(e, u))
            .jsonObject
        assertEquals(dtoJson, e.toSupabaseRowMessageLog(u))
    }

    // --- AppListChange ---
    private fun AppListChangeEntity.toSupabaseRowAppListChange(u: String): JsonObject {
        val o = SupabaseJson.encodeToJsonElement(AppListChangeEntity.serializer(), this).jsonObject
        return JsonObject(o + ("uuid" to JsonPrimitive(u)))
    }

    @Test
    fun appListChangeEntitySerializesIdenticallyToDto() {
        val u = "user-123"
        // JSON-string columns must serialize as native JSON (object/array), matching the DTO's
        // JsonElement fields produced by the mapper's parseToJsonElement.
        val e = AppListChangeEntity(
            id = 12L,
            eventId = "event-applist",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            changedAppJson = """{"packageName":"com.example.app","installedBy":"USER"}""",
            appListJson = """[{"packageName":"com.a"},{"packageName":"com.b"}]"""
        )
        val dtoJson = SupabaseJson
            .encodeToJsonElement(AppListChangeSensorData.serializer(), AppListChangeMapper.map(e, u))
            .jsonObject
        assertEquals(dtoJson, e.toSupabaseRowAppListChange(u))
    }

    @Test
    fun appListChangeEntitySerializesIdenticallyToDtoWithNullJson() {
        val u = "user-123"
        // Null JSON columns must serialize to JSON null on both paths.
        val e = AppListChangeEntity(
            id = 13L,
            eventId = "event-applist-null",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            changedAppJson = null,
            appListJson = null
        )
        val dtoJson = SupabaseJson
            .encodeToJsonElement(AppListChangeSensorData.serializer(), AppListChangeMapper.map(e, u))
            .jsonObject
        assertEquals(dtoJson, e.toSupabaseRowAppListChange(u))
    }
}
