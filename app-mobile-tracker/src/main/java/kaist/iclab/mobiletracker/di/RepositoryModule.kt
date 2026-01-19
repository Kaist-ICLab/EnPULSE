package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.db.TrackerRoomDB
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.DataRepository
import kaist.iclab.mobiletracker.repository.DataRepositoryImpl
import kaist.iclab.mobiletracker.repository.HomeRepository
import kaist.iclab.mobiletracker.repository.HomeRepositoryImpl
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandler
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandlerRegistry
import kaist.iclab.mobiletracker.repository.handlers.phone.AmbientLightDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.AppListChangeDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.AppUsageDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.BatteryDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.BluetoothScanDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.CallLogDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.ConnectivityDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.DataTrafficDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.DeviceModeDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.LocationDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.MediaDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.MessageLogDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.NotificationDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.ScreenDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.StepDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.UserInteractionDataHandler
import kaist.iclab.mobiletracker.repository.handlers.phone.WifiScanDataHandler
import kaist.iclab.mobiletracker.repository.handlers.watch.WatchAccelerometerDataHandler
import kaist.iclab.mobiletracker.repository.handlers.watch.WatchEDADataHandler
import kaist.iclab.mobiletracker.repository.handlers.watch.WatchHeartRateDataHandler
import kaist.iclab.mobiletracker.repository.handlers.watch.WatchPPGDataHandler
import kaist.iclab.mobiletracker.repository.handlers.watch.WatchSkinTemperatureDataHandler
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.upload.PhoneSensorUploadService
import kaist.iclab.mobiletracker.services.upload.WatchSensorUploadService
import org.koin.dsl.module

/**
 * Koin module for Repository layer bindings.
 * Separates data layer concerns from ViewModels and Sensor configuration.
 */
val repositoryModule = module {
    // HomeRepository for Home screen dashboard
    single<HomeRepository> {
        val db = get<TrackerRoomDB>()
        HomeRepositoryImpl(
            phoneSensorDaos = HomeRepositoryImpl.PhoneSensorDaoMap(
                locationDao = db.locationDao(),
                appUsageLogDao = db.appUsageLogDao(),
                stepDao = db.stepDao(),
                batteryDao = db.batteryDao(),
                notificationDao = db.notificationDao(),
                screenDao = db.screenDao(),
                connectivityDao = db.connectivityDao(),
                bluetoothScanDao = db.bluetoothScanDao(),
                ambientLightDao = db.ambientLightDao(),
                appListChangeDao = db.appListChangeDao(),
                callLogDao = db.callLogDao(),
                dataTrafficDao = db.dataTrafficDao(),
                deviceModeDao = db.deviceModeDao(),
                mediaDao = db.mediaDao(),
                messageLogDao = db.messageLogDao(),
                userInteractionDao = db.userInteractionDao(),
                wifiScanDao = db.wifiDao()
            ),
            watchSensorDaos = HomeRepositoryImpl.WatchSensorDaoMap(
                heartRateDao = db.watchHeartRateDao(),
                accelerometerDao = db.watchAccelerometerDao(),
                edaDao = db.watchEDADao(),
                ppgDao = db.watchPPGDao(),
                skinTemperatureDao = db.watchSkinTemperatureDao()
            ),
            watchSensorRepository = get()
        )
    }

    // Sensor Data Handlers Registry
    single<SensorDataHandlerRegistry> {
        val db = get<TrackerRoomDB>()
        val handlers: List<SensorDataHandler> = listOf(
            // Phone sensor handlers
            LocationDataHandler(db.locationDao()),
            AppUsageDataHandler(db.appUsageLogDao()),
            StepDataHandler(db.stepDao()),
            BatteryDataHandler(db.batteryDao()),
            NotificationDataHandler(db.notificationDao()),
            ScreenDataHandler(db.screenDao()),
            ConnectivityDataHandler(db.connectivityDao()),
            BluetoothScanDataHandler(db.bluetoothScanDao()),
            AmbientLightDataHandler(db.ambientLightDao()),
            AppListChangeDataHandler(db.appListChangeDao()),
            CallLogDataHandler(db.callLogDao()),
            DataTrafficDataHandler(db.dataTrafficDao()),
            DeviceModeDataHandler(db.deviceModeDao()),
            MediaDataHandler(db.mediaDao()),
            MessageLogDataHandler(db.messageLogDao()),
            UserInteractionDataHandler(db.userInteractionDao()),
            WifiScanDataHandler(db.wifiDao()),
            // Watch sensor handlers
            WatchHeartRateDataHandler(db.watchHeartRateDao()),
            WatchAccelerometerDataHandler(db.watchAccelerometerDao()),
            WatchEDADataHandler(db.watchEDADao()),
            WatchPPGDataHandler(db.watchPPGDao()),
            WatchSkinTemperatureDataHandler(db.watchSkinTemperatureDao())
        )
        SensorDataHandlerRegistry(handlers)
    }

    // DataRepository for Data screen sensor list
    single<DataRepository> {
        DataRepositoryImpl(
            handlerRegistry = get<SensorDataHandlerRegistry>(),
            syncTimestampService = get<SyncTimestampService>(),
            phoneSensorUploadService = get<PhoneSensorUploadService>(),
            watchSensorUploadService = get<WatchSensorUploadService>(),
            supabaseHelper = get<SupabaseHelper>()
        )
    }
}
