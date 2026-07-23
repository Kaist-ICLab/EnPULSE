package kaist.iclab.tracker.sensor.galaxywatch

import android.Manifest
import android.content.Context
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.SystemClock
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicReference

class IMUSensor(
    context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
) : BaseSensor<IMUSensor.Config, IMUSensor.Entity>(
    permissionManager,
    configStorage,
    stateStorage,
    Config::class,
    Entity::class,
    titleResId = R.string.sensor_imu,
    descriptionResId = R.string.sensor_desc_imu,
    icon = Icons.Default.CompassCalibration
), SensorEventListener {
    companion object {
        private const val SAMPLE_RATE = 50
        private const val TICK_PERIOD_MS = 1000L / SAMPLE_RATE
    }

    @Serializable
    class Config : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val accX: Float,
        val accY: Float,
        val accZ: Float,
        val gyroX: Float,
        val gyroY: Float,
        val gyroZ: Float,
    ) : SensorEntity()

    override val id: String = "IMU"
    override val permissions: Array<String> = arrayOf(Manifest.permission.HIGH_SAMPLING_RATE_SENSORS)
    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else null
    ).toTypedArray()

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val latestAcc = AtomicReference<FloatArray?>(null)
    private val latestGyro = AtomicReference<FloatArray?>(null)

    @Volatile
    private var tickerHandler: Handler? = null
    private var tickerThread: HandlerThread? = null

    private var nextTickMs = 0L

    private val tickRunnable = object : Runnable {
        override fun run() {
            onTick()
            nextTickMs += TICK_PERIOD_MS
            tickerHandler?.postAtTime(this, nextTickMs)
        }
    }

    override fun init() {
        super.init()
        val hasAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        val hasGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        if (!hasAccel || !hasGyro) {
            stateStorage.set(SensorState(SensorState.FLAG.UNAVAILABLE, "Accelerometer or gyroscope unavailable"))
        }
    }

    override fun onStart() {
        if (tickerThread != null) return
        latestAcc.set(null)
        latestGyro.set(null)

        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (accel == null || gyro == null) {
            throw IllegalStateException("WatchHarImuSensor requires accelerometer and gyroscope")
        }

        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)

        val tickThread = HandlerThread("WatchHarImu-Ticker", Process.THREAD_PRIORITY_AUDIO)
        tickThread.start()
        tickerThread = tickThread
        tickerHandler = Handler(tickThread.looper)

        nextTickMs = SystemClock.uptimeMillis() + TICK_PERIOD_MS
        tickerHandler?.postAtTime(tickRunnable, nextTickMs)
    }

    override fun onStop() {
        tickerHandler?.removeCallbacks(tickRunnable)
        tickerHandler = null
        tickerThread?.quitSafely()
        tickerThread = null

        sensorManager.unregisterListener(this)
        latestAcc.set(null)
        latestGyro.set(null)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> latestAcc.set(event.values.clone())
            Sensor.TYPE_GYROSCOPE -> latestGyro.set(event.values.clone())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun onTick() {
        val acc = latestAcc.get() ?: return
        val gyro = latestGyro.get() ?: return
        val timestamp = System.currentTimeMillis()
        val entity = Entity(
            received = timestamp,
            timestamp = timestamp,
            accX = acc[0],
            accY = acc[1],
            accZ = acc[2],
            gyroX = gyro[0],
            gyroY = gyro[1],
            gyroZ = gyro[2],
        )
        listeners.forEach { it.invoke(entity) }
    }
}
