package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.tracker.sensor.core.Sensor
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * The open flavor is built without the Samsung Health SDK, so it has no Samsung Health sensors
 * and no health data permission handler.
 */
val samsungHealthSensorsModule = module {
    single<List<Sensor<*, *>>>(named(SAMSUNG_HEALTH_SENSORS)) { emptyList() }
}
