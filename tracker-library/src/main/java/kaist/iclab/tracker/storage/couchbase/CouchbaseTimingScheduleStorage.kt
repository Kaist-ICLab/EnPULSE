package kaist.iclab.tracker.storage.couchbase

import android.util.Log
import com.couchbase.lite.DataSource
import com.couchbase.lite.Expression
import com.couchbase.lite.Meta
import com.couchbase.lite.MutableDocument
import com.couchbase.lite.Ordering
import com.couchbase.lite.QueryBuilder
import com.couchbase.lite.SelectResult
import kaist.iclab.tracker.TrackerUtil.formatLocalDateTime
import kaist.iclab.tracker.sensor.timing.TimingSchedule
import kaist.iclab.tracker.storage.core.TimingScheduleStorage
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Persists [TimingSensor][kaist.iclab.tracker.sensor.phone.TimingSensor] fire-instant bookkeeping
 * to Couchbase. Mirrors [CouchbaseSurveyScheduleStorage] closely, keyed by a schedule entry's
 * `value` label instead of a `surveyId`, and only tracks a single `firedAt` marker.
 */
class CouchbaseTimingScheduleStorage(
    couchbase: CouchbaseDB,
    collectionName: String,
) : TimingScheduleStorage {
    companion object {
        private val TAG = CouchbaseTimingScheduleStorage::class.simpleName
    }

    private val collection = couchbase.getCollection(collectionName)

    override fun getNextSchedule(value: String?): TimingSchedule? {
        val now = System.currentTimeMillis()
        val query = QueryBuilder.select(SelectResult.expression(Meta.id).`as`("uuid"), SelectResult.all())
            .from(DataSource.collection(collection))
            .where(
                Expression.property("firedAt").isNotValued().and(
                    Expression.property("triggerTime").greaterThan(Expression.value(now - TimeUnit.MINUTES.toMillis(5)))
                ).run {
                    if (value != null) and(Expression.property("value").equalTo(Expression.value(value)))
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
                if (resultDict == null) null else TimingSchedule(
                    scheduleId = docUuid,
                    value = resultDict.getString("value") ?: "",
                    triggerTime = resultDict.getLong("triggerTime"),
                )
            }

            if (result != null) Log.d(TAG, "Next schedule for $value: value=${result.value}, uuid=${result.scheduleId!!}, triggerTime=${result.triggerTime?.formatLocalDateTime()}")
            return result
        } catch (e: Exception) {
            if (e is NoSuchElementException) Log.d(TAG, "No next schedule today for $value")
            else e.printStackTrace()
        }

        return null
    }

    override fun getLastSchedule(value: String?): TimingSchedule? {
        val query = QueryBuilder.select(SelectResult.expression(Meta.id).`as`("uuid"), SelectResult.all())
            .from(DataSource.collection(collection))
            .run {
                if (value != null) where(Expression.property("value").equalTo(Expression.value(value)))
                else where(Expression.value(true))
            }
            .orderBy(Ordering.property("triggerTime").descending())
            .limit(Expression.intValue(1))

        try {
            val result = query.execute().use {
                val result = it.first()
                val docUuid = result.getString("uuid")!!

                val resultDict = result.getDictionary(collection.name)
                if (resultDict == null) null else TimingSchedule(
                    scheduleId = docUuid,
                    value = resultDict.getString("value") ?: "",
                    triggerTime = resultDict.getLong("triggerTime"),
                )
            }

            if (result != null) Log.d(TAG, "Last schedule: value=${result.value}, uuid=${result.scheduleId!!}, triggerTime=${result.triggerTime?.formatLocalDateTime()}")
            else Log.d(TAG, "No last schedule for $value")

            return result
        } catch (e: Exception) {
            if (e is NoSuchElementException) Log.d(TAG, "No last schedule for $value")
            else e.printStackTrace()
            return null
        }
    }

    override fun addSchedule(schedule: TimingSchedule): String {
        val uuid = UUID.randomUUID().toString()

        val mutableDoc = MutableDocument(uuid)
        mutableDoc.apply {
            setString("value", schedule.value)
            if (schedule.triggerTime != null) setLong("triggerTime", schedule.triggerTime)
        }

        collection.save(mutableDoc)
        return uuid
    }

    override fun setFiredAt(scheduleId: String, timestamp: Long) {
        val doc = collection.getDocument(scheduleId)?.toMutable()!!
        doc.setLong("firedAt", timestamp)
        collection.save(doc)
    }

    override fun resetSchedule() {
        val query = QueryBuilder.select(SelectResult.expression(Meta.id).`as`("uuid"))
            .from(DataSource.collection(collection))

        query.execute().use {
            for (result in it) {
                val docUuid = result.getString("uuid")!!
                val docToDelete = collection.getDocument(docUuid)

                if (docToDelete != null) collection.delete(docToDelete)
            }
        }
    }
}
