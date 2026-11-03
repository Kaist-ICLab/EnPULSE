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
            stores.activityRecognition.countAfterFlow(startOfDay),
            stores.step.countAfterFlow(startOfDay),
            stores.battery.countAfterFlow(startOfDay),
            stores.screen.countAfterFlow(startOfDay),
            stores.connectivity.countAfterFlow(startOfDay),
            stores.bluetoothScan.countAfterFlow(startOfDay),
            stores.ambientLight.countAfterFlow(startOfDay),
            stores.appListChange.countAfterFlow(startOfDay),
            stores.dataTraffic.countAfterFlow(startOfDay),
            stores.deviceMode.countAfterFlow(startOfDay),
            stores.media.countAfterFlow(startOfDay),
            stores.wifiScan.countAfterFlow(startOfDay),
            stores.exercise.countAfterFlow(startOfDay),
            stores.sleep.countAfterFlow(startOfDay),
            stores.vad.countAfterFlow(startOfDay)
        ) { args: Array<Int> -> args.toList() }

        // Combine watch sensor flows
        val watchFlow = combine(
            stores.watchHeartRate.countAfterFlow(startOfDay),
            stores.watchAccelerometer.countAfterFlow(startOfDay),
            stores.watchEDA.countAfterFlow(startOfDay),
            stores.watchPPG.countAfterFlow(startOfDay),
            stores.watchSkinTemperature.countAfterFlow(startOfDay),
            stores.watchIMU.countAfterFlow(startOfDay),
            stores.watchGesture.countAfterFlow(startOfDay),
            stores.watchStress.countAfterFlow(startOfDay),
            stores.ecg.countAfterFlow(startOfDay)
        ) { args: Array<Int> -> args.toList() }

        // Combine both flows into final result
        return combine(phoneFlow, watchFlow) { phone, watch ->
            DailySensorCounts(
                // Phone sensors
                locationCount = phone[0],
                appUsageCount = phone[1],
                activityCount = phone[2],
                stepCount = phone[3],
                batteryCount = phone[4],
                screenCount = phone[5],
                connectivityCount = phone[6],
                bluetoothCount = phone[7],
                ambientLightCount = phone[8],
                appListChangeCount = phone[9],
                dataTrafficCount = phone[10],
                deviceModeCount = phone[11],
                mediaCount = phone[12],
                wifiScanCount = phone[13],
                exerciseCount = phone[14],
                sleepCount = phone[15],
                vadCount = phone[16],
                // Watch sensors
                watchHeartRateCount = watch[0],
                watchAccelerometerCount = watch[1],
                watchEDACount = watch[2],
                watchPPGCount = watch[3],
                watchSkinTemperatureCount = watch[4],
                watchIMUCount = watch[5],
                watchGestureCount = watch[6],
                watchStressCount = watch[7],
                ecgCount = watch[8]
            )
        }
    }

    override fun getWatchConnectionInfo(): Flow<WatchConnectionInfo> {
        return watchSensorRepository.getWatchConnectionInfo()
    }
}
