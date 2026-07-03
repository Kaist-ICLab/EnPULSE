package kaist.iclab.mobiletracker

import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.data.sensors.phone.NotificationSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.ScreenSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.StepSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.UserInteractionSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.WifiScanSensorData
import kaist.iclab.mobiletracker.data.sensors.watch.AccelerometerSensorData
import kaist.iclab.mobiletracker.data.sensors.watch.EDASensorData
import kaist.iclab.mobiletracker.data.sensors.watch.HeartRateSensorData
import kaist.iclab.mobiletracker.data.sensors.watch.PPGSensorData
import kaist.iclab.mobiletracker.data.sensors.watch.SkinTemperatureSensorData
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
import kaist.iclab.mobiletracker.db.mapper.AccelerometerMapper
import kaist.iclab.mobiletracker.db.mapper.EDAMapper
import kaist.iclab.mobiletracker.db.mapper.HeartRateMapper
import kaist.iclab.mobiletracker.db.mapper.NotificationMapper
import kaist.iclab.mobiletracker.db.mapper.PPGMapper
import kaist.iclab.mobiletracker.db.mapper.ScreenMapper
import kaist.iclab.mobiletracker.db.mapper.SkinTemperatureMapper
import kaist.iclab.mobiletracker.db.mapper.StepMapper
import kaist.iclab.mobiletracker.db.mapper.UserInteractionMapper
import kaist.iclab.mobiletracker.db.mapper.WifiMapper
import kaist.iclab.mobiletracker.db.obx.SupabaseJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Second batch of the entity↔DTO merge safety net (mirrors [SupabaseSerializationTest]): proves each
 * merged `@Serializable` entity serializes to byte-identical JSON to its (proven, production) DTO
 * path. Covers 5 phone + 5 watch sensors. Runs on the JVM; no device or Supabase needed.
 */
class SupabaseSerializationTestB {

    /** Mirror of what the upload does: serialize the entity, then stamp the upload-time uuid. */
    private fun stampUuid(obj: JsonObject, userUuid: String): JsonObject =
        JsonObject(obj + ("uuid" to JsonPrimitive(userUuid)))

    private fun NotificationEntity.toSupabaseRowNotification(userUuid: String): JsonObject =
        stampUuid(
            SupabaseJson.encodeToJsonElement(NotificationEntity.serializer(), this).jsonObject,
            userUuid
        )

    private fun ScreenEntity.toSupabaseRowScreen(userUuid: String): JsonObject =
        stampUuid(
            SupabaseJson.encodeToJsonElement(ScreenEntity.serializer(), this).jsonObject,
            userUuid
        )

    private fun StepEntity.toSupabaseRowStep(userUuid: String): JsonObject =
        stampUuid(
            SupabaseJson.encodeToJsonElement(StepEntity.serializer(), this).jsonObject,
            userUuid
        )

    private fun UserInteractionEntity.toSupabaseRowUserInteraction(userUuid: String): JsonObject =
        stampUuid(
            SupabaseJson.encodeToJsonElement(UserInteractionEntity.serializer(), this).jsonObject,
            userUuid
        )

    private fun WifiScanEntity.toSupabaseRowWifiScan(userUuid: String): JsonObject =
        stampUuid(
            SupabaseJson.encodeToJsonElement(WifiScanEntity.serializer(), this).jsonObject,
            userUuid
        )

    private fun WatchHeartRateEntity.toSupabaseRowHeartRate(userUuid: String): JsonObject =
        stampUuid(
            SupabaseJson.encodeToJsonElement(WatchHeartRateEntity.serializer(), this).jsonObject,
            userUuid
        )

    private fun WatchAccelerometerEntity.toSupabaseRowAccelerometer(userUuid: String): JsonObject =
        stampUuid(
            SupabaseJson.encodeToJsonElement(WatchAccelerometerEntity.serializer(), this).jsonObject,
            userUuid
        )

    private fun WatchEDAEntity.toSupabaseRowEDA(userUuid: String): JsonObject =
        stampUuid(
            SupabaseJson.encodeToJsonElement(WatchEDAEntity.serializer(), this).jsonObject,
            userUuid
        )

    private fun WatchPPGEntity.toSupabaseRowPPG(userUuid: String): JsonObject =
        stampUuid(
            SupabaseJson.encodeToJsonElement(WatchPPGEntity.serializer(), this).jsonObject,
            userUuid
        )

    private fun WatchSkinTemperatureEntity.toSupabaseRowSkinTemperature(userUuid: String): JsonObject =
        stampUuid(
            SupabaseJson.encodeToJsonElement(WatchSkinTemperatureEntity.serializer(), this).jsonObject,
            userUuid
        )

    @Test
    fun notificationEntitySerializesIdenticallyToDto() {
        val userUuid = "user-123"
        val entity = NotificationEntity(
            id = 1L,
            eventId = "event-notification",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            packageName = "com.example.app",
            eventType = "POSTED",
            title = "Sample Title",
            text = "Sample Text",
            visibility = 1,
            category = "msg"
        )

        val dtoJson = SupabaseJson
            .encodeToJsonElement(
                NotificationSensorData.serializer(),
                NotificationMapper.map(entity, userUuid)
            )
            .jsonObject

        assertEquals(dtoJson, entity.toSupabaseRowNotification(userUuid))
    }

