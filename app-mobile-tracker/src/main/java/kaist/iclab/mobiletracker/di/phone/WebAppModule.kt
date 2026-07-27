package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandlerRegistry
import kaist.iclab.mobiletracker.webapp.StaticWebAppRegistry
import kaist.iclab.mobiletracker.webapp.WebAppConfig
import kaist.iclab.mobiletracker.webapp.WebAppRegistry
import kaist.iclab.mobiletracker.webapp.WebAppTriggerHandler
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
    // Phase 1 stub — replace with a Supabase-backed registry in Phase 2 (see campaign_webapp table
    // in the platform plan). Populate this map to register a webapp for local testing.
    single<WebAppRegistry> {
        StaticWebAppRegistry(configs = mapOf(
            "test" to WebAppConfig(
                id = "test",
                url = "https://www.naver.com",
                allowedOrigin = "https://www.naver.com",
            )
        ))
    }

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
            scheduleStorage = get<SurveyScheduleStorage>()
        )
    }

    single {
        SensorBridgeHandler(
            handlerRegistry = get<SensorDataHandlerRegistry>(),
            watchSensorRepository = get<WatchSensorRepository>()
        )
    }

    single {
        StorageBridgeHandler(
            webAppStorageStore = get()
        )
    }
}
