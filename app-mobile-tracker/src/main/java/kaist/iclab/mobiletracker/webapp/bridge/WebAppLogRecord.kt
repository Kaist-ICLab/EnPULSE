package kaist.iclab.mobiletracker.webapp.bridge

import kaist.iclab.tracker.sensor.core.SensorEntity
import kotlinx.serialization.Serializable

/**
 * The [SensorEntity] source type for the `WebAppLog` "sensor" — built directly by [LogBridgeHandler]
 * from a `logEvent` bridge call rather than emitted by a tracker-library `Sensor`/`Listener`. It
 * plays the same role the library's own sensor entities play for every other [SensorEntity] source:
 * [kaist.iclab.mobiletracker.di.SensorRegistry]'s `WebAppLog` [kaist.iclab.mobiletracker.di.SensorDescriptor]
 * maps it into the stored [kaist.iclab.mobiletracker.db.entity.phone.WebAppLogEntity] row via its
 * `fromSensorEntity` function, exactly like a real sensor's entity would be.
 */
@Serializable
data class WebAppLogRecord(
    val timestamp: Long,
    val webAppId: String,
    val eventName: String,
    val propertiesJson: String?
) : SensorEntity()
