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
import kaist.iclab.mobiletracker.services.upload.handlers.watch.WatchAccelerometerUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.watch.WatchEDAUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.watch.WatchHeartRateUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.watch.WatchLocationUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.watch.WatchPPGUploadHandler
import kaist.iclab.mobiletracker.services.upload.handlers.watch.WatchSkinTemperatureUploadHandler
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

    // Watch sensor upload handler registry
    single<SensorUploadHandlerRegistry>(named("watchUploadHandlerRegistry")) {
        val s = get<SensorStores>()
        val handlers = listOf(
            WatchHeartRateUploadHandler(
                store = s.watchHeartRate,
                service = get<HeartRateSensorService>()
            ),
            WatchAccelerometerUploadHandler(
                store = s.watchAccelerometer,
                service = get<AccelerometerSensorService>()
            ),
            WatchEDAUploadHandler(
                store = s.watchEDA,
                service = get<EDASensorService>()
            ),
            WatchPPGUploadHandler(
                store = s.watchPPG,
                service = get<PPGSensorService>()
            ),
            WatchSkinTemperatureUploadHandler(
                store = s.watchSkinTemperature,
                service = get<SkinTemperatureSensorService>()
            ),
            WatchLocationUploadHandler(
                store = s.location,
                service = get<LocationSensorService>()
            )
        )
        SensorUploadHandlerRegistry(handlers)
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
