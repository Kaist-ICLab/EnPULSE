package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.helpers.DataExportHelper
import kaist.iclab.mobiletracker.repository.CampaignRepository
import kaist.iclab.mobiletracker.repository.DataRepository
import kaist.iclab.mobiletracker.repository.SurveyRepository
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.mobiletracker.viewmodels.data.DataViewModel
import kaist.iclab.mobiletracker.viewmodels.data.SensorDetailViewModel
import kaist.iclab.mobiletracker.viewmodels.home.HomeViewModel
import kaist.iclab.mobiletracker.viewmodels.onboarding.OnboardingViewModel
import kaist.iclab.mobiletracker.viewmodels.settings.AccountSettingsViewModel
import kaist.iclab.mobiletracker.viewmodels.settings.SettingsViewModel
import kaist.iclab.tracker.permission.AndroidPermissionManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for ViewModel bindings.
 * ViewModels should only depend on Repositories and Services, not DAOs directly.
 */
val viewModelModule = module {
    // HomeViewModel
    viewModel {
        HomeViewModel(
            homeRepository = get(),
            backgroundController = get(),
//            syncTimestampService = get(),
            userProfileRepository = get(),
//            campaignSensorRepository = get(),
//            surveyRepository = get()
        )
    }

    // SettingsViewModel
    viewModel {
        SettingsViewModel(
            backgroundController = get(),
            permissionManager = get<AndroidPermissionManager>(),
            syncTimestampService = get(),
            campaignSensorRepository = get(),
//            bleHelper = get(),
//            surveyService = get(),
//            campaignRepository = get(),
//            userProfileRepository = get(),
//            microEmaConfigStorage = get(named("microEmaConfigStorage")),
            autoSyncManager = get()
        )
    }

    // AccountSettingsViewModel
    viewModel {
        AccountSettingsViewModel(
            campaignRepository = get<CampaignRepository>(),
            userProfileRepository = get<UserProfileRepository>(),
            surveyRepository = get<SurveyRepository>(),
            campaignSensorRepository = get(),
            backgroundController = get(),
        )
    }

    // DataViewModel for Data screen
    viewModel {
        DataViewModel(
            dataRepository = get<DataRepository>(),
            dataExportHelper = get<DataExportHelper>(),
            syncTimestampService = get(),
            context = androidContext()
        )
    }

    // SensorDetailViewModel for Sensor Detail screen
    viewModel { (sensorId: String) ->
        SensorDetailViewModel(
            dataRepository = get<DataRepository>(),
            sensorId = sensorId,
            csvExportHelper = get()
        )
    }

    // OnboardingViewModel
    viewModel {
        OnboardingViewModel(
            campaignRepository = get<CampaignRepository>(),
            userProfileRepository = get<UserProfileRepository>(),
//            surveyRepository = get<SurveyRepository>(),
//            campaignSensorRepository = get()
        )
    }
}
