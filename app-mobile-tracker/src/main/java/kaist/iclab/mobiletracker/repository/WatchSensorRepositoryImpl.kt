package kaist.iclab.mobiletracker.repository

import androidx.room.withTransaction
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.TrackerRoomDB
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.dao.common.LocationDao
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchAccelerometerEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchEDAEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchGestureEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchHeartRateEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchIMUEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchPPGEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchSkinTemperatureEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchStressEntity
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
        private const val TAG = "WatchSensorRepository"
    }

    override suspend fun insertHeartRateData(entities: List<WatchHeartRateEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert HeartRate") {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    @Suppress("UNCHECKED_CAST")
                    val dao =
                        watchSensorDaos[Constants.SensorId.HEART_RATE] as? BaseDao<WatchHeartRateEntity, *>
                            ?: throw IllegalStateException("No DAO found for HeartRate sensor")
                    dao.insertBatch(entitiesWithUuid, userUuid)
                }
            }
        }
    }

    override suspend fun insertAccelerometerData(entities: List<WatchAccelerometerEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert Accelerometer") {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    @Suppress("UNCHECKED_CAST")
                    val dao =
                        watchSensorDaos[Constants.SensorId.ACCELEROMETER] as? BaseDao<WatchAccelerometerEntity, *>
                            ?: throw IllegalStateException("No DAO found for Accelerometer sensor")
                    dao.insertBatch(entitiesWithUuid, userUuid)
                }
            }
        }
    }

    override suspend fun insertEDAData(entities: List<WatchEDAEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert EDA") {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    @Suppress("UNCHECKED_CAST")
                    val dao =
                        watchSensorDaos[Constants.SensorId.EDA] as? BaseDao<WatchEDAEntity, *>
                            ?: throw IllegalStateException("No DAO found for EDA sensor")
                    dao.insertBatch(entitiesWithUuid, userUuid)
                }
            }
        }
    }

    override suspend fun insertPPGData(entities: List<WatchPPGEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert PPG") {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    @Suppress("UNCHECKED_CAST")
                    val dao =
                        watchSensorDaos[Constants.SensorId.PPG] as? BaseDao<WatchPPGEntity, *>
                            ?: throw IllegalStateException("No DAO found for PPG sensor")
                    dao.insertBatch(entitiesWithUuid, userUuid)
                }
            }
        }
    }

    override suspend fun insertSkinTemperatureData(entities: List<WatchSkinTemperatureEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert SkinTemperature") {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    @Suppress("UNCHECKED_CAST")
                    val dao =
                        watchSensorDaos[Constants.SensorId.SKIN_TEMPERATURE] as? BaseDao<WatchSkinTemperatureEntity, *>
                            ?: throw IllegalStateException("No DAO found for SkinTemperature sensor")
                    dao.insertBatch(entitiesWithUuid, userUuid)
                }
            }
        }
    }

    override suspend fun insertLocationData(entities: List<LocationEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert Location") {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    val dao =
                        watchSensorDaos[Constants.SensorId.LOCATION] as? LocationDao
                            ?: throw IllegalStateException("No DAO found for Location sensor")
                    dao.insertLocationEntities(entitiesWithUuid)
                }
            }
        }
    }

    override suspend fun insertIMUData(entities: List<WatchIMUEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert IMU") {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    @Suppress("UNCHECKED_CAST")
                    val dao =
                        watchSensorDaos["WatchIMU"] as? BaseDao<WatchIMUEntity, *>
                            ?: throw IllegalStateException("No DAO found for IMU sensor")
                    dao.insertBatch(entitiesWithUuid, userUuid)
                }
            }
        }
    }

    override suspend fun insertGestureData(entities: List<WatchGestureEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert Gesture") {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    @Suppress("UNCHECKED_CAST")
                    val dao =
                        watchSensorDaos["WatchGesture"] as? BaseDao<WatchGestureEntity, *>
                            ?: throw IllegalStateException("No DAO found for Gesture sensor")
                    dao.insertBatch(entitiesWithUuid, userUuid)
                }
            }
        }
    }

    override suspend fun insertStressData(entities: List<WatchStressEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert Stress") {
            if (entities.isNotEmpty()) {
                val userUuid =
                    SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""
                val entitiesWithUuid = entities.map { it.copy(uuid = userUuid) }
                db.withTransaction {
                    @Suppress("UNCHECKED_CAST")
                    val dao =
                        watchSensorDaos["WatchStress"] as? BaseDao<WatchStressEntity, *>
                            ?: throw IllegalStateException("No DAO found for Stress sensor")
                    dao.insertBatch(entitiesWithUuid, userUuid)
                }
            }
        }
    }

    override suspend fun getLatestTimestamp(sensorId: String): Long? {
        return ErrorClassifier.runClassified(TAG, "getLatestTimestamp $sensorId") {
            if (sensorId == Constants.SensorId.LOCATION) {
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
        return ErrorClassifier.runClassified(TAG, "getRecordCount $sensorId") {
            if (sensorId == Constants.SensorId.LOCATION) {
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
        return ErrorClassifier.runClassified(TAG, "deleteAll $sensorId") {
            @Suppress("UNCHECKED_CAST")
            val dao = watchSensorDaos[sensorId] as? BaseDao<*, *>
                ?: throw IllegalArgumentException("Unknown sensor ID: $sensorId")
            dao.deleteAll()
        }
    }

    override suspend fun flushAllData(): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "flush all watch data") {
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
                        trySend(
                            WatchConnectionInfo(
                                WatchConnectionStatus.NOT_INSTALLED,
                                emptyList()
                            )
                        )
                    } else {
                        // 2. Check if any node is currently REACHABLE
                        val reachableNodes = capabilityClient.getCapability(
                            capabilityName,
                            CapabilityClient.FILTER_REACHABLE
                        ).await()
                        if (reachableNodes.nodes.isEmpty()) {
                            // Get device names from all nodes (installed but not reachable)
                            val deviceNames = allNodes.nodes.map { it.displayName }
                            trySend(
                                WatchConnectionInfo(
                                    WatchConnectionStatus.DISCONNECTED,
                                    deviceNames
                                )
                            )
                        } else {
                            // Get device names from reachable nodes
                            val deviceNames = reachableNodes.nodes.map { it.displayName }
                            trySend(
                                WatchConnectionInfo(
                                    WatchConnectionStatus.CONNECTED,
                                    deviceNames
                                )
                            )
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

