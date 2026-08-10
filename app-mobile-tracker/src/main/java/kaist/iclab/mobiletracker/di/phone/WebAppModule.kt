package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.BuildConfig
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandlerRegistry
import kaist.iclab.mobiletracker.services.upload.WebAppLogUploader
import kaist.iclab.mobiletracker.storage.CouchbaseWebAppConfigStorage
import kaist.iclab.mobiletracker.webapp.PersistentWebAppRegistry
import kaist.iclab.mobiletracker.webapp.WebAppConfig
import kaist.iclab.mobiletracker.webapp.WebAppRegistry
import kaist.iclab.mobiletracker.webapp.WebAppTriggerHandler
import kaist.iclab.mobiletracker.webapp.bridge.DeviceBridgeHandler
import kaist.iclab.mobiletracker.webapp.bridge.LogBridgeHandler
import kaist.iclab.mobiletracker.webapp.bridge.SensorBridgeHandler
import kaist.iclab.mobiletracker.webapp.bridge.StorageBridgeHandler
import kaist.iclab.mobiletracker.webapp.bridge.SurveyBridgeHandler
import kaist.iclab.mobiletracker.webapp.storage.CouchbaseWebAppStorage
import kaist.iclab.tracker.storage.core.SurveyScheduleStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * WebApp bridge module - the "EnPULSE WebView platform" from enpulse-webapp-platform-plan.md.
 * SurveyScheduleStorage is resolved from the Koin graph, so this shares the exact same instance
 * registered in [surveySensorModule] rather than creating a second one.
 */
val webAppModule = module {
    // Phase 2 implementation — Supabase-backed registry synced from `campaign_webapp` table.
    single<WebAppRegistry> {
        PersistentWebAppRegistry(storage = get())
    }

    single { CouchbaseWebAppConfigStorage(couchbase = get()) }

    single { CouchbaseWebAppStorage(couchbase = get()) }

    single {
        WebAppTriggerHandler(
            context = androidContext(),
            scheduleStorage = get<SurveyScheduleStorage>(),
            webAppRegistry = get<WebAppRegistry>()
        )
    }

    single {
        SurveyBridgeHandler(
            context = androidContext(),
            surveyConfigStorage = get(),
            scheduleStorage = get<SurveyScheduleStorage>(),
            userProfileRepository = get()
        )
    }

    single {
        SensorBridgeHandler(
            handlerRegistry = get<SensorDataHandlerRegistry>(),
            watchSensorRepository = get<WatchSensorRepository>(),
            webAppRegistry = get<WebAppRegistry>()
        )
    }

    single {
        StorageBridgeHandler(
            webAppStorageStore = get()
        )
    }

    single {
        DeviceBridgeHandler(
            context = androidContext(),
            watchSensorRepository = get(),
            userProfileRepository = get()
        )
    }

    // Webapp analytics logs — kept off the sensor pipeline entirely (own store, own uploader),
    // so they are neither campaign-gated nor tied to sensor collection being started.
    single {
        WebAppLogUploader(
            store = get(),
            supabaseHelper = get(),
            syncTimestampService = get()
        )
    }

    single {
        LogBridgeHandler(
            webAppLogStore = get(),
            webAppRegistry = get<WebAppRegistry>()
        )
    }

}
