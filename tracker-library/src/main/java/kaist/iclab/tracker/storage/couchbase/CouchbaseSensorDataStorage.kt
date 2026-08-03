package kaist.iclab.tracker.storage.couchbase

import android.util.Log
import com.couchbase.lite.DataSource
import com.couchbase.lite.Expression
import com.couchbase.lite.Meta
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.Ordering
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.storage.core.DataStat
import kaist.iclab.tracker.storage.core.SensorDataStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.serializer
import java.util.UUID
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import com.couchbase.lite.Array as CblArray
import com.couchbase.lite.Dictionary as CblDictionary


class CouchbaseSensorDataStorage(
    couchbase: CouchbaseDB,
    collectionName: String
) : SensorDataStorage {
    override val ID: String = collectionName

    private val _stateFlow = MutableStateFlow(DataStat(-1L, 0L))
    override val statFlow: StateFlow<DataStat>
        get() = _stateFlow

    private val json = Json { encodeDefaults = true }
    private val collection = couchbase.getCollection(collectionName)

    init {
        _stateFlow.value = DataStat(
            getLastReceived(),
            collection.count
        )
    }

    override fun insert(data: SensorEntity) {
        _stateFlow.value = _stateFlow.value.copy(
            timestamp = System.currentTimeMillis(),
            count = _stateFlow.value.count + 1
        )

        val documentId = UUID.randomUUID().toString()
        val document = MutableDocument(documentId)

        // Store properties directly using reflection with safe casting
        val properties = data::class.memberProperties
        properties.forEach { property ->
            try {
                val value = property.getter.call(data)
                val propertyName = property.name

                when (value) {
                    is String -> document.setString(propertyName, value)
                    is Long -> document.setLong(propertyName, value)
                    is Int -> document.setLong(propertyName, value.toLong())
                    is Float -> document.setFloat(propertyName, value)
                    is Double -> document.setDouble(propertyName, value)
                    is Boolean -> document.setBoolean(propertyName, value)
                    else -> {
                        // Lists and other complex types go in as JSON text
                        document.setString(
                            "${propertyName}_json",
                            encodeToJson(property.returnType, value)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to access property ${property.name}: ${e.message}")
            }
        }

        // Also store the entity type for deserialization
        document.setString("entityType", data::class.simpleName ?: "Unknown")
        collection.save(document)
    }

    private fun getLastReceived(): Long {
        val query = QueryBuilder.select(SelectResult.all())
            .from(DataSource.collection(collection))
            .orderBy(Ordering.property("received").descending())
        val ret = query.execute().firstOrNull()?.getLong("received")
        return ret ?: -1L
    }

    /**
     * Retrieve all data from the storage
     */
    fun getAllData(): List<String> {
        val query = QueryBuilder.select(SelectResult.expression(Meta.id).`as`("id"))
            .from(DataSource.collection(collection))
            .orderBy(Ordering.property("received").descending())

        val results = mutableListOf<String>()
        query.execute().use { resultSet ->
            resultSet.forEach { result ->
                val docId = result.getString("id")
                if (docId != null) {
                    val document = collection.getDocument(docId)
                    if (document != null) {
                        // Convert document back to JSON format for compatibility
                        val json = convertDocumentToJson(document)
                        results.add(json)
                    }
                }
            }
        }
        return results
    }

    /**
     * Retrieve recent data from the storage (last N entries)
     */
    fun getRecentData(limit: Int = 10): List<String> {
        val query = QueryBuilder.select(SelectResult.expression(Meta.id).`as`("id"))
            .from(DataSource.collection(collection))
            .orderBy(Ordering.property("received").descending())
            .limit(Expression.intValue(limit))

        val results = mutableListOf<String>()
        query.execute().use { resultSet ->
            resultSet.forEach { result ->
                val docId = result.getString("id")
                if (docId != null) {
                    val document = collection.getDocument(docId)
                    if (document != null) {
                        // Convert document back to JSON format for compatibility
                        val json = convertDocumentToJson(document)
                        results.add(json)
                    }
                }
            }
        }
        return results
    }

    /**
     * Get data count in the storage
     */
    fun getDataCount(): Long {
        return collection.count
    }

    /**
     * Get data statistics
     */
    fun getDataStats(): DataStat {
        return _stateFlow.value
    }

    /**
     * Retrieve data from the storage within a specific time range
     *
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @return List of JSON strings for data within the time range
     */
    fun getDataByTimeRange(startTime: Long, endTime: Long): List<String> {
        val query = QueryBuilder.select(SelectResult.expression(Meta.id).`as`("id"))
            .from(DataSource.collection(collection))
            .where(
                Expression.property("received").between(
                    Expression.longValue(startTime),
                    Expression.longValue(endTime)
                )
            )
            .orderBy(Ordering.property("received").ascending())

        val results = mutableListOf<String>()
        query.execute().use { resultSet ->
            resultSet.forEach { result ->
                val docId = result.getString("id")
                if (docId != null) {
                    val document = collection.getDocument(docId)
                    if (document != null) {
                        // Convert document back to JSON format for compatibility
                        val json = convertDocumentToJson(document)
                        results.add(json)
                    }
                }
            }
        }
        return results
    }

    /**
     * Convert a Couchbase document back to JSON format for compatibility
     */
    private fun convertDocumentToJson(document: com.couchbase.lite.Document): String {
        val jsonObject = buildJsonObject {
            // Get all properties from the document
            document.keys.forEach { key ->
                when {
                    key.endsWith("_json") -> {
                        // Splice JSON-encoded properties back in as real JSON, not as a string
                        val originalKey = key.removeSuffix("_json")
                        val jsonValue = document.getString(key)
                        if (jsonValue != null) {
                            put(originalKey, try {
                                json.parseToJsonElement(jsonValue)
                            } catch (_: SerializationException) {
                                // If parsing fails, store as string
                                JsonPrimitive(jsonValue)
                            })
                        }
                    }

                    key == "entityType" -> {
                        // Skip entityType as it's metadata
                    }

                    else -> {
                        // Handle direct properties
                        put(key, document.getValue(key).toJsonElement())
                    }
                }
            }
        }

        return json.encodeToString(JsonObject.serializer(), jsonObject)
    }

    /**
     * Serializes [value] with the serializer for its declared [type]. Falls back to the value's
     * `toString()` when the type carries no `@Serializable` contract, so one exotic property can't
     * take the whole document down with it.
     */
    private fun encodeToJson(type: KType, value: Any?): String = try {
        json.encodeToString(serializer(type), value)
    } catch (e: SerializationException) {
        Log.w(TAG, "No serializer for $type; storing toString() instead: ${e.message}")
        json.encodeToString(JsonElement.serializer(), JsonPrimitive(value.toString()))
    }

    /**
     * Couchbase hands values back as plain types or as its own `Array`/`Dictionary` wrappers; map
     * whatever comes out onto the JSON tree.
     */
    private fun Any?.toJsonElement(): JsonElement {
        return when (val value = this) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is CblArray -> buildJsonArray { value.forEach { add(it.toJsonElement()) } }
            is CblDictionary -> buildJsonObject {
                value.keys.forEach { put(it, value.getValue(it).toJsonElement()) }
            }

            is Map<*, *> -> buildJsonObject {
                value.forEach { (k, v) -> put(k.toString(), v.toJsonElement()) }
            }

            is Iterable<*> -> buildJsonArray { value.forEach { add(it.toJsonElement()) } }
            else -> JsonPrimitive(value.toString())
        }
    }

    companion object {
        private const val TAG = "CouchbaseSensorDataStorage"
    }
}
