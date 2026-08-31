# EnPULSE Watch Benchmark (`app-wearable-benchmark`)

A standalone, lightweight Wear OS module designed specifically for benchmarking watch resource consumption (battery, CPU, and memory) during the EnPULSE system evaluation.

## Why a standalone app?

By decoupling the metrics collection from the core `tracker-library`, we ensure that the benchmarking tools themselves do not artificially inflate power consumption or cause data bottlenecks. It uses standard Android APIs with a Jetpack Compose UI tailored for Wear OS.

## Features

* **Zero-Supervision Logging**: Runs securely in the background via a `ForegroundService` with a partial `WakeLock`. You can walk away while it collects data.
* **Crash Resilience**: Writes data incrementally to a CSV file.
* **Detailed Metrics**: Captures high-resolution power and hardware statistics specific to the wearable device.
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

### 2. Running a Test

1. Open the **EnPULSE Benchmarker** app on your Wear OS device.
2. Enter the **Scenario Name** (e.g., `A_Baseline`).
3. Enter the **Interval** in seconds (default is `60`).
4. Enter the **Duration** in minutes (default is `0` for infinite).
5. Tap the **Play (▶)** button to run.
6. The app will begin background logging and show a persistent notification.
7. To finish early or save the current session, tap the **Stop (■)** button.

### 3. Data Extraction

On Wear OS, `MediaStore` is often restricted or unreliable. Thus, data is saved directly to the app's external files directory.

Each test run creates a new timestamped folder containing `metrics.csv` and `summary.txt`.

To extract this data, use ADB to pull the folder to your host PC:

```bash
adb pull /sdcard/Android/data/kaist.iclab.benchmark.wearable/files/Benchmarks/ ./
```
