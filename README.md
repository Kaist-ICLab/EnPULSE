# EnPULSE
**EnPULSE(Enabling Platform for User Logging and Sensing Environment)** is a sensor data collection platform for mobile and wrist-worn wearable devices. Developed by KAIST Interactive Computing Laboratory.

## Overview
EnPULSE consists of several core components, which can also be used individually.
Together, they support end-to-end sensor data collection.

**Note: Other components will be opened to the public after paper publication.**

### Android Library
The library is capable of collecting various kinds of data from mobile and Galaxy Watch devices. It includes 21 sensors, with 5 of them (`AccelerometerSensor`, `PPGSensor`, `HeartRateSensor`, `SkinTemperatureSensor`, `EDASensor`) only usable on Galaxy Watch and `StepSensor` only usable on the Samsung mobile device.

### Mobile Tracker Application (For Samsung mobile devices)
A mobile app for easy smartphone data collection from Samsung devices. The manual is provided in the [README](https://github.com/Kaist-ICLab/EnPULSE/tree/main/app-mobile-tracker).

### Wearable Tracker Application (For Galaxy Watch devices)
A smartwatch app for easy data collection from the Galaxy Watch. The manual is provided in the [README](https://github.com/Kaist-ICLab/EnPULSE/tree/main/app-wearable-tracker).

### [Backend](https://github.com/Kaist-ICLab/EnPULSE-backend)
Locally-hosted Supabase-based backend component for storing sensor data and campaign configuration.

### [Dashboard](https://github.com/Kaist-ICLab/EnPULSE-dashboard)
Web-based dashboard for campaign configuration, management, and data monitoring.


## Installation & Setup

### For Users (Manual Installation via APK)
If you want to use the pre-built application on your devices without setting up a development environment:
1. Download the latest `app-mobile-tracker-release.apk` and `app-wearable-tracker-release.apk` from the [Releases](https://github.com/Kaist-ICLab/EnPULSE/releases) section.
2. Install `app-mobile-tracker-release.apk` on your Samsung mobile device.
3. Install `app-wearable-tracker-release.apk` on your Galaxy Watch device (e.g., using ADB or tools like Easy Fire Tools).

---

## For Developers: Building from Source

### Required Configuration
#### Download Samsung Health Sensor/Data SDK
The SDK is essential for collecting biosignals in real time from Galaxy Watch devices. Even if you are not going to use the feature, the library currently has dependency to the SDK so it should be configured.
1. Download the Samsung Health [Sensor SDK](https://developer.samsung.com/health/data/overview.html#SDK-download) and [Data SDK](https://developer.samsung.com/health/data/overview.html#SDK-download). Samsung account is required in this case.
2. Rename the downloaded `.aar` file to `samsung-health-sensor-api.aar` and `samsung-health-data-api.aar`.
3. Put the corresponding `.aar` files into `samsung-health-data-api` and `samsung-health-sensor-api` folder.



