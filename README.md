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
- A phone running Android 11 (API 30) or newer. Step, sleep, and exercise data also need a Samsung phone with Samsung Health and a `samsung` build (see [Samsung Health features](#samsung-health-features)).
- A Galaxy Watch running Wear OS, if you want watch data. Available sensors depend on the watch model. Biosignals such as heart rate also need a `samsung` build.
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

### 3. (Optional) Add the Samsung Health SDKs

Skip this step to build the `open` flavor, which has every feature except the Samsung Health sensors. To build the `samsung` flavor with them, add the SDKs. They are not included in this repository.

1. Download the [Sensor SDK](https://developer.samsung.com/health/sensor/overview.html) and the [Data SDK](https://developer.samsung.com/health/data/overview.html). This requires a Samsung account.
2. Rename the `.aar` files and place them here:
   - `samsung-health-sensor-api/samsung-health-sensor-api.aar`
   - `samsung-health-data-api/samsung-health-data-api.aar`

You can add only one of them. The Sensor SDK enables the watch sensors and the Data SDK enables the phone sensors.

> [!IMPORTANT]
> Without an approved Samsung partner license, enable Developer Mode on your test phone. In Samsung Health, open **Settings**, then **About Samsung Health**, and tap the version line 10 times. Otherwise, permission requests fail with `AuthorizationException (Error 2003: Could not get policy)`.

### 4. Build and install

Each app has two flavors:

| Flavor | Needs the SDKs | Samsung Health sensors |
| --- | --- | --- |
| `open` | No | None |
| `samsung` | Yes (step 3) | Phone: step, sleep, exercise. Watch: accelerometer, PPG, heart rate, skin temperature, EDA, stress, on-demand ECG. |

The `samsung` tasks exist only when the matching SDK is in place. The commands below use `Open`; replace it with `Samsung` for the other flavor.

With the phone connected:

```bash
./gradlew :app-mobile-tracker:installOpenDebug
```

With the watch connected through wireless debugging:

```bash
adb connect <watch-ip>:<port>
./gradlew :app-wearable-tracker:installOpenDebug
```

If both devices are connected, the install task installs to all of them. Disconnect the one you are not targeting.

To build APK files instead, use `assembleOpenDebug` or `assembleOpenRelease`. The files are written to:

- `app-mobile-tracker/build/outputs/apk/<flavor>/<buildType>/EnPULSE-Mobile.apk`
- `app-wearable-tracker/build/outputs/apk/<flavor>/<buildType>/EnPULSE-Watch.apk`

The `samsung` files are named `EnPULSE-Mobile-Samsung.apk` and `EnPULSE-Watch-Samsung.apk`.

To build the `open` flavor while the SDKs are in place, add `-Penpulse.samsungHealth=false`.

The phone and watch apps share one application ID and must be signed with the same key, or they cannot exchange data. Install the same flavor on both.

## Samsung Health features

Samsung licenses the Health SDKs for development and testing only. To distribute an app built with them, you need your own Samsung Health partnership ([Sensor SDK process](https://developer.samsung.com/health/sensor/process.html), [Data SDK process](https://developer.samsung.com/health/data/process.html)), which registers your app's package name and signing key. The prebuilt EnPULSE releases are the `open` flavor for this reason.

To distribute a `samsung` build:

1. Apply for the partnership for the SDKs you use, with your own package name and the SHA-256 of your release key.
2. Set your package name in `local.properties`. Both apps use it.

   ```properties
   ENPULSE_APPLICATION_ID=org.example.yourstudy
   ```

   A Gradle property (`-PENPULSE_APPLICATION_ID=...`) or an environment variable of the same name also works.
3. Sign both apps with your own release key. The `release` build type uses the debug key by default.
4. Build and distribute `assembleSamsungRelease` for both apps.

Until the partnership is approved, test with Developer Mode on (see step 3).
