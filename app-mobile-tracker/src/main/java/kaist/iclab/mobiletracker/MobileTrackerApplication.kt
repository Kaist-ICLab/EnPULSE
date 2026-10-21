package kaist.iclab.mobiletracker

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kaist.iclab.mobiletracker.config.SupabaseConfigManager
import kaist.iclab.mobiletracker.di.appModule
import kaist.iclab.mobiletracker.di.authModule
import kaist.iclab.mobiletracker.di.databaseModule
import kaist.iclab.mobiletracker.di.helperModule
import kaist.iclab.mobiletracker.di.phoneSensorModule
import kaist.iclab.mobiletracker.di.repositoryModule
import kaist.iclab.mobiletracker.di.viewModelModule
import kaist.iclab.mobiletracker.di.watchSensorModule
import kaist.iclab.mobiletracker.helpers.LanguageHelper
import kaist.iclab.mobiletracker.helpers.SupabaseHelper
import kaist.iclab.mobiletracker.services.PhoneSensorDataService
import kaist.iclab.mobiletracker.services.SurveyResponseCapture
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependencies
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependenciesProvider
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.controller.OffBodyDetector
import kaist.iclab.tracker.sensor.phone.SurveySensor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import org.koin.core.qualifier.named

/**
 * Application class for MobileTracker app.
 * Handles global initialization and setup that should happen when the app starts.
 */
