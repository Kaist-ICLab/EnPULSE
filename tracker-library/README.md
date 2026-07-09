# Tracker Library

Core Android library of EnPULSE for collecting, storing, and synchronizing
sensor data from smartphones and Galaxy Watch devices. The library exposes a
uniform `Sensor` interface that is implemented by each modality, along with a
`BackgroundController` foreground service that drives the lifecycle of any
combination of sensors at runtime.

## Sensor Modalities

Sensors live under `src/main/java/kaist/iclab/tracker/sensor/` and are grouped
by package according to the device that hosts them. The **Device** column
below records where each sensor actually runs:

- **Phone** &mdash; any Android smartphone.
- **Watch** &mdash; any WearOS smartwatch
- **Samsung Phone** &mdash; Samsung smartphone with Samsung Health installed.
- **Galaxy Watch** &mdash; Wear OS Galaxy Watch (model gates noted in the description).

| Sensor | Device | Description                                                                                                                                                                   | Key fields |
| --- |--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| --- |
| `ActivityRecognitionSensor` | Phone / Watch | Coarse physical-activity classification via Google ActivityRecognitionAPI.                                          | `activityType`, `score`, `probabilities` |
| `AmbientLightSensor` | Phone  | Illuminance readings from the device light sensor, time-sampled at a fixed interval.                                                                                          | `value` (lux), `accuracy` |
| `AppListChangeSensor` | Phone  | Periodic snapshot of installed apps, emitting an event whenever an app is added or removed.                                                                                   | `changedApp`, optional `appList` |
| `AppUsageLogSensor` | Phone  | Foreground-app events from `UsageStatsManager` (MOVE_TO_FOREGROUND, MOVE_TO_BACKGROUND, etc.).                                                                                | `packageName`, `eventType`, `installedBy` |
| `BatterySensor` | Phone / Watch | Battery state broadcast (level, charging status, plug type, temperature).                                                                                                     | `level`, `status`, `temperature`, `connectedType` |
| `BluetoothScanSensor` | Phone  | Discovery of paired and nearby Bluetooth Classic / LE devices, including RSSI.                                                                                                | `name`, `address`, `rssi`, `isLE` |
| `CallLogSensor` | Phone  | Recent call history polled at a configurable interval.                                                                                                                        | `number`, `duration`, `type` |
| `ConnectivitySensor` | Phone / Galaxy Watch | Network state changes (WiFi / cellular / ethernet / VPN, connectivity, internet availability).                                                                                | `networkType`, `isConnected`, `transportTypes` |
| `DataTrafficSensor` | Phone  | Cumulative mobile and total Tx/Rx byte counters sampled on an interval.                                                                                                       | `totalRx/Tx`, `mobileRx/Tx` |
| `DeviceModeSensor` | Phone / Watch | DND (interruption filter), airplane mode, and power-save mode changes.                                                                                                        | `eventType`, `value` |
| `ExerciseSensor` | Samsung Phone | Exercise sessions pulled from the Samsung Health Data SDK.                                                                                                                    | `startTime`, `endTime`, `exerciseType`, `calories`, `distance`, `meanHeartRate` |
| `LocationSensor` | Phone / Galaxy Watch | Fused-provider location updates (lat/lon, altitude, speed, accuracy) with a fully configurable `LocationRequest`.                                                             | `latitude`, `longitude`, `altitude`, `speed`, `accuracy` |
| `MediaSensor` | Phone  | Creation / update events for images, videos, audio, and documents (internal + external storage).                                                                              | `operation`, `mediaType`, `storageType`, `uri`, `mimeType` |
| `MessageLogSensor` | Phone  | SMS and MMS log entries polled at a configurable interval.                                                                                                                    | `number`, `messageType`, `contactType` |
| `NotificationSensor` | Phone  | Posted and removed notifications via `NotificationListenerService`.                                                                                                           | `packageName`, `eventType`, `title`, `text`, `category` |
| `ScreenSensor` | Phone / Watch | Screen on / off and user-present (unlocked) events.                                                                                                                           | `type` |
| `SleepSensor` | Samsung Phone | Sleep sessions with per-stage breakdown pulled from the Samsung Health Data SDK.                                                                                              | `startTime`, `endTime`, `durationSeconds`, `sleepScore`, `stages` |
| `StepSensor` | Samsung Phone | Aggregated step counts pulled from the Samsung Health Data SDK.                                                                                                               | `startTime`, `endTime`, `steps` |
| `SurveySensor` | Phone  | ESM / fixed-time surveys on the phone: scheduling, notifications, and response collection.                                                                                    | `triggerTime`, `surveyStartTime`, `response`, `deviceType` |
| `UserInteractionSensor` | Phone  | UI interaction events from `AccessibilityService` (clicks, focus changes, window transitions), with noisy types filtered.                                                     | `packageName`, `className`, `eventType`, `text` |
| `WifiScanSensor` | Phone  | Wi-Fi scan results (SSID, BSSID, frequency, signal level).                                                                                                                    | `ssid`, `bssid`, `frequency`, `level` |
| `AccelerometerSensor` | Galaxy Watch | Continuous 3-axis acceleration via Samsung Health (raw values converted to m/s&sup2;).                                                                                        | `x`, `y`, `z` per `DataPoint` |
| `AudioSensor` | Watch  | Raw microphone PCM captured at 16 kHz and downsampled to 1 kHz; used standalone or as input to `GestureSensor`.                                                               | `samples`, `sampleRateHz` |
| `EDASensor` | Galaxy Watch (Watch 8+) | Electrodermal activity (skin conductance).                                                                                                                                    | `skinConductance`, `status` |
| `GestureSensor` | Galaxy Watch | On-watch multimodal HAR: fuses IMU and audio through TFLite models to classify 27 daily activities (e.g. brushing, vacuuming, hand-washing).                                  | `classIndex`, `score`, `probabilities` |
| `HeartRateSensor` | Galaxy Watch | Continuous heart rate plus per-beat inter-beat interval (IBI) lists.                                                                                                          | `hr`, `hrStatus`, `ibi`, `ibiStatus` |
| `IMUSensor` | Galaxy Watch | On-device accelerometer + gyroscope sampled and time-aligned at 50 Hz.                                                                                                        | `accX/Y/Z`, `gyroX/Y/Z` |
| `MicroEmaSensor` | Watch | Watch-side micro-EMA prompts triggered by rules and delivered over BLE; responses are surfaced as `SurveySensor.Entity`.                                                      | `response`, `triggerTime`, `responseTime` |
| `PPGSensor` | Galaxy Watch | Raw multi-wavelength photoplethysmography (green / red / IR) with status flags.                                                                                               | `green`, `red`, `ir`, `*Status` |
| `SkinTemperatureSensor` | Galaxy Watch (Watch 5+) | Wrist skin (object) and ambient temperatures.                                                                                                                                 | `objectTemperature`, `ambientTemperature`, `status` |
| `StressSensor` | Galaxy Watch | Derives RMSSD from HR/IBI over a sliding window and flags stress when RMSSD falls below the 20th percentile of recent history. Depends on `HeartRateSensor` + `RmssdHistory`. | `rmssd`, `threshold`, `isStressed`, `ibiCount` |

## Related Modules

- [`app-mobile-tracker`](../app-mobile-tracker/README.md) &mdash; phone application built on this library.
- `app-wearable-tracker` &mdash; Galaxy Watch companion application.

## Acknowledgement
We thank [Sanghyeon Lee](https://github.com/nailegg) and [Arslan Kenbayev](https://github.com/arskenchik) for porting and training the WatchHAR model on Android, which enabled the gesture sensor in EnPULSE.
