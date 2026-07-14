package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.db.TrackerRoomDB
import kaist.iclab.mobiletracker.helpers.BLEHelper
import kaist.iclab.mobiletracker.helpers.DataExportHelper
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.services.CampaignService
import kaist.iclab.mobiletracker.services.ProfileService
import kaist.iclab.mobiletracker.services.SurveyService
import kaist.iclab.mobiletracker.services.TriggerService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val helperModule = module {
    // BLEHelper - injects WatchSensorRepository, MicroEma config, and SyncTimestampService
    single {
        BLEHelper(
            context = androidContext(),
            watchSensorRepository = get<WatchSensorRepository>(),
            timestampService = get(),
            microEmaConfigStorage = get(org.koin.core.qualifier.named("microEmaConfigStorage")),
            microEmaResponseDao = get<TrackerRoomDB>().microEmaResponseDao()
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

    // SurveyService - injects SupabaseHelper
    single {
        SurveyService(
            supabaseHelper = get()
        )
    }

    // TriggerService - injects SupabaseHelper
    single {
        TriggerService(
            supabaseHelper = get()
        )
    }

    // DataExportHelper
    single {
        DataExportHelper(
            handlerRegistry = get()
        )
    }

}

