#!/usr/bin/env python3
"""
enpulse_stats.py — EnPULSE's own energy, CPU, memory and storage, read over ADB.

The benchmark apps can only see device-wide battery numbers and their own memory.
Android keeps per-app statistics itself, so these are read from the device:

  1. Energy and CPU   dumpsys batterystats   (estimated mAh, CPU time, wake locks,
                                               wake-up alarms, network bytes, GPS time)
  2. Memory           dumpsys procstats      (PSS / USS / RSS min, average and max)
  3. Storage          run-as ... du          (app data size and ObjectBox size)
  4. Benchmark runs   EnPULSE's built-in recorder (battery and EnPULSE memory while
                                               collection runs; pradipta/testing builds only)

Workflow for each run:
  1. Unplug the device (batterystats stops counting while charging).
  2. Reset:   python scripts/tools/enpulse_stats.py reset   --serial <serial>
  3. Start collection in EnPULSE (set the scenario under Settings > Benchmark), run it.
  4. Collect: python scripts/tools/enpulse_stats.py collect --serial <serial> --out <dir>

pull_benchmark.py and fleet_manager.py call reset() and collect() for you.
Raw dumps are always saved, so anything the parser misses can be read later.
"""

import argparse
import csv
import glob
import json
import os
import re
import subprocess
import sys
from datetime import datetime

PACKAGE = "kaist.iclab.trackerSystem"  # Phone and watch apps share this ID.
OBJECTBOX_DIR = "files/objectbox"  # MyObjectBox default location inside the app data dir.
# BenchmarkRecorder runs. The phone writes to Downloads, next to the phone benchmark app's runs;
# the watch keeps them in the app's own folder.
PHONE_BENCHMARK_DIR = "/sdcard/Download/EnPULSE"
WATCH_BENCHMARK_DIR = "/sdcard/Android/data/%s/files/EnPULSE-Benchmark" % PACKAGE
BENCHMARK_DIRS = [PHONE_BENCHMARK_DIR, WATCH_BENCHMARK_DIR]
PROCSTATS_HOURS = 24  # Procstats aggregation window; runs longer than this need a larger value.

GPS_SENSOR_ID = "-10000"  # BatteryStats.Uid.Sensor.GPS
# BenchmarkRecorder's own wake lock: counted apart, so partial_wakelock_ms stays EnPULSE's own.
RECORDER_WAKE_LOCK = "EnPULSE:BenchmarkRecorder"


def adb(serial, *args, timeout=120):
    """Runs an ADB command. Returns (ok, stdout, stderr)."""
    cmd = ["adb"]
    if serial and serial != "default":
        cmd += ["-s", serial]
    cmd += list(args)
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        return res.returncode == 0, res.stdout, res.stderr.strip()
    except subprocess.TimeoutExpired:
        return False, "", "Timeout"


# --- Reset (start of run) ---

def reset(serial):
    """
    Clears batterystats and procstats, so the end-of-run collection covers this run
    only. Benchmark runs are kept: each is its own folder. Returns a list of
    (step, ok, message).
    """
    results = []
    ok, out, err = adb(serial, "shell", "dumpsys", "batterystats", "--reset")
    results.append(("batterystats", ok and "reset" in out.lower(), (out or err).strip()))
    ok, out, err = adb(serial, "shell", "dumpsys", "procstats", "--clear")
    results.append(("procstats", ok and "clear" in out.lower(), (out or err).strip()))
    return results


# --- Collect (end of run) ---

def get_uid(serial, package=PACKAGE):
    """Returns the app's Linux UID as a string, or None if not installed."""
    ok, out, _ = adb(serial, "shell", "pm", "list", "packages", "-U", package)
    for line in out.splitlines():
        m = re.match(r"package:(\S+)\s+uid:(\d+)", line.strip())
        if m and m.group(1) == package:
            return m.group(2)
    return None


