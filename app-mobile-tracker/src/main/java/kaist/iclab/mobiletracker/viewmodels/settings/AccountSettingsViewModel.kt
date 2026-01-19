package kaist.iclab.mobiletracker.viewmodels.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.data.campaign.CampaignData
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.Result
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.mobiletracker.services.CampaignService
import kaist.iclab.mobiletracker.services.ProfileService
import kaist.iclab.mobiletracker.utils.AppToast
import kaist.iclab.mobiletracker.utils.SupabaseSessionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountSettingsViewModel(
    private val campaignService: CampaignService,
    private val profileService: ProfileService,
    private val supabaseHelper: SupabaseHelper,
    private val userProfileRepository: UserProfileRepository,
    private val context: Context
) : ViewModel() {
    private val TAG = "AccountSettingsViewModel"
    
    // Campaign state
    private val _campaigns = MutableStateFlow<List<CampaignData>>(emptyList())
    val campaigns: StateFlow<List<CampaignData>> = _campaigns.asStateFlow()
    
    private val _isLoadingCampaigns = MutableStateFlow(false)
    val isLoadingCampaigns: StateFlow<Boolean> = _isLoadingCampaigns.asStateFlow()
    
    private val _campaignError = MutableStateFlow<String?>(null)
    val campaignError: StateFlow<String?> = _campaignError.asStateFlow()
    
    // Selected campaign ID (stored as String for UI compatibility)
    private val _selectedCampaignId = MutableStateFlow<String?>(null)
    val selectedCampaignId: StateFlow<String?> = _selectedCampaignId.asStateFlow()
    
    // Selected campaign name (reactively computed from selectedCampaignId and campaigns)
    val selectedCampaignName: StateFlow<String?> = combine(
        _selectedCampaignId,
        _campaigns
    ) { selectedId, campaigns ->
        if (selectedId == null) {
            null
        } else {
            val campaignIdInt = selectedId.toIntOrNull()
            campaignIdInt?.let { id ->
                campaigns.find { it.id == id }?.name
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    init {
        // Load campaigns when ViewModel is created
        fetchCampaigns()
        
        // Observe cached user profile from UserProfileRepository
        // This profile is loaded once after login, so we don't need to fetch it again
        viewModelScope.launch {
            userProfileRepository.profileFlow.collect { profile ->
                // Update selected campaign ID when profile changes
                profile?.campaign_id?.let { campaignId ->
                    _selectedCampaignId.value = campaignId.toString()
                } ?: run {
                    // Clear selection if profile has no campaign
                    _selectedCampaignId.value = null
                }
            }
        }
    }
    
    /**
     * Fetch all campaigns from Supabase
     * Only fetches if campaigns list is empty and not already loading
     */
    fun fetchCampaigns() {
        if (_campaigns.value.isNotEmpty() || _isLoadingCampaigns.value) {
            return
        }
        
        viewModelScope.launch {
            _isLoadingCampaigns.value = true
            _campaignError.value = null
            
            when (val result = campaignService.getAllCampaigns()) {
                is Result.Success -> {
                    _campaigns.value = result.data
                    _isLoadingCampaigns.value = false
                }
                is Result.Error -> {
                    _campaignError.value = result.message
                    _isLoadingCampaigns.value = false
                    Log.e(TAG, "Error fetching campaigns: ${result.message}", result.exception)
                }
            }
        }
    }
    
    /**
     * Select a campaign by ID and save it to the user's profile
     * @param campaignId The campaign ID to select (as String)
     */
    fun selectCampaign(campaignId: String) {
        _selectedCampaignId.value = campaignId
        
        // Save campaign to profile
        viewModelScope.launch {
            saveCampaignToProfile(campaignId)
        }
    }
    
    /**
     * Save the selected campaign to the user's profile
     * @param campaignId The campaign ID to save (as String)
     */
    private suspend fun saveCampaignToProfile(campaignId: String) {
        try {
            // Get UUID from Supabase session
            val uuid = getUuidFromSession()
            if (uuid == null) {
                Log.e(TAG, "Cannot save campaign: No UUID available")
                return
            }
            
            // Convert campaignId from String to Int
            val campaignIdInt = campaignId.toIntOrNull()
            if (campaignIdInt == null) {
                Log.e(TAG, "Invalid campaign ID format: $campaignId")
                return
            }
            
            // Update profile with campaign ID
            when (val result = profileService.updateCampaignId(uuid, campaignIdInt)) {
                is Result.Success -> {
                    // Campaign saved successfully, refresh cached profile
                    refreshUserProfile()
                    // Show success toast
                    AppToast.show(context, R.string.toast_experiment_group_selected)
                }
                is Result.Error -> {
                    Log.e(TAG, "Error saving campaign to profile: ${result.message}", result.exception)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveCampaignToProfile: ${e.message}", e)
        }
    }
    
    /**
     * Get UUID from Supabase session
     * Returns null if UUID cannot be retrieved
     */
    private fun getUuidFromSession(): String? {
        return SupabaseSessionHelper.getUuidOrNull(supabaseHelper.supabaseClient)
    }
    
    /**
     * Refresh user profile from Supabase and update cache
     * Called after campaign is updated to ensure cache is in sync
     */
    private fun refreshUserProfile() {
        viewModelScope.launch {
            try {
                val uuid = getUuidFromSession()
                if (uuid == null) {
                    return@launch
                }
                
                when (val result = profileService.getProfileByUuid(uuid)) {
                    is Result.Success -> {
                        userProfileRepository.saveProfile(result.data)
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error refreshing user profile: ${result.message}", result.exception)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in refreshUserProfile: ${e.message}", e)
            }
        }
    }
    
}

