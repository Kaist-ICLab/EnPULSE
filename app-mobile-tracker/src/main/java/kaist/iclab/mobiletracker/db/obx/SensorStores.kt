package kaist.iclab.mobiletracker.db.obx

import io.objectbox.BoxStore
import kaist.iclab.mobiletracker.db.entity.common.LocationEntity
import kaist.iclab.mobiletracker.db.entity.phone.AmbientLightEntity
import kaist.iclab.mobiletracker.db.entity.phone.AppListChangeEntity
import kaist.iclab.mobiletracker.db.entity.phone.AppUsageLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.BatteryEntity
import kaist.iclab.mobiletracker.db.entity.phone.BluetoothScanEntity
import kaist.iclab.mobiletracker.db.entity.phone.CallLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.ConnectivityEntity
import kaist.iclab.mobiletracker.db.entity.phone.DataTrafficEntity
import kaist.iclab.mobiletracker.db.entity.phone.DeviceModeEntity
import kaist.iclab.mobiletracker.db.entity.phone.ExerciseEntity
import kaist.iclab.mobiletracker.db.entity.phone.MediaEntity
import kaist.iclab.mobiletracker.db.entity.phone.MessageLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.NotificationEntity
import kaist.iclab.mobiletracker.db.entity.phone.ScreenEntity
import kaist.iclab.mobiletracker.db.entity.phone.SleepEntity
import kaist.iclab.mobiletracker.db.entity.phone.StepEntity
import kaist.iclab.mobiletracker.db.entity.phone.UserInteractionEntity
import kaist.iclab.mobiletracker.db.entity.phone.WifiScanEntity
import kaist.iclab.mobiletracker.db.entity.watch.AccelerometerEntity
import kaist.iclab.mobiletracker.db.entity.watch.ECGEntity
import kaist.iclab.mobiletracker.db.entity.watch.EDAEntity
import kaist.iclab.mobiletracker.db.entity.watch.HeartRateEntity
import kaist.iclab.mobiletracker.db.entity.watch.PPGEntity
import kaist.iclab.mobiletracker.db.entity.watch.SkinTemperatureEntity

/**
 * Central registry of every sensor's [SensorStore]. Each store is created with just the entity
 * class — [SensorStore] resolves `timestamp` and `deviceType` from the ObjectBox-generated
 * `Entity_` meta-class internally.
 */
class SensorStores(boxStore: BoxStore) {
    val ambientLight = SensorStore(boxStore, AmbientLightEntity::class.java)
    val appListChange = SensorStore(boxStore, AppListChangeEntity::class.java)
    val appUsageLog = SensorStore(boxStore, AppUsageLogEntity::class.java)
    val battery = SensorStore(boxStore, BatteryEntity::class.java)
    val bluetoothScan = SensorStore(boxStore, BluetoothScanEntity::class.java)
    val callLog = SensorStore(boxStore, CallLogEntity::class.java)
    val connectivity = SensorStore(boxStore, ConnectivityEntity::class.java)
    val dataTraffic = SensorStore(boxStore, DataTrafficEntity::class.java)
    val deviceMode = SensorStore(boxStore, DeviceModeEntity::class.java)
    val exercise = SensorStore(boxStore, ExerciseEntity::class.java, dedupStrategy = DedupStrategy.TIMESTAMP)
    val media = SensorStore(boxStore, MediaEntity::class.java)
    val messageLog = SensorStore(boxStore, MessageLogEntity::class.java)
    val notification = SensorStore(boxStore, NotificationEntity::class.java)
    val screen = SensorStore(boxStore, ScreenEntity::class.java)
    val sleep = SensorStore(boxStore, SleepEntity::class.java, dedupStrategy = DedupStrategy.TIMESTAMP)
    val step = SensorStore(boxStore, StepEntity::class.java, dedupStrategy = DedupStrategy.TIMESTAMP)
    val userInteraction = SensorStore(boxStore, UserInteractionEntity::class.java)
    val wifiScan = SensorStore(boxStore, WifiScanEntity::class.java)

    // Shared between phone-native GPS fixes (no eventId) and watch-forwarded rows (real eventId) -
    // SensorStore.applyExistingIdsByEventId skips blank eventIds so phone-native fixes always
    // insert fresh, only watch-forwarded rows dedup.
    val location = SensorStore(boxStore, LocationEntity::class.java, dedupStrategy = DedupStrategy.EVENT_ID)

    val watchHeartRate = SensorStore(boxStore, HeartRateEntity::class.java, dedupStrategy = DedupStrategy.EVENT_ID)
    val watchAccelerometer = SensorStore(boxStore, AccelerometerEntity::class.java, dedupStrategy = DedupStrategy.EVENT_ID)
    val watchEDA = SensorStore(boxStore, EDAEntity::class.java, dedupStrategy = DedupStrategy.EVENT_ID)
    val watchPPG = SensorStore(boxStore, PPGEntity::class.java, dedupStrategy = DedupStrategy.EVENT_ID)
    val watchSkinTemperature = SensorStore(boxStore, SkinTemperatureEntity::class.java, dedupStrategy = DedupStrategy.EVENT_ID)
    val ecg = SensorStore(boxStore, ECGEntity::class.java, dedupStrategy = DedupStrategy.EVENT_ID)
}
