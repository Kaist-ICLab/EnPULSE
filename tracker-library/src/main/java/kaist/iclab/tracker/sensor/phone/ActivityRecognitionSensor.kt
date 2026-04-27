package kaist.iclab.tracker.sensor.phone

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import kaist.iclab.tracker.listener.BroadcastListener
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable

class ActivityRecognitionSensor(
    context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
) : BaseSensor<ActivityRecognitionSensor.Config, ActivityRecognitionSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class
) {
    companion object {
        private val TAG = ActivityRecognitionSensor::class.simpleName
    }

    data class Config(
        val intervalMillis: Long
    ) : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val elapsedRealtimeMillis: Long,
        val activityType: Int,
        val score: Int,
        val probabilities: List<Int>,
    ) : SensorEntity()

    override val permissions = listOfNotNull(
        Manifest.permission.ACTIVITY_RECOGNITION
    ).toTypedArray()

    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            null
        }
    ).toTypedArray()

    private val actionName = "kaist.iclab.tracker.${id}_REQUEST"
    private val actionCode = 0x12

    private val activityRecognitionClient: ActivityRecognitionClient by lazy {
        ActivityRecognition.getClient(context)
    }

    private val activityRecognitionIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            actionCode,
            Intent(actionName).setPackage(context.packageName),
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE

                else -> PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

    private val broadcastListener = BroadcastListener(
        context,
        arrayOf(actionName)
    )

    private val mainCallback: (Intent?) -> Unit = callback@{ intent ->
        if (intent == null) return@callback
        Log.d(TAG, "activity recognition extras: ${intent.extras?.keySet()?.joinToString()}")
        val result = ActivityRecognitionResult.extractResult(intent)
        Log.d(TAG, "activity recognition callback: $intent, $result")
        if (result == null) return@callback

        val timestamp = System.currentTimeMillis()
        val mostProbableActivity = result.mostProbableActivity

        listeners.forEach { listener ->
            listener.invoke(
                Entity(
                    received = timestamp,
                    timestamp = result.time,
                    elapsedRealtimeMillis = result.elapsedRealtimeMillis,
                    activityType = mostProbableActivity.type,
                    score = mostProbableActivity.confidence,
                    probabilities = listOf(
                        getConfidence(result, DetectedActivity.IN_VEHICLE),
                        getConfidence(result, DetectedActivity.ON_BICYCLE),
                        getConfidence(result, DetectedActivity.ON_FOOT),
                        getConfidence(result, DetectedActivity.STILL),
                        getConfidence(result, DetectedActivity.UNKNOWN),
                        getConfidence(result, DetectedActivity.TILTING),
                        getConfidence(result, DetectedActivity.WALKING),
                        getConfidence(result, DetectedActivity.RUNNING),
                    )
                )
            )
        }
    }

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    override fun onStart() {
        broadcastListener.addListener(mainCallback)
        activityRecognitionClient.requestActivityUpdates(
            configStateFlow.value.intervalMillis,
            activityRecognitionIntent
        ).addOnFailureListener { exception ->
            Log.e(TAG, "Failed to request activity updates", exception)
        }
    }

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    override fun onStop() {
        activityRecognitionClient.removeActivityUpdates(activityRecognitionIntent)
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to remove activity updates", exception)
            }
        broadcastListener.removeListener(mainCallback)
    }

    private fun getConfidence(result: ActivityRecognitionResult, activityType: Int): Int {
        return result.probableActivities
            .find { it.type == activityType }
            ?.confidence ?: 0
    }
}
