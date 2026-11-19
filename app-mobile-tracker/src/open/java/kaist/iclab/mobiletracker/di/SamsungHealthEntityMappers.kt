package kaist.iclab.mobiletracker.di

import kaist.iclab.mobiletracker.db.entity.phone.ExerciseEntity
import kaist.iclab.mobiletracker.db.entity.phone.SleepEntity
import kaist.iclab.mobiletracker.db.entity.phone.StepEntity
import kaist.iclab.tracker.sensor.core.SensorEntity

/**
 * The open flavor has no Samsung Health sensors, so nothing is converted. Their stores and upload
 * handlers stay registered so that data from a Samsung build stays readable.
 */
object SamsungHealthEntityMappers {
    val exercise: ((SensorEntity, String?) -> ExerciseEntity)? = null
    val sleep: ((SensorEntity, String?) -> SleepEntity)? = null
    val step: ((SensorEntity, String?) -> StepEntity)? = null
}
