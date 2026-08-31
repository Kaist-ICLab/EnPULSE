# EnPULSE Benchmarking Protocol for CHI / Ubicomp

## 1. Hardware Environment

### 1.1 Devices
| Device | Model | OS | Battery | Notes |
|--------|-------|----|---------|-------|
| Smartphone | Samsung Galaxy S22 | Android 13/14 (One UI 5/6) | 3,700 mAh | Primary phone app host |
| Smartwatch | Samsung Galaxy Watch 8 40mm | Wear OS 5 | ~310 mAh | Wearable sensor host |

### 1.2 Connectivity
| Link | Protocol | Notes |
|------|----------|-------|
| Watch ↔ Phone | Bluetooth LE | Standard Samsung pairing via Galaxy Wearable app |
| Phone ↔ Server | Wi-Fi (stable, 5 GHz preferred) | Isolate cellular network variance; keep signal strong to avoid radio power spikes |

### 1.3 Physical Environment
*   **Charger**: Use the **same** charger and cable for all pre-test charging. Charge to **100%** and let the device sit on the charger for an additional 15 minutes after reaching 100% (to stabilize voltage readings).

## 3. Preparation

### 3.1 Campaign Preparation (Supabase Backend)

EnPULSE uses a campaign-based architecture where sensors are activated based on a `campaign_table` configuration fetched from Supabase. For benchmarking, you need dedicated campaigns with precise sensor configurations.

**Steps:**
1.  **Create Benchmark Campaigns on Supabase Dashboard:**
    *   Create a new campaign for each scenario (e.g., "Benchmark-PhoneOnly", "Benchmark-WatchBio", "Benchmark-AllSensors").
    *   For each campaign, add rows to the `campaign_table` table specifying exactly which sensors to enable. The `name` field in `campaign_table` must match the sensor IDs used in the codebase.

2.  **Phone Sensor IDs** (for `campaign_table.name`):

    | Sensor | ID | Type |
    |--------|----|------|
    | ActivityRecognitionSensor | `activity_recognition_sensor` | Event-driven |
    | AmbientLightSensor | `ambient_light_sensor` | Periodic |
    | AppListChangeSensor | `app_list_change_sensor` | Event-driven |
    | AppUsageLogSensor | `app_usage_log_sensor` | Periodic |
    | BatterySensor | `battery_sensor` | Event-driven |
    | BluetoothScanSensor | `bluetooth_scan_sensor` | Periodic |
    | CallLogSensor | `call_log_sensor` | Event-driven |
    | ConnectivitySensor | `connectivity_sensor` | Event-driven |
    | DataTrafficSensor | `data_traffic_sensor` | Periodic |
    | DeviceModeSensor | `device_mode_sensor` | Event-driven |
    | ExerciseSensor | `exercise_sensor` | Event-driven |
    | LocationSensor | `location_sensor` | Continuous |
    | MediaSensor | `media_sensor` | Event-driven |
    | MessageLogSensor | `message_log_sensor` | Event-driven |
    | NotificationSensor | `notification_sensor` | Event-driven |
    | ScreenSensor | `screen_sensor` | Event-driven |
    | SleepSensor | `sleep_sensor` | Periodic |
    | StepSensor | `step_sensor` | Event-driven |
    | TimingSensor | `timing_sensor` | Periodic |
    | UserInteractionSensor | `user_interaction_sensor` | Event-driven |
    | WifiScanSensor | `wifi_scan_sensor` | Periodic |

3.  **Watch Sensor IDs** (managed via watch settings UI, not campaign_table):

    | Sensor | ID | Type | Requires Skin |
    |--------|----|------|:-------------:|
    | AccelerometerSensor | `accelerometer_sensor` | Continuous (50Hz) | ❌ |
    | PPGSensor | `ppg_sensor` | Continuous | ✅ |
    | HeartRateSensor | `heart_rate_sensor` | Continuous | ✅ |
    | SkinTemperatureSensor | `skin_temperature_sensor` | Periodic | ✅ |
    | EDASensor | `eda_sensor` | Periodic | ✅ |
    | IMUSensor | `imu_sensor` | Continuous (50Hz) | ❌ |
    | GestureSensor | `gesture_sensor` | Continuous | ❌ |
    | StressSensor | `stress_sensor` | Continuous | ✅ |
    | LocationSensor | `location_sensor` | Continuous | ❌ |
    | AudioSensor | `audio_sensor` | Continuous | ❌ |

