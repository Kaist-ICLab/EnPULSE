package kaist.iclab.wearabletracker

import android.app.Application
import android.util.Log
import kaist.iclab.wearabletracker.data.SyncAckListener
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependencies
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependenciesProvider
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.qualifier.named
import org.koin.core.logger.Level

class WearableApplication : Application(), KoinComponent, BackgroundControllerDependenciesProvider {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@WearableApplication)
            androidLogger(level = Level.NONE)
            modules(koinModule)
        }

        try {
            getKoin().get<BackgroundController>().controllerStateFlow
        } catch (e: Exception) {
            Log.e("WearableApplication", "Error initializing BackgroundController: ${e.message}", e)
        }

        // Start listening for sync ACKs from the phone
        get<SyncAckListener>().startListening()
    }

    override fun provideBackgroundControllerDependencies(): BackgroundControllerDependencies {
        val koin = getKoin()
        return BackgroundControllerDependencies(
            controllerStateStorage = koin.get(named("watchControllerStateStorage")),
            sensors = koin.get(named("sensors")),
            serviceNotification = koin.get(),
            allowPartialSensing = true
        )
    }
}
