package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.repository.WatchSensorRepositoryImpl
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.supabase.LocationSensorService
import kaist.iclab.mobiletracker.services.upload.WatchSensorUploadService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry
import kaist.iclab.mobiletracker.services.upload.handlers.buildWatchUploadHandlers
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val watchSensorModule = module {
    // Location Supabase service (used by the deviceType-special watch Location upload handler).
    // The other watch sensors now upload via the generic SupabaseUploadService.
    single {
        LocationSensorService(supabaseHelper = get())
    }

    // WatchSensorRepository - bind interface to implementation
    single<WatchSensorRepository> {
        WatchSensorRepositoryImpl(
            context = androidContext(),
            boxStore = get(),
            stores = get<SensorStores>(),
            supabaseHelper = get()
        )
    }

    // Watch sensor upload handler registry (registered in buildWatchUploadHandlers)
    single<SensorUploadHandlerRegistry>(named("watchUploadHandlerRegistry")) {
        SensorUploadHandlerRegistry(buildWatchUploadHandlers(get<SensorStores>()))
    }

    // WatchSensorUploadService - injects handler registry
    single {
        WatchSensorUploadService(
            handlerRegistry = get(named("watchUploadHandlerRegistry")),
            supabaseHelper = get(),
            syncTimestampService = SyncTimestampService(androidContext()),
            campaignSensorRepository = get()
        )
    }
}
