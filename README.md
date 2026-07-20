# EnPULSE

**EnPULSE (Enabling Platform for User Logging and Sensing Environment)** is a sensor data collection platform for mobile and wrist-worn wearable devices. Developed by KAIST Interactive Computing Laboratory.

## Overview

EnPULSE consists of several core components that support end-to-end sensor data collection:

### Android Library (`tracker-library`)
The core sensor tracking library capable of collecting various sensor data from mobile and Galaxy Watch devices. It includes 21 sensor types, with watch-specific sensors (`AccelerometerSensor`, `PPGSensor`, `HeartRateSensor`, `SkinTemperatureSensor`, `EDASensor`, `ECGSensor`) and phone-specific sensors (such as `StepSensor`).

### Mobile Tracker Application (`app-mobile-tracker`)
A handheld application for smartphone sensor data collection, study campaign configuration, local caching, and cloud data synchronization. See the [Mobile Tracker README](app-mobile-tracker/README.md) for details.

### Wearable Tracker Application (`app-wearable-tracker`)
A WearOS application for continuous biosignal sensing on Galaxy Watch devices. See the [Wearable Tracker README](app-wearable-tracker/README.md) for details.

### Backend (Supabase)
Cloud infrastructure for user authentication, study campaign distribution, and real-time/batch sensor data synchronization powered by Supabase.

### Dashboard
Next.js based dashboard for campaign configuration, management, and real-time data monitoring. Available in [this repository](https://github.com/Kaist-ICLab/EnPULSE-dashboard).

---

## Installation & Setup

### For Users (Manual Installation via APK)

Pre-built APKs downloaded from GitHub Releases are **fully pre-configured** and ready to run — users do not need to set up any configuration or keys.

1. Download `EnPULSE-Mobile.apk` and `EnPULSE-Watch.apk` from the [Releases](https://github.com/Kaist-ICLab/EnPULSE/releases) section.
2. Install `EnPULSE-Mobile.apk` on your Samsung mobile device.
3. Install `EnPULSE-Watch.apk` on your Galaxy Watch device (e.g., via ADB or tools like Easy Fire Tools).

---

## For Developers: Building from Source

### Required Configuration

#### 1. Copy `local.properties.example`
Copy `local.properties.example` to `local.properties` in the project root and configure your local Android SDK directory and Supabase credentials:

```bash
cp local.properties.example local.properties
```

#### 2. Download Samsung Health Sensor/Data SDK
The Samsung Health SDKs are required for collecting real-time biosignals from Galaxy Watch devices.

1. Download the Samsung Health [Sensor SDK](https://developer.samsung.com/health/data/overview.html#SDK-download) and [Data SDK](https://developer.samsung.com/health/data/overview.html#SDK-download) (requires a Samsung account).
2. Rename the downloaded `.aar` files to `samsung-health-sensor-api.aar` and `samsung-health-data-api.aar`.
3. Place the `.aar` files into the `samsung-health-sensor-api/` and `samsung-health-data-api/` directories respectively.

#### 3. Add `google-services.json` (Firebase Configuration)
Because `google-services.json` is untracked by Git for security, you must provide your own Firebase project configuration when building from source:

1. Create or open a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android App registered with package name `kaist.iclab.trackerSystem`.
3. Download `google-services.json` and place it inside the `app-mobile-tracker/` directory.
4. (Optional) Enable Google Sign-In under Firebase Authentication settings.
