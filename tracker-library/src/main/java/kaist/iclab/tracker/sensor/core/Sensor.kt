package kaist.iclab.tracker.sensor.core

import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

interface Sensor<C : SensorConfig, E : SensorEntity> {
    val id: String
    val name: String

    val permissions: Array<String>
    val foregroundServiceTypes: Array<Int>

    /* Config-related */
    val initialConfig: C
    val configStateFlow: StateFlow<C>
    val configClass: KClass<C>
    fun updateConfig(changedValues: Map<String, String>)
    fun resetConfig()

    /* State-related */
    val sensorStateFlow: StateFlow<SensorState>

    /* UNAVAILABLE => Check*/
    fun init()

    /* DISABLED => READY */
    fun enable()

    /* READY => DISABLED */
    fun disable()

    /* Start collector to collect data: READY => RUNNING */
    fun start()

    /* Stop collector to stop collecting data: RUNNING => READY */
    fun stop()

    /* Action-related */
    val entityClass: KClass<out E>
    fun addListener(listener: (E) -> Unit)
    fun removeListener(listener: (E) -> Unit)
}