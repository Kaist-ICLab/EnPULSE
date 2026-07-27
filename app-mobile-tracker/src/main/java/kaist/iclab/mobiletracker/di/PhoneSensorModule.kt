package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.di.phone.activitySensorsModule
import kaist.iclab.mobiletracker.di.phone.communicationSensorsModule
import kaist.iclab.mobiletracker.di.phone.controllerModule
import kaist.iclab.mobiletracker.di.phone.coreSensorsModule
import kaist.iclab.mobiletracker.di.phone.microEmaSensorModule
import kaist.iclab.mobiletracker.di.phone.surveySensorModule
import kaist.iclab.mobiletracker.di.phone.uploadModule
import kaist.iclab.mobiletracker.di.phone.webAppModule
import org.koin.dsl.module

/**
 * Phone sensor module - combines all phone sensor sub-modules
 */
val phoneSensorModule = module {
    includes(
        coreSensorsModule,
        communicationSensorsModule,
        activitySensorsModule,
        surveySensorModule,
        controllerModule,
        uploadModule,
        microEmaSensorModule,
        webAppModule
    )
}
