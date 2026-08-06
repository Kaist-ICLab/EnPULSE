package kaist.iclab.tracker.trigger.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/**
 * Sealed class representing an action to execute when a trigger condition is satisfied.
 *
 * Maps to the Supabase `campaign_trigger.action` JSON array format:
 * ```json
 * [
 *   {
 *     "kind": "watch_ema",
 *     "surveyId": 34,
 *     "minIntervalMillis": 50000
 *   },
 *   {
 *     "kind": "broadcast",
 *     "action": "com.example.ACTION",
 *     "extras": [
 *       { "key": "sensor", "value": "stress", "valueType": "String" }
 *     ]
 *   }
 * ]
 * ```
 */
@Serializable(with = TriggerActionConfigSerializer::class)
sealed class TriggerActionConfig {

    /**
     * Minimum interval in milliseconds between consecutive executions of this action.
     * Prevents rapid re-triggering.
     */
    abstract val minIntervalMillis: Long

    /**
     * Trigger a MicroEMA survey on the watch.
     */
    data class WatchEma(
        val surveyId: Int,
        override val minIntervalMillis: Long
    ) : TriggerActionConfig()

    /**
     * Trigger a full EMA survey on the phone.
     */
    data class Ema(
        val surveyId: Int,
        override val minIntervalMillis: Long
    ) : TriggerActionConfig()

    /**
     * Send an Android broadcast intent.
     */
    data class Broadcast(
        val action: String,
        val extras: List<BroadcastExtra>,
        override val minIntervalMillis: Long = 0
    ) : TriggerActionConfig()

    /**
     * Show a local notification.
     *
     * @param url Optional deep link/URL to open when the notification is tapped. If absent, the
     *   notification just opens the EnPULSE app (its main activity) instead.
     */
    data class Notification(
        val title: String,
        val description: String,
        val url: String? = null,
        override val minIntervalMillis: Long = 0
    ) : TriggerActionConfig()
}

/**
 * Key-value pair for broadcast intent extras.
 *
 * @param key The extra key.
 * @param value The extra value as a string.
 * @param valueType The type of the value ("String", "Int", "Boolean", etc.).
 */
@Serializable
data class BroadcastExtra(
    val key: String,
    val value: String,
    val valueType: String
)

/**
 * Custom serializer for [TriggerActionConfig] that handles the "kind" discriminator
 * used in the Supabase JSON format.
 */
object TriggerActionConfigSerializer : KSerializer<TriggerActionConfig> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("TriggerActionConfig")

    override fun deserialize(decoder: Decoder): TriggerActionConfig {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("TriggerActionConfig can only be deserialized from JSON")
        val obj = jsonDecoder.decodeJsonElement().jsonObject
        return parseAction(obj)
    }

    override fun serialize(encoder: Encoder, value: TriggerActionConfig) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("TriggerActionConfig can only be serialized to JSON")
        jsonEncoder.encodeJsonElement(serializeAction(value))
    }

    private fun parseAction(obj: JsonObject): TriggerActionConfig {
        return when (val kind = obj["kind"]?.jsonPrimitive?.content
            ?: throw SerializationException("Missing 'kind' field in action config")) {

            "watch_ema" -> TriggerActionConfig.WatchEma(
                surveyId = (obj["surveyId"] ?: obj["survey_id"])?.jsonPrimitive?.int
                    ?: throw SerializationException("'watch_ema' requires 'surveyId'"),
                minIntervalMillis = obj["minIntervalMillis"]?.jsonPrimitive?.long ?: 0L
            )

            "ema" -> TriggerActionConfig.Ema(
                surveyId = (obj["surveyId"] ?: obj["survey_id"])?.jsonPrimitive?.int
                    ?: throw SerializationException("'ema' requires 'surveyId'"),
                minIntervalMillis = obj["minIntervalMillis"]?.jsonPrimitive?.long ?: 0L
            )

            "broadcast" -> TriggerActionConfig.Broadcast(
                action = obj["action"]?.jsonPrimitive?.content
                    ?: throw SerializationException("'broadcast' requires 'action'"),
                extras = obj["extras"]?.jsonArray?.map { extraElement ->
                    val extraObj = extraElement.jsonObject
                    BroadcastExtra(
                        key = extraObj["key"]?.jsonPrimitive?.content ?: "",
                        value = extraObj["value"]?.jsonPrimitive?.content ?: "",
                        valueType = extraObj["valueType"]?.jsonPrimitive?.content ?: "String"
                    )
                } ?: emptyList(),
                minIntervalMillis = obj["minIntervalMillis"]?.jsonPrimitive?.long ?: 0L
            )

            "notification" -> TriggerActionConfig.Notification(
                title = obj["title"]?.jsonPrimitive?.content
                    ?: throw SerializationException("'notification' requires 'title'"),
                description = obj["description"]?.jsonPrimitive?.content
                    ?: throw SerializationException("'notification' requires 'description'"),
                url = obj["url"]?.jsonPrimitive?.content,
                minIntervalMillis = obj["minIntervalMillis"]?.jsonPrimitive?.long ?: 0L
            )

            else -> throw SerializationException("Unknown action kind: '$kind'")
        }
    }

    private fun serializeAction(action: TriggerActionConfig): JsonObject {
        return when (action) {
            is TriggerActionConfig.WatchEma -> buildJsonObject {
                put("kind", "watch_ema")
                put("surveyId", action.surveyId)
                put("minIntervalMillis", action.minIntervalMillis)
            }

            is TriggerActionConfig.Ema -> buildJsonObject {
                put("kind", "ema")
                put("surveyId", action.surveyId)
                put("minIntervalMillis", action.minIntervalMillis)
            }

            is TriggerActionConfig.Broadcast -> buildJsonObject {
                put("kind", "broadcast")
                put("action", action.action)
                put("extras", buildJsonArray {
                    action.extras.forEach { extra ->
                        add(buildJsonObject {
                            put("key", extra.key)
                            put("value", extra.value)
                            put("valueType", extra.valueType)
                        })
                    }
                })
                put("minIntervalMillis", action.minIntervalMillis)
            }

            is TriggerActionConfig.Notification -> buildJsonObject {
                put("kind", "notification")
                put("title", action.title)
                put("description", action.description)
                put("url", action.url)
                put("minIntervalMillis", action.minIntervalMillis)
            }
        }
    }
}

/**
 * Serializer for `List<TriggerActionConfig>` — the top-level action array from Supabase.
 */
object TriggerActionConfigListSerializer : KSerializer<List<TriggerActionConfig>> {
    private val delegateSerializer = ListSerializer(TriggerActionConfigSerializer)
    override val descriptor: SerialDescriptor = delegateSerializer.descriptor
    override fun deserialize(decoder: Decoder) = delegateSerializer.deserialize(decoder)
    override fun serialize(encoder: Encoder, value: List<TriggerActionConfig>) =
        delegateSerializer.serialize(encoder, value)
}
