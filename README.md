# EnPULSE

**EnPULSE (Enabling Platform for User Logging and Sensing Environment)** is a sensor data collection platform for smartphones and Galaxy Watch devices, developed by the KAIST Interactive Computing Laboratory.

## Components

| Component | Description |
| --- | --- |
| [`tracker-library`](tracker-library/README.md) | Android library with the sensors, background controller, trigger engine, and phone-watch sync. |
| [`app-mobile-tracker`](app-mobile-tracker/README.md) | Phone app. Collects phone sensors, receives watch data, and uploads everything to the backend. |
| [`app-wearable-tracker`](app-wearable-tracker/README.md) | Galaxy Watch app. Collects biosignals and sends them to the phone. |
| [EnPULSE-backend](https://github.com/Kaist-ICLab/EnPULSE-backend) | Self-hosted, Supabase-based server for sensor data and campaign configuration. |
| [EnPULSE-dashboard](https://github.com/Kaist-ICLab/EnPULSE-dashboard) | Web dashboard for managing campaigns and monitoring data. |

Data flows from the watch to the phone over the Wearable Data Layer, then from the phone to the backend. Both apps store data locally in ObjectBox. The watch deletes data once the phone confirms it, and the phone keeps uploaded data for at least 7 more days.

Campaigns decide which sensors run. A participant joins a campaign, and the app collects only the sensors that campaign includes.

## Requirements

- Android SDK with platform 37, and a JDK 17 or newer to run Gradle. Gradle downloads the JDK 17 toolchain used for compilation automatically.
- A phone running Android 11 (API 30) or newer. Step, sleep, and exercise data also need a Samsung phone with Samsung Health.
- A Galaxy Watch running Wear OS, if you want watch data. Available sensors depend on the watch model.
- A running [EnPULSE-backend](https://github.com/Kaist-ICLab/EnPULSE-backend) server.

## Build from source

### 1. Clone the repository

```bash
git clone https://github.com/Kaist-ICLab/EnPULSE.git
cd EnPULSE
```

### 2. Create `local.properties`

```bash
cp local.properties.example local.properties
```

Set `sdk.dir` to your Android SDK location. Android Studio can also set this for you.

For the backend, choose one option:

- **Build it in:** set `SUPABASE_URL` and `SUPABASE_ANON_KEY` to your server's values.
- **Enter it in the app:** delete both lines. The login screen then sends you to **Server Connection**, where you enter the URL. The app fetches the anon key from the server's `anon_key` edge function.

Do not leave the example placeholder values in place. The app treats them as a real server.

### 3. Add the Samsung Health SDKs

The build needs both SDKs, and they are not included in this repository.

1. Download the [Sensor SDK](https://developer.samsung.com/health/sensor/overview.html) and the [Data SDK](https://developer.samsung.com/health/data/overview.html). This requires a Samsung account.
2. Rename the `.aar` files and place them here:
   - `samsung-health-sensor-api/samsung-health-sensor-api.aar`
   - `samsung-health-data-api/samsung-health-data-api.aar`

> [!IMPORTANT]
> Without an approved Samsung partner license, enable Developer Mode on your test phone. In Samsung Health, open **Settings**, then **About Samsung Health**, and tap the version line 10 times. Otherwise, permission requests fail with `AuthorizationException (Error 2003: Could not get policy)`.

### 4. Add `google-services.json`

The phone app does not build without `app-mobile-tracker/google-services.json`. Use one of these:

- Copy the template. This is enough to build the app:

  ```bash
  cp app-mobile-tracker/google-services.json.example app-mobile-tracker/google-services.json
  ```

- Or download the real file from a [Firebase](https://console.firebase.google.com/) project, with an Android app registered as `kaist.iclab.trackerSystem`.

See [Sign-in](app-mobile-tracker/README.md#sign-in) to set up Google sign-in for your own project. Email and password sign-in works without it.

### 5. Build and install

With the phone connected:

```bash
./gradlew :app-mobile-tracker:installDebug
```

With the watch connected through wireless debugging:

```bash
adb connect <watch-ip>:<port>
./gradlew :app-wearable-tracker:installDebug
```

If both devices are connected, `installDebug` installs to all of them. Disconnect the one you are not targeting.

To build APK files instead, use `assembleDebug` or `assembleRelease`. The files are written to:

- `app-mobile-tracker/build/outputs/apk/<buildType>/EnPULSE-Mobile.apk`
- `app-wearable-tracker/build/outputs/apk/<buildType>/EnPULSE-Watch.apk`

The phone and watch apps share one application ID and must be signed with the same key, or they cannot exchange data.
