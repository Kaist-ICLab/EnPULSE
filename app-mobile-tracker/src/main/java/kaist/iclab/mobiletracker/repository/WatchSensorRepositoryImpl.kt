package kaist.iclab.mobiletracker.repository

import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import io.objectbox.BoxStore
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchAccelerometerEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchEDAEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchHeartRateEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchPPGEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchSkinTemperatureEntity
import kaist.iclab.mobiletracker.db.obx.SensorStore
import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Implementation of WatchSensorRepository backed by ObjectBox stores.
 */
class WatchSensorRepositoryImpl(
    private val context: android.content.Context,
    private val boxStore: BoxStore,
    private val stores: SensorStores,
    private val supabaseHelper: SupabaseHelper
) : WatchSensorRepository {

    companion object {
        private const val TAG = "WatchSensorRepository"
    }

    private val storeById: Map<String, SensorStore<*>> = mapOf(
        Constants.SensorId.HEART_RATE to stores.watchHeartRate,
        Constants.SensorId.ACCELEROMETER to stores.watchAccelerometer,
        Constants.SensorId.EDA to stores.watchEDA,
        Constants.SensorId.PPG to stores.watchPPG,
        Constants.SensorId.SKIN_TEMPERATURE to stores.watchSkinTemperature,
        Constants.SensorId.LOCATION to stores.location
    )

    private suspend fun userUuid(): String =
        SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""

    override suspend fun insertHeartRateData(entities: List<WatchHeartRateEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert HeartRate") {
            if (entities.isNotEmpty()) {
                val uuid = userUuid()
                stores.watchHeartRate.insertBatch(entities.map { it.copy(uuid = uuid) })
            }
        }
    }

    override suspend fun insertAccelerometerData(entities: List<WatchAccelerometerEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert Accelerometer") {
            if (entities.isNotEmpty()) {
                val uuid = userUuid()
                stores.watchAccelerometer.insertBatch(entities.map { it.copy(uuid = uuid) })
            }
        }
    }

    override suspend fun insertEDAData(entities: List<WatchEDAEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert EDA") {
            if (entities.isNotEmpty()) {
                val uuid = userUuid()
                stores.watchEDA.insertBatch(entities.map { it.copy(uuid = uuid) })
            }
        }
    }

    override suspend fun insertPPGData(entities: List<WatchPPGEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert PPG") {
            if (entities.isNotEmpty()) {
                val uuid = userUuid()
                stores.watchPPG.insertBatch(entities.map { it.copy(uuid = uuid) })
            }
        }
    }

    override suspend fun insertSkinTemperatureData(entities: List<WatchSkinTemperatureEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert SkinTemperature") {
            if (entities.isNotEmpty()) {
                val uuid = userUuid()
                stores.watchSkinTemperature.insertBatch(entities.map { it.copy(uuid = uuid) })
            }
        }
    }

    override suspend fun insertLocationData(entities: List<LocationEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert Location") {
            if (entities.isNotEmpty()) {
                val uuid = userUuid()
                stores.location.insertBatch(entities.map { it.copy(uuid = uuid) })
            }
        }
    }

    override suspend fun getLatestTimestamp(sensorId: String): Long? {
        return ErrorClassifier.runClassified(TAG, "getLatestTimestamp $sensorId") {
            if (sensorId == Constants.SensorId.LOCATION) {
                stores.location.latestTimestampByDeviceType(DeviceType.WATCH.value)
            } else {
                storeById[sensorId]?.latestTimestamp()
            }
        }.getOrNull()
    }

    override suspend fun getRecordCount(sensorId: String): Int {
        return ErrorClassifier.runClassified(TAG, "getRecordCount $sensorId") {
            if (sensorId == Constants.SensorId.LOCATION) {
                stores.location.countByDeviceType(DeviceType.WATCH.value)
            } else {
                storeById[sensorId]?.count()?.toInt() ?: 0
            }
        }.getOrNull() ?: 0
    }

    override suspend fun deleteAllSensorData(sensorId: String): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "deleteAll $sensorId") {
            val store = storeById[sensorId]
                ?: throw IllegalArgumentException("Unknown sensor ID: $sensorId")
            store.removeAll()
        }
    }

    override suspend fun flushAllData(): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "flush all watch data") {
            boxStore.runInTx {
                storeById.values.forEach { it.removeAll() }
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
