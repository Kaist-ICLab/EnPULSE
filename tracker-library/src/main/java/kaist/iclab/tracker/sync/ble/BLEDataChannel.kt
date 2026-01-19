package kaist.iclab.tracker.sync.ble

import android.content.Context
import kaist.iclab.tracker.sync.core.DataChannel
import kaist.iclab.tracker.sync.core.DataReceiver
import kaist.iclab.tracker.sync.core.DataSender

/**
 * A DataChannel that uses BLE(Bluetooth low energy) channel to transfer data.
 * Suitable for communication between nearby devices, such as a mobile phone and a smartwatch.
 *
 * BLEDataChannel runs on top of DataLayer API, so the namespace and application ID of sending/receiving app **must be the same**.
 *
 * This class now uses the separated sender/receiver pattern.
 */
class BLEDataChannel(
    private val context: Context
) : DataChannel<Unit>() {

    override val sender: DataSender<Unit> = BLESender(context)
    override val receiver: DataReceiver = BLEReceiver().apply {
        initializeLocalNodeId(context)
    }

    /**
     * Send data with urgency flag
     */
    suspend fun send(key: String, value: String, isUrgent: Boolean) {
        (sender as BLESender).send(key, value, isUrgent)
    }
}
