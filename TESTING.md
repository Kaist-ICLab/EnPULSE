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

## 3. Testing WebApp Trigger (Watch-to-Phone Simulation)

This test simulates the watch identifying a `High` stress state from sensor data, evaluating the trigger rule, and sending the action to the phone to open a web app.

### Step 1: Ensure Setup is Synced
1. Open the phone app, log in, and join your campaign.
2. Open the watch app.
3. Tap **Reload & Sync Config** in the phone settings to sync the campaign configurations and triggers to the watch.
4. Turn on the **Accelerometer** and **Stress** toggles on the watch app, and tap **Start Data Collection**.

### Step 2: Trigger High Stress via ADB
Run this command to send a simulated `High` stress state update to the watch:

```bash
adb -s <WATCH_SERIAL> shell am broadcast \
  -a kaist.iclab.wearabletracker.UPDATE_STATE \
  -n kaist.iclab.trackerSystem/kaist.iclab.wearabletracker.trigger.TriggerDebugReceiver \
  --es sensor stress \
  --es value High
```

### Step 3: Observe results
1. **Watch Logs**: You should see a logcat entry indicating the watch received the ADB command:
   ```text
   TriggerDebugReceiver: Received debug state update via ADB: stress -> High
   TriggerEngine: Evaluating trigger "High Stress Triggering" (id=...)
   TriggerEngine:   Condition result: TRUE ✓
   TriggerEngine:   Action[...] notification(url=http://...): EXECUTING
   ```
2. **Phone Action**: The phone will receive the command via BLE and display a notification. Tapping this notification will open the campaign's registered web application URL (e.g., `http://143.248.56.146:1111/`) in the mobile browser.

---

## 4. Testing WebApp Notification Directly (Phone-Side Simulation)

If you want to test the phone's web app launch and notification system directly without involving the watch, you can simulate a web app action broadcast directly on the phone.

Run the following command:

```bash
adb -d shell am broadcast \
  -a kaist.iclab.mobiletracker.test.TRIGGER_WEBAPP \
  -n kaist.iclab.trackerSystem/kaist.iclab.mobiletracker.webapp.WebAppTestReceiver \
  --es webapp_id demo-webapp \
  --es survey_id test_survey
```

**Expected Result:**
The phone will instantly show a notification. Tapping it launches the WebApp viewer inside the EnPULSE phone app for `demo-webapp`.

---

## 5. Useful Logcat Filters for Debugging

Run these commands in separate terminal tabs to monitor the real-time activity of the apps.

### Monitor Watch Sensor & Trigger Engine Activity:
```bash
adb -s <WATCH_SERIAL> logcat -v time -s TriggerDebugReceiver:D TriggerEngine:D WatchTriggerAction:D SamsungHealthSensorInitializer:E
```

### Monitor Phone WebApp Trigger & BLE Communication Activity:
```bash
adb -d logcat -v time -s WebAppTriggerHandler:D WebAppTestReceiver:D BLEDataChannel:D
```
