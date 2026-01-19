package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.db.dao.common.BaseDao
import kaist.iclab.mobiletracker.db.dao.common.LocationDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Implementation of HomeRepository that aggregates sensor count data from multiple DAOs.
 * Uses DAO Maps for cleaner dependency injection.
 */
class HomeRepositoryImpl(
    private val phoneSensorDaos: PhoneSensorDaoMap,
    private val watchSensorDaos: WatchSensorDaoMap,
    private val watchSensorRepository: WatchSensorRepository
) : HomeRepository {

    /**
     * Data class holding all phone sensor DAOs with daily count functions
     */
    data class PhoneSensorDaoMap(
        val locationDao: LocationDao,
        val appUsageLogDao: kaist.iclab.mobiletracker.db.dao.phone.AppUsageLogDao,
        val stepDao: kaist.iclab.mobiletracker.db.dao.phone.StepDao,
        val batteryDao: kaist.iclab.mobiletracker.db.dao.phone.BatteryDao,
        val notificationDao: kaist.iclab.mobiletracker.db.dao.phone.NotificationDao,
        val screenDao: kaist.iclab.mobiletracker.db.dao.phone.ScreenDao,
        val connectivityDao: kaist.iclab.mobiletracker.db.dao.phone.ConnectivityDao,
        val bluetoothScanDao: kaist.iclab.mobiletracker.db.dao.phone.BluetoothScanDao,
        val ambientLightDao: kaist.iclab.mobiletracker.db.dao.phone.AmbientLightDao,
        val appListChangeDao: kaist.iclab.mobiletracker.db.dao.phone.AppListChangeDao,
        val callLogDao: kaist.iclab.mobiletracker.db.dao.phone.CallLogDao,
        val dataTrafficDao: kaist.iclab.mobiletracker.db.dao.phone.DataTrafficDao,
        val deviceModeDao: kaist.iclab.mobiletracker.db.dao.phone.DeviceModeDao,
        val mediaDao: kaist.iclab.mobiletracker.db.dao.phone.MediaDao,
        val messageLogDao: kaist.iclab.mobiletracker.db.dao.phone.MessageLogDao,
        val userInteractionDao: kaist.iclab.mobiletracker.db.dao.phone.UserInteractionDao,
        val wifiScanDao: kaist.iclab.mobiletracker.db.dao.phone.WifiScanDao
    )

    /**
     * Data class holding all watch sensor DAOs with daily count functions
     */
    data class WatchSensorDaoMap(
        val heartRateDao: kaist.iclab.mobiletracker.db.dao.watch.WatchHeartRateDao,
        val accelerometerDao: kaist.iclab.mobiletracker.db.dao.watch.WatchAccelerometerDao,
        val edaDao: kaist.iclab.mobiletracker.db.dao.watch.WatchEDADao,
        val ppgDao: kaist.iclab.mobiletracker.db.dao.watch.WatchPPGDao,
        val skinTemperatureDao: kaist.iclab.mobiletracker.db.dao.watch.WatchSkinTemperatureDao
    )

    override fun getDailySensorCounts(startOfDay: Long): Flow<DailySensorCounts> {
        // Combine phone sensor flows
        val phoneFlow = combine(
            phoneSensorDaos.locationDao.getDailyLocationCount(startOfDay),
            phoneSensorDaos.appUsageLogDao.getDailyAppUsageCount(startOfDay),
            phoneSensorDaos.stepDao.getDailyStepCount(startOfDay),
            phoneSensorDaos.batteryDao.getDailyBatteryCount(startOfDay),
            phoneSensorDaos.notificationDao.getDailyNotificationCount(startOfDay),
            phoneSensorDaos.screenDao.getDailyScreenCount(startOfDay),
            phoneSensorDaos.connectivityDao.getDailyConnectivityCount(startOfDay),
            phoneSensorDaos.bluetoothScanDao.getDailyBluetoothCount(startOfDay),
            phoneSensorDaos.ambientLightDao.getDailyAmbientLightCount(startOfDay),
            phoneSensorDaos.appListChangeDao.getDailyAppListChangeCount(startOfDay),
            phoneSensorDaos.callLogDao.getDailyCallLogCount(startOfDay),
            phoneSensorDaos.dataTrafficDao.getDailyDataTrafficCount(startOfDay),
            phoneSensorDaos.deviceModeDao.getDailyDeviceModeCount(startOfDay),
            phoneSensorDaos.mediaDao.getDailyMediaCount(startOfDay),
            phoneSensorDaos.messageLogDao.getDailyMessageLogCount(startOfDay),
            phoneSensorDaos.userInteractionDao.getDailyUserInteractionCount(startOfDay),
            phoneSensorDaos.wifiScanDao.getDailyWifiScanCount(startOfDay)
        ) { args: Array<Int> -> args.toList() }

        // Combine watch sensor flows
        val watchFlow = combine(
            watchSensorDaos.heartRateDao.getDailyHeartRateCount(startOfDay),
            watchSensorDaos.accelerometerDao.getDailyAccelerometerCount(startOfDay),
            watchSensorDaos.edaDao.getDailyEDACount(startOfDay),
            watchSensorDaos.ppgDao.getDailyPPGCount(startOfDay),
            watchSensorDaos.skinTemperatureDao.getDailySkinTemperatureCount(startOfDay)
        ) { heartRate, accelerometer, eda, ppg, skinTemp ->
            listOf(heartRate, accelerometer, eda, ppg, skinTemp)
        }

        // Combine both flows into final result
        return combine(phoneFlow, watchFlow) { phone, watch ->
            DailySensorCounts(
                // Phone sensors
                locationCount = phone[0],
                appUsageCount = phone[1],
                activityCount = phone[2],
                batteryCount = phone[3],
                notificationCount = phone[4],
                screenCount = phone[5],
                connectivityCount = phone[6],
                bluetoothCount = phone[7],
                ambientLightCount = phone[8],
                appListChangeCount = phone[9],
                callLogCount = phone[10],
                dataTrafficCount = phone[11],
                deviceModeCount = phone[12],
                mediaCount = phone[13],
                messageLogCount = phone[14],
                userInteractionCount = phone[15],
                wifiScanCount = phone[16],
                // Watch sensors
                watchHeartRateCount = watch[0],
                watchAccelerometerCount = watch[1],
                watchEDACount = watch[2],
                watchPPGCount = watch[3],
                watchSkinTemperatureCount = watch[4]
            )
        }
    }

    override fun getWatchConnectionInfo(): Flow<WatchConnectionInfo> {
        return watchSensorRepository.getWatchConnectionInfo()
    }
}
