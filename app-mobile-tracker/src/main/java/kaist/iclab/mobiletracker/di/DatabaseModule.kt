package kaist.iclab.mobiletracker.di

import io.objectbox.BoxStore
import kaist.iclab.mobiletracker.db.entity.MyObjectBox
import kaist.iclab.mobiletracker.db.obx.MicroEmaResponseStore
import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.db.obx.SurveyResponseStore
import kaist.iclab.mobiletracker.db.obx.WebAppLogStore
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

    // Store for webapp analytics logs — not a sensor, so it lives outside SensorStores
    single { WebAppLogStore(get()) }

    // Store for locally cached phone survey responses — not a sensor, so it lives outside SensorStores
    single { SurveyResponseStore(get()) }
}