4.  **Join the Campaign**: On the phone app, log in and join the correct benchmark campaign before starting each scenario.

### 3.2 Device Preparation (Phone — Samsung Galaxy S22)

Perform these steps **once** before the entire benchmark session:

1.  **Factory Reset** (recommended for the cleanest results) or uninstall all unnecessary 3rd-party apps.
2.  **Install EnPULSE** (`app-mobile-tracker`) and `app-benchmark`, granting all requested permissions.
3.  **Settings → Display**:
    *   Screen Timeout: **15 seconds** (the shortest available).
    *   Adaptive Brightness: **OFF** (set brightness to ~30% manually).
4.  **Settings → Battery and Device Care → Battery**:
    *   Adaptive Battery: **OFF**.
    *   Background usage limits → Never sleeping apps: **Add EnPULSE and EnPULSE Benchmark**.
5.  **Settings → Apps → EnPULSE / EnPULSE Benchmark → Battery**:
    *   Set to **Unrestricted**.
6.  **Settings → Connections**:
    *   Wi-Fi: **ON**, connected to benchmark network.
    *   Bluetooth: **ON** for watch scenarios, **OFF** for phone-only scenarios (Scenario B).
    *   Mobile Data: **OFF** (to isolate from cellular radio drain).
    *   NFC: **OFF**.
    *   Airplane Mode: **OFF**.
7.  **Settings → Location**:
    *   Location: **ON**.
    *   Improve Accuracy → Wi-Fi scanning / BT scanning: Leave **ON** (matches realistic usage).
8.  **Samsung Health → Developer Mode**:
    *   Open Samsung Health app → Settings → About Samsung Health → tap the version number **10 times** to enable Developer Mode.
    *   This is **required** for EnPULSE to access Samsung Health SDK sensors (PPG, EDA, HR, SkinTemp, ECG) on the phone side.
9.  **Developer Options**:
    *   Stay Awake: **OFF** (we measure background behavior, not foreground).
    *   USB Debugging: **ON** (for pre/post ADB commands only; cable must be disconnected during the test).

### 3.3 Device Preparation (Watch — Samsung Galaxy Watch 8)

1.  **Factory Reset** (recommended) or remove unnecessary watch faces and apps.
2.  **Install EnPULSE** (`app-wearable-tracker`) and `app-benchmark` via sideload or Galaxy Wearable.
3.  **Settings → Display**:
    *   Screen Timeout: **15 seconds**.
    *   Always On Display (AOD): **OFF**.
    *   Wake-up gesture (raise to wake): **OFF** for shaker tests (avoids waking the screen); **ON** for human-worn tests.
    *   Brightness: Manual, ~30%.
4.  **Settings → Battery**:
    *   Battery Saver / Power Saving Mode: **OFF**.
5.  **Settings → Apps → EnPULSE / EnPULSE Benchmark → Battery**:
    *   Set to **Unrestricted** / Not optimized.
6.  **Settings → Connections**:
    *   Bluetooth: **ON** (paired with the phone).
    *   Wi-Fi: **OFF** on the watch (routes all data through BLE to the phone to measure sync pipeline).
7.  **Health Platform → Developer Mode**:
    *   Open the **Health Platform** app on the watch (not Samsung Health) → tap the version/title area **10 times** to enable Developer Mode.
    *   This is **required** for EnPULSE to access Samsung Health Sensor SDK data (ACC, PPG, HR, EDA, SkinTemp, ECG, Stress) directly on the watch.
8.  **Developer Options** (enabled via Settings → About Watch → tap Build Number 7 times):
    *   ADB Debugging: **ON** (for pre/post data extraction; wireless ADB preferred).
    *   Stay Awake: **OFF**.

### 3.4 Pre-Test Checklist (Run Before EACH Scenario)

