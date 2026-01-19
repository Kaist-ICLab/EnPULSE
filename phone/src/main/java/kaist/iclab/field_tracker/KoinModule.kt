package kaist.iclab.field_tracker

import kaist.iclab.field_tracker.viewmodel.MainViewModel
import kaist.iclab.field_tracker.viewmodel.RealMainViewModel
import kaist.iclab.tracker.auth.Authentication
import kaist.iclab.tracker.controller.BackgroundController
import kaist.iclab.tracker.controller.Controller
import kaist.iclab.tracker.controller.ControllerState
import kaist.iclab.tracker.notification.NotfManager
import kaist.iclab.tracker.notification.NotfManagerImpl
import kaist.iclab.tracker.notification.ServiceNotification
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.permission.PermissionManagerImpl
import kaist.iclab.tracker.sensor.SampleSensor
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.SensorDataStorage
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseSensorDataStorage
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module


val realModule = module {
    singleOf(::CouchbaseDB)

    single<NotfManager> {
        NotfManagerImpl(
            get(),
            ServiceNotification(
                channelId = "TRACKER_SERVICE",
                channelName = "Tracker Service",
                icon = R.drawable.app_icon,
                title = "Tracker Service",
                description = "Tracker service is running now..."
            )
        )
    }
    single<PermissionManager> {
        PermissionManagerImpl(get())
    }
    single<Authentication> { TODO() }

    single<SampleSensor> {
        SampleSensor(
            get(),
            CouchbaseStateStorage(
                get(), SampleSensor.Config(5000L),
                SampleSensor.Config::class.java, "SampleSensorConfig"
            ),
            CouchbaseStateStorage(
                get(),
                SensorState(SensorState.FLAG.UNAVAILABLE),
                SensorState::class.java, "SampleSensorState"
            ),
            SampleSensor.Config(5000L)
        )
    }

    single<List<Sensor<*, *>>>(named("sensors")) {
        listOf(get<SampleSensor>())
    }

    single<List<SensorDataStorage>>(named("storages")) {
        get<List<Sensor<*, *>>>().map {
            CouchbaseSensorDataStorage(get(), it.ID)
        }
    }

    single<Controller> {
        BackgroundController(
            get(),
            CouchbaseStateStorage(
                get(),
                ControllerState(ControllerState.FLAG.DISABLED),
                ControllerState::class.java,
                "ControllerState"
            ),
            get()
        )
    }

    viewModel<MainViewModel> {
        RealMainViewModel(get(), get(), get(), get())
    }
}


////    singleOf(::ActivityTransitionCollector)
////    singleOf(::ActivityRecognitionStatCollector)
//    singleOf(::AmbientLightCollector)
//    singleOf(::AppUsageLogCollector)
//    singleOf(::BatteryCollector)
//    singleOf(::CallLogCollector)
//    singleOf(::DataTrafficStatCollector)
//    singleOf(::LocationCollector)
//    singleOf(::MessageLogCollector)
//    singleOf(::NotificationCollector)
//    singleOf(::ScreenCollector)
//    singleOf(::UserInteractionCollector)
//    singleOf(::WifiScanCollector)
//    singleOf(::BluetoothScanCollector)
//    single<Map<String, CollectorInterface>> {
//        listOf(
//            get<AmbientLightCollector>(),
//            get<BatteryCollector>(),
//            get<ScreenCollector>(),
//            get<NotificationCollector>(),
//            get<UserInteractionCollector>(),
//            get<LocationCollector>(),
//            get<AppUsageLogCollector>(),
//            get<CallLogCollector>(),
//            get<MessageLogCollector>(),
//            get<DataTrafficStatCollector>(),
//            get<WifiScanCollector>(),
//            get<BluetoothScanCollector>()
//        ).map({ it.NAME to it }).toMap()
//    }
//