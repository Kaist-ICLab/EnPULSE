package kaist.iclab.mobiletracker.repository

import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import io.objectbox.BoxStore
import kaist.iclab.mobiletracker.Constants
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
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
        Constants.SensorId.LOCATION to stores.location,
        Constants.SensorId.ECG to stores.ecg
    )

    private suspend fun userUuid(): String =
        SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient) ?: ""

    @Suppress("UNCHECKED_CAST")
    override suspend fun insertBatch(sensorId: String, entities: List<BaseEntity>): Result<Unit> {
        return ErrorClassifier.runClassified(TAG, "insert $sensorId") {
            if (entities.isNotEmpty()) {
                val uuid = userUuid()
                val store = storeById[sensorId] as? SensorStore<BaseEntity>
                    ?: throw IllegalStateException("No store for sensor: $sensorId")
                store.insertBatch(entities.onEach { it.uuid = uuid })
            }
        }
    }

    override suspend fun getLatestTimestamp(sensorId: String): Long? {
        return ErrorClassifier.runClassified(TAG, "getLatestTimestamp $sensorId") {
            if (sensorId == Constants.SensorId.LOCATION) {
                stores.location.latestTimestamp(DeviceType.WATCH.value)
            } else {
                storeById[sensorId]?.latestTimestamp()
            }
        }.getOrNull()
    }

    override suspend fun getRecordCount(sensorId: String): Int {
        return ErrorClassifier.runClassified(TAG, "getRecordCount $sensorId") {
            if (sensorId == Constants.SensorId.LOCATION) {
                stores.location.count(DeviceType.WATCH.value)
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
                    val allNodes =
                        capabilityClient.getCapability(capabilityName, CapabilityClient.FILTER_ALL)
                            .await()

                    if (allNodes.nodes.isEmpty()) {
                        trySend(WatchConnectionInfo(WatchConnectionStatus.NOT_INSTALLED, emptyList()))
                    } else {
                        val reachableNodes = capabilityClient.getCapability(
                            capabilityName,
                            CapabilityClient.FILTER_REACHABLE
                        ).await()
                        if (reachableNodes.nodes.isEmpty()) {
                            trySend(WatchConnectionInfo(WatchConnectionStatus.DISCONNECTED, allNodes.nodes.map { it.displayName }))
                        } else {
                            trySend(WatchConnectionInfo(WatchConnectionStatus.CONNECTED, reachableNodes.nodes.map { it.displayName }))
                        }
                    }
                } catch (e: Exception) {
                    trySend(WatchConnectionInfo(WatchConnectionStatus.DISCONNECTED, emptyList()))
                }
            }
        }

        val listener = CapabilityClient.OnCapabilityChangedListener { _ -> updateStatus() }
        capabilityClient.addListener(listener, capabilityName)
        updateStatus()

        awaitClose { capabilityClient.removeListener(listener) }
    }.onStart {
        emit(WatchConnectionInfo())
    }
}
