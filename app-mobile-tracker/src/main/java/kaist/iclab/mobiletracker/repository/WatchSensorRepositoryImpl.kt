package kaist.iclab.mobiletracker.repository

import androidx.room.withTransaction
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kaist.iclab.mobiletracker.db.TrackerRoomDB
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.dao.common.LocationDao
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.watch.WatchAccelerometerEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchEDAEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchHeartRateEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchPPGEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchSkinTemperatureEntity
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Implementation of WatchSensorRepository using Room database.
 * Uses a Map pattern similar to PhoneSensorRepository for consistency.
 * Now uses unified BaseDao interface for both phone and watch sensors.
 * All operations (inserts and queries) go through the Map pattern for full abstraction.
 */
class WatchSensorRepositoryImpl(
    private val context: android.content.Context,
    private val db: TrackerRoomDB,
    private val watchSensorDaos: Map<String, BaseDao<*, *>>,
    private val supabaseHelper: SupabaseHelper
) : WatchSensorRepository {

    companion object {
        // Watch sensor IDs (matching handler sensorId values)
        private const val HEART_RATE_SENSOR_ID = "WatchHeartRate"
        private const val ACCELEROMETER_SENSOR_ID = "WatchAccelerometer"
        private const val EDA_SENSOR_ID = "WatchEDA"
        private const val PPG_SENSOR_ID = "WatchPPG"
        private const val SKIN_TEMPERATURE_SENSOR_ID = "WatchSkinTemperature"
        private const val LOCATION_SENSOR_ID = "WatchLocation"
    }

    override suspend fun insertHeartRateData(entities: List<WatchHeartRateEntity>): Result<Unit> {
        return runCatchingSuspend {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    @Suppress("UNCHECKED_CAST")
                    val dao =
                        watchSensorDaos[HEART_RATE_SENSOR_ID] as? BaseDao<WatchHeartRateEntity, *>
                    if (dao != null) {
                        dao.insertBatch(entitiesWithUuid, userUuid)
                    } else {
                        throw IllegalStateException("No DAO found for HeartRate sensor")
                    }
                }
            }
        }
    }

    override suspend fun insertAccelerometerData(entities: List<WatchAccelerometerEntity>): Result<Unit> {
        return runCatchingSuspend {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    @Suppress("UNCHECKED_CAST")
                    val dao =
                        watchSensorDaos[ACCELEROMETER_SENSOR_ID] as? BaseDao<WatchAccelerometerEntity, *>
                    if (dao != null) {
                        dao.insertBatch(entitiesWithUuid, userUuid)
                    } else {
                        throw IllegalStateException("No DAO found for Accelerometer sensor")
                    }
                }
            }
        }
    }

    override suspend fun insertEDAData(entities: List<WatchEDAEntity>): Result<Unit> {
        return runCatchingSuspend {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    @Suppress("UNCHECKED_CAST")
                    val dao =
                        watchSensorDaos[EDA_SENSOR_ID] as? BaseDao<WatchEDAEntity, *>
                    if (dao != null) {
                        dao.insertBatch(entitiesWithUuid, userUuid)
                    } else {
                        throw IllegalStateException("No DAO found for EDA sensor")
                    }
                }
            }
        }
    }

    override suspend fun insertPPGData(entities: List<WatchPPGEntity>): Result<Unit> {
        return runCatchingSuspend {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    @Suppress("UNCHECKED_CAST")
                    val dao =
                        watchSensorDaos[PPG_SENSOR_ID] as? BaseDao<WatchPPGEntity, *>
                    if (dao != null) {
                        dao.insertBatch(entitiesWithUuid, userUuid)
                    } else {
                        throw IllegalStateException("No DAO found for PPG sensor")
                    }
                }
            }
        }
    }

    override suspend fun insertSkinTemperatureData(entities: List<WatchSkinTemperatureEntity>): Result<Unit> {
        return runCatchingSuspend {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    @Suppress("UNCHECKED_CAST")
                    val dao =
                        watchSensorDaos[SKIN_TEMPERATURE_SENSOR_ID] as? BaseDao<WatchSkinTemperatureEntity, *>
                    if (dao != null) {
                        dao.insertBatch(entitiesWithUuid, userUuid)
                    } else {
                        throw IllegalStateException("No DAO found for SkinTemperature sensor")
                    }
                }
            }
        }
    }

    override suspend fun insertLocationData(entities: List<LocationEntity>): Result<Unit> {
        return runCatchingSuspend {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    val dao =
                        watchSensorDaos[LOCATION_SENSOR_ID] as? LocationDao
                    if (dao != null) {
                        dao.insertLocationEntities(entitiesWithUuid)
                    } else {
                        throw IllegalStateException("No DAO found for Location sensor")
                    }
                }
            }
        }
    }

    override suspend fun getLatestTimestamp(sensorId: String): Long? {
        return runCatchingSuspend {
            // Special handling for Location sensor to only count watch data
            if (sensorId == LOCATION_SENSOR_ID) {
                val locationDao = watchSensorDaos[sensorId] as? LocationDao
                locationDao?.getLatestTimestampByDeviceType(DeviceType.WATCH.value)
            } else {
                @Suppress("UNCHECKED_CAST")
                val dao = watchSensorDaos[sensorId] as? BaseDao<*, *>
                dao?.getLatestTimestamp()
            }
        }.getOrNull()
    }

    override suspend fun getRecordCount(sensorId: String): Int {
        return runCatchingSuspend {
            // Special handling for Location sensor to only count watch data
            if (sensorId == LOCATION_SENSOR_ID) {
                val locationDao = watchSensorDaos[sensorId] as? LocationDao
                locationDao?.getRecordCountByDeviceType(DeviceType.WATCH.value) ?: 0
            } else {
                @Suppress("UNCHECKED_CAST")
                val dao = watchSensorDaos[sensorId] as? BaseDao<*, *>
                dao?.getRecordCount() ?: 0
            }
        }.getOrNull() ?: 0
    }

    override suspend fun deleteAllSensorData(sensorId: String): Result<Unit> {
        return runCatchingSuspend {
            @Suppress("UNCHECKED_CAST")
            val dao = watchSensorDaos[sensorId] as? BaseDao<*, *>
            if (dao != null) {
                dao.deleteAll()
            } else {
                throw IllegalArgumentException("Unknown sensor ID: $sensorId")
            }
        }
    }

    override suspend fun flushAllData(): Result<Unit> {
        return runCatchingSuspend {
            db.withTransaction {
                watchSensorDaos.values.forEach { dao ->
                    @Suppress("UNCHECKED_CAST")
                    (dao as? BaseDao<*, *>)?.deleteAll()
                }
            }
        }
    }

    override fun getWatchConnectionInfo(): Flow<WatchConnectionInfo> = callbackFlow {
        val capabilityClient = Wearable.getCapabilityClient(context)
        val capabilityName = "watch_tracker_active"

        val updateStatus = {
            launch {
                try {
                    // 1. Check if ANY node has the capability (installed but maybe offline)
                    val allNodes =
                        capabilityClient.getCapability(capabilityName, CapabilityClient.FILTER_ALL)
                            .await()

                    if (allNodes.nodes.isEmpty()) {
                        trySend(WatchConnectionInfo(WatchConnectionStatus.NOT_INSTALLED, emptyList()))
                    } else {
                        // 2. Check if any node is currently REACHABLE
                        val reachableNodes = capabilityClient.getCapability(
                            capabilityName,
                            CapabilityClient.FILTER_REACHABLE
                        ).await()
                        if (reachableNodes.nodes.isEmpty()) {
                            // Get device names from all nodes (installed but not reachable)
                            val deviceNames = allNodes.nodes.map { it.displayName }
                            trySend(WatchConnectionInfo(WatchConnectionStatus.DISCONNECTED, deviceNames))
                        } else {
                            // Get device names from reachable nodes
                            val deviceNames = reachableNodes.nodes.map { it.displayName }
                            trySend(WatchConnectionInfo(WatchConnectionStatus.CONNECTED, deviceNames))
                        }
                    }
                } catch (e: Exception) {
                    trySend(WatchConnectionInfo(WatchConnectionStatus.DISCONNECTED, emptyList()))
                }
            }
        }

        val listener = CapabilityClient.OnCapabilityChangedListener { _ ->
            updateStatus()
        }

        capabilityClient.addListener(listener, capabilityName)

        // Initial check
        updateStatus()

        awaitClose {
            capabilityClient.removeListener(listener)
        }
    }.onStart {
        emit(WatchConnectionInfo())
    }

}
