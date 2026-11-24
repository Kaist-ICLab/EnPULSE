#!/usr/bin/env python3
"""
pull_benchmark.py — Post-test extraction script for EnPULSE benchmarking.

Pulls benchmark CSVs and EnPULSE's own energy, CPU, memory and storage
(see enpulse_stats.py) from phone and/or watch, then generates a summary report.

Usage:
    # Start of run, after unplugging: clear batterystats and procstats
    python scripts/tools/pull_benchmark.py --phone <serial> --watch <serial> --reset

    # End of run
    python scripts/tools/pull_benchmark.py --phone <serial> --watch <serial> --scenario C_BioTracking
    python scripts/tools/pull_benchmark.py --phone <serial> --scenario B_PhoneOnly
    python scripts/tools/pull_benchmark.py --watch <serial> --scenario E2_Accelerometer
"""

import argparse
import csv
import os
import subprocess
import sys
from datetime import datetime
from pathlib import Path

import enpulse_stats


BENCHMARK_PATH_ON_DEVICE = "/sdcard/Download/EnPULSE/"
# The phone and watch apps share one application ID.
PHONE_PACKAGE = "kaist.iclab.trackerSystem"
WATCH_PACKAGE = "kaist.iclab.trackerSystem"


def run_adb(serial: str, *args: str) -> str:
    """Run an ADB command and return stdout."""
    cmd = ["adb"]
    if serial and serial != "default":
        cmd += ["-s", serial]
    cmd += list(args)
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if result.returncode != 0:
        print(f"  ⚠ ADB error: {result.stderr.strip()}", file=sys.stderr)
    return result.stdout


def pull_dir(serial: str, remote_path: str, local_path: str):
    """Pull a directory from the device."""
    os.makedirs(local_path, exist_ok=True)
    cmd = ["adb"]
    if serial and serial != "default":
        cmd += ["-s", serial]
    cmd += ["pull", remote_path, local_path]
    subprocess.run(cmd, capture_output=True, text=True, timeout=300)


def get_battery_level(serial: str) -> int:
    """Get current battery level."""
    output = run_adb(serial, "shell", "dumpsys", "battery")
    for line in output.splitlines():
        if "level:" in line:
            return int(line.split(":")[1].strip())
    return -1


def get_meminfo(serial: str, package: str) -> str:
    """Get memory info for a package."""
    return run_adb(serial, "shell", "dumpsys", "meminfo", package)


def pull_bugreport(serial: str, output_file: str):
    """Pull a bugreport zip."""
    cmd = ["adb"]
    if serial and serial != "default":
        cmd += ["-s", serial]
    cmd += ["bugreport", output_file]
    subprocess.run(cmd, capture_output=True, text=True, timeout=600)


def analyze_benchmark_csv(csv_path: str) -> dict:
    """Parse a benchmark CSV and compute summary metrics."""
    rows = []
    with open(csv_path, "r") as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)

    if not rows:
        return {"error": "Empty CSV"}

    first = rows[0]
    last = rows[-1]

    start_battery = int(first["battery_level_pct"])
    end_battery = int(last["battery_level_pct"])
    start_ms = int(first["timestamp_ms"])
    end_ms = int(last["timestamp_ms"])
    duration_hrs = (end_ms - start_ms) / (1000 * 3600)

    drain_rate = (start_battery - end_battery) / duration_hrs if duration_hrs > 0 else 0

    # Memory of the benchmark app itself, not EnPULSE (see enpulse_stats for EnPULSE's).
    mem_values = [float(r["app_memory_mb"]) for r in rows if float(r["app_memory_mb"]) >= 0]

    return {
        "start_battery": start_battery,
        "end_battery": end_battery,
        "duration_hrs": round(duration_hrs, 2),
        "drain_rate_pct_per_hr": round(drain_rate, 2),
        "avg_benchmark_memory_mb": round(sum(mem_values) / len(mem_values), 1) if mem_values else -1,
        "data_points": len(rows),
    }


