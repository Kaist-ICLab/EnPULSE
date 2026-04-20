package com.example.survey_test_app

import android.app.Application
import kaist.iclab.tracker.sensor.controller.BackgroundController
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependencies
import kaist.iclab.tracker.sensor.controller.BackgroundControllerDependenciesProvider
import kaist.iclab.tracker.sensor.controller.ControllerState
import kaist.iclab.tracker.sensor.survey.SurveySensor
import kaist.iclab.tracker.storage.core.StateStorage
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.qualifier.named
import org.koin.core.logger.Level

class MyApplication : Application(), KoinComponent, BackgroundControllerDependenciesProvider {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApplication)
            androidLogger(level = Level.NONE)
            modules(koinModule)
        }
    }

    override fun provideBackgroundControllerDependencies(): BackgroundControllerDependencies {
        val koin = getKoin()
        return BackgroundControllerDependencies(
            controllerStateStorage = koin.get<StateStorage<ControllerState>>(named("controllerState")),
            sensors = listOf(koin.get<SurveySensor>()),
            serviceNotification = koin.get<BackgroundController.ServiceNotification>(),
            allowPartialSensing = true
        )
    }
}
