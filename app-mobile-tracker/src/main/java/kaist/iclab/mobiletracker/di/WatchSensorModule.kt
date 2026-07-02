package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.repository.WatchSensorRepositoryImpl
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.supabase.AccelerometerSensorService
import kaist.iclab.mobiletracker.services.supabase.EDASensorService
import kaist.iclab.mobiletracker.services.supabase.HeartRateSensorService
import kaist.iclab.mobiletracker.services.supabase.LocationSensorService
import kaist.iclab.mobiletracker.services.supabase.PPGSensorService
import kaist.iclab.mobiletracker.services.supabase.SkinTemperatureSensorService
import kaist.iclab.mobiletracker.services.upload.WatchSensorUploadService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry
import kaist.iclab.mobiletracker.services.upload.handlers.buildWatchUploadHandlers
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val watchSensorModule = module {
    // Watch Sensor Services - inject SupabaseHelper
    single {
        LocationSensorService(supabaseHelper = get())
    }

    single {
        AccelerometerSensorService(supabaseHelper = get())
    }

    single {
        EDASensorService(supabaseHelper = get())
    }

    single {
        HeartRateSensorService(supabaseHelper = get())
    }

    single {
        PPGSensorService(supabaseHelper = get())
    }

    single {
        SkinTemperatureSensorService(supabaseHelper = get())
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
