# EnPULSE
**EnPULSE(Enabling Platform for User Logging and Sensing Environment)** is a sensor data collection platform for mobile and wrist-worn wearable device.

## Overview
EnPULSE consist of several core components, which can be also used individually.
Together, they support end-to-end sensor data collection.

Note: Other components will be opened to public after paper publication.

### Android Library
The library is capable of collecting various kinds of data from mobile and Galaxy Watch devices. It includes 21 sensors, with 5 of them (`AccelerometerSensor`, `PPGSensor`, `HeartRateSensor`, `SkinTemperatureSensor`, `EDASensor`) only usable on Galaxy Watch and `StepSensor` only usable on the Samsung mobile device.

### Mobile Tracker Application (For Samsung mobile devices)
A mobile app for easy smartmphone data collection from samsung devices. The manual is provided in the [README](https://github.com/Kaist-ICLab/EnPULSE/tree/main/app-mobile-tracker).

### Wearable Tracker Application (For Galaxy Watch devices)
A smartwatch app for easy data collection from Galaxy watch. The manual is provided in the [README](https://github.com/Kaist-ICLab/EnPULSE/tree/main/app-wearable-tracker).

### Backend

### Dashboard
Next.js based dashboard for campaign configuration, management and data monitoring. You can check out in [this repo](https://github.com/Kaist-ICLab/EnPULSE-dashboard).

## Required Configuration
### Download Samsung Health Sensor/Data SDK
The SDK is essential for collecting biosignals in real time from Galaxy Watch devices. Even if you are not going to use the feature, the library currently has dependency to the SDK so it should be configured.
1. Download the Samsung Health [Sensor SDK](https://developer.samsung.com/health/data/overview.html#SDK-download) and [Data SDK](https://developer.samsung.com/health/data/overview.html#SDK-download). Samsung account is required in this case.
2. Rename the downloaded `.aar` file to `samsung-health-sensor-api.aar` and `samsung-health-data-api.aar`.
3. Put the corresponding `.aar` files into `samsung-health-data-api` and `samsung-health-sensor-api` folder.


