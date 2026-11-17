package kaist.iclab.mobiletracker.di.phone

import kaist.iclab.mobiletracker.storage.CouchbaseSensorStateStorage
import kaist.iclab.mobiletracker.storage.SimpleStateStorage
import kaist.iclab.tracker.permission.AndroidPermissionManager
import kaist.iclab.tracker.sensor.phone.BluetoothScanSensor
// import kaist.iclab.tracker.sensor.phone.CallLogSensor
// import kaist.iclab.tracker.sensor.phone.MessageLogSensor
// import kaist.iclab.tracker.sensor.phone.NotificationSensor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

/**
 * Communication sensors module - bluetooth
 *
 * The call log, message log and notification sensors are commented out: Play Protect and Samsung
 * Knox block a downloaded APK that requests SMS or call log access or declares a notification
 * listener service. To use them again, uncomment these and their entries in ControllerModule,
 * and remove the matching `tools:node="remove"` entries from AndroidManifest.xml.
 */
val communicationSensorsModule = module {
    // // Call Log Sensor
    // single {
    //     CallLogSensor(
    //         context = androidContext(),
    //         permissionManager = get<AndroidPermissionManager>(),
    //         configStorage = SimpleStateStorage(
    //             CallLogSensor.Config(TimeUnit.MINUTES.toMillis(1))
    //         ),
    //         stateStorage = CouchbaseSensorStateStorage(
    //             couchbase = get(),
    //             collectionName = CallLogSensor::class.simpleName ?: ""
    //         )
    //     )
    // }

    // // Message Log Sensor
    // single {
    //     MessageLogSensor(
    //         context = androidContext(),
    //         permissionManager = get<AndroidPermissionManager>(),
    //         configStorage = SimpleStateStorage(
    //             MessageLogSensor.Config(interval = TimeUnit.SECONDS.toMillis(10))
    //         ),
    //         stateStorage = CouchbaseSensorStateStorage(
    //             couchbase = get(),
    //             collectionName = MessageLogSensor::class.simpleName ?: ""
    //         )
    //     )
    // }

    // // Notification Sensor
    // single {
    //     NotificationSensor(
    //         permissionManager = get<AndroidPermissionManager>(),
    //         configStorage = SimpleStateStorage(NotificationSensor.Config()),
    //         stateStorage = CouchbaseSensorStateStorage(
    //             couchbase = get(),
    //             collectionName = NotificationSensor::class.simpleName ?: ""
    //         )
    //     )
    // }

    // Bluetooth Scan Sensor
    single {
        BluetoothScanSensor(
            context = androidContext(),
            permissionManager = get<AndroidPermissionManager>(),
            configStorage = SimpleStateStorage(
                BluetoothScanSensor.Config(
                    doScan = true,
                    interval = TimeUnit.SECONDS.toMillis(10),
                    scanDuration = TimeUnit.SECONDS.toMillis(1)
                )
            ),
            stateStorage = CouchbaseSensorStateStorage(
                couchbase = get(),
                collectionName = BluetoothScanSensor::class.simpleName ?: ""
            )
        )
    }
}