def _num(value):
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def parse_batterystats_checkin(text, uid):
    """
    Extracts the app's lines from `dumpsys batterystats --checkin`.
    Lines look like: 9,<uid>,l,<section>,<fields...>
    """
    stats = {
        "estimated_mah": None,
        "cpu_user_ms": None,
        "cpu_system_ms": None,
        "partial_wakelock_ms": 0,
        "partial_wakelock_count": 0,
        "recorder_wakelock_ms": 0,
        "wakeup_alarms": 0,
        "gps_ms": 0,
        "mobile_rx_bytes": None,
        "mobile_tx_bytes": None,
        "wifi_rx_bytes": None,
        "wifi_tx_bytes": None,
        "bt_rx_bytes": None,
        "bt_tx_bytes": None,
    }
    found = False
    for line in text.splitlines():
        f = line.strip().split(",")
        if len(f) < 5 or f[1] != uid or f[2] != "l":
            continue
        found = True
        section, data = f[3], f[4:]

        if section == "pwi" and data and data[0] == "uid":
            mah = _num(data[1]) if len(data) > 1 else None
            if mah is not None:
                stats["estimated_mah"] = (stats["estimated_mah"] or 0) + mah
        elif section == "cpu" and len(data) >= 2:
            stats["cpu_user_ms"] = _num(data[0])
            stats["cpu_system_ms"] = _num(data[1])
        elif section == "wl":
            # wl,<name>,<full_ms>,f,<count>,<partial_ms>,p,<count>,...
            if "p" in data[1:]:
                i = data.index("p", 1)
                if data[0] == RECORDER_WAKE_LOCK:
                    stats["recorder_wakelock_ms"] += _num(data[i - 1]) or 0
                    continue
                stats["partial_wakelock_ms"] += _num(data[i - 1]) or 0
                if i + 1 < len(data):
                    stats["partial_wakelock_count"] += int(_num(data[i + 1]) or 0)
        elif section == "wua" and len(data) >= 2:
            stats["wakeup_alarms"] += int(_num(data[-1]) or 0)
        elif section == "sr" and len(data) >= 2 and data[0] == GPS_SENSOR_ID:
            stats["gps_ms"] += _num(data[1]) or 0
        elif section == "nt":
            names = ["mobile_rx_bytes", "mobile_tx_bytes", "wifi_rx_bytes", "wifi_tx_bytes"]
            for name, value in zip(names, data[:4]):
                stats[name] = _num(value)
            if len(data) >= 12:
                stats["bt_rx_bytes"] = _num(data[10])
                stats["bt_tx_bytes"] = _num(data[11])

    if not found:
        return None
    if stats["cpu_user_ms"] is not None and stats["cpu_system_ms"] is not None:
        stats["cpu_total_s"] = round((stats["cpu_user_ms"] + stats["cpu_system_ms"]) / 1000.0, 1)
    return stats


def parse_estimated_mah_text(text, uid):
    """Fallback: 'UID u0a123: 12.3 ( cpu=...' from the human-readable batterystats dump."""
    app_id = int(uid) % 100000 - 10000
    m = re.search(r"UID u0a%d:\s*([\d.]+)" % app_id, text)
    return float(m.group(1)) if m else None


_SIZE = re.compile(r"([\d.]+)\s*([KMG])B?", re.IGNORECASE)


def _to_mb(token):
    m = _SIZE.fullmatch(token.strip())
    if not m:
        return None
    value, unit = float(m.group(1)), m.group(2).upper()
    return round(value / 1024.0 if unit == "K" else value * 1024.0 if unit == "G" else value, 1)


def parse_procstats(text):
    """
    Reads the TOTAL line of the app's process, e.g.
      TOTAL: 100% (45MB-50MB-55MB/40MB-45MB-50MB/60MB-65MB-70MB over 12)
    Groups are PSS / USS / RSS, each as min-avg-max. Older Android has no RSS group.
    """
    m = re.search(r"TOTAL:\s*[\d.]+%\s*\(([^)]*)\)", text)
    if not m:
        return None
    body = m.group(1)
    samples = None
    over = re.search(r"over\s+(\d+)", body)
    if over:
        samples = int(over.group(1))
        body = body[:over.start()].strip()

    result = {"samples": samples}
    for name, group in zip(["pss", "uss", "rss"], body.split("/")):
        parts = group.strip().split("-")
        if len(parts) == 3:
            for label, token in zip(["min", "avg", "max"], parts):
                result[f"{name}_{label}_mb"] = _to_mb(token)
    return result if len(result) > 1 else None


