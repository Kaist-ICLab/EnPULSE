package kaist.iclab.tracker.sensor.phone

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import com.samsung.android.sdk.health.data.request.Ordering
import kaist.iclab.tracker.listener.AlarmListener
import kaist.iclab.tracker.listener.SamsungHealthDataInitializer
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.math.max

class ExerciseSensor(
    context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    val stateStorage: StateStorage<SensorState>,
    samsungHealthDataInitializer: SamsungHealthDataInitializer
) : BaseSensor<ExerciseSensor.Config, ExerciseSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class
) {
    override val permissions = arrayOf(DataTypes.EXERCISE.name)

    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else null
    ).toTypedArray()

    data class Config(
        // The time period to sync past exercise data at the initialization of the class, in seconds
        val syncPastLimitSeconds: Long,
        // The time period to load exercise data at each invocation, as exercise data sync is often not in real-time.
        val timeMarginSeconds: Long,
        val readIntervalMillis: Long,
    ) : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val duration: Long,
        val exerciseType: String,
        val customTitle: String?,
        val calories: Float,
        val distance: Float?,
        val count: Int?,
        val meanHeartRate: Float?,
        val maxHeartRate: Float?,
        val minHeartRate: Float?,
        val altitudeGain: Float?,
        val altitudeLoss: Float?,
        val meanCadence: Float?,
        val maxCadence: Float?,
        val meanPower: Float?,
        val maxPower: Float?,
        val meanSpeed: Float?,
        val maxSpeed: Float?,
        val meanRpm: Float?,
        val maxRpm: Float?
    ) : SensorEntity()

    private val actionName = "kaist.iclab.tracker.${name}_REQUEST"
    private val store = samsungHealthDataInitializer.store
    private val actionCode = 0x11
    private val alarmListener = AlarmListener(
        context = context,
        actionName = actionName,
        actionCode = actionCode,
        initialConfig.readIntervalMillis
    )

    private var lastSynced =
        System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(configStateFlow.value.syncPastLimitSeconds)

    private val mainCallback = { _: Intent? ->
        val config = configStateFlow.value
        val fromTime =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(lastSynced), ZoneId.systemDefault())
                .minusSeconds(config.timeMarginSeconds)

        val req = DataTypes.EXERCISE
            .readDataRequestBuilder
            .setLocalTimeFilter(
                LocalTimeFilter.since(fromTime)
            )
            .setOrdering(Ordering.ASC)
            .build()
        val timestamp = System.currentTimeMillis()

        CoroutineScope(Dispatchers.IO).launch {
            store.readData(req).dataList.forEach { dataPoint ->
                dataPoint.getValueOrDefault(DataType.ExerciseType.SESSIONS, emptyList()).forEach { session ->
                    val entity = Entity(
                        timestamp,
                        session.startTime.toEpochMilli(),
                        session.duration.toMillis(),
                        session.exerciseType.name,
                        session.customTitle,
                        session.calories,
                        session.distance,
                        session.count,
                        session.meanHeartRate,
                        session.maxHeartRate,
                        session.minHeartRate,
                        session.altitudeGain,
                        session.altitudeLoss,
                        session.meanCadence,
                        session.maxCadence,
                        session.meanPower,
                        session.maxPower,
                        session.meanSpeed,
                        session.maxSpeed,
                        session.meanRpm,
                        session.maxRpm
                    )
                    lastSynced = max(lastSynced, session.endTime.toEpochMilli())

                    listeners.forEach {
                        it.invoke(entity)
                    }
                }
            }
        }
        Unit
    }

    override fun onStart() {
        alarmListener.addListener(mainCallback)
    }

    override fun onStop() {
        alarmListener.removeListener(mainCallback)
    }
}
