package kaist.iclab.mobiletracker.webapp.storage

import com.couchbase.lite.MutableDocument
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Couchbase-backed key-value storage for third-party webapps (bridge `getStorageData`/`setStorageData`).
 * One document per `{webAppId}:{key}` pair in a single shared collection — Couchbase's native
 * per-document JSON storage fits this better than a typed ObjectBox entity, since the value is
 * arbitrary webapp-supplied JSON rather than a fixed schema.
 */
class CouchbaseWebAppStorage(
    couchbase: CouchbaseDB,
    collectionName: String = "WebAppStorage"
) {
    private val collection = couchbase.getCollection(collectionName)

    fun get(webAppId: String, key: String): JsonElement? {
        val document = collection.getDocument(documentId(webAppId, key)) ?: return null
        return try {
            Json.parseToJsonElement(document.toJSON()).jsonObject["value"]
        } catch (e: Exception) {
            null
        }
    }

    fun set(webAppId: String, key: String, value: JsonElement) {
        val docId = documentId(webAppId, key)
        val body = buildJsonObject { put("value", value) }.toString()

        val existingDoc = collection.getDocument(docId)
        val mutableDoc = existingDoc?.toMutable()?.setJSON(body) ?: MutableDocument(docId, body)
        collection.save(mutableDoc)
    }

    private fun documentId(webAppId: String, key: String) = "$webAppId:$key"
}
