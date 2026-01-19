package kaist.iclab.tracker.storage.couchbase

import android.content.Context
import com.couchbase.lite.CouchbaseLite
import com.couchbase.lite.Database

class CouchbaseDB(
    context: Context
) {
    companion object {
        private const val DB_NAME = "TRACKER"
    }

    val database: Database by lazy {
        Database(DB_NAME)
    }

    init {
        CouchbaseLite.init(context)
    }

    fun getCollection(collectionName: String): com.couchbase.lite.Collection {
        val collection = database.getCollection(collectionName)
            ?: database.createCollection(collectionName)
        return collection
    }
}