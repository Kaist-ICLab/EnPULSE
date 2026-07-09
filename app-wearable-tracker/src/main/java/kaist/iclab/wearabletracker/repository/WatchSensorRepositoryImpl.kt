package kaist.iclab.wearabletracker.repository

import kaist.iclab.wearabletracker.db.obx.WatchSensorStore
import kaist.iclab.wearabletracker.helpers.SyncPreferencesHelper
import kotlinx.coroutines.flow.Flow

class WatchSensorRepositoryImpl(
    private val sensorDataStorages: Map<String, WatchSensorStore<*>>,
    private val syncPreferencesHelper: SyncPreferencesHelper
) : WatchSensorRepository {
    private val TAG = WatchSensorRepositoryImpl::class.simpleName ?: "WatchSensorRepositoryImpl"

    override suspend fun deleteAllSensorData(): Result<Unit> =
        ErrorClassifier.runClassified(TAG, "delete all sensor data") {
            sensorDataStorages.values.forEach { it.deleteAll() }
        }

    override fun getLastSyncTimestamp(): Long? = syncPreferencesHelper.getLastSyncTimestamp()

    override val lastSyncTimestampFlow: Flow<Long?> = syncPreferencesHelper.lastSyncTimestampFlow

    override fun saveLastSyncTimestamp(timestamp: Long) {
        syncPreferencesHelper.saveLastSyncTimestamp(timestamp)
    }

    override suspend fun getTotalRecordCount(): Int =
        sensorDataStorages.values.sumOf { it.getCount() }

    override suspend fun getRecordCountSince(timestamp: Long): Int =
        sensorDataStorages.values.sumOf { it.getCountSince(timestamp) }

    override val autoSyncEnabledFlow: Flow<Boolean> = syncPreferencesHelper.autoSyncEnabledFlow
    override val autoSyncIntervalFlow: Flow<Long> = syncPreferencesHelper.autoSyncIntervalFlow

    override fun setAutoSyncEnabled(enabled: Boolean) {
        syncPreferencesHelper.setAutoSyncEnabled(enabled)
    }

    override fun setAutoSyncInterval(intervalMs: Long) {
        syncPreferencesHelper.setAutoSyncInterval(intervalMs)
    }
}
