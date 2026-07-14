package kaist.iclab.wearabletracker

import androidx.room.Room
import com.google.android.gms.location.Priority
import kaist.iclab.tracker.listener.SamsungHealthSensorInitializer
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.common.LocationSensor
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.galaxywatch.AccelerometerSensor
import kaist.iclab.tracker.sensor.galaxywatch.AudioSensor
import kaist.iclab.tracker.sensor.galaxywatch.EDASensor
import kaist.iclab.tracker.sensor.galaxywatch.GestureSensor
import kaist.iclab.tracker.sensor.galaxywatch.HeartRateSensor
import kaist.iclab.tracker.sensor.galaxywatch.IMUSensor
import kaist.iclab.tracker.sensor.galaxywatch.PPGSensor
import kaist.iclab.tracker.sensor.galaxywatch.SkinTemperatureSensor
import kaist.iclab.tracker.sensor.galaxywatch.StressSensor
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.storage.core.RmssdHistory
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage
import kaist.iclab.tracker.trigger.adapter.galaxywatch.GestureDetectionAdapter
import kaist.iclab.tracker.trigger.adapter.galaxywatch.StressDetectionAdapter
import kaist.iclab.tracker.trigger.engine.DefaultTriggerEngine
import kaist.iclab.tracker.trigger.engine.TriggerEngine
import kaist.iclab.tracker.trigger.state.DetectionStateTracker
import kaist.iclab.wearabletracker.data.AutoSyncManager
import kaist.iclab.wearabletracker.data.PhoneCommunicationManager
import kaist.iclab.wearabletracker.data.SyncAckListener
import kaist.iclab.wearabletracker.db.TrackerRoomDB
import kaist.iclab.wearabletracker.db.dao.BaseDao
import kaist.iclab.wearabletracker.ema.MicroEmaRepository
import kaist.iclab.wearabletracker.ema.MicroEmaResponseManager
import kaist.iclab.wearabletracker.ema.MicroEmaViewModel
import kaist.iclab.wearabletracker.helpers.MicroEmaPreferencesHelper
import kaist.iclab.wearabletracker.helpers.SyncPreferencesHelper
import kaist.iclab.wearabletracker.repository.WatchSensorRepository
import kaist.iclab.wearabletracker.repository.WatchSensorRepositoryImpl
import kaist.iclab.wearabletracker.storage.RoomRmssdHistory
import kaist.iclab.wearabletracker.storage.SensorDataReceiver
import kaist.iclab.wearabletracker.trigger.TriggerConfigReceiver
import kaist.iclab.wearabletracker.trigger.TriggerConfigStorage
import kaist.iclab.wearabletracker.trigger.WatchTriggerActionHandler
import kaist.iclab.wearabletracker.ui.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val koinModule = module {
    single {
        SamsungHealthSensorInitializer(context = androidContext())
    }

    single {
        CouchbaseDB(context = androidContext())
    }

    single {
        Room.databaseBuilder(
            androidContext(),
            TrackerRoomDB::class.java,
            "wearable_tracker_db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    single {
        AndroidPermissionManager(context = androidContext())
    }

    // Sensors
    single {
        AccelerometerSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = AccelerometerSensor.Config(),
                clazz = AccelerometerSensor.Config::class.java,
                collectionName = (AccelerometerSensor::class.simpleName ?: "") + "config"
            ),
            stateStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = SensorState(SensorState.FLAG.UNAVAILABLE),
                clazz = SensorState::class.java,
                collectionName = AccelerometerSensor::class.simpleName ?: ""
            ),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        PPGSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = PPGSensor.Config(),
                clazz = PPGSensor.Config::class.java,
                collectionName = (PPGSensor::class.simpleName ?: "") + "config"
            ),
            stateStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = SensorState(SensorState.FLAG.UNAVAILABLE),
                clazz = SensorState::class.java,
                collectionName = PPGSensor::class.simpleName ?: ""
            ),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        HeartRateSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = HeartRateSensor.Config(),
                clazz = HeartRateSensor.Config::class.java,
                collectionName = (HeartRateSensor::class.simpleName ?: "") + "config"
            ),
            stateStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = SensorState(SensorState.FLAG.UNAVAILABLE),
                clazz = SensorState::class.java,
                collectionName = HeartRateSensor::class.simpleName ?: ""
            ),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        SkinTemperatureSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = SkinTemperatureSensor.Config(),
                clazz = SkinTemperatureSensor.Config::class.java,
                collectionName = (SkinTemperatureSensor::class.simpleName ?: "") + "config"
            ),
            stateStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = SensorState(SensorState.FLAG.UNAVAILABLE),
                clazz = SensorState::class.java,
                collectionName = SkinTemperatureSensor::class.simpleName ?: ""
            ),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        EDASensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = EDASensor.Config(),
                clazz = EDASensor.Config::class.java,
                collectionName = (EDASensor::class.simpleName ?: "") + "config"
            ),
            stateStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = SensorState(SensorState.FLAG.UNAVAILABLE),
                clazz = SensorState::class.java,
                collectionName = EDASensor::class.simpleName ?: ""
            ),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        IMUSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = IMUSensor.Config(),
                clazz = IMUSensor.Config::class.java,
                collectionName = (IMUSensor::class.simpleName ?: "") + "config"
            ),
            stateStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = SensorState(SensorState.FLAG.UNAVAILABLE),
                clazz = SensorState::class.java,
                collectionName = IMUSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        AudioSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = AudioSensor.Config(),
                clazz = AudioSensor.Config::class.java,
                collectionName = (AudioSensor::class.simpleName ?: "") + "config"
            ),
            stateStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = SensorState(SensorState.FLAG.UNAVAILABLE),
                clazz = SensorState::class.java,
                collectionName = AudioSensor::class.simpleName ?: ""
            )
        )
    }

    single {
        GestureSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = GestureSensor.Config(),
                clazz = GestureSensor.Config::class.java,
                collectionName = (GestureSensor::class.simpleName ?: "") + "config"
            ),
            stateStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = SensorState(SensorState.FLAG.UNAVAILABLE),
                clazz = SensorState::class.java,
                collectionName = GestureSensor::class.simpleName ?: ""
            ),
            imuSensor = get(),
            audioSensor = get()
        )
    }

    single<RmssdHistory> {
        RoomRmssdHistory(dao = get<TrackerRoomDB>().rmssdHistoryDao())
    }

    single {
        StressSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = StressSensor.Config(),
                clazz = StressSensor.Config::class.java,
                collectionName = (StressSensor::class.simpleName ?: "") + "config"
            ),
            stateStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = SensorState(SensorState.FLAG.UNAVAILABLE),
                clazz = SensorState::class.java,
                collectionName = StressSensor::class.simpleName ?: ""
            ),
            heartRateSensor = get(),
            rmssdHistory = get()
        )
    }

    single {
        LocationSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = LocationSensor.Config(
                    interval = TimeUnit.SECONDS.toMillis(1),
                    maxUpdateAge = 0,
                    maxUpdateDelay = 0,
                    minUpdateDistance = 0.0f,
                    minUpdateInterval = 0,
                    priority = Priority.PRIORITY_HIGH_ACCURACY,
                    waitForAccurateLocation = true,
                ),
                clazz = LocationSensor.Config::class.java,
                collectionName = (LocationSensor::class.simpleName ?: "") + "config"
            ),
            stateStorage = CouchbaseStateStorage(
                couchbase = get(),
                defaultVal = SensorState(SensorState.FLAG.UNAVAILABLE),
                clazz = SensorState::class.java,
                collectionName = LocationSensor::class.simpleName ?: ""
            )
        )
    }

    single(named("sensors")) {
        listOf(
            get<AccelerometerSensor>(),
            get<PPGSensor>(),
            get<HeartRateSensor>(),
            get<SkinTemperatureSensor>(),
            get<EDASensor>(),
            get<IMUSensor>(),
            get<GestureSensor>(),
            get<StressSensor>(),
            get<LocationSensor>()
        )
    }

    single<Map<String, BaseDao<*>>>(named("sensorDataStorages")) {
        mapOf(
            get<AccelerometerSensor>().id to get<TrackerRoomDB>().accelerometerDao(),
            get<PPGSensor>().id to get<TrackerRoomDB>().ppgDao(),
            get<HeartRateSensor>().id to get<TrackerRoomDB>().heartRateDao(),
            get<SkinTemperatureSensor>().id to get<TrackerRoomDB>().skinTemperatureDao(),
            get<EDASensor>().id to get<TrackerRoomDB>().edaDao(),
            get<LocationSensor>().id to get<TrackerRoomDB>().locationDao(),
            get<IMUSensor>().id to get<TrackerRoomDB>().imuDao(),
            get<GestureSensor>().id to get<TrackerRoomDB>().gestureDao(),
            get<StressSensor>().id to get<TrackerRoomDB>().stressDao()
        )
    }

    // Global Controller
    single {
        val context = androidContext()
        BackgroundController.ServiceNotification(
            channelId = "BackgroundControllerService",
            channelName = "EnPULSE",
            notificationId = 1,
            title = context.getString(R.string.notification_title),
            description = context.getString(R.string.notification_description),
            icon = R.drawable.ic_launcher_foreground
        )
    }

    single<StateStorage<ControllerState>>(named("watchControllerStateStorage")) {
        CouchbaseStateStorage(
            couchbase = get(),
            defaultVal = ControllerState(ControllerState.FLAG.DISABLED),
            clazz = ControllerState::class.java,
            collectionName = BackgroundController::class.simpleName ?: ""
        )
    }

    single {
        BackgroundController(
            context = androidContext(),
            controllerStateStorage = get(named("watchControllerStateStorage")),
            sensors = get(qualifier("sensors")),
            serviceNotification = get<BackgroundController.ServiceNotification>(),
            allowPartialSensing = true
        )
    }

    single {
        SensorDataReceiver(
            context = androidContext(),
        )
    }

    // SyncPreferencesHelper for managing sync metadata
    single {
        SyncPreferencesHelper(context = androidContext())
    }

    single {
        MicroEmaPreferencesHelper(context = androidContext())
    }

    single {
        PhoneCommunicationManager(
            androidContext = androidContext(),
            daos = get(named("sensorDataStorages")),
            syncPreferencesHelper = get(),
            coroutineScope = get()
        )
    }

    // AutoSyncManager
    single {
        AutoSyncManager(
            context = androidContext(),
            phoneCommunicationManager = get(),
            microEmaResponseManager = get<MicroEmaResponseManager>(),
            syncPreferencesHelper = get(),
            controllerStateFlow = get<BackgroundController>().controllerStateFlow,
            coroutineScope = get()
        )
    }

    // SyncAckListener to handle acknowledgment messages from the phone and perform data cleanup
    single<SyncAckListener> {
        SyncAckListener(
            bleChannel = get<PhoneCommunicationManager>().getBleChannel(),
            daos = get(named("sensorDataStorages")),
            syncPreferencesHelper = get(),
            coroutineScope = get()
        )
    }

    // Repository
    single<WatchSensorRepository> {
        WatchSensorRepositoryImpl(
            sensorDataStorages = get(named("sensorDataStorages")),
            syncPreferencesHelper = get()
        )
    }

    // ViewModel
    viewModel {
        SettingsViewModel(
            sensorController = get(),
            sensorDataReceiver = get(),
            phoneCommunicationManager = get(),
            repository = get(),
            samsungHealthSensorInitializer = get(),
            applicationContext = androidContext(),
            autoSyncManager = get()
        )
    }

    // Global CoroutineScope for background operations (PhoneCommunicationManager, AutoSyncManager,
    // SyncAckListener, SensorDataReceiverService). This scope intentionally lives for the entire
    // process lifetime and is never cancelled, matching the Koin singleton lifecycle.
    single<kotlinx.coroutines.CoroutineScope> {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    }

    // --- MicroEMA ---

    single {
        MicroEmaRepository(prefsHelper = get())
    }

    single {
        get<TrackerRoomDB>().microEmaResponseDao()
    }

    single {
        MicroEmaResponseManager(
            context = androidContext(),
            repository = get(),
            dao = get(),
            phoneCommunicationManager = get(),
            coroutineScope = get()
        )
    }

    viewModel {
        MicroEmaViewModel(
            repository = get(),
            responseManager = get()
        )
    }

    // --- Dynamic Trigger Engine ---

    single {
        DetectionStateTracker()
    }

    single {
        TriggerConfigStorage(context = androidContext())
    }

    single {
        StressDetectionAdapter(
            stressSensor = get(),
            tracker = get()
        )
    }

    single {
        GestureDetectionAdapter(
            gestureSensor = get(),
            tracker = get()
        )
    }

    single<TriggerEngine> {
        DefaultTriggerEngine(
            context = androidContext(),
            detectionStateTracker = get(),
            coroutineScope = get()
        )
    }

    single {
        WatchTriggerActionHandler(
            context = androidContext(),
            microEmaRepository = get(),
            bleChannel = get<PhoneCommunicationManager>().getBleChannel()
        )
    }

    single {
        TriggerConfigReceiver(
            bleChannel = get<PhoneCommunicationManager>().getBleChannel(),
            triggerEngine = get(),
            actionHandler = get(),
            backgroundController = get(),
            detectionStateTracker = get(),
            storage = get()
        )
    }

    // Wire up: set action handler on engine, start adapters, start config listener
    single(named("triggerInitializer")) {
        val engine = get<TriggerEngine>()
        val actionHandler = get<WatchTriggerActionHandler>()
        val stressAdapter = get<StressDetectionAdapter>()
        val gestureAdapter = get<GestureDetectionAdapter>()
        val configReceiver = get<TriggerConfigReceiver>()
        val storage = get<TriggerConfigStorage>()

        // Set the action handler
        engine.setActionHandler(actionHandler)

        // Restore persisted config if it exists
        storage.loadConfig()?.let { payload ->
            actionHandler.updateSurveyConfigs(payload.surveyConfigs)
            engine.loadTriggers(payload.triggers)
        }

        // Start adapters (they attach listeners to sensors)
        stressAdapter.start()
        gestureAdapter.start()

        // Start listening for trigger config from phone
        configReceiver.startListening()

        // Start the engine (it will begin evaluating when detection states change)
        engine.start()

        true // return value to satisfy Koin
    }
}