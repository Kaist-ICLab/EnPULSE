# Tracker Library

The core Android library of EnPULSE. It provides the sensors, the background service that runs them, the trigger engine, and the phone-watch sync used by both apps.

The library is used as a Gradle module (`project(":tracker-library")`). It needs the Samsung Health SDKs described in the [root README](../README.md#3-add-the-samsung-health-sdks).

## Structure

Packages are under `src/main/java/kaist/iclab/tracker/`:

| Package | Contents |
| --- | --- |
| `sensor` | `core` has the `Sensor` interface and `BaseSensor`. `phone`, `galaxywatch`, and `common` have the implementations. `controller` has `BackgroundController`, the foreground service that starts and stops sensors. |
| `listener` | Alarm, broadcast, and Samsung Health connection helpers |
| `permission` | `AndroidPermissionManager` and hardware availability checks |
| `storage` | Storage interfaces and Couchbase implementations for configuration and state |
| `sync` | Data channel interfaces and the BLE implementation, built on the Wearable Data Layer |
| `trigger` | Trigger engine that evaluates rules against sensor state and runs actions |
| `auth` | `Authentication` interfaces and `GoogleAuth` |

The library does not store sensor data itself. Each sensor emits entities to its listeners, and the app decides where to store them. Both EnPULSE apps use ObjectBox.

## Sensors

The **Device** column shows where each sensor runs:

- **Phone:** any Android phone.
- **Watch:** any Wear OS watch.
- **Samsung Phone:** a Samsung phone with Samsung Health.
- **Galaxy Watch:** a Wear OS Galaxy Watch, through the Samsung Health Sensor SDK. Availability depends on the model.

| Sensor | Device | Description | Key fields |
| --- | --- | --- | --- |
| `ActivityRecognitionSensor` | Phone / Watch | Physical activity classification from Google Activity Recognition. | `activityType`, `score`, `probabilities` |
| `AmbientLightSensor` | Phone | Illuminance from the device light sensor. | `value`, `accuracy` |
| `AppListChangeSensor` | Phone | Emits an event when an installed app is added or removed. | `changedApp`, optional `appList` |
| `AppUsageLogSensor` | Phone | App usage events from `UsageStatsManager`. | `packageName`, `installedBy`, `eventType` |
| `AudioSensor` | Phone / Watch | Raw microphone audio at 16 kHz. Input to `VADSensor` and `GestureSensor`. | `samples`, `sampleRateHz` |
| `BatterySensor` | Phone / Watch | Battery level, charging status, plug type, and temperature. | `level`, `status`, `temperature`, `connectedType` |
| `BluetoothScanSensor` | Phone | Paired and nearby Bluetooth Classic and LE devices. | `name`, `address`, `rssi`, `isLE` |
| `ConnectivitySensor` | Phone | Network type, connection state, and internet availability. | `networkType`, `isConnected`, `hasInternet`, `transportTypes` |
| `DataTrafficSensor` | Phone | Total and mobile byte counters. | `totalRx`, `totalTx`, `mobileRx`, `mobileTx` |
| `DeviceModeSensor` | Phone | Do Not Disturb, airplane mode, and power-save changes. | `eventType`, `value` |
| `ExerciseSensor` | Samsung Phone | Exercise sessions from the Samsung Health Data SDK. | `duration`, `exerciseType`, `calories`, `distance`, `meanHeartRate` |
| `LocationSensor` | Phone / Galaxy Watch | Fused location updates. | `latitude`, `longitude`, `altitude`, `speed`, `accuracy` |
| `MediaSensor` | Phone | Media file changes in internal and external storage. | `operation`, `mediaType`, `storageType`, `uri`, `mimeType` |
| `MicroEmaSensor` | Phone | Receives micro-EMA responses from the watch over BLE and reports them as `SurveySensor.Entity`. | `response`, `triggerTime`, `deviceType` |
| `ScreenSensor` | Phone | Screen on, screen off, and user-present events. | `type` |
| `SleepSensor` | Samsung Phone | Sleep sessions with stages from the Samsung Health Data SDK. | `duration`, `sleepScore`, `stages` |
| `StepSensor` | Samsung Phone | Step counts from the Samsung Health Data SDK. | `duration`, `steps` |
| `SurveySensor` | Phone | Scheduled phone surveys, notifications, and responses. | `triggerTime`, `surveyStartTime`, `response`, `deviceType` |
| `TimingSensor` | Phone | Fires named schedule events (ESM or fixed time), used as trigger conditions. | `value`, `timestamp` |
| `VADSensor` | Phone | Voice activity detection with YAMNet, on audio from `AudioSensor`. | `isSpeech`, `speechProbability`, `inferenceTimeMs` |
| `WifiScanSensor` | Phone | Wi-Fi scan results. | `ssid`, `bssid`, `frequency`, `level` |
| `AccelerometerSensor` | Galaxy Watch | Continuous 3-axis acceleration. | `x`, `y`, `z` |
| `ECGSensor` | Galaxy Watch | On-demand ECG. Requires a region and model where Samsung's ECG feature is available. | `ecgMv`, `leadOff`, `sequence`, `ppgGreen` |
| `EDASensor` | Galaxy Watch | Continuous electrodermal activity. | `skinConductance`, `status` |
| `GestureSensor` | Galaxy Watch | On-watch activity recognition. Fuses IMU and audio with TFLite models to classify 27 classes. | `classIndex`, `score`, `probabilities` |
| `HeartRateSensor` | Galaxy Watch | Continuous heart rate and inter-beat intervals. | `hr`, `hrStatus`, `ibi`, `ibiStatus` |
| `IMUSensor` | Watch | Accelerometer and gyroscope at 50 Hz, from Android sensors. | `accX/Y/Z`, `gyroX/Y/Z` |
| `PPGSensor` | Galaxy Watch | Green, red, and infrared PPG with status flags. | `green`, `red`, `ir`, `*Status` |
| `SkinTemperatureSensor` | Galaxy Watch | Continuous skin and ambient temperature. | `objectTemperature`, `ambientTemperature`, `status` |
| `StressSensor` | Galaxy Watch | RMSSD from `HeartRateSensor` over 1-minute and 5-minute windows. Flags stress below the 20th percentile of recent history. | `rmssd1m`, `rmssd5m`, `threshold`, `isStressed` |

## Related modules

- [`app-mobile-tracker`](../app-mobile-tracker/README.md): phone app
- [`app-wearable-tracker`](../app-wearable-tracker/README.md): watch app

## Acknowledgement

We thank [Sanghyeon Lee](https://github.com/nailegg) and [Arslan Kenbayev](https://github.com/arskenchik) for porting and training the WatchHAR model on Android, which enabled the gesture sensor in EnPULSE.
