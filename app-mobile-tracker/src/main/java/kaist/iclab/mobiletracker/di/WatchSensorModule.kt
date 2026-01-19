package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.db.TrackerRoomDB
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.repository.WatchSensorRepositoryImpl
import kaist.iclab.mobiletracker.services.supabase.AccelerometerSensorService
import kaist.iclab.mobiletracker.services.supabase.EDASensorService
import kaist.iclab.mobiletracker.services.supabase.HeartRateSensorService
import kaist.iclab.mobiletracker.services.supabase.LocationSensorService
import kaist.iclab.mobiletracker.services.supabase.PPGSensorService
import kaist.iclab.mobiletracker.services.supabase.SkinTemperatureSensorService
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.upload.WatchSensorUploadService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry
import kaist.iclab.mobiletracker.services.upload.handlers.watch.*
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
    
    // Map of sensor IDs to DAOs for storing watch sensor data in Room database
    single<Map<String, BaseDao<*, *>>>(named("watchSensorDaos")) {
        val db = get<TrackerRoomDB>()
        mapOf(
            "WatchHeartRate" to db.watchHeartRateDao(),
            "WatchAccelerometer" to db.watchAccelerometerDao(),
            "WatchEDA" to db.watchEDADao(),
            "WatchPPG" to db.watchPPGDao(),
            "WatchSkinTemperature" to db.watchSkinTemperatureDao(),
            "WatchLocation" to db.locationDao(),
        )
    }
    
    // WatchSensorRepository - bind interface to implementation
    single<WatchSensorRepository> {
        WatchSensorRepositoryImpl(
            context = androidContext(),
            db = get(),
            watchSensorDaos = get<Map<String, BaseDao<*, *>>>(named("watchSensorDaos")),
            supabaseHelper = get()
        )
    }
    
    // Watch sensor upload handler registry
    single<SensorUploadHandlerRegistry>(named("watchUploadHandlerRegistry")) {
        val db = get<TrackerRoomDB>()
        val handlers = listOf(
            WatchHeartRateUploadHandler(
                dao = db.watchHeartRateDao(),
                service = get<HeartRateSensorService>()
            ),
            WatchAccelerometerUploadHandler(
                dao = db.watchAccelerometerDao(),
                service = get<AccelerometerSensorService>()
            ),
            WatchEDAUploadHandler(
                dao = db.watchEDADao(),
                service = get<EDASensorService>()
            ),
            WatchPPGUploadHandler(
                dao = db.watchPPGDao(),
                service = get<PPGSensorService>()
            ),
            WatchSkinTemperatureUploadHandler(
                dao = db.watchSkinTemperatureDao(),
                service = get<SkinTemperatureSensorService>()
            ),
            WatchLocationUploadHandler(
                dao = db.locationDao(),
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
            syncTimestampService = SyncTimestampService(androidContext())
        )
    }
}
