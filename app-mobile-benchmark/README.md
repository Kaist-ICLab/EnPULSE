# EnPULSE Benchmark (`app-benchmark`)

A standalone, lightweight Android module designed specifically for benchmarking device resource consumption (battery, CPU, and memory) during the EnPULSE system evaluation.

## Why a standalone app?
By decoupling the metrics collection from the core `tracker-library`, we ensure that the benchmarking tools themselves do not artificially inflate the power consumption or cause data bottlenecks. It uses only standard Android APIs with absolutely zero external dependencies.

## Features
*   **Zero-Supervision Logging**: Runs securely in the background via a `ForegroundService` with a partial `WakeLock`. You can lock the screen, disconnect ADB, and walk away.
*   **Crash Resilience**: Writes data incrementally to a CSV file. If the device's battery completely dies during an extreme stress test, the CSV up to the point of shutdown is perfectly preserved.
*   **Detailed Metrics**: Captures high-resolution power and hardware statistics.
*   **Auto-Summary**: Automatically calculates drain rates and total duration when stopped.

## What is Recorded
For every sampling interval, a new row is appended to `metrics.csv` containing:
1.  **Timestamp**: Both ISO-8601 string and epoch milliseconds.
2.  **Battery Level**: 0-100% capacity.
3.  **Battery Voltage**: Raw voltage in millivolts (mV).
4.  **Battery Temperature**: Internal temperature in °C.
5.  **Battery Current**: Instantaneous current in milliamperes (mA). Negative values indicate the device is discharging.
6.  **Battery Status**: Operating status (e.g., `charging`, `discharging`, `full`).
7.  **CPU Usage**: System-wide CPU usage percentage (parsed directly from `/proc/stat`).
8.  **App Memory (PSS)**: Total RAM footprint of the benchmark process in MB.
9.  **Available System RAM**: Total free RAM left on the device in MB.

## Usage Guide

### 1. Installation
Build and install via ADB to your phone and/or watch:
```bash
./gradlew :app-benchmark:installDebug
```

### 2. Running a Test
1.  Open the **EnPULSE Benchmark** app.
2.  Enter the **Sampling Interval** in seconds (default is `60`).
3.  Enter the **Scenario Name** exactly as written in the experimental protocol (see [BENCHMARKING_PROTOCOL.md](../BENCHMARKING_PROTOCOL.md), e.g., `A_Baseline`, `C_BioTracking`).
4.  Press **▶ Start**.
5.  The app will show a persistent notification and begin background logging. The UI will show a live view of the battery drain (e.g., `90% (Dropped: 5%)`).
6.  When the scenario is complete, press **■ Stop**.

### 3. Data Extraction
The app uses the `MediaStore` API to bypass Android 10+ scoped storage restrictions, allowing it to save data to the public `Downloads` directory without requiring root access.

Each test run creates a new timestamped folder:
```text
/sdcard/Download/EnPULSE/Benchmark_<ScenarioName>_<Date>/
├── metrics.csv
└── summary.txt
```

To extract this data, use the Python script located in the repository's `scripts/` directory:
```bash
# On your host PC:
python scripts/pull_benchmark.py --phone <serial> --watch <serial> --scenario "C_BioTracking"
```
The script will pull the CSVs, grab the `dumpsys batterystats`, and generate a consolidated report.
