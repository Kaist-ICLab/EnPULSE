package kaist.iclab.tracker.sensor.galaxywatch

import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.microema.EmaConstants
import kaist.iclab.tracker.sensor.microema.MicroEmaResponse
import kaist.iclab.tracker.sensor.microema.WatchSurveyConfig
import kaist.iclab.tracker.sensor.phone.SurveySensor
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Phone-side sensor that listens for MicroEMA responses from the watch via BLE.
 *
 * This sensor bridges watch survey responses into the standard [SurveySensor.Entity]
 * format so they can be synced to Supabase alongside phone survey responses.
 *
 * Note: The trigger logic has been moved to the watch-side [DynamicTriggerEngine].
 * This sensor now only handles response reception.
 */
class MicroEmaSensor(
    permissionManager: PermissionManager,
    private val configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
    private val bleChannel: BLEDataChannel
): BaseSensor<MicroEmaSensor.Config, SurveySensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, SurveySensor.Entity::class
) {

    @Serializable
    data class Config(
        val watchSurveyConfigs: Map<Int, WatchSurveyConfig> = emptyMap()
    ) : SensorConfig {
        companion object {
            fun fromJson(jsonString: String): Config {
                return Json.Default.decodeFromString<Config>(jsonString)
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
                jsonElement is JsonPrimitive -> jsonElement.content
                else -> jsonElement.toString()
            }
            val response = json.decodeFromString<MicroEmaResponse>(jsonString)
            // Convert MicroEmaResponse to a generic JsonElement for the Survey entity
            val responseJson = json.encodeToJsonElement(response)

            // Map the MicroEMA data into the regular SurveySensor.Entity structure
            val surveyEntity = SurveySensor.Entity(
                triggerTime = response.triggerTime,
                actualTriggerTime = response.triggerTime,
                surveyStartTime = response.surveyStartTime,
                responseSubmissionTime = response.responseTime ?: System.currentTimeMillis(),
                response = responseJson,
                deviceType = 1 // Watch
            )

            // Emit the response as a standard survey sensor entity
            listeners.forEach { callback ->
                callback(surveyEntity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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