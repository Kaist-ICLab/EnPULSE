package kaist.iclab.mobiletracker.di

import io.objectbox.BoxStore
import kaist.iclab.mobiletracker.db.entity.MyObjectBox
import kaist.iclab.mobiletracker.db.obx.MicroEmaResponseStore
import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    // CouchbaseDB - for sensor state storage
    single {
        CouchbaseDB(context = androidContext())
    }

    // ObjectBox - for sensor data storage
    single<BoxStore> {
        MyObjectBox.builder()
            .androidContext(androidContext())
            .build()
    }

    // Registry of every sensor's generic store (replaces the per-sensor Room DAOs)
    single { SensorStores(get()) }

    // Store for locally cached microEMA responses
    single { MicroEmaResponseStore(get()) }
}