| # | Step | Phone | Watch | Notes |
|---|------|:-----:|:-----:|-------|
| 1 | Charge to 100% and wait 15 min | ✅ | ✅ | Stabilizes voltage readings |
| 2 | Close all recent apps | ✅ | ✅ | Swipe away all from recents |
| 3 | Connect via ADB (wireless or USB) | ✅ | ✅ | `adb connect <ip>:<port>` |
| 4 | Reset battery stats | ✅ | ✅ | `adb shell dumpsys batterystats --reset` |
| 5 | Record starting battery level | ✅ | ✅ | `adb shell dumpsys battery \| grep level` |
| 6 | Record starting timestamp | ✅ | ✅ | `date +%s` |
| 7 | **Open app-benchmark** | ✅ | ✅ | Set interval (60s recommended), enter scenario name |
| 8 | **Press "Start" in app-benchmark** | ✅ | ✅ | CSV logging begins immediately |
| 9 | Open EnPULSE, join correct campaign | ✅ | — | Skip for Scenario A (baseline) |
| 10 | Toggle correct sensors ON in UI | ✅ | ✅ | Skip for Scenario A (baseline) |
| 11 | Press "Start Logging" in EnPULSE | ✅ | ✅ | Skip for Scenario A (baseline) |
| 12 | Lock screen, disconnect USB cable | ✅ | ✅ | — |
| 13 | Place on shaker / put on wrist | ✅ | ✅ | Per scenario instructions |
| 14 | Start a timer for the scenario duration | — | — | — |

---

## 4. Benchmarking Scenarios

### Scenario A: Baseline (The Control)

> **Purpose**: Establish the natural drain rate of the OS without EnPULSE running.

**Configuration:**
*   EnPULSE installed but data collection is **NOT** started.
*   Both devices left stationary on a desk (no shaker).

**Duration:** 4 hours.

**Step-by-step execution:**
1.  Complete Pre-Test Checklist Steps 1–8 (start `app-benchmark` on both devices with scenario name `"A_Baseline"`).
2.  **Skip** Steps 9–11 (do NOT start EnPULSE data collection).
3.  Ensure EnPULSE is NOT running (no foreground service notification visible).
4.  Lock both device screens.
5.  Disconnect USB cables.
6.  Leave devices stationary on desk for **4 hours**.
7.  After 4 hours:
    *   Open `app-benchmark` on both devices → Press **"Stop"**.
    *   Connect via ADB and run post-test extraction:
    ```bash
    # Phone
    adb -s <phone_serial> shell dumpsys battery | grep level
    adb -s <phone_serial> shell dumpsys batterystats --checkin > phone_baseline_stats.txt
    adb -s <phone_serial> bugreport > phone_baseline_bugreport.zip
    adb -s <phone_serial> pull /sdcard/Download/EnPULSE/ ./results/A_Baseline/phone/

    # Watch
    adb -s <watch_serial> shell dumpsys battery | grep level
    adb -s <watch_serial> shell dumpsys batterystats --checkin > watch_baseline_stats.txt
    adb -s <watch_serial> pull /sdcard/Download/EnPULSE/ ./results/A_Baseline/watch/
    ```
8.  Record ending battery levels and timestamps.

---

### Scenario B: Typical Mobile Context Study (Phone Only)

> **Purpose**: Evaluate the impact of passive smartphone-based digital phenotyping.

**Configuration:**
*   **Campaign**: "Benchmark-PhoneOnly"
*   **Phone Sensors ON**: `LocationSensor`, `AppUsageLogSensor`, `BatterySensor`, `ScreenSensor`, `StepSensor`, `WifiScanSensor`, `UserInteractionSensor`, `ActivityRecognitionSensor`, `ConnectivitySensor`.
*   **Watch**: Data collection **NOT** started (isolate phone-only drain).
*   **Phone Bluetooth**: **OFF** (no watch sync overhead).
*   **Shaker**: Phone mounted on pedometer shaker.

**Duration:** 8 hours (simulates a typical workday).

**Step-by-step execution:**
1.  Complete full Pre-Test Checklist (`app-benchmark` scenario name: `"B_PhoneOnly"`).
2.  On phone, open EnPULSE → join "Benchmark-PhoneOnly" campaign.
3.  Toggle ON only the sensors listed above.
4.  Go to Phone Settings → Connections → Bluetooth → **OFF**.
5.  Press "Start Logging" on the phone.
6.  Lock screen, disconnect USB cable.
7.  Mount phone on pedometer shaker. Start shaker.
8.  Leave running for **8 hours**.
9.  After 8 hours, stop shaker.
    *   Open `app-benchmark` → Press **"Stop"**.
    *   Open EnPULSE → Press "Stop Logging".
    *   Connect via ADB:
    ```bash
    adb -s <phone_serial> shell dumpsys battery | grep level
    adb -s <phone_serial> shell dumpsys batterystats --checkin > phone_mobile_stats.txt
    adb -s <phone_serial> bugreport > phone_mobile_bugreport.zip
    adb -s <phone_serial> shell dumpsys meminfo kaist.iclab.mobiletracker > phone_mobile_meminfo.txt
    adb -s <phone_serial> pull /sdcard/Download/EnPULSE/ ./results/B_PhoneOnly/phone/
    ```
