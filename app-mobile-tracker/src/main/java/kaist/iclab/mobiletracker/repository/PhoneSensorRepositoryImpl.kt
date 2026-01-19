package kaist.iclab.mobiletracker.repository

import android.util.Log
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.dao.common.LocationDao
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kaist.iclab.tracker.sensor.core.SensorEntity

/**
 * Implementation of PhoneSensorRepository using Room database DAOs.
 * Delegates to appropriate DAOs based on sensor ID.
 */
class PhoneSensorRepositoryImpl(
    private val sensorDataStorages: Map<String, BaseDao<*, *>>,
    private val supabaseHelper: SupabaseHelper
) : PhoneSensorRepository {

    companion object {
        private const val TAG = "PhoneSensorRepository"
    }

    override suspend fun insertSensorData(sensorId: String, entity: SensorEntity): Result<Unit> {
        return runCatchingSuspend {
            @Suppress("UNCHECKED_CAST")
            val dao = sensorDataStorages[sensorId] as? BaseDao<SensorEntity, *>
            if (dao != null) {
                // Get user UUID from Supabase session (nullable if not logged in)
                val userUuid = SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient)
                dao.insert(entity, userUuid)
            } else {
                val error = IllegalStateException("No DAO found for sensor ID: $sensorId")
                Log.w(TAG, error.message ?: "Unknown error")
                throw error
            }
        }
    }

    override suspend fun deleteAllSensorData(sensorId: String): Result<Unit> {
        return runCatchingSuspend {
            @Suppress("UNCHECKED_CAST")
            val dao = sensorDataStorages[sensorId] as? BaseDao<*, *>
            if (dao != null) {
                dao.deleteAll()
            } else {
                val error = IllegalStateException("No DAO found for sensor ID: $sensorId")
                Log.w(TAG, error.message ?: "Unknown error")
                throw error
            }
        }
    }

    override fun hasStorageForSensor(sensorId: String): Boolean {
        return sensorDataStorages.containsKey(sensorId)
    }

    override suspend fun flushAllData(): Result<Unit> {
        return runCatchingSuspend {
            sensorDataStorages.values.forEach { dao ->
                @Suppress("UNCHECKED_CAST")
                (dao as? BaseDao<*, *>)?.deleteAll()
            }
        }
    }

    override suspend fun getLatestRecordedTimestamp(sensorId: String): Long? {
        return runCatchingSuspend {
            // Special handling for Location sensor to only count phone data
            if (sensorId == "Location") {
                val locationDao = sensorDataStorages[sensorId] as? LocationDao
                locationDao?.getLatestTimestampByDeviceType(DeviceType.PHONE.value)
            } else {
                @Suppress("UNCHECKED_CAST")
                val dao = sensorDataStorages[sensorId] as? BaseDao<*, *>
                dao?.getLatestTimestamp()
            }
        }.getOrNull()
    }

    override suspend fun getRecordCount(sensorId: String): Int {
        return runCatchingSuspend {
            // Special handling for Location sensor to only count phone data
            if (sensorId == "Location") {
                val locationDao = sensorDataStorages[sensorId] as? LocationDao
                locationDao?.getRecordCountByDeviceType(DeviceType.PHONE.value) ?: 0
            } else {
                @Suppress("UNCHECKED_CAST")
                val dao = sensorDataStorages[sensorId] as? BaseDao<*, *>
                dao?.getRecordCount() ?: 0
            }
        }.getOrNull() ?: 0
    }
}