def get_storage(serial, package=PACKAGE):
    """
    App data size and ObjectBox size via run-as (needs a debuggable build).
    Falls back to `dumpsys diskstats`, which the system refreshes about once a day.
    """
    storage = {"method": None, "data_mb": None, "objectbox_mb": None}
    raw = []

    ok, out, err = adb(serial, "shell", "run-as", package, "du", "-sk", ".", OBJECTBOX_DIR)
    raw.append("$ run-as %s du -sk . %s\n%s%s" % (package, OBJECTBOX_DIR, out, err))
    sizes = {}
    for line in out.splitlines():
        parts = line.split()
        if len(parts) == 2 and parts[0].isdigit():
            sizes[parts[1]] = round(int(parts[0]) / 1024.0, 1)
    if "." in sizes:
        storage.update(method="run-as", data_mb=sizes["."], objectbox_mb=sizes.get(OBJECTBOX_DIR))
        return storage, "\n".join(raw)

    ok, out, err = adb(serial, "shell", "dumpsys", "diskstats")
    raw.append("$ dumpsys diskstats\n%s%s" % (out, err))
    names = re.search(r"Package Names: (\[.*\])", out)
    data = re.search(r"App Data Sizes: (\[.*\])", out)
    if names and data:
        try:
            idx = json.loads(names.group(1)).index(package)
            storage.update(
                method="diskstats (may be up to a day old)",
                data_mb=round(json.loads(data.group(1))[idx] / (1024.0 * 1024.0), 1),
            )
        except (ValueError, IndexError, json.JSONDecodeError):
            pass
    return storage, "\n".join(raw)


def summarize_benchmark_run(runs_dir):
    """
    Battery and EnPULSE memory for the newest BenchmarkRecorder run in runs_dir
    (one folder per run, each with metrics.csv and, once finished, summary.txt).
    Benchmark-app runs pulled from the same Downloads folder are skipped: only
    EnPULSE's recorder writes the collection_state column.
    """
    runs = []
    for csv_path in glob.glob(os.path.join(runs_dir, "*", "metrics.csv")):
        with open(csv_path, newline="") as f:
            reader = csv.DictReader(f)
            if "collection_state" not in (reader.fieldnames or []):
                continue
            rows = list(reader)
        if rows and _num(rows[0].get("timestamp_ms")) is not None:
            runs.append((_num(rows[0]["timestamp_ms"]), csv_path, rows))
    if not runs:
        return None
    _, csv_path, rows = max(runs, key=lambda run: run[0])
    folder = os.path.dirname(csv_path)

    def column(name, scale=1.0):
        return [v / scale for v in (_num(row.get(name)) for row in rows) if v is not None]

    def avg(values):
        return round(sum(values) / len(values), 1) if values else None

    def peak(values):
        return round(max(values), 1) if values else None

    times = column("timestamp_ms")
    hours = (times[-1] - times[0]) / 3600000.0 if len(times) > 1 else 0.0
    level = [int(v) for v in column("battery_level_pct")]
    charge = [int(v) for v in column("battery_charge_uah")]
    pss = column("total_pss_mb")
    native = column("native_heap_allocated_bytes", 1024 * 1024)
    java = column("java_heap_used_bytes", 1024 * 1024)
    charge_dropped = charge[0] - charge[-1] if len(charge) > 1 else None

    def counter_total(name):
        """A process counter restarts from 0 in a new process; count that value as the step."""
        values = [_num(row.get(name)) for row in rows]
        if all(v is None for v in values):
            return None
        return sum(0 if a is None or b is None else (b - a if b >= a else b)
                   for a, b in zip(values, values[1:]))

    def total(name):
        values = column(name)
        return sum(values) if values else None

    cpu_ms = counter_total("cpu_time_ms")
    span_ms = (times[-1] - times[0]) if len(times) > 1 else 0
    started = {row.get("process_started_ms") for row in rows if row.get("process_started_ms")}
    pending = [int(v) for v in column("pending_rows")]
    threads = [int(v) for v in column("threads")]
    fds = [int(v) for v in column("open_fds")]
    net_rx, net_tx, saved = total("net_rx_bytes"), total("net_tx_bytes"), total("rows_saved")

    return {
        "run": os.path.basename(folder),
        "runs_on_device": len(runs),
        "finished": os.path.exists(os.path.join(folder, "summary.txt")),
        "samples": len(rows),
        "duration_hrs": round(hours, 2),
        "longest_gap_s": round(max(b - a for a, b in zip(times, times[1:])) / 1000) if len(times) > 1 else None,
        "battery_start_pct": level[0] if level else None,
        "battery_end_pct": level[-1] if level else None,
        "charge_dropped_uah": charge_dropped,
        "avg_current_ma": round(charge_dropped / hours / 1000.0, 2) if charge_dropped is not None and hours > 0 else None,
        "charging_seen": any(row.get("battery_status") == "charging" for row in rows),
        "pss_avg_mb": avg(pss),
        "pss_max_mb": peak(pss),
        "native_heap_avg_mb": avg(native),
        "native_heap_max_mb": peak(native),
        "java_heap_avg_mb": avg(java),
        "java_heap_max_mb": peak(java),
        # Columns below exist only in runs from builds with the extra metrics; None otherwise.
        "cpu_total_s": round(cpu_ms / 1000.0, 1) if cpu_ms is not None else None,
        "cpu_avg_pct": round(cpu_ms * 100.0 / span_ms, 2) if cpu_ms is not None and span_ms > 0 else None,
        "net_rx_kb": round(net_rx / 1024.0, 1) if net_rx is not None else None,
        "net_tx_kb": round(net_tx / 1024.0, 1) if net_tx is not None else None,
        "rows_saved": int(saved) if saved is not None else None,
        "pending_rows_end": pending[-1] if pending else None,
        "pending_rows_max": max(pending) if pending else None,
        "threads_max": max(threads) if threads else None,
        "open_fds_max": max(fds) if fds else None,
        "process_restarts": max(len(started) - 1, 0) if started else None,
    }


