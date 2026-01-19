package kaist.iclab.wearabletracker.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kaist.iclab.wearabletracker.db.dao.AccelerometerDao
import kaist.iclab.wearabletracker.db.dao.EDADao
import kaist.iclab.wearabletracker.db.dao.HeartRateDao
import kaist.iclab.wearabletracker.db.dao.LocationDao
import kaist.iclab.wearabletracker.db.dao.PPGDao
import kaist.iclab.wearabletracker.db.dao.SkinTemperatureDao
import kaist.iclab.wearabletracker.db.entity.AccelerometerEntity
import kaist.iclab.wearabletracker.db.entity.EDAEntity
import kaist.iclab.wearabletracker.db.entity.HeartRateEntity
import kaist.iclab.wearabletracker.db.entity.LocationEntity
import kaist.iclab.wearabletracker.db.entity.PPGEntity
import kaist.iclab.wearabletracker.db.entity.SkinTemperatureEntity

@Database(
    version = 1,
    entities = [
        AccelerometerEntity::class,
        PPGEntity::class,
        HeartRateEntity::class,
        SkinTemperatureEntity::class,
        EDAEntity::class,
        LocationEntity::class,
    ],
    exportSchema = false
)
@TypeConverters(Converter::class)
abstract class TrackerRoomDB : RoomDatabase() {
    abstract fun accelerometerDao(): AccelerometerDao
    abstract fun ppgDao(): PPGDao
    abstract fun heatRateDao(): HeartRateDao
    abstract fun skinTemperatureDao(): SkinTemperatureDao
    abstract fun edaDao(): EDADao
    abstract fun locationDao(): LocationDao
}