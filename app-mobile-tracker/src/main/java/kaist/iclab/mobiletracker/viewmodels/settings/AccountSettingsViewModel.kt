package kaist.iclab.mobiletracker.viewmodels.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.data.campaign.CampaignData
import kaist.iclab.mobiletracker.data.campaign.CampaignTableData
import kaist.iclab.mobiletracker.repository.AppError
import kaist.iclab.mobiletracker.repository.CampaignRepository
import kaist.iclab.mobiletracker.repository.CampaignSensorRepository
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountSettingsViewModel(
    private val campaignRepository: CampaignRepository,
    private val userProfileRepository: UserProfileRepository,
    campaignSensorRepository: CampaignSensorRepository,
    backgroundController: BackgroundController,
) : ViewModel() {

    // One-shot UI events (e.g. toasts), collected by the host screen.
    private val _uiEvent = MutableSharedFlow<AccountSettingsUiEvent>()
    val uiEvent: SharedFlow<AccountSettingsUiEvent> = _uiEvent.asSharedFlow()

    // Campaign state from repository
    val campaigns: StateFlow<List<CampaignData>> = campaignRepository.campaignsFlow

    // Active sensors for the currently selected campaign
    val activeSensors: StateFlow<List<CampaignTableData>> =
        campaignSensorRepository.activeSensorsFlow

    private val _isSyncingSurveys = MutableStateFlow(false)
    val isSyncingSurveys: StateFlow<Boolean> = _isSyncingSurveys.asStateFlow()

    private val _isReloadingConfig = MutableStateFlow(false)
    val isReloadingConfig: StateFlow<Boolean> = _isReloadingConfig.asStateFlow()

    private val _selectedCampaignId = MutableStateFlow<String?>(null)

    val isDataCollectionRunning: StateFlow<Boolean> = backgroundController.controllerStateFlow
        .map { it.flag == ControllerState.FLAG.RUNNING }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = backgroundController.controllerStateFlow.value.flag == ControllerState.FLAG.RUNNING
        )

    val selectedCampaignName: StateFlow<String?> = combine(
        _selectedCampaignId,
        campaigns
    ) { selectedId, campaignList ->
        selectedId?.toIntOrNull()?.let { id ->
            campaignList.find { it.id == id }?.name
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        // Loads campaign names so the current campaign can be shown by name.
        viewModelScope.launch { campaignRepository.fetchCampaigns() }

        viewModelScope.launch {
            userProfileRepository.profileFlow.collect { profile ->
                _selectedCampaignId.value = profile?.campaignId?.toString()
            }
        }
    }

    fun reloadConfig() {
        if (_isReloadingConfig.value) return

        viewModelScope.launch {
            _isReloadingConfig.value = true
            try {
                val result = userProfileRepository.syncFullStudyConfig()

                if (result.isSuccess) {
                    _uiEvent.emit(AccountSettingsUiEvent.ShowToast(R.string.toast_success_saved))
                } else {
                    val exception = (result as? Result.Error)?.exception
                    if (exception is AppError.CollectionRunning) {
                        _uiEvent.emit(AccountSettingsUiEvent.ShowToast(R.string.turn_off_data_collection_first))
                    } else if (exception is AppError.Network) {
                        _uiEvent.emit(AccountSettingsUiEvent.ShowToast(R.string.toast_config_download_failed))
                    } else {
                        _uiEvent.emit(AccountSettingsUiEvent.ShowToast(R.string.error_generic))
                    }
                }
            } finally {
                _isReloadingConfig.value = false
            }
        }
    }
}

/**
 * One-shot UI events emitted by [AccountSettingsViewModel].
 */
sealed interface AccountSettingsUiEvent {
    data class ShowToast(val messageResId: Int) : AccountSettingsUiEvent
}
