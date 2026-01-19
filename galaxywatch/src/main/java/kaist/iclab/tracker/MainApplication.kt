package kaist.iclab.tracker

import android.app.Application
import android.util.Log
import kaist.iclab.tracker.collector.core.Collector
import kaist.iclab.tracker.controller.CollectorController
import kaist.iclab.tracker.notification.NotfManager
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Tracker.initialize(this@MainApplication)
        startKoin {
            androidContext(this@MainApplication)
            androidLogger(level = Level.NONE)
            modules(koinModule)
        }
        initConfiguration()
    }

    fun initConfiguration() {
        val collectorController = get<CollectorController>()
        collectorController.initializeCollectors(get())

        val collectorMap = get<Map<String, Collector>>()
        collectorMap.forEach { (name, collector) ->
            collector.listener = { data ->
                Log.d(collector.NAME, "Data: $data")
            }
        }

        val notfManager = get<NotfManager>()
        notfManager.setServiceNotfDescription(
            icon = R.drawable.ic_notf
        )
    }
}