package kaist.iclab.tracker.sensor.galaxywatch

import android.content.Context
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.sensor.sigrep.StressBinaryInferencer
import kaist.iclab.tracker.sensor.sigrep.TimestampedRingBuffer
import kaist.iclab.tracker.sensor.sigrep.magnitude
import kaist.iclab.tracker.sensor.sigrep.resampleAndZscore
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.serialization.Serializable

class StressSensor(
    private val context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    private val stateStorage: StateStorage<SensorState>,
    private val accelerometerSensor: AccelerometerSensor,
    private val ppgSensor: PPGSensor,
    private val heartRateSensor: HeartRateSensor,
) : BaseSensor<StressSensor.Config, StressSensor.Entity>(
    permissionManager, configStorage, stateStorage, Config::class, Entity::class
) {
    companion object {
        private const val WINDOW_MS = 4_000L

        private const val ACC_FS = 25
        private const val PPG_FS = 25
        private const val HR_FS = 1

        private const val ACC_CHANNELS = 1
        private const val PPG_CHANNELS = 3
        private const val HR_CHANNELS = 1

        private const val BUFFER_SEC = 8
    }

    data class Config(
        val interpreterThreads: Int = 2,
        val predictionStrideMs: Long = 1_000L,
    ) : SensorConfig

    override val initialConfig: Config = Config()

    @Serializable
    data class Entity(
        val received: Long,
        val windowStartMs: Long,
        val windowEndMs: Long,
        val probability: Float,
        val isHighStress: Boolean,
    ) : SensorEntity()

    override val id: String = "Stress"
    override val permissions: Array<String> = emptyArray()
    override val foregroundServiceTypes: Array<Int> = emptyArray()

    private val accBuffer = TimestampedRingBuffer(ACC_CHANNELS, ACC_FS * BUFFER_SEC + 32)
    private val ppgBuffer = TimestampedRingBuffer(PPG_CHANNELS, PPG_FS * BUFFER_SEC + 32)
    private val hrBuffer = TimestampedRingBuffer(HR_CHANNELS, HR_FS * BUFFER_SEC + 8)

    private val dataLock = Any()
    private var inferencer: StressBinaryInferencer? = null
    private var lastEmittedWindowEndMs = Long.MIN_VALUE

    private var ownsAcc = false
    private var ownsPpg = false
    private var ownsHr = false

    private val accListener: (AccelerometerSensor.Entity) -> Unit = { handleAcc(it) }
    private val ppgListener: (PPGSensor.Entity) -> Unit = { handlePpg(it) }
    private val hrListener: (HeartRateSensor.Entity) -> Unit = { handleHr(it) }

    override fun init() {
        super.init()
        accelerometerSensor.init()
        ppgSensor.init()
        heartRateSensor.init()
    }

    override fun onStart() {
        inferencer = StressBinaryInferencer(context, configStateFlow.value.interpreterThreads)
        synchronized(dataLock) {
            accBuffer.clear()
            ppgBuffer.clear()
            hrBuffer.clear()
            lastEmittedWindowEndMs = Long.MIN_VALUE
        }

        accelerometerSensor.addListener(accListener)
        ppgSensor.addListener(ppgListener)
        heartRateSensor.addListener(hrListener)

        ownsAcc = ensureRunning(accelerometerSensor)
        ownsPpg = ensureRunning(ppgSensor)
        ownsHr = ensureRunning(heartRateSensor)
    }

    override fun onStop() {
        accelerometerSensor.removeListener(accListener)
        ppgSensor.removeListener(ppgListener)
        heartRateSensor.removeListener(hrListener)

        if (ownsAcc && accelerometerSensor.sensorStateFlow.value.flag == SensorState.FLAG.RUNNING) {
            accelerometerSensor.stop()
        }
        if (ownsPpg && ppgSensor.sensorStateFlow.value.flag == SensorState.FLAG.RUNNING) {
            ppgSensor.stop()
        }
        if (ownsHr && heartRateSensor.sensorStateFlow.value.flag == SensorState.FLAG.RUNNING) {
            heartRateSensor.stop()
        }
        ownsAcc = false
        ownsPpg = false
        ownsHr = false

        synchronized(dataLock) {
            accBuffer.clear()
            ppgBuffer.clear()
            hrBuffer.clear()
            lastEmittedWindowEndMs = Long.MIN_VALUE
        }
        inferencer?.close()
        inferencer = null
    }

    private fun ensureRunning(sensor: BaseSensor<*, *>): Boolean {
        if (sensor.sensorStateFlow.value.flag == SensorState.FLAG.DISABLED) {
            sensor.enable()
        }
        return if (sensor.sensorStateFlow.value.flag != SensorState.FLAG.RUNNING) {
            sensor.start()
            true
        } else {
            false
        }
    }

    private fun handleAcc(entity: AccelerometerSensor.Entity) {
        synchronized(dataLock) {
            val ch = FloatArray(ACC_CHANNELS)
            for (p in entity.dataPoint) {
                ch[0] = magnitude(p.x, p.y, p.z)
                accBuffer.append(p.timestamp, ch)
            }
        }
        maybePredict()
    }

    private fun handlePpg(entity: PPGSensor.Entity) {
        synchronized(dataLock) {
            val ch = FloatArray(PPG_CHANNELS)
            for (p in entity.dataPoint) {
                ch[0] = p.green.toFloat()
                ch[1] = p.red.toFloat()
                ch[2] = p.ir.toFloat()
                ppgBuffer.append(p.timestamp, ch)
            }
        }
        maybePredict()
    }

    private fun handleHr(entity: HeartRateSensor.Entity) {
        synchronized(dataLock) {
            val ch = FloatArray(HR_CHANNELS)
            for (p in entity.dataPoint) {
                ch[0] = p.hr.toFloat()
                hrBuffer.append(p.timestamp, ch)
            }
        }
        maybePredict()
    }

    private class Snapshot(
        val windowStartMs: Long,
        val windowEndMs: Long,
        val acc: FloatArray,
        val ppg: FloatArray,
        val hr: FloatArray,
    )

    private fun buildSnapshot(): Snapshot? = synchronized(dataLock) {
        val accNew = accBuffer.newestTimestampMsOrNull() ?: return@synchronized null
        val ppgNew = ppgBuffer.newestTimestampMsOrNull() ?: return@synchronized null
        val hrNew = hrBuffer.newestTimestampMsOrNull() ?: return@synchronized null

        val windowEndMs = minOf(accNew, ppgNew, hrNew)
        val windowStartMs = windowEndMs - WINDOW_MS

        val strideMs = configStateFlow.value.predictionStrideMs
        if (lastEmittedWindowEndMs != Long.MIN_VALUE &&
            windowEndMs - lastEmittedWindowEndMs < strideMs
        ) return@synchronized null

        if ((accBuffer.oldestTimestampMsOrNull() ?: Long.MAX_VALUE) > windowStartMs) {
            return@synchronized null
        }
        if ((ppgBuffer.oldestTimestampMsOrNull() ?: Long.MAX_VALUE) > windowStartMs) {
            return@synchronized null
        }
        if ((hrBuffer.oldestTimestampMsOrNull() ?: Long.MAX_VALUE) > windowStartMs) {
            return@synchronized null
        }

        val accSnap = accBuffer.snapshot() ?: return@synchronized null
        val ppgSnap = ppgBuffer.snapshot() ?: return@synchronized null
        val hrSnap = hrBuffer.snapshot() ?: return@synchronized null

        val accTensor = resampleAndZscore(
            accSnap.first, accSnap.second, ACC_CHANNELS, windowStartMs, WINDOW_MS, ACC_FS
        ) ?: return@synchronized null
        val ppgTensor = resampleAndZscore(
            ppgSnap.first, ppgSnap.second, PPG_CHANNELS, windowStartMs, WINDOW_MS, PPG_FS
        ) ?: return@synchronized null
        val hrTensor = resampleAndZscore(
            hrSnap.first, hrSnap.second, HR_CHANNELS, windowStartMs, WINDOW_MS, HR_FS
        ) ?: return@synchronized null

        lastEmittedWindowEndMs = windowEndMs
        Snapshot(windowStartMs, windowEndMs, accTensor, ppgTensor, hrTensor)
    }

    private fun maybePredict() {
        val snapshot = buildSnapshot() ?: return
        val inf = inferencer ?: return
        val prediction = runCatching {
            inf.predict(snapshot.acc, snapshot.ppg, snapshot.hr)
        }.getOrNull() ?: return
        emit(snapshot.windowStartMs, snapshot.windowEndMs, prediction)
    }

    private fun emit(
        windowStartMs: Long,
        windowEndMs: Long,
        prediction: StressBinaryInferencer.Prediction,
    ) {
        val entity = Entity(
            received = System.currentTimeMillis(),
            windowStartMs = windowStartMs,
            windowEndMs = windowEndMs,
            probability = prediction.probability,
            isHighStress = prediction.isHighStress,
        )
        listeners.forEach { it.invoke(entity) }
    }
}
