# Wearable Tracker

The EnPULSE Galaxy Watch app. It collects biosignals, stores them in ObjectBox, and sends them to the phone app over the Wearable Data Layer. The phone uploads them to the backend.

For build setup, follow the [root README](../README.md#build-from-source). The watch app needs the Samsung Health SDKs and `local.properties`, but not `google-services.json`.

## Installation

A watch cannot open a downloaded APK, so install over wireless debugging. On the watch, enable **Developer options**, then **Wireless debugging**, and then run:

```bash
adb connect <watch-ip>:<port>
./gradlew :app-wearable-tracker:installDebug
```

Install the phone app as well, signed with the same key as the watch app. Both apps use the application ID `kaist.iclab.trackerSystem`, and the Wearable Data Layer connects them only when the ID and signing key match.

## Sensors

| Sensor | Data |
| --- | --- |
| `AccelerometerSensor` | 3-axis acceleration |
| `PPGSensor` | Green, red, and infrared PPG |
| `HeartRateSensor` | Heart rate and inter-beat intervals |
| `SkinTemperatureSensor` | Skin and ambient temperature |
| `EDASensor` | Skin conductance |
| `IMUSensor` | Accelerometer and gyroscope at 50 Hz |
| `GestureSensor` | On-watch activity recognition from IMU and audio |
| `StressSensor` | RMSSD-based stress flag from heart rate data |
| `LocationSensor` | Location updates |
| `ECGSensor` | On-demand ECG, started from a measurement screen |

Availability depends on the watch model.

The active campaign, set on the phone, decides which sensors run.

## Other features

- **Micro-EMA:** short surveys on the watch, with responses sent to the phone.
- **Triggers:** rules evaluated from sensor state can start a survey or a notification.
- **Off-body detection:** sensors pause while the watch is not worn and resume when it is worn again.

Collection stays off after a reboot until it is started again from the app.

## Troubleshooting

If a body sensor permission toggle flashes and fails on the watch, grant the permissions over ADB:

```bash
adb -s <watch-serial> shell pm grant kaist.iclab.trackerSystem android.permission.BODY_SENSORS
adb -s <watch-serial> shell pm grant kaist.iclab.trackerSystem android.permission.BODY_SENSORS_BACKGROUND
```

To follow trigger activity:

```bash
adb -s <watch-serial> logcat -v time -s TriggerEngine:D WatchEmaTriggerRcvr:D WatchNotificationRcvr:D
```

## Related modules

- [`tracker-library`](../tracker-library/README.md): sensors and shared infrastructure
- [`app-mobile-tracker`](../app-mobile-tracker/README.md): phone app
