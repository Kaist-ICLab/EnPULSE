package kaist.iclab.tracker.sensor.microema

import android.content.Context
import kaist.iclab.tracker.ema.EmaConstants
import kaist.iclab.tracker.ema.MicroEmaResponse
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.tracker.trigger.TriggerEngine
import kaist.iclab.tracker.trigger.TriggerRule
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kaist.iclab.tracker.sensor.survey.SurveySensor

class MicroEmaSensor(
    private val context: Context,
    permissionManager: PermissionManager,
    private val configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
    private val triggerEngine: TriggerEngine,
    private val bleChannel: BLEDataChannel
): BaseSensor<MicroEmaSensor.Config, SurveySensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, SurveySensor.Entity::class
) {

    @Serializable
    data class Config(
        val rules: List<TriggerRule> = emptyList()
    ) : SensorConfig {
        companion object {
            fun fromJson(jsonString: String): Config {
                return Json.decodeFromString<Config>(jsonString)
            }
        }
    }



    override val permissions: Array<String> = emptyArray() // BLE permissions are handled centrally
    override val foregroundServiceTypes: Array<Int> = emptyArray()

    private val json = Json { ignoreUnknownKeys = true }

    // Listener for incoming responses from the watch
    private val responseListener: (String, JsonElement) -> Unit = { _, jsonElement ->
        try {
            val jsonString = when {
                jsonElement is kotlinx.serialization.json.JsonPrimitive -> jsonElement.content
                else -> jsonElement.toString()
            }
            val response = json.decodeFromString<MicroEmaResponse>(jsonString)
            // Convert MicroEmaResponse to a generic JsonElement for the Survey entity
            val responseJson = json.encodeToJsonElement(response)
            
            // Map the MicroEMA data into the regular SurveySensor.Entity structure
            val surveyEntity = SurveySensor.Entity(
                triggerTime = response.triggerTime,
                actualTriggerTime = response.triggerTime, // Or extract from response if available
                surveyStartTime = response.surveyStartTime,
                responseSubmissionTime = response.responseTime ?: System.currentTimeMillis(),
                response = responseJson
            )
            
            // Emit the response as a standard survey sensor entity
            listeners.forEach { callback ->
                callback(surveyEntity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun init() {
        super.init()
        // Load config and rules
        val currentConfig: Config = configStorage.get()
        currentConfig.rules.forEach { rule: TriggerRule ->
            triggerEngine.addRule(rule)
        }
    }

    override fun onStart() {
        // Start listening for responses via BLE
        bleChannel.addOnReceivedListener(
            setOf(EmaConstants.BLEKeys.MICRO_EMA_RESPONSE),
            responseListener
        )
    }

    override fun onStop() {
        // Stop listening
        bleChannel.removeOnReceivedListener(
            setOf(EmaConstants.BLEKeys.MICRO_EMA_RESPONSE),
            responseListener
        )
    }
}
