package kaist.iclab.mobiletracker.viewmodels.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.data.campaign.CampaignData
import kaist.iclab.mobiletracker.data.survey.SurveyConfig
import kaist.iclab.mobiletracker.repository.CampaignRepository
import kaist.iclab.mobiletracker.repository.CampaignSensorRepository
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.SurveyRepository
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.mobiletracker.repository.onFailure
import kaist.iclab.mobiletracker.repository.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for OnboardingScreen
 */
data class OnboardingUiState(
    val campaigns: List<CampaignData> = emptyList(),
    val selectedCampaign: CampaignData? = null,
    val surveyConfigs: List<SurveyConfig> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
)

/**
 * ViewModel for handling campaign onboarding flow
 */
class OnboardingViewModel(
    private val campaignRepository: CampaignRepository,
    private val userProfileRepository: UserProfileRepository,
    private val surveyRepository: SurveyRepository,
    private val campaignSensorRepository: CampaignSensorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun loadCampaigns() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            campaignRepository.fetchCampaigns()
                .onSuccess { campaigns ->
                    _uiState.update { it.copy(campaigns = campaigns, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun selectCampaign(campaign: CampaignData) {
        _uiState.update { it.copy(selectedCampaign = campaign) }
    }

    fun confirmSelection() {
        val selectedCampaign = _uiState.value.selectedCampaign ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = userProfileRepository.updateCampaignId(selectedCampaign.id)) {
                is Result.Success -> {
                    // Fetch surveys
                    val surveyResult = surveyRepository.fetchAndPersistSurveys(selectedCampaign.id)

                    // Fetch active sensors for the campaign
                    val sensorResult =
                        campaignSensorRepository.fetchActiveSensors(selectedCampaign.id.toLong())

                    // Refresh profile to trigger navigation
                    userProfileRepository.refreshProfile()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isComplete = true,
                            error = if (surveyResult.isError) "Campaign saved, but failed to load surveys" else null
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    suspend fun joinCampaign(password: String): Boolean {
        val selectedCampaign = _uiState.value.selectedCampaign ?: return false
        return when (val result =
            campaignRepository.joinCampaign(selectedCampaign.idString, password)) {
            is Result.Success<Boolean> -> result.data
            is Result.Error -> false
        }
    }
}
