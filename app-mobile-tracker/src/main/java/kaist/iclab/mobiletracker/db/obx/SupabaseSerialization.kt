package kaist.iclab.mobiletracker.db.obx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import java.time.Instant

/**
 * Serializes an epoch-millis `Long` as an ISO-8601 string, matching the representation the Supabase
 * DTOs used (`Instant.ofEpochMilli(x).toString()`). ObjectBox still stores/queries the raw `Long`;
 * this only affects kotlinx JSON output, so one field can serve both local storage and upload.
 */
object EpochMillisIsoSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EpochMillisIso", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeString(Instant.ofEpochMilli(value).toString())
    }

    override fun deserialize(decoder: Decoder): Long =
        Instant.parse(decoder.decodeString()).toEpochMilli()
}

/**
 * Serializes a comma-joined `String` (how ObjectBox stores the list locally) as a JSON array of the
 * non-blank parts, matching the DTO's `List<String>` column. Mirrors the old mapper's
 * `transportTypes.split(",").filter { it.isNotBlank() }`, so one entity field serves both storage
 * and upload. Only serialization is exercised on the upload path; deserialization is provided for
 * round-trip completeness.
 */
object CommaJoinedListSerializer : KSerializer<String> {
    private val delegate = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: String) {
        val list = value.split(",").filter { it.isNotBlank() }
        delegate.serialize(encoder, list)
    }

    override fun deserialize(decoder: Decoder): String =
        delegate.deserialize(decoder).joinToString(",")
}

/**
 * Serializes a `String` that already contains JSON text as a native JSON element (object/array),
 * so Supabase stores it as JSONB rather than a stringified string. Mirrors the old mapper's
 * `Json.parseToJsonElement(...)` with a null fallback on parse failure. Used for the AppListChange
 * `changed_app`/`app_list` columns. Apply to a nullable `String?` property so `null` stays `null`.
 */
object JsonStringElementSerializer : KSerializer<String> {
    private val lenientJson = Json { ignoreUnknownKeys = true }
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: String) {
        val jsonEncoder = encoder as JsonEncoder
        val element: JsonElement = try {
            lenientJson.parseToJsonElement(value)
        } catch (e: Exception) {
            JsonNull
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as JsonDecoder
        return jsonDecoder.decodeJsonElement().toString()
    }
}

/**
 * JSON used to turn a stored entity into its Supabase row. `encodeDefaults = true` so every column
 * is always present (entity fields carry ObjectBox defaults), matching the DTOs' required fields.
 */
val SupabaseJson: Json = Json { encodeDefaults = true }
