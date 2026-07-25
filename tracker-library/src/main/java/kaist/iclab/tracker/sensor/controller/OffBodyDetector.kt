package kaist.iclab.tracker.sensor.controller

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * Detects whether the watch is currently being worn on the user's wrist using the
 * [Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT] sensor.
 *
 * When the sensor reports `1.0`, the watch is on-wrist; `0.0` means off-wrist.
 * If the sensor is unavailable on the device, [isOnWrist] defaults to `true` so that
 * data collection is never incorrectly paused on unsupported hardware.
 */
class OffBodyDetector(context: Context) {

    companion object {
        private val TAG = OffBodyDetector::class.simpleName
    }

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val offBodySensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)

    private val _isOnWrist = MutableStateFlow(true) // assume worn until told otherwise
    /** `true` when the watch is on the user's wrist, `false` when off-wrist. */
    val isOnWrist: StateFlow<Boolean> = _isOnWrist.asStateFlow()

    /** Whether the device actually has the off-body sensor. */
    val isSupported: Boolean get() = offBodySensor != null

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val worn = event.values[0].roundToInt() == 1
            if (_isOnWrist.value != worn) {
                Log.i(TAG, "Wrist state changed: ${if (worn) "ON-WRIST" else "OFF-WRIST"}")
            }
            _isOnWrist.value = worn
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Not used for off-body detection
        }
    }

    /**
     * Start listening for off-body events.
     * If the sensor is not available, [isOnWrist] stays `true` (worn) permanently.
     */
    fun start() {
        if (offBodySensor == null) {
            Log.w(TAG, "TYPE_LOW_LATENCY_OFFBODY_DETECT sensor not available; " +
                    "off-body pausing is disabled.")
            return
        }
        sensorManager.registerListener(
            listener,
            offBodySensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        Log.i(TAG, "Off-body detection started")
    }

    /**
     * Stop listening for off-body events.
     */
    fun stop() {
        sensorManager.unregisterListener(listener)
        Log.i(TAG, "Off-body detection stopped")
    }
}
