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
    }

    val connectionStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    // SDK Policy Error state - true when dev mode is not enabled on Health Platform
    private val _sdkPolicyErrorStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val sdkPolicyErrorStateFlow: StateFlow<Boolean> = _sdkPolicyErrorStateFlow

    private val connectionListener: ConnectionListener =
        object : ConnectionListener {
            override fun onConnectionSuccess() {
                Log.d(TAG, "Connection Success")
                connectionStateFlow.value = true
            }

            override fun onConnectionEnded() {
                connectionStateFlow.value = false
                Log.d(TAG, "Connection Ended")
            }

            override fun onConnectionFailed(e: HealthTrackerException?) {
                connectionStateFlow.value = false
                Log.e(TAG, "Connection Failed: $e")
            }
        }

    private val healthTrackingService = HealthTrackingService(connectionListener, context)

    init {
        healthTrackingService.connectService()
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

                else -> Log.e(javaClass.simpleName, "ERROR: Unknown ${trackerError.name}")
            }
        }

        override fun onFlushCompleted() {
            Log.d(javaClass.simpleName, "onFlushCompleted")
        }
    }
}
