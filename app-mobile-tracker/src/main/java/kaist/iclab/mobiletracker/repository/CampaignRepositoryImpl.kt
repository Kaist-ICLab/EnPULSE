package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.data.campaign.CampaignData
import kaist.iclab.mobiletracker.services.CampaignService
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementation of CampaignRepository.
 * Fetches campaigns from CampaignService and caches them.
 */
class CampaignRepositoryImpl(
    private val campaignService: CampaignService,
    private val backgroundController: BackgroundController
) : CampaignRepository {
    companion object {
        private const val TAG = "CampaignRepo"
    }

    private val _campaignsFlow = MutableStateFlow<List<CampaignData>>(emptyList())
    override val campaignsFlow: StateFlow<List<CampaignData>> = _campaignsFlow.asStateFlow()

    override suspend fun fetchCampaigns(): Result<List<CampaignData>> {
        if (backgroundController.controllerStateFlow.value.flag == ControllerState.FLAG.RUNNING) {
            return Result.Error(AppError.CollectionRunning("Cannot fetch campaigns while data collection is running"))
        }

        return ErrorClassifier.runClassified(TAG, "fetch campaigns") {
            when (val result = campaignService.getAllCampaigns()) {
                is Result.Success -> {
                    _campaignsFlow.value = result.data
                    result.data
                }

                is Result.Error -> {
                    throw result.exception ?: Exception(result.message)
                }
            }
        }
    }

    override fun getCachedCampaigns(): List<CampaignData> {
        return _campaignsFlow.value
    }

    override suspend fun joinCampaign(campaignId: String, password: String): Result<Boolean> {
        return campaignService.joinCampaign(campaignId, password)
    }
}
