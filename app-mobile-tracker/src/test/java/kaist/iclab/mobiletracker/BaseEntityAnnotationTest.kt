package kaist.iclab.mobiletracker

import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import kaist.iclab.mobiletracker.db.obx.EpochMillisIsoSerializer
import kaist.iclab.mobiletracker.db.obx.SupabaseJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that annotations placed on abstract properties in a base class are respected by
 * kotlinx.serialization when the property is overridden in a concrete data class — the key
 * question before committing to a BaseEntity approach.
 *
 * Checks:
 *  - @Transient on base → id is excluded from JSON
 *  - @SerialName("event_id") on base → key is "event_id", not "eventId"
 *  - @SerialName("device_type") on base → key is "device_type", not "deviceType"
 *  - @Serializable(with = EpochMillisIsoSerializer) on base → timestamp serializes as ISO string
 */

@Serializable
abstract class TestBaseEntity {
    @Id
    @Transient
    open var id: Long = 0
    @SerialName("event_id") abstract val eventId: String
    abstract val uuid: String
    @Serializable(with = EpochMillisIsoSerializer::class) abstract val received: Long
    @Serializable(with = EpochMillisIsoSerializer::class) abstract val timestamp: Long
    @SerialName("device_type") abstract val deviceType: Int

//    protected fun initBaseProperty(id: Long, eventId: String, uuid: String, received: Long, timestamp: Long, deviceType: Int) {
//        this.id = id
//        this.eventId = eventId
//        this.uuid = uuid
//        this.received = received
//        this.timestamp = timestamp
//        this.deviceType = deviceType
//    }
}

@Serializable
@Entity
data class TestSensorEntity(
    override var id: Long = 0,
    override var eventId: String,
    override var uuid: String = "",
    override var received: Long = 0,
    override var timestamp: Long = 0,
    override var deviceType: Int = 0,
    var value: Float = 0f
): TestBaseEntity()
//{
//    var value: Float = 0f
//
//    constructor(): super()
//
//    constructor(id: Long, eventId: String, uuid: String, received: Long, timestamp: Long, deviceType: Int, value: Float) {
//        initBaseProperty(id, eventId, uuid, received, timestamp, deviceType)
//        this.value = value
//    }
//}

class BaseEntityAnnotationTest {
    private val entity = TestSensorEntity(
        id = 99L,
        eventId = "event-123",
        uuid = "user-abc",
        received = 1_700_000_001_500L,
        timestamp = 1_700_000_000_000L,
        deviceType = 0,
        value = 42.0f
    )

    private val json by lazy {
        SupabaseJson.encodeToJsonElement(TestSensorEntity.serializer(), entity).toString()
    }

    @Test
    fun idIsExcluded() {
        assertFalse("id should be excluded by @Transient on base", json.contains("\"id\""))
    }

    @Test
    fun eventIdSerializesAsSnakeCase() {
        assertTrue("event_id key must be present", json.contains("\"event_id\""))
        assertFalse("eventId key must not be present", json.contains("\"eventId\""))
    }

    @Test
    fun deviceTypeSerializesAsSnakeCase() {
        assertTrue("device_type key must be present", json.contains("\"device_type\""))
        assertFalse("deviceType key must not be present", json.contains("\"deviceType\""))
    }

    @Test
    fun timestampsSerializeAsIsoStrings() {
        // EpochMillisIsoSerializer produces quoted ISO-8601 strings, not bare numbers
        assertTrue("timestamp must be an ISO string", json.contains("\"timestamp\":\""))
        assertTrue("received must be an ISO string", json.contains("\"received\":\""))
    }
}
