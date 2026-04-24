package kaist.iclab.mobiletracker.di

import androidx.room.Room
import kaist.iclab.mobiletracker.db.TrackerRoomDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    // CouchbaseDB - for sensor state storage
    single {
        CouchbaseDB(context = androidContext())
    }

    // Room Database - for phone sensor data storage
    single {
        Room.databaseBuilder(
            androidContext(),
            TrackerRoomDB::class.java,
            "mobile_tracker_db"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }
}

