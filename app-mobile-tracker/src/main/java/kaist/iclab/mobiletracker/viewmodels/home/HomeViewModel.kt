package kaist.iclab.mobiletracker.viewmodels.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.data.sensors.phone.ProfileData
import kaist.iclab.mobiletracker.repository.CampaignSensorRepository
import kaist.iclab.mobiletracker.repository.HomeRepository
import kaist.iclab.mobiletracker.repository.SurveyRepository
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.mobiletracker.repository.WatchConnectionInfo
import kaist.iclab.mobiletracker.repository.WatchConnectionStatus
import kaist.iclab.mobiletracker.repository.onSuccess
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * UI state for the Home screen.
 *
 * @property isTrackingActive Whether sensor data collection is currently active
 * @property watchStatus Connection status of the paired smartwatch
 * @property connectedDevices List of currently connected device names
 * @property userName Display name of the logged-in user
 */
data class HomeUiState(
    val isTrackingActive: Boolean = false,
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
    val watchSkinTemperatureCount: Int = 0,
    // Other
    val watchStatus: WatchConnectionStatus = WatchConnectionStatus.DISCONNECTED,
    val connectedDevices: List<String> = emptyList(),
    val userName: String? = null
)

/**
 * ViewModel for the Home screen.
 *
 * Provides real-time sensor data counts for today and watch connection status.
 * The UI state is updated automatically using Kotlin Flows that combine:
 * - Background controller state (tracking on/off)
 * - User profile information
 * - Daily sensor counts from the repository
 * - Watch connection information
 *
 * @param homeRepository Repository for home screen data operations
 * @param backgroundController Controller managing sensor background collection
 * @param syncTimestampService Service for tracking sync timestamps
 * @param userProfileRepository Repository for user profile data
 */
class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val backgroundController: BackgroundController,
    private val syncTimestampService: SyncTimestampService,
    private val userProfileRepository: UserProfileRepository,
    private val campaignSensorRepository: CampaignSensorRepository,
    private val surveyRepository: SurveyRepository,
) : ViewModel() {
 
    init {
        viewModelScope.launch {
            userProfileRepository.profileFlow
                .map { it?.campaignId }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { campaignId ->
                    // Auto-fetch configs on startup or when campaign changes
                    campaignSensorRepository.fetchActiveSensors(campaignId.toLong())
                    surveyRepository.fetchAndPersistSurveys(campaignId.toInt())
                }
        }
    }

    private fun getStartOfDay(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // A flow that emits the start of day timestamp at initialization and then at least every minute
    // to ensure we capture midnight transitions.
    private val startOfDayFlow = flow {
        while (true) {
            emit(getStartOfDay())
            kotlinx.coroutines.delay(60000) // Refresh every minute
        }
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        backgroundController.controllerStateFlow,
        userProfileRepository.profileFlow,
        startOfDayFlow,
        homeRepository.getWatchConnectionInfo()
    ) { state, profile, startOfDay, watchInfo ->
        DataSnapshot(state, profile, startOfDay, watchInfo)
    }.flatMapLatest { (state, profile, startOfDay, watchInfo) ->
        homeRepository.getDailySensorCounts(startOfDay).map { counts ->
            HomeUiState(
                isTrackingActive = state.flag == ControllerState.FLAG.RUNNING,
                watchStatus = watchInfo.status,
                connectedDevices = watchInfo.connectedDevices,
                // Phone sensors
                locationCount = counts.locationCount,
                appUsageCount = counts.appUsageCount,
                activityCount = counts.activityCount,
                batteryCount = counts.batteryCount,
                notificationCount = counts.notificationCount,
                screenCount = counts.screenCount,
                connectivityCount = counts.connectivityCount,
                bluetoothCount = counts.bluetoothCount,
                ambientLightCount = counts.ambientLightCount,
                appListChangeCount = counts.appListChangeCount,
                callLogCount = counts.callLogCount,
                dataTrafficCount = counts.dataTrafficCount,
                deviceModeCount = counts.deviceModeCount,
                mediaCount = counts.mediaCount,
                messageLogCount = counts.messageLogCount,
                userInteractionCount = counts.userInteractionCount,
                wifiScanCount = counts.wifiScanCount,
                // Watch sensors
                watchHeartRateCount = counts.watchHeartRateCount,
                watchAccelerometerCount = counts.watchAccelerometerCount,
                watchEDACount = counts.watchEDACount,
                watchPPGCount = counts.watchPPGCount,
                watchSkinTemperatureCount = counts.watchSkinTemperatureCount,
                userName = profile?.email?.split("@")?.firstOrNull()
                    ?.replaceFirstChar { it.uppercase() } ?: "User"
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )


    private data class DataSnapshot(
        val state: ControllerState,
        val profile: ProfileData?,
        val startOfDay: Long,
        val watchInfo: WatchConnectionInfo
    )
}
