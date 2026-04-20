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
import kaist.iclab.mobiletracker.services.PhoneSensorDataService
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.ControllerState
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

/**
 * Application class for MobileTracker app.
 * Handles global initialization and setup that should happen when the app starts.
 */
class MobileTrackerApplication : Application(), KoinComponent {

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
        // Eagerly initialize BackgroundController to ensure service locator is set up
        // This prevents crashes when Android creates the ControllerService before
        // BackgroundController is initialized
        try {
            val backgroundController = getKoin().get<BackgroundController>()
            // Access the controller to trigger its initialization
            // The init block will set up BackgroundControllerServiceLocator
            backgroundController.controllerStateFlow
        } catch (e: Exception) {
            // Log error but don't crash - this is just eager initialization
            Log.e(
                "MobileTrackerApplication",
                "Error initializing BackgroundController: ${e.message}",
                e
            )
        }

        // Observe controller state to manage PhoneSensorDataService lifecycle.
        // This is placed at the Application level so it persists across
        // Activity/ViewModel recreations (navigation, config changes, etc.).
        observeControllerStateForService()

        // Additional initialization can be added here:
        // - Crash reporting
        // - Analytics
        // - Global error handlers
        // - Third-party SDK initialization
    }

    /**
     * Observes BackgroundController state and starts/stops PhoneSensorDataService accordingly.
     * Placed at the Application level so it persists regardless of Activity/ViewModel lifecycle.
     */
    private fun observeControllerStateForService() {
        val backgroundController = getKoin().get<BackgroundController>()
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            backgroundController.controllerStateFlow.collect { state ->
                try {
                    when (state.flag) {
                        ControllerState.FLAG.RUNNING -> PhoneSensorDataService.start(this@MobileTrackerApplication)
                        else -> PhoneSensorDataService.stop(this@MobileTrackerApplication)
                    }
                } catch (e: Exception) {
                    Log.e("MobileTrackerApplication", "Error managing phone sensor service: ${e.message}", e)
                }
            }
        }
    }
}
