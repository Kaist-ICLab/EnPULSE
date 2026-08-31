package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.db.obx.MicroEmaResponseStore
import kaist.iclab.mobiletracker.helpers.BLEHelper
import kaist.iclab.mobiletracker.helpers.DataExportHelper
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.services.AutoSyncManager
import kaist.iclab.mobiletracker.services.CampaignService
import kaist.iclab.mobiletracker.services.ProfileService
import kaist.iclab.mobiletracker.services.SurveyService
import kaist.iclab.mobiletracker.services.TriggerService
import kaist.iclab.mobiletracker.services.WatchSurveyConfigPusher
import kaist.iclab.mobiletracker.services.WebAppService
import kaist.iclab.mobiletracker.utils.CsvExportHelper
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.tracker.trigger.state.DetectionStateTracker
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
            microEmaResponseStore = get<MicroEmaResponseStore>(),
            detectionStateTracker = get<DetectionStateTracker>()
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

    // WebAppService - injects SupabaseHelper
    single {
        WebAppService(
            supabaseHelper = get()
        )
    }

    // DataExportHelper
    single {
        DataExportHelper(
            context = androidContext(),
            handlerRegistry = get()
        )
    }

    // CsvExportHelper - holds application context for CSV export/share
    single {
        CsvExportHelper(
            context = androidContext()
        )
    }

    // AutoSyncManager - wraps AutoSyncService start/stop
    single {
        AutoSyncManager(
            context = androidContext()
        )
    }

    // WatchSurveyConfigPusher
    single {
        WatchSurveyConfigPusher(
            bleChannel = BLEDataChannel(androidContext())
        )
    }

}

