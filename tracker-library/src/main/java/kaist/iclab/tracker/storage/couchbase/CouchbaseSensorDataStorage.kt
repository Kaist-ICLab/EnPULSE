package kaist.iclab.tracker.storage.couchbase

import android.util.Log
import com.couchbase.lite.DataSource
import com.couchbase.lite.Expression
import com.couchbase.lite.Meta
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.Ordering
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import com.google.gson.Gson
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.storage.core.DataStat
import kaist.iclab.tracker.storage.core.SensorDataStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import kotlin.reflect.full.memberProperties


class CouchbaseSensorDataStorage(
    couchbase: CouchbaseDB,
    collectionName: String
) : SensorDataStorage {
    override val ID: String = collectionName

    private val _stateFlow = MutableStateFlow(DataStat(-1L, 0L))
    override val statFlow: StateFlow<DataStat>
        get() = _stateFlow

    private val gson = Gson()
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
                    is List<*> -> {
                        // Handle lists by converting to JSON for complex types
                        val listJson = gson.toJson(value)
                        document.setString("${propertyName}_json", listJson)
                    }

                    else -> {
                        // For other types, convert to JSON as fallback
                        val jsonValue = gson.toJson(value)
                        document.setString("${propertyName}_json", jsonValue)
                    }
                }
            } catch (e: Exception) {
                Log.w(
                    "CouchbaseSensorDataStorage",
                    "Failed to access property ${property.name}: ${e.message}"
                )
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
            .limit(com.couchbase.lite.Expression.intValue(limit))

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
        val jsonObject = mutableMapOf<String, Any?>()

        // Get all properties from the document
        document.keys.forEach { key ->
            when {
                key.endsWith("_json") -> {
                    // Handle JSON-encoded properties
                    val originalKey = key.removeSuffix("_json")
                    val jsonValue = document.getString(key)
                    if (jsonValue != null) {
                        try {
                            // Parse the JSON string back to an object
                            val parsedValue = gson.fromJson(jsonValue, Any::class.java)
                            jsonObject[originalKey] = parsedValue
                        } catch (e: Exception) {
                            // If parsing fails, store as string
                            jsonObject[originalKey] = jsonValue
                        }
                    }
                }

                key == "entityType" -> {
                    // Skip entityType as it's metadata
                }

                else -> {
                    // Handle direct properties
                    val value = document.getValue(key)
                    jsonObject[key] = value
                }
            }
        }

        return gson.toJson(jsonObject)
    }
}
