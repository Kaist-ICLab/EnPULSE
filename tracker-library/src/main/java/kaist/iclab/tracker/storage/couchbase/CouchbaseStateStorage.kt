package kaist.iclab.tracker.storage.couchbase

import android.util.Log
import com.couchbase.lite.MutableDocument
import com.google.gson.Gson
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


open class CouchbaseStateStorage<T>(
    couchbase: CouchbaseDB,
    private val defaultVal: T,
    private val clazz: Class<T>,
    private val collectionName: String
) : StateStorage<T> {
    private val _stateFlow = MutableStateFlow(defaultVal)
    override val stateFlow: StateFlow<T>
        get()
        = _stateFlow

    private val collection = couchbase.getCollection(collectionName)
    private val gson = Gson()

    init {
        _stateFlow.value = get()
        Log.d("CouchbaseStateStorage", "initialize")
//        CoroutineScope(Dispatchers.IO).launch{
//            collection.documentChangeFlow(collectionName).collect { documentChange ->
//                Log.d("CouchbaseStateStorage", "init: ${get()}")
//            }
//        }
//        Log.d("CouchbaseStateStorage", "init: ${_stateFlow.value}")
    }

    override fun set(value: T) {
        Log.d("CouchbaseStateStorage", "set: $value")
        val json = gson.toJson(value)

        val existingDoc = collection.getDocument(collectionName)
        val mutableDoc = if (existingDoc != null) {
            Log.d(this::class.simpleName, "Mutating the existing doc...")
            existingDoc.toMutable().setJSON(json)
        } else {
            Log.d(this::class.simpleName, "Doc doesn't exist!")
            MutableDocument(collectionName, json)
        }

        collection.save(mutableDoc)
        _stateFlow.value = value
    }

    override fun get(): T {
        val document = collection.getDocument(collectionName)
        if (document == null) {
            set(defaultVal)
            return defaultVal
        }
        val json = document.toJSON()
        return gson.fromJson(json, clazz)
    }
}