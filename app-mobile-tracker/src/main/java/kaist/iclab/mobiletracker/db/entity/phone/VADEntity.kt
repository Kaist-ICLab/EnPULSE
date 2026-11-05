package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale
import java.util.UUID

@Entity
@Serializable
class VADEntity : BaseEntity, CsvSerializable, RecordSerializable {
    @SerialName("is_speech")
    var isSpeech: Boolean = false

    @SerialName("speech_probability")
    var speechProbability: Float = 0f

    @SerialName("inference_time_ms")
    var inferenceTimeMs: Float = 0f

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        isSpeech: Boolean = false,
        speechProbability: Float = 0f,
        inferenceTimeMs: Float = 0f
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.isSpeech = isSpeech
        this.speechProbability = speechProbability
        this.inferenceTimeMs = inferenceTimeMs
    }

    override fun csvHeader() =
        "eventId,uuid,received,timestamp,isSpeech,speechProbability,inferenceTimeMs"

    override fun toCsvRow() =
        "$eventId,$uuid,$received,$timestamp,$isSpeech,$speechProbability,$inferenceTimeMs"

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf(
            "Is Speech" to isSpeech.toString(),
            "Probability" to String.format(Locale.getDefault(), "%.3f", speechProbability),
            "Inf Time" to String.format(Locale.getDefault(), "%.2f ms", inferenceTimeMs)
        )
    )
}
