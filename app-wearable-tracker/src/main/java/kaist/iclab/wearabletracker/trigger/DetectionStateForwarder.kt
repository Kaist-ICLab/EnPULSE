package kaist.iclab.wearabletracker.trigger

import android.util.Log
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.tracker.trigger.state.DetectionStateTracker
import kaist.iclab.wearabletracker.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Keeps the watch's local [DetectionStateTracker] in sync with the phone, in both directions —
 * the trigger engine now lives entirely on the phone, so condition evaluation needs to see the
 * watch's live sensor readings (stress/gesture) there.
 *
 * - Outbound: subscribes to [DetectionStateTracker.stateChanges] (fed locally by
 *   `StressDetectionAdapter`/`GestureDetectionAdapter`, unchanged) and BLE-sends each update on
 *   `KEY_DETECTION_STATE_UPDATE` to the phone.
 * - Inbound: still listens on the same key for ADB/dashboard-driven debug simulation
 *   (`TriggerDebugReceiver`'s use case) — a simulated state gets applied to the local tracker,
 *   which immediately re-forwards it outbound too, exercising the full pipeline end-to-end
 *   without real sensor hardware.
 */
class DetectionStateForwarder(
    private val bleChannel: BLEDataChannel,
    private val detectionStateTracker: DetectionStateTracker,
    private val coroutineScope: CoroutineScope,
) {
    companion object {
        private const val TAG = "DetectionStateForwarder"
    }

    private var isStarted = false

    fun start() {
        if (isStarted) return
        isStarted = true

        coroutineScope.launch {
            detectionStateTracker.stateChanges.collect { (sensor, state) ->
                try {
                    val payload = buildJsonObject { put(sensor, state.value) }.toString()
                    bleChannel.send(Constants.BLE.KEY_DETECTION_STATE_UPDATE, payload)
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Failed to forward detection state ($sensor=${state.value}): ${e.message}",
                        e
                    )
                }
            }
        }

        bleChannel.addOnReceivedListener(setOf(Constants.BLE.KEY_DETECTION_STATE_UPDATE)) { _, json ->
            handleSimulatedDetectionState(json)
        }

        Log.d(TAG, "Started forwarding detection states to phone")
    }

    private fun handleSimulatedDetectionState(json: JsonElement) {
        try {
            val payload = when (json) {
                is kotlinx.serialization.json.JsonObject -> json
                is kotlinx.serialization.json.JsonPrimitive -> kotlinx.serialization.json.Json.parseToJsonElement(
                    json.content
                ).jsonObject

                else -> kotlinx.serialization.json.Json.parseToJsonElement(json.toString()).jsonObject
            }

            val now = System.currentTimeMillis()
            payload.forEach { (sensor, valueElement) ->
                val value = valueElement.jsonPrimitive.content
                detectionStateTracker.updateState(sensor, value, now)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying simulated detection state: ${e.message}", e)
        }
    }
}
