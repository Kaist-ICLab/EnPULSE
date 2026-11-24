# EnPULSE Mobile Benchmark (`app-mobile-benchmark`)

A standalone, lightweight Android module designed specifically for benchmarking device resource
consumption (battery, CPU, memory, thermal state) during the EnPULSE system evaluation.

## Why a standalone app?

By decoupling the metrics collection from the core `tracker-library`, we ensure that the
benchmarking tools themselves do not artificially inflate power consumption or cause data
bottlenecks. It uses standard Android APIs with zero external third-party dependencies.

## Features

* **Zero-Supervision Logging**: Runs securely in the background via a `ForegroundService` with a
  partial `WakeLock`. You can lock the screen, disconnect ADB, and walk away.
* **Crash Resilience**: Writes data incrementally to a CSV file. If the device's battery completely
  dies during an extreme stress test, the CSV up to the point of shutdown is perfectly preserved.
* **Detailed Metrics**: Captures high-resolution power and hardware statistics (battery current, voltage, charge uAh, energy nWh, thermal status, CPU usage & temp, PSS RAM, native heap).
* **Pause & Resume Support**: Pause and resume tests without splitting log files.
* **Auto-Stop Timer**: Configurable duration auto-stop with sound notification alerts.
* **Auto-Summary**: Automatically calculates drain rates, peak memory/temperature, and total duration when stopped.

## What is Recorded

For every sampling interval, a new row is appended to `metrics.csv` containing:

1. **Timestamp (ISO-8601 & epoch ms)**: `timestamp_iso`, `timestamp_ms`.
2. **Battery Metrics**: Level (`%`), Voltage (`mV`), Temperature (`°C`), Current (`mA`, negative = discharging), Operating Status (`charging`, `discharging`, etc.), Remaining Charge (`µAh`), Remaining Energy (`nWh`).
3. **Thermal Status**: System-wide OS thermal throttling status (API 29+).
4. **CPU Usage & Temp**: System-wide CPU usage percentage (from `/proc/stat`) and SoC/CPU temperature (`°C` from sysfs).
5. **Memory Footprint**: Process memory PSS (`MB`), System Available RAM (`MB`), Native Heap allocated (`Bytes`).

## Usage Guide

### 1. Installation

Build and install via ADB to your phone:

```bash
./gradlew :app-mobile-benchmark:installDebug
```

Or deploy across multiple phones automatically using Fleet Manager:

```bash
python3 scripts/fleet_manager.py install
```

### 2. Running a Test

1. Open the **EnPULSE Benchmark** app.
2. Grant unrestricted battery permissions by tapping **🔋 Request Unrestricted Battery** (recommended on first run).
3. Enter the **Sampling Interval** in seconds (default is `60`).
4. Enter the **Duration** in minutes (`0` for indefinite/manual stop).
5. Enter the **Scenario Name** exactly as written in the experimental protocol (e.g., `A_Baseline`, `C_BioTracking`).
6. Press **▶ Start**.
7. The app will show a persistent notification and begin background logging.
8. When the scenario is complete, press **■ Stop**.

### 3. Data Extraction

The app uses the `MediaStore` API to save data to the public `Downloads` directory:

Each test run creates a new timestamped folder:

```text
/sdcard/Download/EnPULSE/phone-<DeviceModel>_<ScenarioName>_<Timestamp>/
├── metrics.csv
└── summary.txt
```

To extract data from all connected devices in parallel, use **Fleet Manager**:

```bash
python3 scripts/fleet_manager.py pull
```

Alternatively, pull data manually via ADB:

```bash
adb pull /sdcard/Download/EnPULSE/ ./
```
