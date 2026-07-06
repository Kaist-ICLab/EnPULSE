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
import kaist.iclab.mobiletracker.repository.handlers.SensorDataHandlerRegistry
import kaist.iclab.mobiletracker.services.SyncTimestampService
import kaist.iclab.mobiletracker.services.upload.SensorUploadService
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

    // DataRepository for Data screen sensor list
    single<DataRepository> {
        DataRepositoryImpl(
            handlerRegistry = get<SensorDataHandlerRegistry>(),
            syncTimestampService = get<SyncTimestampService>(),
            sensorUploadService = get<SensorUploadService>(),
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