def extract_device(serial: str, device_name: str, scenario: str, output_dir: str, package: str):
    """Extract all data from a single device."""
    device_dir = os.path.join(output_dir, device_name)
    os.makedirs(device_dir, exist_ok=True)

    print(f"\n{'=' * 50}")
    print(f"  Extracting from {device_name} ({serial})")
    print(f"{'=' * 50}")

    # 1. Current battery level
    level = get_battery_level(serial)
    print(f"  Current battery: {level}%")

    # 2. Pull benchmark CSVs
    print(f"  Pulling benchmark CSVs...")
    csv_dir = os.path.join(device_dir, "csvs")
    pull_dir(serial, BENCHMARK_PATH_ON_DEVICE, csv_dir)

    # 3. EnPULSE energy, CPU, memory and storage
    print(f"  Pulling EnPULSE stats (batterystats, procstats, storage)...")
    stats = enpulse_stats.collect(serial, os.path.join(device_dir, "enpulse_stats"), package)
    print(enpulse_stats.format_summary(stats))

    # 4. Memory snapshot at pull time (procstats above covers the whole run)
    print(f"  Pulling memory info...")
    meminfo = get_meminfo(serial, package)
    with open(os.path.join(device_dir, "meminfo.txt"), "w") as f:
        f.write(meminfo)

    # 5. Bugreport (optional, can be slow)
    print(f"  Pulling bugreport (this may take a minute)...")
    pull_bugreport(serial, os.path.join(device_dir, f"bugreport_{device_name}.zip"))

    # 6. Analyze CSVs
    csv_files = list(Path(csv_dir).rglob("*.csv"))
    analyses = {}
    for csv_file in csv_files:
        print(f"  Analyzing: {csv_file.name}")
        analyses[csv_file.name] = analyze_benchmark_csv(str(csv_file))

    return analyses, stats


def print_summary(scenario: str, results: dict):
    """Print a formatted summary table."""
    print(f"\n{'=' * 70}")
    print(f"  BENCHMARK SUMMARY: {scenario}")
    print(f"{'=' * 70}")
    print(f"{'Device':<10} {'File':<40} {'Start%':>6} {'End%':>5} {'Hrs':>5} {'%/hr':>6} {'Bench MB':>8}")
    print("-" * 70)

    for device_name, analyses in results.items():
        for filename, data in analyses.items():
            if "error" in data:
                print(f"{device_name:<10} {filename:<40} ERROR: {data['error']}")
                continue
            print(
                f"{device_name:<10} {filename:<40} "
                f"{data['start_battery']:>5}% "
                f"{data['end_battery']:>4}% "
                f"{data['duration_hrs']:>5} "
                f"{data['drain_rate_pct_per_hr']:>5.1f} "
                f"{data['avg_benchmark_memory_mb']:>8.1f}"
            )


