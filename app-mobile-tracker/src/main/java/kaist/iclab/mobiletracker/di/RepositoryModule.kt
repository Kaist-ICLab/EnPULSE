package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.db.obx.SensorStores
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.repository.CampaignRepository
import kaist.iclab.mobiletracker.repository.CampaignRepositoryImpl
import kaist.iclab.mobiletracker.repository.CampaignSensorRepository
import kaist.iclab.mobiletracker.repository.CampaignSensorRepositoryImpl
import kaist.iclab.mobiletracker.repository.DataRepository
import kaist.iclab.mobiletracker.repository.DataRepositoryImpl
import kaist.iclab.mobiletracker.repository.HomeRepository
import kaist.iclab.mobiletracker.repository.HomeRepositoryImpl
import kaist.iclab.mobiletracker.repository.SurveyRepository
import kaist.iclab.mobiletracker.repository.SurveyRepositoryImpl
import kaist.iclab.mobiletracker.repository.UserProfileRepository
import kaist.iclab.mobiletracker.repository.UserProfileRepositoryImpl
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
import kaist.iclab.mobiletracker.storage.CampaignSensorConfigStorage
import org.koin.core.qualifier.named
import org.koin.dsl.module


/**
 * Koin module for Repository layer bindings.
 * Separates data layer concerns from ViewModels and Sensor configuration.
 */
val repositoryModule = module {
    // HomeRepository for Home screen dashboard
    single<HomeRepository> {
        HomeRepositoryImpl(
            stores = get<SensorStores>(),
            watchSensorRepository = get()
        )
    }

    // Sensor Data Handlers Registry
    single<SensorDataHandlerRegistry> {
        val s = get<SensorStores>()
        val handlers: List<SensorDataHandler> = listOf(
            // Phone sensor handlers
            LocationDataHandler(s.location),
            AppUsageDataHandler(s.appUsageLog),
            StepDataHandler(s.step),
            BatteryDataHandler(s.battery),
            NotificationDataHandler(s.notification),
            ScreenDataHandler(s.screen),
            ConnectivityDataHandler(s.connectivity),
            BluetoothScanDataHandler(s.bluetoothScan),
            AmbientLightDataHandler(s.ambientLight),
            AppListChangeDataHandler(s.appListChange),
            CallLogDataHandler(s.callLog),
            DataTrafficDataHandler(s.dataTraffic),
            DeviceModeDataHandler(s.deviceMode),
            MediaDataHandler(s.media),
            MessageLogDataHandler(s.messageLog),
            UserInteractionDataHandler(s.userInteraction),
            WifiScanDataHandler(s.wifiScan),
            // Watch sensor handlers
            WatchHeartRateDataHandler(s.watchHeartRate),
            WatchAccelerometerDataHandler(s.watchAccelerometer),
            WatchEDADataHandler(s.watchEDA),
            WatchPPGDataHandler(s.watchPPG),
            WatchSkinTemperatureDataHandler(s.watchSkinTemperature)
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

    // SurveyRepository for survey configuration management
    single<SurveyRepository> {
        SurveyRepositoryImpl(
            surveyService = get(),
            persistentStorage = get(),
            phoneSensorConfigStorage = get(named("surveySensorConfigStorage")),
            microEmaConfigStorage = get(named("microEmaConfigStorage")),
            scheduleStorage = get()
        )
    }

    // CampaignRepository for campaign data management
    single<CampaignRepository> {
        CampaignRepositoryImpl(
            campaignService = get()
        )
    }

    // CampaignSensorRepository for active sensor management
    single {
        CampaignSensorConfigStorage(
            couchbase = get()
        )
    }

    single<CampaignSensorRepository> {
        CampaignSensorRepositoryImpl(
            supabaseHelper = get(),
            persistentStorage = get()
        )
    }

    // UserProfileRepository for user profile management
    single {
        kaist.iclab.mobiletracker.storage.UserProfileStorage(
            couchbase = get()
        )
    }

    single<UserProfileRepository> {
        UserProfileRepositoryImpl(
            profileService = get(),
            supabaseHelper = get(),
            persistentStorage = get(),
            campaignSensorRepository = get(),
            surveyRepository = get()
        )
    }

}

