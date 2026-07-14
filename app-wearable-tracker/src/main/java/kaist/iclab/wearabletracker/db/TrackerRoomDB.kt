package kaist.iclab.wearabletracker.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kaist.iclab.wearabletracker.db.dao.AccelerometerDao
import kaist.iclab.wearabletracker.db.dao.EDADao
import kaist.iclab.wearabletracker.db.dao.GestureDao
import kaist.iclab.wearabletracker.db.dao.HeartRateDao
import kaist.iclab.wearabletracker.db.dao.IMUDao
import kaist.iclab.wearabletracker.db.dao.LocationDao
import kaist.iclab.wearabletracker.db.dao.MicroEmaResponseDao
import kaist.iclab.wearabletracker.db.dao.PPGDao
import kaist.iclab.wearabletracker.db.dao.RmssdHistoryDao
import kaist.iclab.wearabletracker.db.dao.SkinTemperatureDao
import kaist.iclab.wearabletracker.db.dao.StressDao
import kaist.iclab.wearabletracker.db.entity.AccelerometerEntity
import kaist.iclab.wearabletracker.db.entity.EDAEntity
import kaist.iclab.wearabletracker.db.entity.GestureEntity
import kaist.iclab.wearabletracker.db.entity.HeartRateEntity
import kaist.iclab.wearabletracker.db.entity.IMUEntity
import kaist.iclab.wearabletracker.db.entity.LocationEntity
import kaist.iclab.wearabletracker.db.entity.MicroEmaResponseEntity
import kaist.iclab.wearabletracker.db.entity.PPGEntity
import kaist.iclab.wearabletracker.db.entity.RmssdHistoryEntity
import kaist.iclab.wearabletracker.db.entity.SkinTemperatureEntity
import kaist.iclab.wearabletracker.db.entity.StressEntity

@Database(
    version = 4,
    entities = [
        AccelerometerEntity::class,
        PPGEntity::class,
        HeartRateEntity::class,
        SkinTemperatureEntity::class,
        EDAEntity::class,
        LocationEntity::class,
        MicroEmaResponseEntity::class,
        IMUEntity::class,
        GestureEntity::class,
        StressEntity::class,
        RmssdHistoryEntity::class,
    ],
    exportSchema = true
)
@TypeConverters(Converter::class)
abstract class TrackerRoomDB : RoomDatabase() {
    abstract fun accelerometerDao(): AccelerometerDao
    abstract fun ppgDao(): PPGDao
    abstract fun heartRateDao(): HeartRateDao
    abstract fun skinTemperatureDao(): SkinTemperatureDao
    abstract fun edaDao(): EDADao
    abstract fun locationDao(): LocationDao
    abstract fun microEmaResponseDao(): MicroEmaResponseDao
    abstract fun imuDao(): IMUDao
    abstract fun gestureDao(): GestureDao
    abstract fun stressDao(): StressDao
    abstract fun rmssdHistoryDao(): RmssdHistoryDao
}