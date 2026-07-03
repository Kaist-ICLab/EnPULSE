package kaist.iclab.mobiletracker

import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.data.sensors.common.LocationSensorData
import kaist.iclab.mobiletracker.data.sensors.phone.BatterySensorData
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.entity.phone.BatteryEntity
import kaist.iclab.mobiletracker.db.mapper.BatteryMapper
import kaist.iclab.mobiletracker.db.mapper.LocationMapper
import kaist.iclab.mobiletracker.db.obx.SupabaseJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Safety net for the entity↔DTO merge: proves that serializing a merged `@Serializable` entity
 * produces byte-identical JSON to the (proven, production) DTO path — the exact bytes Supabase used
 * to receive. If these match, the merged entity is a drop-in for the DTO. Runs on the JVM; no
 * device or Supabase needed.
 */
class SupabaseSerializationTest {

    /** Mirror of what the upload does: serialize the entity, then stamp the upload-time uuid. */
    private fun BatteryEntity.toSupabaseRow(userUuid: String): JsonObject {
        val obj = SupabaseJson.encodeToJsonElement(BatteryEntity.serializer(), this).jsonObject
        return JsonObject(obj + ("uuid" to JsonPrimitive(userUuid)))
    }

    @Test
    fun batteryEntitySerializesIdenticallyToDto() {
        val userUuid = "user-123"
        // Note: stored uuid ("OLD") deliberately differs, to prove the upload-time override.
        val entity = BatteryEntity(
            id = 42L,
            eventId = "event-abc",
            uuid = "OLD",
            timestamp = 1_700_000_000_000L,
            received = 1_700_000_001_500L,
            deviceType = DeviceType.PHONE.value,
            connectedType = 2,
            status = 3,
            level = 87,
            temperature = 251
        )

        val dtoJson = SupabaseJson
            .encodeToJsonElement(BatterySensorData.serializer(), BatteryMapper.map(entity, userUuid))
            .jsonObject

        val entityJson = entity.toSupabaseRow(userUuid)

        assertEquals(dtoJson, entityJson)
    }

    private fun LocationEntity.toSupabaseRow(userUuid: String): JsonObject {
        val obj = SupabaseJson.encodeToJsonElement(LocationEntity.serializer(), this).jsonObject
        return JsonObject(obj + ("uuid" to JsonPrimitive(userUuid)))
    }

    @Test
    fun locationEntitySerializesIdenticallyToDto() {
        val userUuid = "user-123"
        val entity = LocationEntity(
            id = 7L,
            eventId = "event-loc",
            uuid = "OLD",
            deviceType = DeviceType.WATCH.value,
            received = 1_700_000_001_500L,
            timestamp = 1_700_000_000_000L,
            latitude = 37.5665,
            longitude = 126.9780,
            altitude = 38.0,
            speed = 1.4f,
            accuracy = 5.0f
        )

        val dtoJson = SupabaseJson
            .encodeToJsonElement(LocationSensorData.serializer(), LocationMapper.map(entity, userUuid))
            .jsonObject

        assertEquals(dtoJson, entity.toSupabaseRow(userUuid))
    }
}
