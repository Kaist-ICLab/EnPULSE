package kaist.iclab.mobiletracker.db.obx

import io.objectbox.BoxStore
import kaist.iclab.mobiletracker.db.entity.phone.AmbientLightEntity
import kaist.iclab.mobiletracker.db.entity.phone.AmbientLightEntity_
import kaist.iclab.mobiletracker.db.entity.phone.AppListChangeEntity
import kaist.iclab.mobiletracker.db.entity.phone.AppListChangeEntity_
import kaist.iclab.mobiletracker.db.entity.phone.AppUsageLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.AppUsageLogEntity_
import kaist.iclab.mobiletracker.db.entity.phone.BatteryEntity
import kaist.iclab.mobiletracker.db.entity.phone.BatteryEntity_
import kaist.iclab.mobiletracker.db.entity.phone.BluetoothScanEntity
import kaist.iclab.mobiletracker.db.entity.phone.BluetoothScanEntity_
import kaist.iclab.mobiletracker.db.entity.phone.CallLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.CallLogEntity_
import kaist.iclab.mobiletracker.db.entity.phone.ConnectivityEntity
import kaist.iclab.mobiletracker.db.entity.phone.ConnectivityEntity_
import kaist.iclab.mobiletracker.db.entity.phone.DataTrafficEntity
import kaist.iclab.mobiletracker.db.entity.phone.DataTrafficEntity_
import kaist.iclab.mobiletracker.db.entity.phone.DeviceModeEntity
import kaist.iclab.mobiletracker.db.entity.phone.DeviceModeEntity_
import kaist.iclab.mobiletracker.db.entity.phone.MediaEntity
import kaist.iclab.mobiletracker.db.entity.phone.MediaEntity_
import kaist.iclab.mobiletracker.db.entity.phone.MessageLogEntity
import kaist.iclab.mobiletracker.db.entity.phone.MessageLogEntity_
import kaist.iclab.mobiletracker.db.entity.phone.NotificationEntity
import kaist.iclab.mobiletracker.db.entity.phone.NotificationEntity_
import kaist.iclab.mobiletracker.db.entity.phone.ScreenEntity
import kaist.iclab.mobiletracker.db.entity.phone.ScreenEntity_
import kaist.iclab.mobiletracker.db.entity.phone.StepEntity
import kaist.iclab.mobiletracker.db.entity.phone.StepEntity_
import kaist.iclab.mobiletracker.db.entity.phone.UserInteractionEntity
import kaist.iclab.mobiletracker.db.entity.phone.UserInteractionEntity_
import kaist.iclab.mobiletracker.db.entity.phone.WifiScanEntity
import kaist.iclab.mobiletracker.db.entity.phone.WifiScanEntity_
import kaist.iclab.mobiletracker.db.entity.watch.WatchAccelerometerEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchAccelerometerEntity_
import kaist.iclab.mobiletracker.db.entity.watch.WatchEDAEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchEDAEntity_
import kaist.iclab.mobiletracker.db.entity.watch.WatchHeartRateEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchHeartRateEntity_
import kaist.iclab.mobiletracker.db.entity.watch.WatchPPGEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchPPGEntity_
import kaist.iclab.mobiletracker.db.entity.watch.WatchSkinTemperatureEntity
import kaist.iclab.mobiletracker.db.entity.watch.WatchSkinTemperatureEntity_

/**
 * Central registry of every sensor's [SensorStore]. These ~one-line registrations are the entire
 * replacement for the 23 per-sensor Room DAOs that used to exist — each just binds an ObjectBox
 * entity to the generic store via its generated `Entity_` property metadata (id / timestamp /
 * eventId, which every sensor entity shares).
 */
