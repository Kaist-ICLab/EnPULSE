package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.db.entity.phone.ExerciseEntity
import kaist.iclab.mobiletracker.db.entity.phone.SleepEntity
import kaist.iclab.mobiletracker.db.entity.phone.StepEntity
import kaist.iclab.mobiletracker.db.obx.SupabaseJson
import kaist.iclab.tracker.sensor.core.SensorEntity
import kaist.iclab.tracker.sensor.phone.ExerciseSensor
import kaist.iclab.tracker.sensor.phone.SleepSensor
import kaist.iclab.tracker.sensor.phone.StepSensor

/**
 * Converts Samsung Health sensor entities into the entities stored on the phone. Used by
 * [buildAllSensorDescriptors]. The open flavor has no such sensors, so its copy holds nulls.
 */
object SamsungHealthEntityMappers {
    val exercise: (SensorEntity, String?) -> ExerciseEntity = { e, uuid ->
        e as ExerciseSensor.Entity
        ExerciseEntity(
            uuid = uuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            duration = e.duration,
            exerciseType = e.exerciseType,
            customTitle = e.customTitle,
            calories = e.calories,
            distance = e.distance,
            count = e.count,
            meanHeartRate = e.meanHeartRate,
            maxHeartRate = e.maxHeartRate,
            minHeartRate = e.minHeartRate,
            altitudeGain = e.altitudeGain,
            altitudeLoss = e.altitudeLoss,
            meanCadence = e.meanCadence,
            maxCadence = e.maxCadence,
            meanPower = e.meanPower,
            maxPower = e.maxPower,
            meanSpeed = e.meanSpeed,
            maxSpeed = e.maxSpeed,
            meanRpm = e.meanRpm,
            maxRpm = e.maxRpm
        )
    }

    val sleep: (SensorEntity, String?) -> SleepEntity = { e, uuid ->
        e as SleepSensor.Entity
        SleepEntity(
            uuid = uuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            duration = e.duration,
            sleepScore = e.sleepScore,
            stagesJson = SupabaseJson.encodeToString(e.stages)
        )
    }

    val step: (SensorEntity, String?) -> StepEntity = { e, uuid ->
        e as StepSensor.Entity
        StepEntity(
            uuid = uuid ?: "",
            received = e.received,
            timestamp = e.timestamp,
            duration = e.duration,
            steps = e.steps
        )
    }
}
