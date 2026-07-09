package kaist.iclab.mobiletracker.repository

import kaist.iclab.mobiletracker.db.obx.SensorStores
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Implementation of HomeRepository that aggregates sensor daily counts from the ObjectBox
 * [SensorStores]. Each count is an ObjectBox reactive query bridged to a [Flow] via
 * [kaist.iclab.mobiletracker.db.obx.SensorStore.countAfterFlow].
 */
class HomeRepositoryImpl(
    private val stores: SensorStores,
    private val watchSensorRepository: WatchSensorRepository
) : HomeRepository {

    override fun getDailySensorCounts(startOfDay: Long): Flow<DailySensorCounts> {
        // Combine phone sensor flows
        val phoneFlow = combine(
            stores.location.countAfterFlow(startOfDay),
            stores.appUsageLog.countAfterFlow(startOfDay),
            stores.step.countAfterFlow(startOfDay),
            stores.battery.countAfterFlow(startOfDay),
            stores.notification.countAfterFlow(startOfDay),
            stores.screen.countAfterFlow(startOfDay),
            stores.connectivity.countAfterFlow(startOfDay),
            stores.bluetoothScan.countAfterFlow(startOfDay),
            stores.ambientLight.countAfterFlow(startOfDay),
            stores.appListChange.countAfterFlow(startOfDay),
            stores.callLog.countAfterFlow(startOfDay),
            stores.dataTraffic.countAfterFlow(startOfDay),
            stores.deviceMode.countAfterFlow(startOfDay),
            stores.media.countAfterFlow(startOfDay),
            stores.messageLog.countAfterFlow(startOfDay),
            stores.userInteraction.countAfterFlow(startOfDay),
            stores.wifiScan.countAfterFlow(startOfDay),
            stores.exercise.countAfterFlow(startOfDay),
            stores.sleep.countAfterFlow(startOfDay)
        ) { args: Array<Int> -> args.toList() }

        // Combine watch sensor flows
        val watchFlow = combine(
            stores.watchHeartRate.countAfterFlow(startOfDay),
            stores.watchAccelerometer.countAfterFlow(startOfDay),
            stores.watchEDA.countAfterFlow(startOfDay),
            stores.watchPPG.countAfterFlow(startOfDay),
            stores.watchSkinTemperature.countAfterFlow(startOfDay)
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
                exerciseCount = phone[17],
                sleepCount = phone[18],
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