def write_summary_csv(scenario: str, results: dict, output_dir: str):
    """Write a machine-readable summary CSV."""
    csv_path = os.path.join(output_dir, "summary.csv")
    with open(csv_path, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow([
            "scenario", "device", "csv_file", "start_battery_pct", "end_battery_pct",
            "duration_hrs", "drain_rate_pct_per_hr", "avg_benchmark_memory_mb", "data_points"
        ])
        for device_name, analyses in results.items():
            for filename, data in analyses.items():
                if "error" in data:
                    continue
                writer.writerow([
                    scenario, device_name, filename,
                    data["start_battery"], data["end_battery"],
                    data["duration_hrs"], data["drain_rate_pct_per_hr"],
                    data["avg_benchmark_memory_mb"], data["data_points"],
                ])
    print(f"\n  Summary CSV saved to: {csv_path}")


ENPULSE_COLUMNS = [
    ("batterystats", "estimated_mah"), ("batterystats", "cpu_total_s"),
    ("batterystats", "cpu_user_ms"), ("batterystats", "cpu_system_ms"),
    ("batterystats", "partial_wakelock_ms"), ("batterystats", "partial_wakelock_count"),
    ("batterystats", "wakeup_alarms"), ("batterystats", "gps_ms"),
    ("batterystats", "wifi_rx_bytes"), ("batterystats", "wifi_tx_bytes"),
    ("batterystats", "mobile_rx_bytes"), ("batterystats", "mobile_tx_bytes"),
    ("batterystats", "bt_rx_bytes"), ("batterystats", "bt_tx_bytes"),
    ("procstats", "pss_avg_mb"), ("procstats", "pss_max_mb"), ("procstats", "rss_avg_mb"),
    ("procstats", "rss_max_mb"), ("procstats", "samples"),
    ("memory_log", "pss_avg_mb"), ("memory_log", "pss_max_mb"),
    ("memory_log", "native_heap_avg_mb"), ("memory_log", "native_heap_max_mb"),
    ("memory_log", "java_heap_avg_mb"), ("memory_log", "java_heap_max_mb"), ("memory_log", "samples"),
    ("storage", "data_mb"), ("storage", "objectbox_mb"), ("storage", "method"),
]


def write_enpulse_csv(scenario: str, stats_by_device: dict, output_dir: str):
    """One row per device with EnPULSE's own energy, CPU, memory and storage."""
    csv_path = os.path.join(output_dir, "enpulse_summary.csv")
    with open(csv_path, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["scenario", "device"] + [f"{group}_{key}" for group, key in ENPULSE_COLUMNS])
        for device_name, stats in stats_by_device.items():
            row = [scenario, device_name]
            for group, key in ENPULSE_COLUMNS:
                value = (stats.get(group) or {}).get(key)
                row.append("" if value is None else value)
            writer.writerow(row)
    print(f"  EnPULSE summary CSV saved to: {csv_path}")


def main():
    parser = argparse.ArgumentParser(description="EnPULSE Benchmark Data Extractor")
    parser.add_argument("--phone", help="Phone ADB serial (e.g., 192.168.1.5:5555)")
    parser.add_argument("--watch", help="Watch ADB serial")
    parser.add_argument("--scenario", help="Scenario name (e.g., C_BioTracking); required unless --reset")
    parser.add_argument("--reset", action="store_true",
                        help="Start of run: clear batterystats and procstats, then exit. Unplug first.")
    parser.add_argument("--output", default="results", help="Output directory (default: results/)")
    args = parser.parse_args()

    if not args.phone and not args.watch:
        parser.error("At least one of --phone or --watch must be specified")

    if args.reset:
        for label, serial in (("phone", args.phone), ("watch", args.watch)):
            if not serial:
                continue
            print(f"Resetting stats on {label} ({serial})...")
            for step, ok, msg in enpulse_stats.reset(serial):
                print(f"  {'✅' if ok else '⚠'} {step}: {msg or 'no output'}")
        print("\nDone. Keep the devices unplugged and start the run now.")
        return

    if not args.scenario:
        parser.error("--scenario is required unless --reset is given")

    output_dir = os.path.join(args.output, args.scenario)
    os.makedirs(output_dir, exist_ok=True)

    print(f"EnPULSE Benchmark Extractor")
    print(f"Scenario: {args.scenario}")
    print(f"Output:   {output_dir}")
    print(f"Time:     {datetime.now().isoformat()}")

    results = {}
    stats = {}

    if args.phone:
        results["phone"], stats["phone"] = extract_device(
            args.phone, "phone", args.scenario, output_dir, PHONE_PACKAGE
        )

    if args.watch:
        results["watch"], stats["watch"] = extract_device(
            args.watch, "watch", args.scenario, output_dir, WATCH_PACKAGE
        )

    print_summary(args.scenario, results)
    write_summary_csv(args.scenario, results, output_dir)
    write_enpulse_csv(args.scenario, stats, output_dir)

    print(f"\n✅ Done! All data saved to: {output_dir}/")


if __name__ == "__main__":
    main()
