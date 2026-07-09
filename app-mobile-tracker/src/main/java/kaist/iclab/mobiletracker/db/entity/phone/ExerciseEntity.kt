package kaist.iclab.mobiletracker.db.entity.phone

import io.objectbox.annotation.Entity
import kaist.iclab.mobiletracker.data.DeviceType
import kaist.iclab.mobiletracker.db.entity.BaseEntity
import kaist.iclab.mobiletracker.db.entity.CsvSerializable
import kaist.iclab.mobiletracker.db.entity.RecordSerializable
import kaist.iclab.mobiletracker.repository.SensorRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity
@Serializable
class ExerciseEntity : BaseEntity, CsvSerializable, RecordSerializable {
    @SerialName("start_time")
    var startTime: Long = 0

    @SerialName("end_time")
    var endTime: Long = 0

    @SerialName("duration_seconds")
    var durationSeconds: Long = 0

    @SerialName("exercise_type")
    var exerciseType: String = ""

    @SerialName("custom_title")
    var customTitle: String? = null

    var calories: Float = 0f
    var distance: Float? = null
    var count: Int? = null

    @SerialName("mean_heart_rate")
    var meanHeartRate: Float? = null

    @SerialName("max_heart_rate")
    var maxHeartRate: Float? = null

    @SerialName("min_heart_rate")
    var minHeartRate: Float? = null

    @SerialName("altitude_gain")
    var altitudeGain: Float? = null

    @SerialName("altitude_loss")
    var altitudeLoss: Float? = null

    @SerialName("mean_cadence")
    var meanCadence: Float? = null

    @SerialName("max_cadence")
    var maxCadence: Float? = null

    @SerialName("mean_power")
    var meanPower: Float? = null

    @SerialName("max_power")
    var maxPower: Float? = null

    @SerialName("mean_speed")
    var meanSpeed: Float? = null

    @SerialName("max_speed")
    var maxSpeed: Float? = null

    @SerialName("mean_rpm")
    var meanRpm: Float? = null

    @SerialName("max_rpm")
    var maxRpm: Float? = null

    constructor() : super()

    constructor(
        id: Long = 0,
        eventId: String = UUID.randomUUID().toString(),
        uuid: String = "",
        received: Long = 0,
        timestamp: Long = 0,
        deviceType: Int = DeviceType.PHONE.value,
        startTime: Long = 0,
        endTime: Long = 0,
        durationSeconds: Long = 0,
        exerciseType: String = "",
        customTitle: String? = null,
        calories: Float = 0f,
        distance: Float? = null,
        count: Int? = null,
        meanHeartRate: Float? = null,
        maxHeartRate: Float? = null,
        minHeartRate: Float? = null,
        altitudeGain: Float? = null,
        altitudeLoss: Float? = null,
        meanCadence: Float? = null,
        maxCadence: Float? = null,
        meanPower: Float? = null,
        maxPower: Float? = null,
        meanSpeed: Float? = null,
        maxSpeed: Float? = null,
        meanRpm: Float? = null,
        maxRpm: Float? = null
    ) {
        initBaseEntity(id, eventId, uuid, received, timestamp, deviceType)
        this.startTime = startTime
        this.endTime = endTime
        this.durationSeconds = durationSeconds
        this.exerciseType = exerciseType
        this.customTitle = customTitle
        this.calories = calories
        this.distance = distance
        this.count = count
        this.meanHeartRate = meanHeartRate
        this.maxHeartRate = maxHeartRate
        this.minHeartRate = minHeartRate
        this.altitudeGain = altitudeGain
        this.altitudeLoss = altitudeLoss
        this.meanCadence = meanCadence
        this.maxCadence = maxCadence
        this.meanPower = meanPower
        this.maxPower = maxPower
        this.meanSpeed = meanSpeed
        this.maxSpeed = maxSpeed
        this.meanRpm = meanRpm
        this.maxRpm = maxRpm
    }

    override fun csvHeader() =
        "eventId,uuid,received,timestamp,startTime,endTime,durationSeconds,exerciseType,customTitle,calories,distance,count,meanHeartRate,maxHeartRate,minHeartRate,altitudeGain,altitudeLoss,meanCadence,maxCadence,meanPower,maxPower,meanSpeed,maxSpeed,meanRpm,maxRpm"

    override fun toCsvRow(): String {
        val escapedCustomTitle = customTitle?.replace("\"", "\"\"") ?: ""
        return "$eventId,$uuid,$received,$timestamp,$startTime,$endTime,$durationSeconds,$exerciseType,\"$escapedCustomTitle\",$calories,$distance,$count,$meanHeartRate,$maxHeartRate,$minHeartRate,$altitudeGain,$altitudeLoss,$meanCadence,$maxCadence,$meanPower,$maxPower,$meanSpeed,$maxSpeed,$meanRpm,$maxRpm"
    }

    override fun toRecord() = SensorRecord(
        id = id,
        timestamp = timestamp,
        fields = mapOf(
            "Type" to exerciseType,
            "Duration" to "${durationSeconds}s",
            "Calories" to calories.toString()
        )
    )
}