10. Pull the local database for data yield analysis:
    ```bash
    adb -s <phone_serial> pull /data/data/kaist.iclab.mobiletracker/files/ ./results/B_PhoneOnly/phone_db/
    ```

---

### Scenario C: Wearable Bio-Tracking (Watch + Phone)

> **Purpose**: Evaluate the heavy, continuous bio-signal load typical in emotion or stress detection studies.

**Configuration:**
*   **Watch Sensors ON**: `AccelerometerSensor`, `PPGSensor`, `HeartRateSensor`, `EDASensor`, `SkinTemperatureSensor`.
*   **Phone Sensors ON**: `BatterySensor` only (to measure the cost of receiving BLE data).
*   **Watch**: Worn on a **human wrist** (mandatory for bio-sensors).
*   **Shaker**: **DO NOT USE**.

**Duration:** 4 hours.

**Step-by-step execution:**
1.  Complete full Pre-Test Checklist on both devices (`app-benchmark` scenario name: `"C_BioTracking"`).
2.  On phone, open EnPULSE → ensure campaign has only `BatterySensor` enabled for phone.
3.  On watch, open EnPULSE → toggle ON: Accelerometer, PPG, HeartRate, EDA, SkinTemperature.
4.  Ensure phone Bluetooth is **ON** (watch data syncs via BLE).
5.  Press "Start Logging" on both phone and watch.
6.  Lock phone screen, place phone on desk. Disconnect USB from both devices.
7.  **Wear the watch on your wrist**. Perform normal desk activities (typing, reading, walking occasionally) — do not exercise heavily or remove the watch.
8.  Leave running for **4 hours**.
9.  After 4 hours:
    *   Open `app-benchmark` on both devices → Press **"Stop"**.
    *   Stop EnPULSE logging on both devices.
    *   Connect both devices via ADB:
    ```bash
    # Phone
    adb -s <phone_serial> shell dumpsys battery | grep level
    adb -s <phone_serial> bugreport > phone_bio_bugreport.zip
    adb -s <phone_serial> pull /sdcard/Download/EnPULSE/ ./results/C_BioTracking/phone/

    # Watch
    adb -s <watch_serial> shell dumpsys battery | grep level
    adb -s <watch_serial> shell dumpsys batterystats --checkin > watch_bio_stats.txt
    adb -s <watch_serial> shell dumpsys meminfo kaist.iclab.wearabletracker > watch_bio_meminfo.txt
    adb -s <watch_serial> pull /sdcard/Download/EnPULSE/ ./results/C_BioTracking/watch/
    ```
10. Pull watch database:
    ```bash
    adb -s <watch_serial> pull /data/data/kaist.iclab.wearabletracker/files/ ./results/C_BioTracking/watch_db/
    ```

---

### Scenario D: Extreme Stress Test (All Sensors)

> **Purpose**: Find the absolute upper bound of resource consumption and identify data bottlenecks.

**Configuration:**
*   **ALL** available sensors enabled on both phone and watch simultaneously.
*   **Watch**: Worn on a **human wrist** (bio-sensors are active).
*   **Shaker**: Phone on shaker.

**Duration:** Run until the **smartwatch battery fully drains** (reaches 0%).

**Step-by-step execution:**
1.  Complete full Pre-Test Checklist on both devices (`app-benchmark` scenario name: `"D_StressTest"`).
2.  On phone, join a campaign that enables all phone sensors.
3.  On watch, toggle ON **all** sensors (Accelerometer, PPG, HeartRate, EDA, SkinTemp, IMU, Gesture, Stress, Location, Audio).
4.  Phone Bluetooth: **ON**.
5.  Press "Start Logging" on both devices.
6.  Lock phone, mount on shaker. Wear the watch.
7.  **Note the starting time.**
8.  Continue until the watch powers off on its own (battery = 0%).
9.  **Note the ending time.** Calculate total runtime.
10. Open `app-benchmark` on the **phone** → Press **"Stop"** (watch is dead, its CSV is already saved to Downloads).
11. Stop EnPULSE on the phone.
12. Connect the phone via ADB:
    ```bash
    adb -s <phone_serial> shell dumpsys battery | grep level
    adb -s <phone_serial> bugreport > phone_stress_bugreport.zip
    adb -s <phone_serial> shell dumpsys meminfo kaist.iclab.mobiletracker > phone_stress_meminfo.txt
    adb -s <phone_serial> pull /sdcard/Download/EnPULSE/ ./results/D_StressTest/phone/
    ```
