package kaist.iclab.tracker.storage.couchbase

import android.util.Log
import com.couchbase.lite.DataSource
import com.couchbase.lite.DictionaryInterface
import com.couchbase.lite.Expression
import com.couchbase.lite.Meta
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.Ordering
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import kaist.iclab.tracker.sensor.survey.SurveySchedule
import kaist.iclab.tracker.storage.core.SurveyScheduleStorage
import java.util.UUID
import java.util.concurrent.TimeUnit

class CouchbaseSurveyScheduleStorage(
    couchbase: CouchbaseDB,
    collectionName: String,
): SurveyScheduleStorage {
    companion object {
        private val TAG = CouchbaseSurveyScheduleStorage::class.simpleName
    }

    private val collection = couchbase.getCollection(collectionName)

    override fun getNextSchedule(surveyId: String?): SurveySchedule? {
        val now = System.currentTimeMillis()
        val query = QueryBuilder.select(SelectResult.expression(Meta.id).`as`("uuid"), SelectResult.all())
            .from(DataSource.collection(collection))
            .where(
                Expression.property("actualTriggerTime").isNotValued().and(
                    Expression.property("triggerTime").greaterThan(Expression.value(now - TimeUnit.MINUTES.toMillis(5)))
                ).run {
                    if(surveyId != null) and(Expression.property("surveyId").equalTo(Expression.value(surveyId)))
                    else this
                }
            )
            .orderBy(Ordering.property("triggerTime").ascending())
            .limit(Expression.intValue(1))

        try {
            val result = query.execute().use {
                val result = it.first()
                val docUuid = result.getString("uuid")!!

                val resultDict = result.getDictionary(collection.name)
                if(resultDict == null) null else SurveySchedule(
                    scheduleId = docUuid,
                    surveyId = resultDict.getString("surveyId") ?: "",
                    triggerTime = resultDict.getLongOrNull("triggerTime"),
                )
            }

            return result

        } catch(e: Exception) {
            if (e !is NoSuchElementException) Log.e(TAG, "Failed to read a schedule", e)
        }

        return null
    }

    override fun getLastSchedule(surveyId: String?): SurveySchedule? {
        val query = QueryBuilder.select(SelectResult.expression(Meta.id).`as`("uuid"), SelectResult.all())
            .from(DataSource.collection(collection))
            .run {
                if(surveyId !== null) where(Expression.property("surveyId").equalTo(Expression.value(surveyId)))
                else where(Expression.value(true))
            }
            .orderBy(Ordering.property("triggerTime").descending())
            .limit(Expression.intValue(1))

        try {
            val result = query.execute().use {
                val result = it.first()
                val docUuid = result.getString("uuid")!!

                val resultDict = result.getDictionary(collection.name)
                if(resultDict == null) null else SurveySchedule(
                    scheduleId = docUuid,
                    surveyId = resultDict.getString("surveyId") ?: "",
                    triggerTime = resultDict.getLongOrNull("triggerTime"),
                )
            }


            return result

        } catch(e: Exception) {
            if (e !is NoSuchElementException) Log.e(TAG, "Failed to read a schedule", e)
            return null
        }
    }

    override fun getScheduleByScheduleId(scheduleId: String): SurveySchedule? {
        /* The scheduleId is the document ID assigned in addSchedule, so look the document up
         * directly — the same way setActualTriggerTime and friends do. This previously filtered on
         * a "uuid" property that no document ever carries (the UUID is document metadata, not a
         * field), so it always matched nothing and returned null. */
        val document = collection.getDocument(scheduleId) ?: return null

        return SurveySchedule(
            scheduleId = scheduleId,
            surveyId = document.getString("surveyId") ?: "",
            triggerTime = document.getLongOrNull("triggerTime"),
            actualTriggerTime = document.getLongOrNull("actualTriggerTime"),
            surveyStartTime = document.getLongOrNull("surveyStartTime"),
            responseSubmissionTime = document.getLongOrNull("responseSubmissionTime"),
        )
    }

    override fun addSchedule(schedule: SurveySchedule): String {
        val uuid = UUID.randomUUID().toString()

        val mutableDoc = MutableDocument(uuid)
        mutableDoc.apply {
            setString("surveyId", schedule.surveyId)
            if(schedule.triggerTime != null) setLong("triggerTime", schedule.triggerTime)
        }

        collection.save(mutableDoc)
        return uuid
    }

    override fun setActualTriggerTime(scheduleId: String, timestamp: Long) {
        val doc = collection.getDocument(scheduleId)?.toMutable() ?: run {
            Log.w(TAG, "Cannot setActualTriggerTime: Schedule $scheduleId not found")
            return
        }
        doc.setLong("actualTriggerTime", timestamp)
        collection.save(doc)
    }

    override fun setSurveyStartTime(scheduleId: String, timestamp: Long) {
        val doc = collection.getDocument(scheduleId)?.toMutable() ?: run {
            Log.w(TAG, "Cannot setSurveyStartTime: Schedule $scheduleId not found")
            return
        }
        doc.setLong("surveyStartTime", timestamp)
        collection.save(doc)
    }

    override fun setResponseSubmissionTime(scheduleId: String, timestamp: Long) {
        val doc = collection.getDocument(scheduleId)?.toMutable() ?: run {
            Log.w(TAG, "Cannot setResponseSubmissionTime: Schedule $scheduleId not found")
            return
        }
        doc.setLong("responseSubmissionTime", timestamp)
        collection.save(doc)
    }

    override fun resetSchedule() {
        val query = QueryBuilder.select(SelectResult.expression(Meta.id).`as`("uuid"))
            .from(DataSource.collection(collection))

        query.execute().use {
            for(result in it) {
                val docUuid = result.getString("uuid")!!
                val docToDelete = collection.getDocument(docUuid)

                if(docToDelete != null) collection.delete(docToDelete)
            }
        }
    }

}

/** Couchbase returns 0 for a field that was never set, which would upload as a 1970 timestamp. */
private fun DictionaryInterface.getLongOrNull(key: String): Long? =
    if (contains(key)) getLong(key) else null
