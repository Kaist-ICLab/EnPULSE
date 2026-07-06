package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.repository.WatchSensorRepository
import kaist.iclab.mobiletracker.repository.WatchSensorRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val watchSensorModule = module {
    single<WatchSensorRepository> {
        WatchSensorRepositoryImpl(
            context = androidContext(),
            boxStore = get(),
            stores = get<SensorStores>(),
            supabaseHelper = get()
        )
    }
}