def collect(serial, out_dir, package=PACKAGE, procstats_hours=PROCSTATS_HOURS):
    """
    Dumps batterystats, procstats and storage for EnPULSE into out_dir,
    then writes enpulse_stats.json with the parsed numbers. Returns that dict.
    """
    os.makedirs(out_dir, exist_ok=True)

    def save(name, text):
        with open(os.path.join(out_dir, name), "w") as f:
            f.write(text)

    summary = {"collected_at": datetime.now().isoformat(timespec="seconds"), "package": package}

    uid = get_uid(serial, package)
    summary["uid"] = uid
    if uid is None:
        summary["error"] = "%s is not installed" % package
        save("enpulse_stats.json", json.dumps(summary, indent=2))
        return summary

    # 1. Energy and CPU
    _, checkin, _ = adb(serial, "shell", "dumpsys", "batterystats", "--checkin", timeout=300)
    save("batterystats_checkin.txt", checkin)
    _, text, _ = adb(serial, "shell", "dumpsys", "batterystats", package, timeout=300)
    save("batterystats_enpulse.txt", text)
    battery = parse_batterystats_checkin(checkin, uid) or {}
    if battery.get("estimated_mah") is None:
        battery["estimated_mah"] = parse_estimated_mah_text(text, uid)
    summary["batterystats"] = battery or None

    # 2. Memory
    _, procstats, _ = adb(serial, "shell", "dumpsys", "procstats", "--hours", str(procstats_hours), package)
    save("procstats_enpulse.txt", procstats)
    memory = parse_procstats(procstats)
    if memory is None:
        _, current, _ = adb(serial, "shell", "dumpsys", "procstats", "--current", package)
        save("procstats_enpulse_current.txt", current)
        memory = parse_procstats(current)
    summary["procstats"] = memory

    # 3. Storage
    storage, raw = get_storage(serial, package)
    save("storage.txt", raw)
    summary["storage"] = storage

    # 4. Benchmark runs (all of them are pulled; the newest is summarised)
    runs_dir = os.path.join(out_dir, "benchmark_runs")
    os.makedirs(runs_dir, exist_ok=True)
    for remote in BENCHMARK_DIRS:
        adb(serial, "pull", remote + "/.", runs_dir, timeout=300)
    summary["benchmark_run"] = summarize_benchmark_run(runs_dir)

    save("enpulse_stats.json", json.dumps(summary, indent=2))
    return summary


