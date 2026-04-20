package kaist.iclab.mobiletracker

import android.app.Application
import android.content.Context
import android.util.Log
import kaist.iclab.mobiletracker.di.appModule
import kaist.iclab.mobiletracker.di.authModule
import kaist.iclab.mobiletracker.di.databaseModule
import kaist.iclab.mobiletracker.di.helperModule
import kaist.iclab.mobiletracker.di.phoneSensorModule
import kaist.iclab.mobiletracker.di.repositoryModule
import kaist.iclab.mobiletracker.di.viewModelModule
import kaist.iclab.mobiletracker.di.watchSensorModule
import kaist.iclab.mobiletracker.helpers.LanguageHelper
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependencies
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependenciesProvider
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.qualifier.named
import org.koin.core.logger.Level

/**
 * Application class for MobileTracker app.
 * Handles global initialization and setup that should happen when the app starts.
 */
class MobileTrackerApplication : Application(), KoinComponent, BackgroundControllerDependenciesProvider {

    override fun attachBaseContext(base: Context) {
        val context = LanguageHelper(base).attachBaseContextWithLanguage(base)
        super.attachBaseContext(context)
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin Dependency Injection
        startKoin {
            androidLogger(level = Level.NONE)
            androidContext(this@MobileTrackerApplication)
            modules(
                appModule,
                authModule,
                databaseModule,
                watchSensorModule,
                phoneSensorModule,
                repositoryModule,
                helperModule,
                viewModelModule
            )
        }

        // Additional initialization
        initializeApp()
    }

    private fun initializeApp() {
        // Eagerly initialize BackgroundController so sensors are prepared before services bind.
        try {
            val backgroundController = getKoin().get<BackgroundController>()
            backgroundController.controllerStateFlow
        } catch (e: Exception) {
            Log.e(
                "MobileTrackerApplication",
                "Error initializing BackgroundController: ${e.message}",
                e
            )
        }

        // Additional initialization can be added here:
        // - Crash reporting
        // - Analytics
        // - Global error handlers
        // - Third-party SDK initialization
    }

    override fun provideBackgroundControllerDependencies(): BackgroundControllerDependencies {
        val koin = getKoin()
        return BackgroundControllerDependencies(
            controllerStateStorage = koin.get(named("phoneControllerStateStorage")),
            sensors = koin.get(named("phoneSensors")),
            serviceNotification = koin.get(),
            allowPartialSensing = true
        )
    }
}

