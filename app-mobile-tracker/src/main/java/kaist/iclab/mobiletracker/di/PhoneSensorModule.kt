package kaist.iclab.mobiletracker.di

import com.google.android.gms.location.Priority
import kaist.iclab.mobiletracker.R
import kaist.iclab.mobiletracker.db.TrackerRoomDB
import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.repository.PhoneSensorRepository
import kaist.iclab.mobiletracker.repository.PhoneSensorRepositoryImpl
import kaist.iclab.mobiletracker.services.SensorServiceRegistry
import kaist.iclab.mobiletracker.services.SensorServiceRegistryImpl
import kaist.iclab.mobiletracker.services.supabase.AmbientLightSensorService
import kaist.iclab.mobiletracker.services.supabase.AppListChangeSensorService
import kaist.iclab.mobiletracker.services.supabase.AppUsageLogSensorService
import kaist.iclab.mobiletracker.services.supabase.BatterySensorService
import kaist.iclab.mobiletracker.services.supabase.BluetoothScanSensorService
import kaist.iclab.mobiletracker.services.supabase.ConnectivitySensorService
import kaist.iclab.mobiletracker.services.supabase.MediaSensorService
import kaist.iclab.mobiletracker.services.supabase.MessageLogSensorService
import kaist.iclab.mobiletracker.services.supabase.NotificationSensorService
import kaist.iclab.mobiletracker.services.supabase.UserInteractionSensorService
import kaist.iclab.mobiletracker.services.supabase.CallLogSensorService
import kaist.iclab.mobiletracker.services.supabase.DataTrafficSensorService
import kaist.iclab.mobiletracker.services.supabase.DeviceModeSensorService
import kaist.iclab.mobiletracker.services.supabase.LocationSensorService
import kaist.iclab.mobiletracker.services.upload.PhoneSensorUploadService
import kaist.iclab.mobiletracker.services.supabase.ScreenSensorService
import kaist.iclab.mobiletracker.services.supabase.StepSensorService
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.supabase.WifiSensorService
import kaist.iclab.mobiletracker.storage.CouchbaseSensorStateStorage
import kaist.iclab.mobiletracker.storage.SimpleStateStorage
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
import kaist.iclab.tracker.sensor.phone.DataTrafficSensor
import kaist.iclab.tracker.sensor.phone.DeviceModeSensor
import kaist.iclab.tracker.sensor.phone.MediaSensor
import kaist.iclab.tracker.sensor.phone.MessageLogSensor
import kaist.iclab.tracker.sensor.phone.ConnectivitySensor
import kaist.iclab.tracker.sensor.phone.NotificationSensor
import kaist.iclab.tracker.sensor.phone.ScreenSensor
import kaist.iclab.tracker.sensor.phone.StepSensor
import kaist.iclab.tracker.sensor.phone.UserInteractionSensor
import kaist.iclab.tracker.sensor.phone.WifiScanSensor
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val phoneSensorModule = module {
    // Sensor Management Dependencies
    single {
        SamsungHealthDataInitializer(context = androidContext())
    }

    single {
        AndroidPermissionManager(context = androidContext())
    }

    // Phone Sensors
    single {
        AmbientLightSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                AmbientLightSensor.Config(interval = 100L)
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
                AppUsageLogSensor.Config(interval = 60000L)
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
                CallLogSensor.Config(TimeUnit.MINUTES.toMillis(1))
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
                DataTrafficSensor.Config(interval = TimeUnit.MINUTES.toMillis(1))
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
            configStorage = SimpleStateStorage(DeviceModeSensor.Config()),
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
                MessageLogSensor.Config(interval = TimeUnit.SECONDS.toMillis(10))
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
            samsungHealthDataInitializer = get()
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

    // Sensors list
    single(named("phoneSensors")) {
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

    // BackgroundController
    single {
        val context = androidContext()
        BackgroundController.ServiceNotification(
            channelId = "BackgroundControllerService",
            channelName = "MobileTracker",
            notificationId = 1,
            title = context.getString(R.string.notification_title),
            description = context.getString(R.string.notification_description),
            icon = R.drawable.ic_launcher_foreground
        )
    }

    single {
        BackgroundController(
            context = androidContext(),
            controllerStateStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = ControllerState(ControllerState.FLAG.DISABLED),
                clazz = ControllerState::class.java,
                collectionName = BackgroundController::class.simpleName ?: ""
            ),
            sensors = get(qualifier("phoneSensors")),
            serviceNotification = get<BackgroundController.ServiceNotification>(),
            allowPartialSensing = true,
        )
    }

    // Map of sensor IDs to DAOs for storing phone sensor data in Room database
    single<Map<String, BaseDao<*, *>>>(named("sensorDataStorages")) {
        val db = get<TrackerRoomDB>()
        mapOf(
            get<AmbientLightSensor>().id to db.ambientLightDao(),
            get<AppListChangeSensor>().id to db.appListChangeDao(),
            get<AppUsageLogSensor>().id to db.appUsageLogDao(),
            get<BatterySensor>().id to db.batteryDao(),
            get<BluetoothScanSensor>().id to db.bluetoothScanDao(),
            get<ConnectivitySensor>().id to db.connectivityDao(),
            get<CallLogSensor>().id to db.callLogDao(),
            get<MessageLogSensor>().id to db.messageLogDao(),
            get<UserInteractionSensor>().id to db.userInteractionDao(),
            get<DataTrafficSensor>().id to db.dataTrafficDao(),
            get<DeviceModeSensor>().id to db.deviceModeDao(),
            get<LocationSensor>().id to db.locationDao(),
            get<ScreenSensor>().id to db.screenDao(),
            get<MediaSensor>().id to db.mediaDao(),
            get<NotificationSensor>().id to db.notificationDao(),
            get<StepSensor>().id to db.stepDao(),
            get<WifiScanSensor>().id to db.wifiDao(),
        )
    }

    // PhoneSensorRepository - bind interface to implementation
    single<PhoneSensorRepository> {
        PhoneSensorRepositoryImpl(
            sensorDataStorages = get<Map<String, BaseDao<*, *>>>(named("sensorDataStorages")),
            supabaseHelper = get()
        )
    }

    // AmbientLightSensorService for uploading to Supabase
    single {
        AmbientLightSensorService(supabaseHelper = get())
    }

    // AppListChangeSensorService for uploading to Supabase
    single {
        AppListChangeSensorService(supabaseHelper = get())
    }

    // AppUsageLogSensorService for uploading to Supabase
    single {
        AppUsageLogSensorService(supabaseHelper = get())
    }

    // BatterySensorService for uploading to Supabase
    single {
        BatterySensorService(supabaseHelper = get())
    }

    // BluetoothScanSensorService for uploading to Supabase
    single {
        BluetoothScanSensorService(supabaseHelper = get())
    }

    // CallLogSensorService for uploading to Supabase
    single {
        CallLogSensorService(supabaseHelper = get())
    }

    // ConnectivitySensorService for uploading to Supabase
    single {
        ConnectivitySensorService(supabaseHelper = get())
    }

    // MediaSensorService for uploading to Supabase
    single {
        MediaSensorService(supabaseHelper = get())
    }

    // NotificationSensorService for uploading to Supabase
    single {
        NotificationSensorService(supabaseHelper = get())
    }

    // MessageLogSensorService for uploading to Supabase
    single {
        MessageLogSensorService(supabaseHelper = get())
    }

    // StepSensorService for uploading to Supabase
    single {
        StepSensorService(supabaseHelper = get())
    }
    // UserInteractionSensorService for uploading to Supabase
    single {
        UserInteractionSensorService(supabaseHelper = get())
    }

    // DataTrafficSensorService for uploading to Supabase
    single {
        DataTrafficSensorService(supabaseHelper = get())
    }

    // DeviceModeSensorService for uploading to Supabase
    single {
        DeviceModeSensorService(supabaseHelper = get())
    }

    // LocationSensorService for uploading to Supabase (shared by phone and watch)
    single {
        LocationSensorService(supabaseHelper = get())
    }

    // ScreenSensorService for uploading to Supabase
    single {
        ScreenSensorService(supabaseHelper = get())
    }

    // WifiSensorService for uploading to Supabase
    single {
        WifiSensorService(supabaseHelper = get())
    }

    // Phone sensor service registry
    single<SensorServiceRegistry>(named("phoneSensorServiceRegistry")) {
        val ambientLightService = get<AmbientLightSensorService>()
        val appListChangeService = get<AppListChangeSensorService>()
        val appUsageLogService = get<AppUsageLogSensorService>()
        val batteryService = get<BatterySensorService>()
        val bluetoothScanService = get<BluetoothScanSensorService>()
        val connectivityService = get<ConnectivitySensorService>()
        val mediaService = get<MediaSensorService>()
        val messageLogService = get<MessageLogSensorService>()
        val notificationService = get<NotificationSensorService>()
        val stepService = get<StepSensorService>()
        val userInteractionService = get<UserInteractionSensorService>()
        val callLogService = get<CallLogSensorService>()
        val dataTrafficService = get<DataTrafficSensorService>()
        val deviceModeService = get<DeviceModeSensorService>()
        val locationService = get<LocationSensorService>()
        val screenService = get<ScreenSensorService>()
        val wifiService = get<WifiSensorService>()
        
        SensorServiceRegistryImpl(
            mapOf(
                get<AmbientLightSensor>().id to ambientLightService,
                get<AppListChangeSensor>().id to appListChangeService,
                get<AppUsageLogSensor>().id to appUsageLogService,
                get<BatterySensor>().id to batteryService,
                get<BluetoothScanSensor>().id to bluetoothScanService,
                get<ConnectivitySensor>().id to connectivityService,
                get<MediaSensor>().id to mediaService,
                get<MessageLogSensor>().id to messageLogService,
                get<NotificationSensor>().id to notificationService,
                get<StepSensor>().id to stepService,
                get<UserInteractionSensor>().id to userInteractionService,
                get<CallLogSensor>().id to callLogService,
                get<DataTrafficSensor>().id to dataTrafficService,
                get<DeviceModeSensor>().id to deviceModeService,
                get<LocationSensor>().id to locationService,
                get<ScreenSensor>().id to screenService,
                get<WifiScanSensor>().id to wifiService,
            )
        )
    }

    // SyncTimestampService for tracking upload timestamps
    single {
        SyncTimestampService(context = androidContext())
    }

    // SensorUploadHandlerRegistry for phone sensors
    single<kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry> {
        val db = get<TrackerRoomDB>()
        val handlers = listOf(
            kaist.iclab.mobiletracker.services.upload.handlers.phone.LocationUploadHandler(
                dao = db.locationDao(),
                service = get<LocationSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.BatteryUploadHandler(
                dao = db.batteryDao(),
                service = get<BatterySensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.ScreenUploadHandler(
                dao = db.screenDao(),
                service = get<ScreenSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.WifiScanUploadHandler(
                dao = db.wifiDao(),
                service = get<WifiSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.StepUploadHandler(
                dao = db.stepDao(),
                service = get<StepSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.AmbientLightUploadHandler(
                dao = db.ambientLightDao(),
                service = get<AmbientLightSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.AppListChangeUploadHandler(
                dao = db.appListChangeDao(),
                service = get<AppListChangeSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.AppUsageLogUploadHandler(
                dao = db.appUsageLogDao(),
                service = get<AppUsageLogSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.BluetoothScanUploadHandler(
                dao = db.bluetoothScanDao(),
                service = get<BluetoothScanSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.CallLogUploadHandler(
                dao = db.callLogDao(),
                service = get<CallLogSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.ConnectivityUploadHandler(
                dao = db.connectivityDao(),
                service = get<ConnectivitySensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.DataTrafficUploadHandler(
                dao = db.dataTrafficDao(),
                service = get<DataTrafficSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.DeviceModeUploadHandler(
                dao = db.deviceModeDao(),
                service = get<DeviceModeSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.MediaUploadHandler(
                dao = db.mediaDao(),
                service = get<MediaSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.MessageLogUploadHandler(
                dao = db.messageLogDao(),
                service = get<MessageLogSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.NotificationUploadHandler(
                dao = db.notificationDao(),
                service = get<NotificationSensorService>()
            ),
            kaist.iclab.mobiletracker.services.upload.handlers.phone.UserInteractionUploadHandler(
                dao = db.userInteractionDao(),
                service = get<UserInteractionSensorService>()
            )
        )
        kaist.iclab.mobiletracker.services.upload.handlers.SensorUploadHandlerRegistry(handlers)
    }

    // PhoneSensorUploadService for handling phone sensor data uploads
    single {
        PhoneSensorUploadService(
            handlerRegistry = get(),
            supabaseHelper = get(),
            syncTimestampService = get()
        )
    }
}

