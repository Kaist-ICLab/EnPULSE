package kaist.iclab.wearabletracker.data

import android.util.Log
import kaist.iclab.tracker.storage.core.StateStorage
import kaist.iclab.tracker.sync.ble.BLEDataChannel
import kaist.iclab.wearabletracker.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Persisted campaign sensor config. `activeSensorIds == null` means "never synced with the
 * phone yet" (distinct from an empty campaign) so gating can fail open on a fresh install.
 */
@Serializable
data class CampaignSensorConfig(val activeSensorIds: Set<String>? = null)

/**
 * Receives the campaign's active-sensor list pushed from the phone over BLE and persists it,
 * so the watch only collects/shows sensors that are actually part of the wearer's campaign.
 */
class CampaignSensorConfigRepository(
    private val bleChannel: BLEDataChannel,
    private val storage: StateStorage<CampaignSensorConfig>,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = javaClass.simpleName

    val configFlow: StateFlow<CampaignSensorConfig> = storage.stateFlow

    fun startListening() {
        bleChannel.addOnReceivedListener(setOf(Constants.BLE.KEY_ACTIVE_SENSOR_CONFIG)) { _, jsonElement ->
            coroutineScope.launch {
                try {
                    val ids = (jsonElement as JsonArray).map { it.jsonPrimitive.content }.toSet()
                    storage.set(CampaignSensorConfig(ids))
                    Log.d(TAG, "Received active sensor config: $ids")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse active sensor config: ${e.message}")
                }
            }
        }
    }
}
