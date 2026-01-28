package kaist.iclab.mobiletracker.viewmodels.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.data.campaign.CampaignData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.mobiletracker.services.CampaignService
import kaist.iclab.mobiletracker.services.ProfileService
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
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
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
)

/**
 * ViewModel for handling campaign onboarding flow
 */
class OnboardingViewModel(
    private val campaignService: CampaignService,
    private val profileService: ProfileService,
    private val supabaseHelper: SupabaseHelper,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    companion object {
        private const val TAG = "OnboardingViewModel"
    }

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Load available campaigns from Supabase
     */
    fun loadCampaigns() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = campaignService.getAllCampaigns()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            campaigns = result.data,
                            isLoading = false
                        )
                    }
                }

                is Result.Error -> {
                    Log.e(TAG, "Failed to load campaigns: ${result.message}", result.exception)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Select a campaign
     */
    fun selectCampaign(campaign: CampaignData) {
        _uiState.update { it.copy(selectedCampaign = campaign) }
    }

    /**
     * Confirm selection and save to profile
     */
    fun confirmSelection() {
        val selectedCampaign = _uiState.value.selectedCampaign ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val uuid = getUuidFromSession()
                if (uuid == null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Unable to get user session")
                    }
                    return@launch
                }

                when (val result = profileService.updateCampaignId(uuid, selectedCampaign.id)) {
                    is Result.Success -> {
                        // Refresh user profile to update cached data
                        refreshUserProfile(uuid)
                        _uiState.update {
                            it.copy(isLoading = false, isComplete = true)
                        }
                    }

                    is Result.Error -> {
                        Log.e(
                            TAG,
                            "Failed to update campaign: ${result.message}",
                            result.exception
                        )
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error confirming selection: ${e.message}", e)
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    /**
     * Get UUID from Supabase session using SupabaseSessionHelper
     */
    private fun getUuidFromSession(): String? {
        return try {
            SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting UUID: ${e.message}", e)
            null
        }
    }

    /**
     * Refresh user profile after updating campaign
     */
    private suspend fun refreshUserProfile(uuid: String) {
        when (val result = profileService.getProfileByUuid(uuid)) {
            is Result.Success -> {
                userProfileRepository.saveProfile(result.data)
            }

            is Result.Error -> {
                Log.e(TAG, "Error refreshing profile: ${result.message}", result.exception)
            }
        }
    }
}