class SensorStores(private val boxStore: BoxStore) {
    val ambientLight = SensorStore(boxStore, AmbientLightEntity::class.java, AmbientLightEntity_.id, AmbientLightEntity_.timestamp, AmbientLightEntity_.eventId)
    val appListChange = SensorStore(boxStore, AppListChangeEntity::class.java, AppListChangeEntity_.id, AppListChangeEntity_.timestamp, AppListChangeEntity_.eventId)
    val appUsageLog = SensorStore(boxStore, AppUsageLogEntity::class.java, AppUsageLogEntity_.id, AppUsageLogEntity_.timestamp, AppUsageLogEntity_.eventId)
    val battery = SensorStore(boxStore, BatteryEntity::class.java, BatteryEntity_.id, BatteryEntity_.timestamp, BatteryEntity_.eventId)
    val bluetoothScan = SensorStore(boxStore, BluetoothScanEntity::class.java, BluetoothScanEntity_.id, BluetoothScanEntity_.timestamp, BluetoothScanEntity_.eventId)
    val callLog = SensorStore(boxStore, CallLogEntity::class.java, CallLogEntity_.id, CallLogEntity_.timestamp, CallLogEntity_.eventId)
    val connectivity = SensorStore(boxStore, ConnectivityEntity::class.java, ConnectivityEntity_.id, ConnectivityEntity_.timestamp, ConnectivityEntity_.eventId)
    val dataTraffic = SensorStore(boxStore, DataTrafficEntity::class.java, DataTrafficEntity_.id, DataTrafficEntity_.timestamp, DataTrafficEntity_.eventId)
    val deviceMode = SensorStore(boxStore, DeviceModeEntity::class.java, DeviceModeEntity_.id, DeviceModeEntity_.timestamp, DeviceModeEntity_.eventId)
    val media = SensorStore(boxStore, MediaEntity::class.java, MediaEntity_.id, MediaEntity_.timestamp, MediaEntity_.eventId)
    val messageLog = SensorStore(boxStore, MessageLogEntity::class.java, MessageLogEntity_.id, MessageLogEntity_.timestamp, MessageLogEntity_.eventId)
    val notification = SensorStore(boxStore, NotificationEntity::class.java, NotificationEntity_.id, NotificationEntity_.timestamp, NotificationEntity_.eventId)
    val screen = SensorStore(boxStore, ScreenEntity::class.java, ScreenEntity_.id, ScreenEntity_.timestamp, ScreenEntity_.eventId)
    val step = SensorStore(boxStore, StepEntity::class.java, StepEntity_.id, StepEntity_.timestamp, StepEntity_.eventId)
    val userInteraction = SensorStore(boxStore, UserInteractionEntity::class.java, UserInteractionEntity_.id, UserInteractionEntity_.timestamp, UserInteractionEntity_.eventId)
    val wifiScan = SensorStore(boxStore, WifiScanEntity::class.java, WifiScanEntity_.id, WifiScanEntity_.timestamp, WifiScanEntity_.eventId)

    val location = LocationStore(boxStore)

    val watchHeartRate = SensorStore(boxStore, WatchHeartRateEntity::class.java, WatchHeartRateEntity_.id, WatchHeartRateEntity_.timestamp, WatchHeartRateEntity_.eventId)
    val watchAccelerometer = SensorStore(boxStore, WatchAccelerometerEntity::class.java, WatchAccelerometerEntity_.id, WatchAccelerometerEntity_.timestamp, WatchAccelerometerEntity_.eventId)
    val watchEDA = SensorStore(boxStore, WatchEDAEntity::class.java, WatchEDAEntity_.id, WatchEDAEntity_.timestamp, WatchEDAEntity_.eventId)
    val watchPPG = SensorStore(boxStore, WatchPPGEntity::class.java, WatchPPGEntity_.id, WatchPPGEntity_.timestamp, WatchPPGEntity_.eventId)
    val watchSkinTemperature = SensorStore(boxStore, WatchSkinTemperatureEntity::class.java, WatchSkinTemperatureEntity_.id, WatchSkinTemperatureEntity_.timestamp, WatchSkinTemperatureEntity_.eventId)
}
