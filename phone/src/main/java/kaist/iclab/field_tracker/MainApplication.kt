package kaist.iclab.field_tracker

import android.app.Application
import kaist.iclab.tracker.notification.NotfManager
import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.storage.core.SensorDataStorage
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(realModule)
        }
        init()
    }

    fun init() {
        val sensors: List<Sensor<*, *>> = get(named("collectors"))
        val storages: List<SensorDataStorage> = get(named("storages"))
        sensors.forEach { sensor ->
            sensor.addListener { entity ->
                val storage = storages.find { it.ID == sensor.ID }
                    ?: error("Storage not found for ${sensor.ID}")
                storage.insert(entity)
            }
        }

        val notfManager: NotfManager = get()
        notfManager.init()
    }
}