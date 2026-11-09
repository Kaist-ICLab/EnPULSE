package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.db.obx.PhoneSensorStore
import kaist.iclab.mobiletracker.di.AppCoroutineScope
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kaist.iclab.tracker.sensor.core.SensorEntity

/**
 * Implementation of PhoneSensorRepository backed by ObjectBox [PhoneSensorStore]s.
 * Delegates to the appropriate store based on sensor ID.
 */
class PhoneSensorRepositoryImpl(
    private val sensorDataStorages: Map<String, PhoneSensorStore<*>>,
    private val supabaseHelper: SupabaseHelper,
    @Suppress("unused") private val appScope: AppCoroutineScope
) : PhoneSensorRepository {
    companion object {
        private const val TAG = "PhoneSensorRepository"
    }

    override suspend fun insertSensorDataBatch(
        sensorId: String,
        entities: List<SensorEntity>
    ): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insertBatch $sensorId") {
            val store = sensorDataStorages[sensorId]
                ?: throw IllegalStateException("No store found for sensor ID: $sensorId")
            val userUuid = SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient)
            store.insertBatch(entities, userUuid)
        }
    }

    override suspend fun deleteAllSensorData(sensorId: String): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "deleteAll $sensorId") {
            val store = sensorDataStorages[sensorId]
                ?: throw IllegalStateException("No store found for sensor ID: $sensorId")
            store.deleteAll()
        }
    }

    override fun hasStorageForSensor(sensorId: String): Boolean {
        return sensorDataStorages.containsKey(sensorId)
    }

    override suspend fun getRecordCount(sensorId: String): Int {
        return ErrorClassifier.runClassified(TAG, "getRecordCount $sensorId") {
            sensorDataStorages[sensorId]?.recordCount() ?: 0
        }.getOrNull() ?: 0
    }
}
