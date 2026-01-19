package kaist.iclab.tracker.sensor.core

import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.permission.PermissionState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

abstract class BaseSensor<C : SensorConfig, E : SensorEntity>(
    val permissionManager: PermissionManager,
    private val configStorage: StateStorage<C>,
    private val stateStorage: StateStorage<SensorState>,
    override val configClass: KClass<C>,
    override val entityClass: KClass<E>,
) : Sensor<C, E> {
    override val id: String = extractId(this::class.simpleName ?: "Unknown")
    override val name: String = extractName(this::class.simpleName ?: "Unknown")

    /* Config-related */
    override val configStateFlow: StateFlow<C>
        get() = configStorage.stateFlow

    override val initialConfig = configStorage.get()

    override fun updateConfig(changedValues: Map<String, String>) {
        if (sensorStateFlow.value.flag == SensorState.FLAG.RUNNING) {
            throw IllegalStateException("Cannot update config while running")
        } else {
            val constructor = configClass.primaryConstructor
                ?: throw IllegalArgumentException("No primary constructor found")
            val currentConfig = configStateFlow.value
            val args = constructor.parameters.associateWith { param ->
                val value = changedValues[param.name]
                    ?: currentConfig::class.memberProperties.find { it.name == param.name }?.getter?.call(
                        currentConfig
                    )?.toString()
                value ?: throw IllegalArgumentException("Missing value for ${param.name}")
                when (param.type.classifier) {
                    Long::class -> value.toLong()
                    Int::class -> value.toInt()
                    Float::class -> value.toFloat()
                    Double::class -> value.toDouble()
                    String::class -> value
                    else -> throw IllegalArgumentException("Unsupported type")
                }
            }
            configStorage.set(constructor.callBy(args))
        }
    }

    override fun resetConfig() {
        configStorage.set(initialConfig)
    }

    /* State-related */
    override val sensorStateFlow: StateFlow<SensorState>
        get() = stateStorage.stateFlow

    override fun init() {
        val sensorState = when (stateStorage.get().flag) {
            SensorState.FLAG.ENABLED, SensorState.FLAG.RUNNING -> if (permissionManager.getPermissionFlow(
                    permissions
                ).value.values.any { it != PermissionState.GRANTED }
            ) {
                SensorState(SensorState.FLAG.DISABLED)
            } else {
                stateStorage.get()
            }

            else -> SensorState(SensorState.FLAG.DISABLED)
        }

        stateStorage.set(sensorState)
        permissionManager.registerPermission(permissions)
    }

    override fun enable() {
        if (sensorStateFlow.value.flag == SensorState.FLAG.DISABLED) {
            if (
                permissionManager.getPermissionFlow(permissions).value.any { (permission, state) ->
                    permissions.contains(permission) && state != PermissionState.GRANTED
                }
            ) {
                stateStorage.set(SensorState(SensorState.FLAG.DISABLED, "Permission required"))
            } else {
                stateStorage.set(SensorState(SensorState.FLAG.ENABLED))
            }
        }
    }

    override fun disable() {
        if (sensorStateFlow.value.flag == SensorState.FLAG.ENABLED) {
            stateStorage.set(SensorState(SensorState.FLAG.DISABLED))
        }
    }

    override fun start() {
        if (sensorStateFlow.value.flag == SensorState.FLAG.ENABLED) {
            stateStorage.set(SensorState(SensorState.FLAG.RUNNING))
        }
        onStart()
    }

    abstract fun onStart()

    override fun stop() {
        onStop()
        if (sensorStateFlow.value.flag == SensorState.FLAG.RUNNING) {
            stateStorage.set(SensorState(SensorState.FLAG.ENABLED))
        }
    }

    abstract fun onStop()

    /* Data-related */
    protected val listeners = mutableListOf<(E) -> Unit>()
    override fun addListener(listener: (E) -> Unit) {
        listeners.add(listener)
    }

    override fun removeListener(listener: (E) -> Unit) {
        listeners.remove(listener)
    }

    private fun extractName(className: String): String {
        // Replace "Sensor" with an empty string
        val tmp = className.replace("Sensor", "")
        // Split the name into parts based on camel case
        val parts =
            tmp.split("(?=\\p{Upper})|_|(?<=\\p{Lower})(?=\\p{Upper})".toRegex())

        // Join the parts using whitespace and convert
        return parts.joinToString(" ").trim()
    }

    private fun extractId(className: String): String {
        // Replace "Sensor" with an empty string
        val tmp = className.replace("Sensor", "")
        return tmp
    }
}