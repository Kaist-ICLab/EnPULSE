package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.db.obx.PhoneSensorStore
import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.di.SensorDescriptor
import kaist.iclab.mobiletracker.di.buildAllSensorDescriptors
import kaist.iclab.mobiletracker.repository.PhoneSensorRepository
import kaist.iclab.mobiletracker.repository.PhoneSensorRepositoryImpl
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandlerRegistry
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.supabase.SupabaseUploadService
import kaist.iclab.mobiletracker.services.upload.SensorUploadService
import kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val uploadModule = module {
    single { SyncTimestampService(context = androidContext()) }

    single<List<SensorDescriptor<*>>> {
        buildAllSensorDescriptors(get<SensorStores>())
    }

    single<SensorDataHandlerRegistry> {
        SensorDataHandlerRegistry(get<List<SensorDescriptor<*>>>().map { it.toDataHandler() })
    }

    single<Map<String, PhoneSensorStore<*>>>(named("sensorDataStorages")) {
        get<List<SensorDescriptor<*>>>()
            .mapNotNull { desc -> desc.toPhoneSensorStore()?.let { desc.sensorId to it } }
            .toMap()
    }

    single<PhoneSensorRepository> {
        PhoneSensorRepositoryImpl(
            sensorDataStorages = get<Map<String, PhoneSensorStore<*>>>(named("sensorDataStorages")),
            supabaseHelper = get(),
            appScope = get()
        )
    }

    single { SupabaseUploadService(supabaseHelper = get()) }

    single<SensorUploadHandlerRegistry> {
        val supabase = get<SupabaseUploadService>()
        SensorUploadHandlerRegistry(get<List<SensorDescriptor<*>>>().map { it.toUploadHandler(supabase) })
    }

    single {
        SensorUploadService(
            handlerRegistry = get(),
            supabaseHelper = get(),
            syncTimestampService = get(),
            campaignSensorRepository = get()
        )
    }
}
