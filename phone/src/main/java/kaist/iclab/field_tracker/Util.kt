package kaist.iclab.field_tracker

import kaist.iclab.tracker.sensor.core.Sensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorState
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.reflect.KClass

fun Sensor<*, *>.toSensorUIModel(): SensorUIModel {
    return SensorUIModel(
        id = this.ID,
        name = this.NAME,
        permissions = this.permissions,
        configStateFlow = this.configStateFlow,
        configClass = this.configClass,
        sensorStateFlow = this.sensorStateFlow,
        enable = { this.enable() },
        disable = { this.disable() },
        updateConfig = { this.updateConfig(it) }
    )
}


data class SensorUIModel(
    val id: String,
    val name: String,
    val permissions: Array<String>,
    val sensorStateFlow: StateFlow<SensorState>,
    val configStateFlow: StateFlow<SensorConfig>,
    val configClass: KClass<out SensorConfig>,
    val updateConfig: (Map<String, String>) -> Unit,
    val enable: () -> Unit,
    val disable: () -> Unit,
)

fun convertUnixToFormatted(timestampMs: Long): String {
    val date = Date(timestampMs)
    val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss.SSS", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul") // UTC+0900
    return sdf.format(date) + " (UTC+0900)"
}