class MobileTrackerApplication : Application(), KoinComponent,
    BackgroundControllerDependenciesProvider {

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

        // SurveySensor isn't in the "phoneSensors" list BackgroundController manages (see
        // SurveyResponseCapture's doc comment), so its permission registration/initial state
        // — normally handled by BackgroundController's `sensors.forEach { it.init() }` — has to
        // happen here instead. One-time and independent of Start/Stop Logging.
        try {
            getKoin().get<SurveySensor>().init()
        } catch (e: Exception) {
            Log.e("MobileTrackerApplication", "Error initializing SurveySensor: ${e.message}", e)
        }

        // Observe controller state to manage PhoneSensorDataService and survey response capture
        // lifecycle. This is placed at the Application level so it persists across
        // Activity/ViewModel recreations (navigation, config changes, etc.).
        observeControllerStateForService()

        // Refresh the Supabase Anonymous Key from the server-side edge function in the
        // background. SupabaseHelper builds its client synchronously at cold start using
        // whatever key was last cached (or the build-time default), so this keeps that
        // cache up to date without blocking startup.
        refreshAnonKeyFromServer()

        // Initialize BLEHelper for watch communication
        try {
            val bleHelper = getKoin().get<kaist.iclab.mobiletracker.helpers.BLEHelper>()
            bleHelper.initialize()
        } catch (e: Exception) {
            Log.e("MobileTrackerApplication", "Error initializing BLEHelper: ${e.message}")
        }

        // Start the trigger engine and its detection adapters. Unlike the sensors it evaluates
        // conditions from (e.g. TimingSensor, gated by the "collection running" lifecycle via
        // BackgroundController), the engine itself runs continuously — mirrors how the watch used
        // to start its DefaultTriggerEngine unconditionally in WearableApplication.onCreate().
        //
        // loadTriggers() must be called here too: DefaultTriggerEngine.triggers starts empty on
        // every process start and is otherwise only populated by
        // UserProfileRepositoryImpl.syncFullStudyConfig() (login/onboarding/manual resync in
        // Settings — and that's a no-op while collection is RUNNING). Without this, any process
        // restart that isn't a fresh login/onboarding (OEM background kill, reboot, crash — all
        // routine for a long-running background tracker) leaves the engine subscribed and logging
        // "State changed: ..." for every detection, but with zero triggers to evaluate, so no
        // action ever fires. Loading from TriggerRepository's persisted Couchbase cache here
        // requires no network call and keeps this in sync with whatever was last synced.
        try {
            getKoin().get<kaist.iclab.tracker.trigger.adapter.TimingDetectionAdapter>().start()
            val triggerEngine = getKoin().get<kaist.iclab.tracker.trigger.engine.TriggerEngine>()
            triggerEngine.loadTriggers(
                getKoin().get<kaist.iclab.mobiletracker.repository.TriggerRepository>()
                    .getParsedTriggers()
            )
            triggerEngine.start()
        } catch (e: Exception) {
            Log.e("MobileTrackerApplication", "Error starting trigger engine: ${e.message}", e)
        }

        // Re-arm auto-sync on process restart, mirroring the trigger engine's loadTriggers()
        // fix above. AutoSyncManager.start() is otherwise only called from Settings' "Start
        // Logging" UI action, so without this, any process restart that isn't a fresh
        // Start-Logging tap (OEM background kill, reboot, memory-pressure eviction — all routine
        // for a long-running background tracker) leaves auto-sync permanently off with no crash
        // and no log, even though data collection itself (via BackgroundController's persisted
        // state, above) resumes normally. getDataCollectionStarted() is the same persisted flag
        // AutoSyncService.checkAndSyncIfNeeded() gates on, so this only restarts the service when
        // the user actually had logging running.
        try {
            val syncTimestampService =
                getKoin().get<kaist.iclab.mobiletracker.services.SyncTimestampService>()
            if (syncTimestampService.getDataCollectionStarted() != null) {
                getKoin().get<kaist.iclab.mobiletracker.services.AutoSyncManager>().start()
            }
        } catch (e: Exception) {
            Log.e("MobileTrackerApplication", "Error restarting auto-sync: ${e.message}", e)
        }

        // Additional initialization can be added here:
        // - Crash reporting
        // - Analytics
        // - Global error handlers
        // - Third-party SDK initialization
    }

    /**
     * Fetches the latest Anonymous Key from the server-side `anon_key` edge function and,
     * if it differs from the currently cached key, persists it and rebuilds the Supabase
     * client via [SupabaseHelper.reinitialize] so subsequent requests use the fresh key.
     * Runs on the process lifecycle scope so it isn't cancelled by Activity recreation.
     */
    private fun refreshAnonKeyFromServer() {
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            try {
                val configManager = SupabaseConfigManager(this@MobileTrackerApplication)
                val url = configManager.getUrl()
                configManager.fetchAnonKeyFromServer(url).onSuccess { newKey ->
                    if (newKey != configManager.getAnonKey()) {
                        configManager.saveFetchedAnonKey(newKey)
                        getKoin().get<SupabaseHelper>().reinitialize()
                    }
                }.onFailure { e ->
                    Log.e("MobileTrackerApplication", "Error fetching anon key: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(
                    "MobileTrackerApplication",
                    "Error refreshing anon key: ${e.message}",
                    e
                )
            }
        }
    }

    /**
     * Observes BackgroundController state and starts/stops PhoneSensorDataService + survey
     * response capture accordingly. Placed at the Application level so it persists regardless of
     * Activity/ViewModel lifecycle.
     *
     * Survey rides the same RUNNING/not-RUNNING transitions as PhoneSensorDataService — capture
     * tracks Start/Stop Logging like every other sensor — but goes through
     * [SurveyResponseCapture] directly instead of the `"phoneSensors"` list's per-sensor
     * ENABLED/campaign_table filtering (see its doc comment for why).
     */
    private fun observeControllerStateForService() {
        val backgroundController = getKoin().get<BackgroundController>()
        val surveyResponseCapture = getKoin().get<SurveyResponseCapture>()
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            backgroundController.controllerStateFlow.collect { state ->
                try {
                    when (state.flag) {
                        ControllerState.FLAG.RUNNING -> {
                            PhoneSensorDataService.start(this@MobileTrackerApplication)
                            surveyResponseCapture.start()
                        }

                        else -> {
                            PhoneSensorDataService.stop(this@MobileTrackerApplication)
                            surveyResponseCapture.stop()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(
                        "MobileTrackerApplication",
                        "Error managing phone sensor service: ${e.message}",
                        e
                    )
                }
            }
        }
    }

    override fun provideBackgroundControllerDependencies(): BackgroundControllerDependencies {
        val koin = getKoin()
        return BackgroundControllerDependencies(
            controllerStateStorage = koin.get(named("phoneControllerStateStorage")),
            sensors = koin.get(named("phoneSensors")),
            serviceNotification = koin.get(),
            allowPartialSensing = true,
            offBodyDetector = OffBodyDetector(
                context = this
            )
        )
    }
}
