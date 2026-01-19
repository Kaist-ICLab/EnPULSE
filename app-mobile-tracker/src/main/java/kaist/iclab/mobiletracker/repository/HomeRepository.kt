package kaist.iclab.mobiletracker.repository

import kotlinx.coroutines.flow.Flow

/**
 * Data class holding aggregated daily sensor counts for the Home screen dashboard.
 */
data class DailySensorCounts(
    // Phone sensors
    val locationCount: Int = 0,
    val appUsageCount: Int = 0,
    val activityCount: Int = 0,
    val batteryCount: Int = 0,
    val notificationCount: Int = 0,
    val screenCount: Int = 0,
    val connectivityCount: Int = 0,
    val bluetoothCount: Int = 0,
    val ambientLightCount: Int = 0,
    val appListChangeCount: Int = 0,
    val callLogCount: Int = 0,
    val dataTrafficCount: Int = 0,
    val deviceModeCount: Int = 0,
    val mediaCount: Int = 0,
    val messageLogCount: Int = 0,
    val userInteractionCount: Int = 0,
    val wifiScanCount: Int = 0,
    // Watch sensors
    val watchHeartRateCount: Int = 0,
    val watchAccelerometerCount: Int = 0,
    val watchEDACount: Int = 0,
    val watchPPGCount: Int = 0,
    val watchSkinTemperatureCount: Int = 0
)

/**
 * Repository interface for Home screen dashboard data.
 * Abstracts the DAO layer from the ViewModel.
 */
interface HomeRepository {
    /**
     * Returns a reactive flow of daily sensor counts, starting from the given timestamp.
     * @param startOfDay The Unix timestamp for the start of the day (midnight).
     * @return A Flow emitting the aggregated sensor counts.
     */
    fun getDailySensorCounts(startOfDay: Long): Flow<DailySensorCounts>

    /**
     * Returns a reactive flow of the watch connection info including device names.
     */
    fun getWatchConnectionInfo(): Flow<WatchConnectionInfo>
}