    @Test
    fun screenEntitySerializesIdenticallyToDto() {
        val userUuid = "user-123"
        val entity = ScreenEntity(
            id = 1L,
            eventId = "event-screen",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            type = "SCREEN_ON"
        )

        val dtoJson = SupabaseJson
            .encodeToJsonElement(ScreenSensorData.serializer(), ScreenMapper.map(entity, userUuid))
            .jsonObject

        assertEquals(dtoJson, entity.toSupabaseRowScreen(userUuid))
    }

    @Test
    fun stepEntitySerializesIdenticallyToDto() {
        val userUuid = "user-123"
        val entity = StepEntity(
            id = 1L,
            eventId = "event-step",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            startTime = 1_699_999_000_000L,
            endTime = 1_700_000_000_000L,
            steps = 1234L
        )

        val dtoJson = SupabaseJson
            .encodeToJsonElement(StepSensorData.serializer(), StepMapper.map(entity, userUuid))
            .jsonObject

        assertEquals(dtoJson, entity.toSupabaseRowStep(userUuid))
    }

    @Test
    fun userInteractionEntitySerializesIdenticallyToDto() {
        val userUuid = "user-123"
        val entity = UserInteractionEntity(
            id = 1L,
            eventId = "event-userinteraction",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            packageName = "com.example.app",
            className = "com.example.app.MainActivity",
            eventType = 32,
            text = "Sample interaction"
        )

        val dtoJson = SupabaseJson
            .encodeToJsonElement(
                UserInteractionSensorData.serializer(),
                UserInteractionMapper.map(entity, userUuid)
            )
            .jsonObject

        assertEquals(dtoJson, entity.toSupabaseRowUserInteraction(userUuid))
    }

    @Test
    fun wifiScanEntitySerializesIdenticallyToDto() {
        val userUuid = "user-123"
        val entity = WifiScanEntity(
            id = 1L,
            eventId = "event-wifiscan",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            ssid = "MyNetwork",
            bssid = "00:11:22:33:44:55",
            frequency = 5180,
            level = -42
        )

        val dtoJson = SupabaseJson
            .encodeToJsonElement(WifiScanSensorData.serializer(), WifiMapper.map(entity, userUuid))
            .jsonObject

        assertEquals(dtoJson, entity.toSupabaseRowWifiScan(userUuid))
    }

    @Test
    fun watchHeartRateEntitySerializesIdenticallyToDto() {
        val userUuid = "user-123"
        val entity = WatchHeartRateEntity(
            id = 1L,
            eventId = "event-heartrate",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.WATCH.value,
            hr = 72,
            hrStatus = 1,
            ibi = intArrayOf(800, 810),
            ibiStatus = intArrayOf(0, 1)
        )

        val dtoJson = SupabaseJson
            .encodeToJsonElement(
                HeartRateSensorData.serializer(),
                HeartRateMapper.map(entity, userUuid)
            )
            .jsonObject

        assertEquals(dtoJson, entity.toSupabaseRowHeartRate(userUuid))
    }

    @Test
    fun watchAccelerometerEntitySerializesIdenticallyToDto() {
        val userUuid = "user-123"
        val entity = WatchAccelerometerEntity(
            id = 1L,
            eventId = "event-accelerometer",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.WATCH.value,
            x = 0.12f,
            y = -9.81f,
            z = 3.45f
        )

        val dtoJson = SupabaseJson
            .encodeToJsonElement(
                AccelerometerSensorData.serializer(),
                AccelerometerMapper.map(entity, userUuid)
            )
            .jsonObject

        assertEquals(dtoJson, entity.toSupabaseRowAccelerometer(userUuid))
    }

    @Test
    fun watchEDAEntitySerializesIdenticallyToDto() {
        val userUuid = "user-123"
        val entity = WatchEDAEntity(
            id = 1L,
            eventId = "event-eda",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.WATCH.value,
            skinConductance = 5.67f,
            status = 2
        )

        val dtoJson = SupabaseJson
            .encodeToJsonElement(EDASensorData.serializer(), EDAMapper.map(entity, userUuid))
            .jsonObject

        assertEquals(dtoJson, entity.toSupabaseRowEDA(userUuid))
    }

    @Test
    fun watchPPGEntitySerializesIdenticallyToDto() {
        val userUuid = "user-123"
        val entity = WatchPPGEntity(
            id = 1L,
            eventId = "event-ppg",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.WATCH.value,
            green = 100,
            greenStatus = 1,
            red = 200,
            redStatus = 2,
            ir = 300,
            irStatus = 3
        )

        val dtoJson = SupabaseJson
            .encodeToJsonElement(PPGSensorData.serializer(), PPGMapper.map(entity, userUuid))
            .jsonObject

        assertEquals(dtoJson, entity.toSupabaseRowPPG(userUuid))
    }

    @Test
    fun watchSkinTemperatureEntitySerializesIdenticallyToDto() {
        val userUuid = "user-123"
        val entity = WatchSkinTemperatureEntity(
            id = 1L,
            eventId = "event-skintemperature",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.WATCH.value,
            ambientTemp = 24.5f,
            objectTemp = 33.2f,
            status = 1
        )

        val dtoJson = SupabaseJson
            .encodeToJsonElement(
                SkinTemperatureSensorData.serializer(),
                SkinTemperatureMapper.map(entity, userUuid)
            )
            .jsonObject

        assertEquals(dtoJson, entity.toSupabaseRowSkinTemperature(userUuid))
    }
}
