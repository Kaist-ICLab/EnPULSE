package kaist.iclab.wearabletracker

import com.google.android.gms.location.Priority
import kaist.iclab.tracker.listener.SamsungHealthSensorInitializer
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.common.LocationSensor
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.galaxywatch.AccelerometerSensor
import kaist.iclab.tracker.sensor.galaxywatch.AudioSensor
import kaist.iclab.tracker.sensor.galaxywatch.ECGSensor
import kaist.iclab.tracker.sensor.galaxywatch.EDASensor
import kaist.iclab.tracker.sensor.galaxywatch.GestureSensor
import kaist.iclab.tracker.sensor.galaxywatch.HeartRateSensor
import kaist.iclab.tracker.sensor.galaxywatch.IMUSensor
import kaist.iclab.tracker.sensor.galaxywatch.PPGSensor
import kaist.iclab.tracker.sensor.galaxywatch.SkinTemperatureSensor
import kaist.iclab.tracker.sensor.galaxywatch.StressSensor
import kaist.iclab.tracker.storage.core.RmssdHistory
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.storage.couchbase.CouchbaseDB
import kaist.iclab.tracker.storage.couchbase.CouchbaseStateStorage
import kaist.iclab.tracker.trigger.adapter.galaxywatch.GestureDetectionAdapter
import kaist.iclab.tracker.trigger.adapter.galaxywatch.StressDetectionAdapter
import kaist.iclab.tracker.trigger.state.DetectionStateTracker
import kaist.iclab.wearabletracker.data.AutoSyncManager
import kaist.iclab.wearabletracker.data.CampaignSensorConfigRepository
import kaist.iclab.wearabletracker.data.PhoneCommunicationManager
import kaist.iclab.wearabletracker.data.SyncAckListener
import kaist.iclab.wearabletracker.db.entity.MyObjectBox
import kaist.iclab.wearabletracker.db.obx.MicroEmaResponseStore
import kaist.iclab.wearabletracker.db.obx.WatchSensorStore
import kaist.iclab.wearabletracker.db.obx.WatchSensorStores
import kaist.iclab.wearabletracker.ecg.EcgViewModel
import kaist.iclab.wearabletracker.ema.MicroEmaRepository
import kaist.iclab.wearabletracker.ema.MicroEmaResponseManager
import kaist.iclab.wearabletracker.ema.MicroEmaViewModel
import kaist.iclab.wearabletracker.helpers.MicroEmaPreferencesHelper
import kaist.iclab.wearabletracker.helpers.SyncPreferencesHelper
import kaist.iclab.wearabletracker.repository.WatchSensorRepository
import kaist.iclab.wearabletracker.repository.WatchSensorRepositoryImpl
import kaist.iclab.wearabletracker.storage.ObjectBoxRmssdHistory
import kaist.iclab.wearabletracker.storage.SensorDataReceiver
import kaist.iclab.wearabletracker.trigger.DetectionStateForwarder
import kaist.iclab.wearabletracker.trigger.WatchEmaTriggerReceiver
import kaist.iclab.wearabletracker.trigger.WatchNotificationTriggerReceiver
import kaist.iclab.wearabletracker.trigger.WatchSurveyConfigReceiver
import kaist.iclab.wearabletracker.trigger.WatchSurveyConfigStorage
import kaist.iclab.wearabletracker.ui.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        MyObjectBox.builder()
            .androidContext(androidContext())
            .maxSizeInKByte(Constants.DB.OBJECTBOX_MAX_SIZE_KB)
            .build()
    }

    single {
        WatchSensorStores(boxStore = get())
    }

    single {
        MicroEmaResponseStore(boxStore = get())
    }

    single {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    single {
        AndroidPermissionManager(
            context = androidContext(),
            scope = get<CoroutineScope>()
        )
    }

    // Sensors
    single {
        AccelerometerSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(AccelerometerSensor::class, AccelerometerSensor.Config()),
            stateStorage = sensorState(AccelerometerSensor::class),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        PPGSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(PPGSensor::class, PPGSensor.Config()),
            stateStorage = sensorState(PPGSensor::class),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        HeartRateSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(HeartRateSensor::class, HeartRateSensor.Config()),
            stateStorage = sensorState(HeartRateSensor::class),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        SkinTemperatureSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(
                SkinTemperatureSensor::class,
                SkinTemperatureSensor.Config()
            ),
            stateStorage = sensorState(SkinTemperatureSensor::class),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        EDASensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(EDASensor::class, EDASensor.Config()),
            stateStorage = sensorState(EDASensor::class),
            samsungHealthSensorInitializer = get()
        )
    }

    single {
        IMUSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(IMUSensor::class, IMUSensor.Config()),
            stateStorage = sensorState(IMUSensor::class)
        )
    }

    single {
        AudioSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(AudioSensor::class, AudioSensor.Config()),
            stateStorage = sensorState(AudioSensor::class)
        )
    }

    single {
        GestureSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(GestureSensor::class, GestureSensor.Config()),
            stateStorage = sensorState(GestureSensor::class),
            imuSensor = get(),
            audioSensor = get()
        )
    }

    single<RmssdHistory> {
        ObjectBoxRmssdHistory(boxStore = get())
    }

    single {
        StressSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(StressSensor::class, StressSensor.Config()),
            stateStorage = sensorState(StressSensor::class),
            heartRateSensor = get(),
            rmssdHistory = get()
        )
    }

    single {
        LocationSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(
                LocationSensor::class,
                LocationSensor.Config(
                    interval = TimeUnit.SECONDS.toMillis(1),
                    maxUpdateAge = 0,
                    maxUpdateDelay = 0,
                    minUpdateDistance = 0.0f,
                    minUpdateInterval = 0,
                    priority = Priority.PRIORITY_HIGH_ACCURACY,
                    waitForAccurateLocation = true,
                )
            ),
            stateStorage = sensorState(LocationSensor::class)
        )
    }

    // ECG is on-demand only (Samsung HealthTrackerType.ECG_ON_DEMAND), triggered manually via
    // EcgViewModel — intentionally NOT added to watchSensorDescriptors so it is excluded from
    // BackgroundController's auto start/stop loop and SensorDataReceiver's auto-listener wiring.
    single {
        ECGSensor(
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = sensorConfig(ECGSensor::class, ECGSensor.Config()),
            stateStorage = sensorState(ECGSensor::class),
            samsungHealthSensorInitializer = get()
        )
    }

    // Single source of truth: all descriptors drive both registries below
    single(named("watchSensorDescriptors")) {
        val stores = get<WatchSensorStores>()
        listOf(
            WatchSensorDescriptor(get<AccelerometerSensor>(), stores.accelerometer),
            WatchSensorDescriptor(get<PPGSensor>(), stores.ppg),
            WatchSensorDescriptor(get<HeartRateSensor>(), stores.heartRate),
            WatchSensorDescriptor(get<SkinTemperatureSensor>(), stores.skinTemperature),
            WatchSensorDescriptor(get<EDASensor>(), stores.eda),
            WatchSensorDescriptor(get<IMUSensor>(), stores.watchIMU),
            WatchSensorDescriptor(get<GestureSensor>(), stores.watchGesture),
            WatchSensorDescriptor(get<StressSensor>(), stores.watchStress),
            WatchSensorDescriptor(get<LocationSensor>(), stores.location),
        )
    }

    single(named("sensors")) {
        get<List<WatchSensorDescriptor>>(named("watchSensorDescriptors")).map { it.sensor }
    }

    single<Map<String, WatchSensorStore<*>>>(named("sensorDataStorages")) {
        val base = get<List<WatchSensorDescriptor>>(named("watchSensorDescriptors"))
            .filter { it.store != null }
            .associate { it.sensor.id to it.store!! }
        // ECG's store still participates in phone-sync/record-counts/flush even though
        // the sensor itself isn't part of watchSensorDescriptors (see ECGSensor single above).
        val ecgSensor = get<ECGSensor>()
        val stores = get<WatchSensorStores>()
        base + (ecgSensor.id to stores.ecg)
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
            serializer = ControllerState.serializer(),
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
        SensorDataReceiver(context = androidContext())
    }

    single {
        SyncPreferencesHelper(context = androidContext())
    }

    single {
        MicroEmaPreferencesHelper(context = androidContext())
    }

    single {
        PhoneCommunicationManager(
            androidContext = androidContext(),
            stores = get(named("sensorDataStorages")),
            syncPreferencesHelper = get(),
            coroutineScope = get()
        )
    }

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

    single<SyncAckListener> {
        SyncAckListener(
            bleChannel = get<PhoneCommunicationManager>().getBleChannel(),
            stores = get(named("sensorDataStorages")),
            syncPreferencesHelper = get(),
            boxStore = get(),
            coroutineScope = get()
        )
    }

    single {
        CampaignSensorConfigRepository(
            bleChannel = get<PhoneCommunicationManager>().getBleChannel(),
            storage = campaignSensorConfigStorage(),
            coroutineScope = get()
        )
    }

    single<WatchSensorRepository> {
        WatchSensorRepositoryImpl(
            sensorDataStorages = get(named("sensorDataStorages")),
            syncPreferencesHelper = get()
        )
    }

    viewModel {
        SettingsViewModel(
            sensorController = get(),
            sensorDataReceiver = get(),
            phoneCommunicationManager = get(),
            repository = get(),
            samsungHealthSensorInitializer = get(),
            applicationContext = androidContext(),
            autoSyncManager = get(),
            ecgSensor = get(),
            campaignSensorConfigRepository = get()
        )
    }

    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    // --- MicroEMA ---

    single {
        MicroEmaRepository(
            prefsHelper = get()
        )
    }

    single {
        MicroEmaResponseManager(
            context = androidContext(),
            repository = get(),
            store = get(),
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

    // --- ECG (on-demand) ---

    viewModel {
        EcgViewModel(
            ecgSensor = get(),
            watchSensorStores = get(),
            permissionManager = get<AndroidPermissionManager>()
        )
    }

    // --- Trigger condition sources ---
    // The trigger engine now lives entirely on the phone (see
    // kaist.iclab.mobiletracker.di.phone.triggerModule). The watch only: (1) keeps its local
    // DetectionStateTracker fed by sensor adapters and forwards it to the phone, (2) executes
    // WatchEma commands the phone sends back, and (3) shows notification-action alerts the phone
    // sends over BLE (native Wear OS bridging was tried for this and didn't reliably show up).

    single {
        DetectionStateTracker()
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

    single {
        DetectionStateForwarder(
            bleChannel = get<PhoneCommunicationManager>().getBleChannel(),
            detectionStateTracker = get(),
            coroutineScope = get()
        )
    }

    single {
        WatchSurveyConfigStorage(context = androidContext())
    }

    single {
        WatchSurveyConfigReceiver(
            bleChannel = get<PhoneCommunicationManager>().getBleChannel(),
            microEmaRepository = get(),
            storage = get()
        )
    }

    single {
        WatchEmaTriggerReceiver(
            context = androidContext(),
            bleChannel = get<PhoneCommunicationManager>().getBleChannel(),
            microEmaRepository = get(),
            surveyConfigReceiver = get()
        )
    }

    single {
        WatchNotificationTriggerReceiver(
            context = androidContext(),
            bleChannel = get<PhoneCommunicationManager>().getBleChannel()
        )
    }
}
