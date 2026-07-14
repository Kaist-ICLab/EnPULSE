package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.storage.CouchbaseSensorStateStorage
import kaist.iclab.mobiletracker.storage.SimpleStateStorage
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.galaxywatch.MicroEmaSensor
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val microEmaSensorModule = module {

    single<StateStorage<MicroEmaSensor.Config>>(named("microEmaConfigStorage")) {
        SimpleStateStorage(MicroEmaSensor.Config())
    }

    single<StateStorage<SensorState>>(named("microEmaStateStorage")) {
        CouchbaseSensorStateStorage(
            couchbase = get(),
            collectionName = MicroEmaSensor::class.simpleName ?: "MicroEmaSensor"
        )
    }

    single {
        MicroEmaSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = get(named("microEmaConfigStorage")),
            stateStorage = get(named("microEmaStateStorage")),
            bleChannel = BLEDataChannel(androidContext())
        )
    }
}