13. Charge the watch enough to boot (plug in for ~5 min), then connect via ADB:
    ```bash
    adb -s <watch_serial> shell dumpsys batterystats --checkin > watch_stress_stats.txt
    adb -s <watch_serial> pull /sdcard/Download/EnPULSE/ ./results/D_StressTest/watch/
    ```
14. Pull databases from both devices for data yield analysis.

> [!TIP]
> Even though the watch died, the **`app-benchmark` CSV is safely stored** in `Downloads/EnPULSE/Benchmark_.../` because it writes rows incrementally. The CSV contains a complete battery drain curve from 100% down to the last reading before shutdown.

---

### Scenario E: Sensor Ablation Studies (Micro-Benchmarks)

> **Purpose**: Isolate the exact cost of individual high-drain components.

**Configuration:** Enable only **one** target sensor at a time against the baseline. Run multiple sub-tests.

**Duration:** 1 hour per sub-test.

**Sub-tests:**

| Sub-test | Target Sensor(s) | Device | Shaker? | Worn? |
|----------|-------------------|--------|:-------:|:-----:|
| E1 | None (BLE keep-alive only) | Watch + Phone | ❌ Desk | ❌ |
| E2 | AccelerometerSensor (50Hz) | Watch | ✅ Shaker | ❌ |
| E3 | IMUSensor (ACC + Gyro 50Hz) | Watch | ✅ Shaker | ❌ |
| E4 | HeartRateSensor | Watch | ❌ | ✅ Worn |
| E5 | PPGSensor | Watch | ❌ | ✅ Worn |
| E6 | EDASensor | Watch | ❌ | ✅ Worn |
| E7 | AudioSensor | Watch | ❌ Desk | ❌ |
| E8 | LocationSensor (GPS) | Phone | ✅ Shaker | — |
| E9 | StressSensor (HR + RMSSD) | Watch | ❌ | ✅ Worn |

**Step-by-step execution (repeat for each sub-test):**
1.  Complete full Pre-Test Checklist (`app-benchmark` scenario name: `"E<N>_<SensorName>"`, e.g., `"E2_Accelerometer"`).
2.  On the target device, toggle ON **only** the sub-test's target sensor in EnPULSE. All others OFF.
3.  Press "Start Logging" in EnPULSE.
4.  Lock screen, disconnect USB.
5.  Place on shaker (if applicable) or wear (if applicable) or leave on desk.
6.  Wait **1 hour**.
7.  Open `app-benchmark` → Press **"Stop"**.
8.  Stop EnPULSE logging.
9.  Connect via ADB and extract:
    ```bash
    adb -s <device_serial> shell dumpsys battery | grep level
    adb -s <device_serial> shell dumpsys batterystats --checkin > ablation_E<N>_stats.txt
    adb -s <device_serial> shell dumpsys meminfo <package> > ablation_E<N>_meminfo.txt
    adb -s <device_serial> pull /sdcard/Download/EnPULSE/ ./results/E<N>/
    ```
10. Pull database for data yield.
11. **Reset battery stats** before the next sub-test:
    ```bash
    adb -s <device_serial> shell dumpsys batterystats --reset
    ```
12. **Delete previous benchmark CSVs** from the device to avoid confusion:
    ```bash
    adb -s <device_serial> shell rm -rf /sdcard/Download/EnPULSE/*
    ```
13. Recharge to 100% before the next sub-test.

---

## 5. Metrics to Report

### A. Resource Metrics (Extracted via `dumpsys batterystats` & `app-benchmark`)
*   **Battery Drain Rate**: Average `% decrease per hour`.
*   **Estimated Lifespan**: Projected total hours of operation from 100% to 0%.
*   **Power Consumption**: Estimated `mAh` consumed by the EnPULSE package.
*   **CPU / Wake-locks**: Time spent holding partial wake-locks (preventing device sleep).
*   **Memory Footprint**: Peak and average `MB` of RAM consumed (from `dumpsys meminfo`).

### B. Reliability Metrics (Extracted via Database queries)
*   **Data Yield Ratio**: `(Actual Rows Stored / Theoretical Max Rows) * 100`.
    *   *Formula for 50Hz ACC over 1 hour*: $50 \times 60 \times 60 = 180,000$ expected points.
    *   *Formula for event-driven sensors*: Compare against a ground-truth log (e.g., manually trigger 10 notifications, check if 10 rows are stored).
