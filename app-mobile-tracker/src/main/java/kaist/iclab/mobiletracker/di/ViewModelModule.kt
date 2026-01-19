package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.DataRepository
import kaist.iclab.mobiletracker.repository.HomeRepository
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.mobiletracker.services.CampaignService
import kaist.iclab.mobiletracker.services.ProfileService
import kaist.iclab.mobiletracker.repository.PhoneSensorRepository
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.viewmodels.data.DataViewModel
import kaist.iclab.mobiletracker.viewmodels.data.SensorDetailViewModel
import kaist.iclab.mobiletracker.viewmodels.home.HomeViewModel
import kaist.iclab.mobiletracker.viewmodels.settings.AccountSettingsViewModel
import kaist.iclab.mobiletracker.viewmodels.settings.DataSyncSettingsViewModel
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
            syncTimestampService = get(),
            userProfileRepository = get()
        )
    }

    // SettingsViewModel
    viewModel {
        SettingsViewModel(
            backgroundController = get(),
            permissionManager = get<AndroidPermissionManager>(),
            syncTimestampService = get(),
            context = androidContext()
        )
    }
    
    // AccountSettingsViewModel
    viewModel {
        AccountSettingsViewModel(
            campaignService = get<CampaignService>(),
            profileService = get<ProfileService>(),
            supabaseHelper = get<SupabaseHelper>(),
            userProfileRepository = get<UserProfileRepository>(),
            context = androidContext()
        )
    }
    
    // ServerSyncSettingsViewModel
    viewModel {
        DataSyncSettingsViewModel(
            phoneSensorRepository = get<PhoneSensorRepository>(),
            watchSensorRepository = get<WatchSensorRepository>(),
            timestampService = get(),
            context = androidContext()
        )
    }
    
    // DataViewModel for Data screen
    viewModel {
        DataViewModel(
            dataRepository = get<DataRepository>(),
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
}
