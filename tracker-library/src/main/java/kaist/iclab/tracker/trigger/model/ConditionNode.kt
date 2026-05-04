package kaist.iclab.tracker.trigger.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Sealed class hierarchy representing a boolean condition tree.
 *
 * Maps directly to the Supabase `campaign_trigger.condition` JSON format:
 * ```json
 * {
 *   "type": "and",
 *   "children": [
 *     { "type": "detection", "sensor": "stress", "value": "High" },
 *     {
 *       "type": "not",
 *       "child": {
 *         "type": "or",
 *         "children": [
 *           { "type": "detection", "sensor": "physical_activity", "value": "In Vehicle" },
 *           { "type": "detection", "sensor": "physical_activity", "value": "On Bicycle" }
 *         ]
 *       }
 *     }
 *   ]
 * }
 * ```
 */
@Serializable(with = ConditionNodeSerializer::class)
sealed class ConditionNode {

    /**
     * Logical AND — all children must evaluate to true.
     */
    data class And(val children: List<ConditionNode>) : ConditionNode()

    /**
     * Logical OR — at least one child must evaluate to true.
     */
    data class Or(val children: List<ConditionNode>) : ConditionNode()

    /**
     * Logical NOT — negates the child condition.
     */
    data class Not(val child: ConditionNode) : ConditionNode()

    /**
     * Leaf detection node — matches a sensor's current detection label.
     *
     * @param sensor The sensor name (e.g., "stress", "physical_activity").
     *               Must match the [SensorDetectionAdapter.sensorName] value.
     * @param value The expected detection label (e.g., "High", "Drinking").
     */
    data class Detection(val sensor: String, val value: String) : ConditionNode()
}

/**
 * Custom serializer for [ConditionNode] that handles the polymorphic "type" discriminator
 * used in the Supabase JSON format.
 */
object ConditionNodeSerializer : KSerializer<ConditionNode> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ConditionNode")

    override fun deserialize(decoder: Decoder): ConditionNode {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("ConditionNode can only be deserialized from JSON")
        val jsonElement = jsonDecoder.decodeJsonElement()
        return parseNode(jsonElement.jsonObject)
    }

    override fun serialize(encoder: Encoder, value: ConditionNode) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("ConditionNode can only be serialized to JSON")
        jsonEncoder.encodeJsonElement(serializeNode(value))
    }

    private fun parseNode(obj: JsonObject): ConditionNode {
        return when (val type = obj["type"]?.jsonPrimitive?.content
            ?: throw SerializationException("Missing 'type' field in condition node")) {
            "and" -> {
                val children = obj["children"]?.jsonArray
                    ?: throw SerializationException("'and' node requires 'children' array")
                ConditionNode.And(children.map { parseNode(it.jsonObject) })
            }

            "or" -> {
                val children = obj["children"]?.jsonArray
                    ?: throw SerializationException("'or' node requires 'children' array")
                ConditionNode.Or(children.map { parseNode(it.jsonObject) })
            }

            "not" -> {
                val child = obj["child"]?.jsonObject
                    ?: throw SerializationException("'not' node requires 'child' object")
                ConditionNode.Not(parseNode(child))
            }

            "detection" -> {
                val sensor = obj["sensor"]?.jsonPrimitive?.content
                    ?: throw SerializationException("'detection' node requires 'sensor' field")
                val value = obj["value"]?.jsonPrimitive?.content
                    ?: throw SerializationException("'detection' node requires 'value' field")
                ConditionNode.Detection(sensor, value)
            }

            else -> throw SerializationException("Unknown condition node type: '$type'")
        }
    }

    private fun serializeNode(node: ConditionNode): JsonObject {
        return when (node) {
            is ConditionNode.And -> buildJsonObject {
                put("type", "and")
                put("children", buildJsonArray {
                    node.children.forEach { add(serializeNode(it)) }
                })
            }

            is ConditionNode.Or -> buildJsonObject {
                put("type", "or")
                put("children", buildJsonArray {
                    node.children.forEach { add(serializeNode(it)) }
                })
            }

            is ConditionNode.Not -> buildJsonObject {
                put("type", "not")
                put("child", serializeNode(node.child))
            }

            is ConditionNode.Detection -> buildJsonObject {
                put("type", "detection")
                put("sensor", node.sensor)
                put("value", node.value)
            }
        }
    }
}