*   **Data Continuity**: Maximum gap duration (in seconds) between contiguous data points.
*   **Sync Latency**: Time difference between the sensor timestamp on the watch and the insertion timestamp on the server (requires clock sync or NTP comparison).
*   **Storage Footprint**: Size of local ObjectBox database (in MB) after 1h / 4h / 8h of collection per scenario.

---

## 6. Automated Testing Pipeline

The benchmarking uses a **two-layer** automation strategy to minimize supervision:

### Layer 1: On-Device — `app-benchmark` (Primary Data Source)

The `app-benchmark` app runs independently on both phone and watch during the entire test. It is the **primary source** for battery drain curves because:
*   It logs every interval (default 60s) to a CSV file in `Downloads/EnPULSE/Benchmark_<Scenario>_<Date>/`.
*   It survives even if the watch battery dies completely (rows are written incrementally).
*   It requires zero ADB connectivity during the test.

**What you do:**
1.  Before each scenario: Open `app-benchmark` → set interval (60s) → type scenario name → press **Start**.
2.  Start EnPULSE data collection as described in the scenario steps.
3.  Lock screen, disconnect everything, walk away.
4.  When the test duration is over: Open `app-benchmark` → press **Stop**.

### Layer 2: Post-Test — `pull_benchmark.py` (Extraction & Reporting)

After stopping both `app-benchmark` and EnPULSE, connect via ADB and run:
```bash
python scripts/pull_benchmark.py \
    --phone <phone_serial> \
    --watch <watch_serial> \
    --scenario "C_BioTracking"
```

The script automatically:
1.  Pulls benchmark CSVs from `Downloads/EnPULSE/` on both devices.
2.  Pulls `dumpsys batterystats --checkin` from both devices.
3.  Pulls `dumpsys meminfo` for EnPULSE packages.
4.  Pulls `bugreport.zip` for Battery Historian analysis.
5.  Generates a summary CSV: `results/<scenario>/summary.csv` with columns:
    *   `device`, `start_battery`, `end_battery`, `drain_rate_pct_per_hr`, `duration_hrs`, `avg_cpu_pct`, `avg_memory_mb`
6.  Prints a formatted results table to the terminal.

### Workflow Summary per Scenario

| Phase | Action | Supervision Required |
|-------|--------|:--------------------:|
| **Setup** (~5 min) | Charge, reset stats, start app-benchmark, start EnPULSE, lock screen | ✅ Manual |
| **Test** (1–8 hours) | Devices running unattended | ❌ None |
| **Extraction** (~5 min) | Stop apps, run `pull_benchmark.py` | ✅ Manual |

---

## 7. Post-Test Data Analysis

### 7.1 Battery Analysis
*   **Primary**: Parse the `app-benchmark` CSVs to plot battery drain curves over time. Each row has timestamp + battery level + voltage + current, giving a high-resolution drain timeline.
*   **Secondary**: Upload `bugreport.zip` files to [Google Battery Historian](https://github.com/google/battery-historian) to generate detailed per-app power consumption breakdowns (mAh, wake-lock counts, radio usage).

### 7.2 End-to-End Data Yield Analysis
*   Query the **Supabase (PostgreSQL)** backend directly using `scripts/yield_calculator.py`.
*   Run `python scripts/yield_calculator.py --url <SUPABASE_URL> --key <SERVICE_KEY> --campaign <CAMPAIGN_ID> --start <START_TIME> --end <END_TIME>`
*   The script queries each sensor table (`accelerometer_sensor`, `ppg_sensor`, etc.) for row counts and timestamp deltas.
*   **Yield Calculation**: Compare actual recorded rows against theoretical maximums based on sensor sampling frequency × duration (e.g., $50\text{Hz} \times 3600\text{s} = 180,000$ points).
*   **Continuity & Gap Detection**: Identify sample gaps exceeding $2\times$ expected sampling period ($> 40\text{ms}$ for 50Hz).
*   **End-to-End Latency**: Compute average sync latency (server ingestion timestamp vs sensor event timestamp).

### 7.3 Results Presentation (for the Paper)
*   **Table**: Battery drain rates across all scenarios (A through E).
*   **Line Chart**: Battery drain curves over time from `app-benchmark` CSVs (one line per scenario).
*   **Bar Chart**: Ablation study showing per-sensor battery cost (Scenario E sub-tests).
*   **Table**: Data yield percentages per sensor per scenario.
*   **Timeline Chart**: Detailed per-component breakdown from Battery Historian.
