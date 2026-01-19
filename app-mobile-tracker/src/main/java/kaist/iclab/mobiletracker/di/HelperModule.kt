package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.helpers.BLEHelper
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.services.CampaignService
import kaist.iclab.mobiletracker.services.ProfileService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val helperModule = module {
    // BLEHelper - injects WatchSensorRepository and SyncTimestampService
    single {
        BLEHelper(
            context = androidContext(),
            watchSensorRepository = get<WatchSensorRepository>(),
            timestampService = get()
        )
    }

    // CampaignService - injects SupabaseHelper
    single {
        CampaignService(
            supabaseHelper = get()
        )
    }

    // ProfileService - injects SupabaseHelper
    single {
        ProfileService(
            supabaseHelper = get()
        )
    }
}

