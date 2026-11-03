# EnPULSE Testing Guide

This guide provides step-by-step instructions on how to test the EnPULSE mobile and wearable application features using ADB (Android Debug Bridge).

---

## 1. Finding Your Connected Devices

Before running any ADB command, you must identify the serial numbers of your connected mobile phone and Galaxy Watch.

Run the following command:
```bash
adb devices
```

**Example Output:**
```text
List of devices attached
adb-RFKYC063PRD-9ek9oO._adb-tls-connect._tcp   device    # <-- Galaxy Watch (Wi-Fi ADB)
1234567890abcdef                              device    # <-- Mobile Phone (USB/ADB)
```

In the commands below:
- Replace `<WATCH_SERIAL>` with your watch serial (e.g., `adb-RFKYC063PRD-9ek9oO._adb-tls-connect._tcp`).
- Replace `<PHONE_SERIAL>` with your phone serial (or use `-d` if it is the only USB-connected physical phone).

---

## 2. Granting Sensor Permissions (Wear OS 4+ & Android 16+)

On modern Wear OS versions (including Android 16 / Baklava), the system may restrict runtime popups for background sensor usage (`BODY_SENSORS_BACKGROUND`), causing the permission toggle on the watch UI to flash and fail.

If you encounter `ERROR: Permission Failed` in the logs, you can grant them manually via ADB:

```bash
# 1. Grant foreground body sensor access
adb -s <WATCH_SERIAL> shell pm grant kaist.iclab.trackerSystem android.permission.BODY_SENSORS

# 2. Grant background body sensor access
adb -s <WATCH_SERIAL> shell pm grant kaist.iclab.trackerSystem android.permission.BODY_SENSORS_BACKGROUND
```

---

## 3. Useful Logcat Filters for Debugging

Run these commands in separate terminal tabs to monitor the real-time activity of the apps.

### Monitor Watch Sensor & Trigger Engine Activity:
```bash
adb -s <WATCH_SERIAL> logcat -v time -s TriggerEngine:D WatchTriggerAction:D SamsungHealthSensorInitializer:E
```

### Monitor Phone WebApp Trigger & BLE Communication Activity:
```bash
adb -d logcat -v time -s WebAppTriggerHandler:D BLEDataChannel:D
```