def format_summary(summary):
    """One short, readable block for terminal output."""
    if summary.get("error"):
        return "  ⚠ %s" % summary["error"]
    b = summary.get("batterystats") or {}
    p = summary.get("procstats") or {}
    s = summary.get("storage") or {}
    m = summary.get("benchmark_run") or {}

    def fmt(value, unit=""):
        return "N/A" if value is None else "%s%s" % (value, unit)

    wl_s = round(b["partial_wakelock_ms"] / 1000.0, 1) if b.get("partial_wakelock_ms") is not None else None
    rec_s = round(b["recorder_wakelock_ms"] / 1000.0, 1) if b.get("recorder_wakelock_ms") else None
    return "\n".join([
        "  Energy:  %s mAh estimated%s" % (fmt(b.get("estimated_mah")),
                                          "   (includes the recorder's wake lock: %s s)" % rec_s if rec_s else ""),
        "  CPU:     %s s   wake locks: %s s (%s)   wake-up alarms: %s"
        % (fmt(b.get("cpu_total_s")), fmt(wl_s), fmt(b.get("partial_wakelock_count")), fmt(b.get("wakeup_alarms"))),
        "  Memory:  PSS avg %s / max %s   (%s samples)"
        % (fmt(p.get("pss_avg_mb"), " MB"), fmt(p.get("pss_max_mb"), " MB"), fmt(p.get("samples"))),
        "  Benchmark run: %s%s" % (m.get("run") or "none found", "" if m.get("finished", True) else "  (still recording)"),
        "    %s h, battery %s -> %s, %s uAh used (%s avg)%s"
        % (fmt(m.get("duration_hrs")), fmt(m.get("battery_start_pct"), "%"), fmt(m.get("battery_end_pct"), "%"),
           fmt(m.get("charge_dropped_uah")), fmt(m.get("avg_current_ma"), " mA"),
           "   ⚠ charged during run" if m.get("charging_seen") else ""),
        "    PSS avg %s / max %s, native heap max %s, Java heap max %s   (%s samples)"
        % (fmt(m.get("pss_avg_mb"), " MB"), fmt(m.get("pss_max_mb"), " MB"),
           fmt(m.get("native_heap_max_mb"), " MB"), fmt(m.get("java_heap_max_mb"), " MB"), fmt(m.get("samples"))),
        "    CPU %s s (avg %s), network %s in / %s out, threads max %s, restarts %s"
        % (fmt(m.get("cpu_total_s")), fmt(m.get("cpu_avg_pct"), "%"), fmt(m.get("net_rx_kb"), " KB"),
           fmt(m.get("net_tx_kb"), " KB"), fmt(m.get("threads_max")), fmt(m.get("process_restarts"))),
        "    rows saved %s, pending at end %s (max %s)"
        % (fmt(m.get("rows_saved")), fmt(m.get("pending_rows_end")), fmt(m.get("pending_rows_max"))),
        "  Storage: data %s, ObjectBox %s   [%s]"
        % (fmt(s.get("data_mb"), " MB"), fmt(s.get("objectbox_mb"), " MB"), s.get("method") or "unavailable"),
    ])


def main():
    parser = argparse.ArgumentParser(description="EnPULSE per-app energy, CPU, memory and storage")
    sub = parser.add_subparsers(dest="command", required=True)
    r = sub.add_parser("reset", help="Clear batterystats and procstats (run after unplugging)")
    r.add_argument("--serial", default="default")
    c = sub.add_parser("collect", help="Dump and parse EnPULSE stats at the end of a run")
    c.add_argument("--serial", default="default")
    c.add_argument("--out", default="enpulse_stats")
    c.add_argument("--hours", type=int, default=PROCSTATS_HOURS, help="Procstats window in hours")
    args = parser.parse_args()

    if args.command == "reset":
        for step, ok, msg in reset(args.serial):
            print("  %s %s: %s" % ("✅" if ok else "⚠", step, msg or "no output"))
    else:
        summary = collect(args.serial, args.out, procstats_hours=args.hours)
        print(format_summary(summary))
        print("  Saved to %s/" % args.out)


if __name__ == "__main__":
    sys.exit(main())
