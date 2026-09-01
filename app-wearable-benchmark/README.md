# EnPULSE Watch Benchmark (`app-wearable-benchmark`)

A standalone, lightweight Wear OS module designed specifically for benchmarking watch resource
consumption (battery, CPU, and memory) during the EnPULSE system evaluation.

## Why a standalone app?

By decoupling the metrics collection from the core `tracker-library`, we ensure that the
benchmarking tools themselves do not artificially inflate power consumption or cause data
bottlenecks. It uses standard Android APIs with a Jetpack Compose UI tailored for Wear OS.

## Features

* **Zero-Supervision Logging**: Runs securely in the background via a `ForegroundService` with a
  partial `WakeLock`. You can walk away while it collects data.
* **Crash Resilience**: Writes data incrementally to a CSV file.
* **Detailed Metrics**: Captures high-resolution power and hardware statistics specific to the
  wearable device.
* **Auto-Sync to Phone**: When the benchmark completes or is stopped, data is automatically zipped and sent wirelessly to the paired phone (`app-mobile-benchmark`).
* **Auto-Summary**: Automatically calculates drain rates and total duration when stopped.

## What is Recorded

For every sampling interval, a new row is appended to `metrics.csv` containing:

1. **Timestamp**: Both ISO-8601 string and epoch milliseconds.
2. **Battery Level**: 0-100% capacity.
3. **Battery Voltage**: Raw voltage in millivolts (mV).
4. **Battery Temperature**: Internal temperature in °C.
5. **Battery Current**: Instantaneous current in milliamperes (mA).
6. **Battery Status**: Operating status (e.g., `charging`, `discharging`, `full`).
7. **Battery Charge**: Remaining charge in microampere-hours (µAh).
8. **Battery Energy**: Remaining energy in nanowatt-hours (nWh).
9. **Thermal Status**: System thermal status.
10. **CPU Usage**: System-wide CPU usage percentage.
11. **CPU Temperature**: Thermal temperature in °C.
12. **App Memory (PSS)**: Total RAM footprint of the benchmark process in MB.
13. **Available System RAM**: Total free RAM left on the device in MB.
14. **Native Heap**: Native heap allocation in bytes.

## Usage Guide

### 1. Installation

Build and install via ADB to your watch:

```bash
./gradlew :app-wearable-benchmark:installDebug
```

Or deploy across multiple watches automatically using Fleet Manager:

```bash
python3 scripts/fleet_manager.py install
```

### 2. Running a Test

1. Open the **EnPULSE Benchmarker** app on your Wear OS device.
2. Enter the **Scenario Name** (e.g., `A_Baseline`).
3. Enter the **Interval** in seconds (default is `60`).
4. Enter the **Duration** in minutes (default is `0` for infinite).
5. Tap the **Play (▶)** button to run.
6. The app will begin background logging and show a persistent notification.
7. To finish early or save the current session, tap the **Stop (■)** button.

### 3. Data Extraction & Automatic Sync

#### 🔄 Automatic Sync to Phone
When the benchmark test finishes (either upon reaching target duration or when tapping **Stop**), the watch automatically zips the session data folder and sends it wirelessly to the connected smartphone (`app-mobile-benchmark`) over Google Wearable `ChannelClient`. The phone receives and unzips the files into `/sdcard/Download/EnPULSE/`.

#### 💾 Local Watch Directory
Local backup copies remain on the watch in the app's external files directory:

```text
/sdcard/Android/data/kaist.iclab.benchmark.wearable/files/Benchmarks/watch-<DeviceModel>_<ScenarioName>_<Timestamp>/
├── metrics.csv
└── summary.txt
```

To extract data from all connected devices in parallel, use **Fleet Manager**:

```bash
python3 scripts/fleet_manager.py pull
```

Alternatively, pull data manually via ADB:

```bash
adb pull /sdcard/Android/data/kaist.iclab.benchmark.wearable/files/Benchmarks/ ./
```
