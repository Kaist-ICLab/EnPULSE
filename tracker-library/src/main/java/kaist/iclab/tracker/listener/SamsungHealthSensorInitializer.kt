package kaist.iclab.tracker.listener

import android.content.Context
import android.util.Log
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.PpgType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SamsungHealthSensorInitializer(context: Context) {
    companion object {
        private val TAG = SamsungHealthSensorInitializer::class.simpleName
        private const val MIN_RETRY_DELAY = 1000L // 1 second
        private const val MAX_RETRY_DELAY = 30000L // 30 seconds
    }

    val connectionStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    // SDK Policy Error state - true when dev mode is not enabled on Health Platform
    private val _sdkPolicyErrorStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val sdkPolicyErrorStateFlow: StateFlow<Boolean> = _sdkPolicyErrorStateFlow

    private val connectionListener: ConnectionListener =
        object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.i(TAG, "Samsung Health Tracking Service: Connection Success")
                connectionStateFlow.value = true
                retryCount = 0
                currentDelay = MIN_RETRY_DELAY
            }

            override fun onConnectionEnded() {
                Log.w(TAG, "Samsung Health Tracking Service: Connection Ended. Attempting reconnect...")
                connectionStateFlow.value = false
                scheduleReconnect()
            }

            override fun onConnectionFailed(e: HealthTrackerException?) {
                Log.e(TAG, "Samsung Health Tracking Service: Connection Failed ($e). Attempting reconnect...")
                connectionStateFlow.value = false
                scheduleReconnect()
            }
        }

    private val healthTrackingService = HealthTrackingService(connectionListener, context)
    private var retryCount = 0
    private var currentDelay = MIN_RETRY_DELAY

    init {
        connect()
    }

    private fun connect() {
        try {
            Log.d(TAG, "Connecting to Samsung Health Tracking Service (Attempt ${retryCount + 1})...")
            healthTrackingService.connectService()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate connection: ${e.message}")
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        // Simple exponential backoff
        val delay = currentDelay
        currentDelay = minOf(MAX_RETRY_DELAY, currentDelay * 2)
        retryCount++

        Log.d(TAG, "Scheduling reconnection in ${delay / 1000}s...")
        
        // Using a Handler or similar to avoid complex coroutine dependency in this basic initializer
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            connect()
        }, delay)
    }

    fun getTracker(trackerType: HealthTrackerType): HealthTracker {
        return healthTrackingService.getHealthTracker(trackerType)
    }

    fun getTracker(trackerType: HealthTrackerType, ppgsets: Set<PpgType>): HealthTracker {
        return healthTrackingService.getHealthTracker(trackerType, ppgsets)
    }

    fun isTrackerAvailable(trackerType: HealthTrackerType): Boolean {
        val supportedTrackers = healthTrackingService.trackingCapability.supportHealthTrackerTypes
        return trackerType in supportedTrackers
    }

    /**
     * Report SDK Policy Error. Called from DataListener when it receives this error.
     */
    fun reportSdkPolicyError() {
        _sdkPolicyErrorStateFlow.value = true
    }

    /**
     * Clear SDK Policy Error. Called when user dismisses or retries.
     */
    fun clearSdkPolicyError() {
        _sdkPolicyErrorStateFlow.value = false
    }

    /**
     * Creates a DataListener that will report SDK Policy Errors to this initializer.
     */
    fun createDataListener(callback: (MutableList<DataPoint>) -> Unit): HealthTracker.TrackerEventListener {
        return DataListener(this, callback)
    }

    class DataListener(
        private val initializer: SamsungHealthSensorInitializer? = null,
        private val callback: (MutableList<DataPoint>) -> Unit
    ) : HealthTracker.TrackerEventListener {
        override fun onDataReceived(dataPoints: MutableList<DataPoint>) {
            callback(dataPoints)
        }

        override fun onError(trackerError: HealthTracker.TrackerError) {
            Log.d(javaClass.simpleName, "onError")
            when (trackerError) {
                HealthTracker.TrackerError.PERMISSION_ERROR ->
                    Log.e(javaClass.simpleName, "ERROR: Permission Failed")

                HealthTracker.TrackerError.SDK_POLICY_ERROR -> {
                    Log.e(javaClass.simpleName, "ERROR: SDK Policy Error")
                    initializer?.reportSdkPolicyError()
                }
            }
        }

        override fun onFlushCompleted() {
            Log.d(javaClass.simpleName, "onFlushCompleted")
        }
    }
}
