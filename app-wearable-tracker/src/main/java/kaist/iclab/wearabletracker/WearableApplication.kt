package kaist.iclab.wearabletracker

import android.app.Application
import kaist.iclab.wearabletracker.data.SyncAckListener
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class WearableApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@WearableApplication)
            androidLogger(level = Level.NONE)
            modules(koinModule)
        }

        // Start listening for sync ACKs from the phone
        get<SyncAckListener>().startListening()
    }
}
