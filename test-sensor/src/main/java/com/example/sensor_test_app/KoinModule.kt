package com.example.sensor_test_app

import com.example.sensor_test_app.storage.CouchbaseSensorStateStorage
import com.example.sensor_test_app.storage.SimpleStateStorage
import com.example.sensor_test_app.ui.SensorViewModel
import com.google.android.gms.location.Priority
import kaist.iclab.tracker.MetaData
import kaist.iclab.tracker.listener.SamsungHealthDataInitializer
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.common.LocationSensor
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.phone.AmbientLightSensor
import kaist.iclab.tracker.sensor.phone.AppListChangeSensor
import kaist.iclab.tracker.sensor.phone.AppUsageLogSensor
import kaist.iclab.tracker.sensor.phone.BatterySensor
import kaist.iclab.tracker.sensor.phone.BluetoothScanSensor
import kaist.iclab.tracker.sensor.phone.CallLogSensor
import kaist.iclab.tracker.sensor.phone.ConnectivitySensor
import kaist.iclab.tracker.sensor.phone.DataTrafficSensor
import kaist.iclab.tracker.sensor.phone.DeviceModeSensor
import kaist.iclab.tracker.sensor.phone.MediaSensor
import kaist.iclab.tracker.sensor.phone.MessageLogSensor
import kaist.iclab.tracker.sensor.phone.NotificationSensor
import kaist.iclab.tracker.sensor.phone.ScreenSensor
import kaist.iclab.tracker.sensor.phone.StepSensor
import kaist.iclab.tracker.sensor.phone.UserInteractionSensor
import kaist.iclab.tracker.sensor.phone.WifiScanSensor
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val koinModule = module {
    single {
        SamsungHealthDataInitializer(context = androidContext())
    }

    single {
        CouchbaseDB(context = androidContext())
    }

    single {
        AndroidPermissionManager(context = androidContext())
    }

    // Sensors
    single {
        AmbientLightSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                AmbientLightSensor.Config(
                    interval = 100L
                ),
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = AmbientLightSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        AppUsageLogSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                AppUsageLogSensor.Config(
                    interval = 100L
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = AppUsageLogSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        AppListChangeSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                AppListChangeSensor.Config(
                    periodicIntervalMillis = TimeUnit.SECONDS.toMillis(10),
                    includeSystemApps = false,
                    includeDisabledApps = false
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = AppListChangeSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        BatterySensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(BatterySensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = BatterySensor::class.simpleName ?: ""
            )
        )
    }

    single {
        BluetoothScanSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                BluetoothScanSensor.Config(
                    doScan = true,
                    interval = TimeUnit.SECONDS.toMillis(10),
                    scanDuration = TimeUnit.SECONDS.toMillis(1)
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = BluetoothScanSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        CallLogSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                CallLogSensor.Config(
                    TimeUnit.MINUTES.toMillis(1)
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = CallLogSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        DataTrafficSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                DataTrafficSensor.Config(
                    interval = TimeUnit.MINUTES.toMillis(1)
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = DataTrafficSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        DeviceModeSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                DeviceModeSensor.Config(
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = DeviceModeSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        LocationSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                LocationSensor.Config(
                    interval = TimeUnit.SECONDS.toMillis(1),
                    maxUpdateAge = 0,
                    maxUpdateDelay = 0,
                    minUpdateDistance = 0.0f,
                    minUpdateInterval = 0,
                    priority = Priority.PRIORITY_HIGH_ACCURACY,
                    waitForAccurateLocation = false,
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = LocationSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        MediaSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(MediaSensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = MediaSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        MessageLogSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                MessageLogSensor.Config(
                    interval = TimeUnit.SECONDS.toMillis(10)
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = MessageLogSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        NotificationSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(NotificationSensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = NotificationSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        ScreenSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(ScreenSensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = ScreenSensor::class.simpleName ?: ""
            )
        )
    }


    single {
        StepSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                StepSensor.Config(
                    syncPastLimitSeconds = TimeUnit.DAYS.toSeconds(7),
                    timeMarginSeconds = TimeUnit.HOURS.toSeconds(1),
                    bucketSizeMinutes = 10,
                    readIntervalMillis = TimeUnit.SECONDS.toMillis(10)
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = StepSensor::class.simpleName ?: ""
            ),
            samsungHealthDataInitializer = SamsungHealthDataInitializer(androidContext())
        )
    }

    single {
        UserInteractionSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(UserInteractionSensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = UserInteractionSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        WifiScanSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(WifiScanSensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = WifiScanSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        ConnectivitySensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(ConnectivitySensor.Config()),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = ConnectivitySensor::class.simpleName ?: ""
            )
        )
    }

    single(named("sensors")) {
        listOf(
            get<AmbientLightSensor>(),
            get<AppListChangeSensor>(),
            get<AppUsageLogSensor>(),
            get<BatterySensor>(),
            get<BluetoothScanSensor>(),
            get<CallLogSensor>(),
            get<DataTrafficSensor>(),
            get<DeviceModeSensor>(),
            get<LocationSensor>(),
            get<MediaSensor>(),
            get<MessageLogSensor>(),
            get<ConnectivitySensor>(),
            get<NotificationSensor>(),
            get<ScreenSensor>(),
            get<StepSensor>(),
            get<UserInteractionSensor>(),
            get<WifiScanSensor>(),
        )
    }

    // Global Controller
    single {
        BackgroundController(
            context = androidContext(),
            controllerStateStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = ControllerState(ControllerState.FLAG.DISABLED),
                clazz = ControllerState::class.java,
                collectionName = BackgroundController::class.simpleName ?: ""
            ),
            sensors = get(qualifier("sensors")),
            serviceNotification = BackgroundController.ServiceNotification(
                channelId = "BackgroundControllerService",
                channelName = "TrackerTest",
                notificationId = 1,
                title = "Tracker Test App",
                description = "Background sensor controller is running",
                icon = R.drawable.ic_launcher_foreground
            ),
            allowPartialSensing = true,
        )
    }


    single {
        MetaData(androidContext())
    }

    // ViewModel
    viewModel {
        SensorViewModel(
            backgroundController = get(),
            permissionManager = get<AndroidPermissionManager>()
        )
    }
}