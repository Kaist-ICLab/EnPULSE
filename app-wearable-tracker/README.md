# Wearable Tracker

WearOS application for Galaxy Watch sensor data collection, storage, and synchronization with the
companion mobile application.

## Overview

The Wearable Tracker app runs continuously on WearOS (Galaxy Watch) devices to capture
high-frequency biosignals and physical activity data. Captured sensor data is cached locally using
ObjectBox and periodically synced to the companion smartphone application (`app-mobile-tracker`)
over Bluetooth LE / Google Wearable Data Layer.

## Supported Sensors

- **IMU Sensor**: Accelerometer and Gyroscope raw sensor data.
- **Heart Rate & PPG**: Real-time heart rate and photoplethysmography blood flow signal.
- **Skin Temperature**: Continuous skin temperature monitoring.
- **EDA (Electrodermal Activity)**: Galvanic skin response tracking.
- **ECG Sensor**: Electrocardiogram signal recording.
- **Stress & Exercise**: WearOS exercise and stress estimation algorithms.
- **Micro EMA**: Wrist-wearable micro-survey triggers and responses.

## Required Configuration

1. **Samsung Health SDK Setup**: Ensure you have placed the required Samsung Health AAR files in the
   project root module as documented in the main [
   `README.md`](../README.md#download-samsung-health-sensordata-sdk).
2. **Companion Pairing**: Install and launch `app-mobile-tracker` on your smartphone to receive
   synced data from the watch.

## Related Modules

- [`tracker-library`](../tracker-library/README.md) - Core sensor tracking engine and sensor
  definitions
- [`app-mobile-tracker`](../app-mobile-tracker/README.md) - Handheld companion application
