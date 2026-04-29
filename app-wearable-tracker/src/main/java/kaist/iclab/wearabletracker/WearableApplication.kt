package kaist.iclab.wearabletracker

import android.app.Application
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependencies
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependenciesProvider
import kaist.iclab.wearabletracker.data.SyncAckListener
import kaist.iclab.wearabletracker.ema.MicroEmaResponseManager
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import org.koin.core.qualifier.named

class WearableApplication : Application(), KoinComponent, BackgroundControllerDependenciesProvider {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@WearableApplication)
            androidLogger(level = Level.NONE)
            modules(koinModule)
        }

        // Start listening for sync ACKs from the phone
        get<SyncAckListener>().startListening()
        get<MicroEmaResponseManager>().startListening()
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
