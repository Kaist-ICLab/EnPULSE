package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.helpers.DataExportHelper
import kaist.iclab.mobiletracker.repository.CampaignRepository
import kaist.iclab.mobiletracker.repository.DataRepository
import kaist.iclab.mobiletracker.repository.PhoneSensorRepository
import kaist.iclab.mobiletracker.repository.SurveyRepository
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.viewmodels.data.DataViewModel
import kaist.iclab.mobiletracker.viewmodels.data.SensorDetailViewModel
import kaist.iclab.mobiletracker.viewmodels.home.HomeViewModel
import kaist.iclab.mobiletracker.viewmodels.settings.AccountSettingsViewModel
import kaist.iclab.mobiletracker.viewmodels.settings.DataSyncSettingsViewModel
import kaist.iclab.mobiletracker.viewmodels.settings.SettingsViewModel
import kaist.iclab.mobiletracker.viewmodels.settings.SurveySettingsViewModel
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.survey.SurveySensor
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
            syncTimestampService = get(),
            userProfileRepository = get(),
            campaignSensorRepository = get(),
            surveyRepository = get()
        )
    }

    // SettingsViewModel
    viewModel {
        SettingsViewModel(
            backgroundController = get(),
            permissionManager = get<AndroidPermissionManager>(),
            syncTimestampService = get(),
            campaignSensorRepository = get(),
            context = androidContext()
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
            context = androidContext()
        )
    }

    // ServerSyncSettingsViewModel
    viewModel {
        DataSyncSettingsViewModel(
            phoneSensorRepository = get<PhoneSensorRepository>(),
            watchSensorRepository = get<WatchSensorRepository>(),
            timestampService = get(),
            sensors = get(qualifier = org.koin.core.qualifier.named("phoneSensors")),
            phoneSensorUploadService = get(),
            watchSensorUploadService = get(),
            context = androidContext()
        )
    }

    // DataViewModel for Data screen
    viewModel {
        DataViewModel(
            dataRepository = get<DataRepository>(),
            dataExportHelper = get<DataExportHelper>(),
            context = androidContext()
        )
    }

    // SensorDetailViewModel for Sensor Detail screen
    viewModel { (sensorId: String) ->
        SensorDetailViewModel(
            dataRepository = get<DataRepository>(),
            sensorId = sensorId,
            context = androidContext()
        )
    }

    // SurveySettingsViewModel
    viewModel {
        SurveySettingsViewModel(
            surveySensor = get<SurveySensor>()
        )
    }

    // OnboardingViewModel
    viewModel {
        kaist.iclab.mobiletracker.viewmodels.onboarding.OnboardingViewModel(
            campaignRepository = get<CampaignRepository>(),
            userProfileRepository = get<UserProfileRepository>(),
            surveyRepository = get<SurveyRepository>(),
            campaignSensorRepository = get()
        )
    }
}
