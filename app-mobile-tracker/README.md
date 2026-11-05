# Mobile Tracker

The EnPULSE phone app. It collects phone sensor data, receives data from the watch app, stores everything in ObjectBox, and uploads it to the EnPULSE backend.

For build setup, follow the [root README](../README.md#build-from-source).

```bash
./gradlew :app-mobile-tracker:installDebug
```

## Features

- **Campaigns:** join a campaign created on the dashboard. The campaign decides which sensors run.
- **Data collection:** start and stop collection from Settings. Collection stays off after a reboot until the user starts it again.
- **Upload:** automatic sync and a manual upload option.
- **Data view:** daily record counts on Home, per-sensor records, and CSV export.
- **Surveys and triggers:** phone and watch surveys, with notifications and broadcasts triggered from sensor state.
- **WebApps:** campaign web apps, hosted in a WebView, with access to sensors through a JavaScript bridge.
- **Languages:** English and Korean.

## Sensors

| Group | Sensors |
| --- | --- |
| Activity | `ActivityRecognitionSensor`, `AppUsageLogSensor`, `AppListChangeSensor`, `MediaSensor`, `ScreenSensor` |
| Device | `AmbientLightSensor`, `BatterySensor`, `DeviceModeSensor` |
| Connectivity | `BluetoothScanSensor`, `ConnectivitySensor`, `DataTrafficSensor`, `WifiScanSensor` |
| Location | `LocationSensor` |
| Samsung Health | `StepSensor`, `SleepSensor`, `ExerciseSensor` |
| Audio | `VADSensor` |
| Study | `TimingSensor`, `MicroEmaSensor`, `SurveySensor` |

`SurveySensor` always captures responses. It has no Settings toggle and does not depend on the collection switch.

Call log, SMS, notification, and accessibility-based sensors are not included, because Google Play Protect and Samsung Knox block APKs that request those permissions.

## Connecting to a backend

The server can be set in two ways:

- At build time, with `SUPABASE_URL` and `SUPABASE_ANON_KEY` in `local.properties`.
- In the app, on the **Server Connection** screen. You enter the URL, and the app fetches the anon key from the server's `anon_key` edge function. A URL set here overrides the build-time value.

## Sign-in

The app signs in through Supabase Auth, with two options:

- **Email and password:** no extra setup.
- **Google:** uses Credential Manager. The client ID is set by `default_web_client_id` in `src/main/res/values/strings.xml` and `values-ko/strings.xml`, and currently points to the KAIST ICLab project. For your own deployment, replace it in both files with your own web client ID, and enable the Google provider on your Supabase server.

The `WEB_CLIENT_ID` line in `local.properties.example` is not read by the build.

## Code layout

Packages are under `src/main/java/kaist/iclab/mobiletracker/`:

| Package | Contents |
| --- | --- |
| `auth`, `config` | Supabase sign-in and server configuration |
| `db` | ObjectBox entities and stores |
| `di` | Koin modules. `SensorRegistry.kt` maps each sensor to its store and Supabase table |
| `repository` | Data access and error classification |
| `services` | Foreground services for collection, sync, and upload |
| `trigger`, `webapp` | Trigger actions, and the WebApp host and bridge |
| `ui`, `viewmodels`, `navigation` | Compose screens and their state |

## Related modules

- [`tracker-library`](../tracker-library/README.md): sensors and shared infrastructure
- [`app-wearable-tracker`](../app-wearable-tracker/README.md): watch app
