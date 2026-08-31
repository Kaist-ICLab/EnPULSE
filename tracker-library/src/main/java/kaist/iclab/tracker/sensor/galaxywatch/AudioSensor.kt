package kaist.iclab.tracker.sensor.galaxywatch

import android.Manifest
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import kaist.iclab.tracker.R
import kaist.iclab.tracker.permission.PermissionManager
import kaist.iclab.tracker.sensor.core.BaseSensor
import kaist.iclab.tracker.sensor.core.SensorConfig
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.core.SensorState
import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class AudioSensor(
    context: Context,
    permissionManager: PermissionManager,
    configStorage: StateStorage<Config>,
    stateStorage: StateStorage<SensorState>,
) : BaseSensor<AudioSensor.Config, AudioSensor.Entity>(
    permissionManager,
    configStorage,
    stateStorage,
    Config::class,
    Entity::class,
    titleResId = R.string.sensor_media, // Placeholder if no audio specific
    descriptionResId = R.string.sensor_desc_media,
    icon = Icons.Default.Mic
) {
    companion object {
        private const val AUDIO_SAMPLE_RATE = 16000
        private const val READ_CHUNK_SIZE = 320
        private const val OUTPUT_SAMPLE_RATE = AUDIO_SAMPLE_RATE
    }

    @Serializable
    data class Config(
        val chunkSizeMillis: Long = 20L,
    ) : SensorConfig

    @Serializable
    data class Entity(
        val received: Long,
        val timestamp: Long,
        val sampleRateHz: Int,
        val samples: ShortArray,
    ) : SensorEntity()

    override val id: String = "Audio"
    override val permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)
    override val foregroundServiceTypes: Array<Int> = listOfNotNull(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            null
        }
    ).toTypedArray()

    private var repositoryScope: CoroutineScope? = null
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null


    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onStart() {
        if (recordingJob?.isActive == true) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            AUDIO_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val internalBufferSize = maxOf(minBufferSize * 2, AUDIO_SAMPLE_RATE)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            AUDIO_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            internalBufferSize
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("AudioRecord initialization failed")
        }

        audioRecord = recorder
        recorder.startRecording()

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        repositoryScope = scope
        recordingJob = scope.launch {
            val readBuffer = ShortArray(READ_CHUNK_SIZE)

            while (currentCoroutineContext().isActive) {
                val readCount = audioRecord?.read(readBuffer, 0, READ_CHUNK_SIZE) ?: -1
                if (readCount <= 0) continue

                val timestamp = System.currentTimeMillis()
                val entity = Entity(
                    received = timestamp,
                    timestamp = timestamp,
                    sampleRateHz = OUTPUT_SAMPLE_RATE,
                    samples = readBuffer.copyOfRange(0, readCount)
                )
                listeners.forEach { it.invoke(entity) }
            }
        }
    }

    override fun onStop() {
        recordingJob?.cancel()
        recordingJob = null
        repositoryScope?.cancel()
        repositoryScope = null

        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        audioRecord?.release()
        audioRecord = null
    }
}
