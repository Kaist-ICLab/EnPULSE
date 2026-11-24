#!/usr/bin/env python3
"""
enpulse_stats.py — EnPULSE's own energy, CPU, memory and storage, read over ADB.

The benchmark apps can only see device-wide battery numbers and their own memory.
Android keeps per-app statistics itself, so these are read from the device:

  1. Energy and CPU   dumpsys batterystats   (estimated mAh, CPU time, wake locks,
                                               wake-up alarms, network bytes, GPS time)
  2. Memory           dumpsys procstats      (PSS / USS / RSS min, average and max)
  3. Storage          run-as ... du          (app data size and ObjectBox size)
  4. Memory over time EnPULSE's MemoryLogger  (CSV every 60 s, pradipta/testing builds only)

Workflow for each run:
  1. Unplug the device (batterystats stops counting while charging).
  2. Reset:   python scripts/enpulse_stats.py reset   --serial <serial>
  3. Start the benchmark app and EnPULSE, run the scenario.
  4. Collect: python scripts/enpulse_stats.py collect --serial <serial> --out <dir>

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
MEMORY_LOG_DIR = "/sdcard/Android/data/%s/files/EnPULSE-Memory" % PACKAGE  # MemoryLogger output
PROCSTATS_HOURS = 24  # Procstats aggregation window; runs longer than this need a larger value.

GPS_SENSOR_ID = "-10000"  # BatteryStats.Uid.Sensor.GPS


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
    Clears batterystats, procstats and old MemoryLogger files, so the end-of-run
    collection covers this run only. Returns a list of (step, ok, message).
    """
    results = []
    ok, out, err = adb(serial, "shell", "dumpsys", "batterystats", "--reset")
    results.append(("batterystats", ok and "reset" in out.lower(), (out or err).strip()))
    ok, out, err = adb(serial, "shell", "dumpsys", "procstats", "--clear")
    results.append(("procstats", ok and "clear" in out.lower(), (out or err).strip()))
    # A running EnPULSE recreates its current file, with a header, on the next sample.
    ok, out, err = adb(serial, "shell", "rm -f %s/*.csv" % MEMORY_LOG_DIR)
    results.append(("memory logs", ok, (out or err).strip() or "cleared"))
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


def summarize_memory_logs(log_dir):
    """Average and peak of EnPULSE's own memory across the MemoryLogger CSVs in log_dir."""
    pss, native, java = [], [], []
    files = sorted(glob.glob(os.path.join(log_dir, "*.csv")))
    for path in files:
        with open(path, newline="") as f:
            for row in csv.DictReader(f):
                for column, values in (("total_pss_mb", pss), ("native_heap_mb", native), ("java_heap_mb", java)):
                    value = _num(row.get(column))
                    if value is not None:
                        values.append(value)
    if not pss:
        return None

    def avg(values):
        return round(sum(values) / len(values), 1) if values else None

    return {
        "files": len(files),
        "samples": len(pss),
        "pss_avg_mb": avg(pss),
        "pss_max_mb": max(pss),
        "native_heap_avg_mb": avg(native),
        "native_heap_max_mb": max(native) if native else None,
        "java_heap_avg_mb": avg(java),
        "java_heap_max_mb": max(java) if java else None,
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

    # 4. Memory over time (MemoryLogger CSVs)
    memory_dir = os.path.join(out_dir, "memory_log")
    os.makedirs(memory_dir, exist_ok=True)
    adb(serial, "pull", MEMORY_LOG_DIR + "/.", memory_dir, timeout=300)
    summary["memory_log"] = summarize_memory_logs(memory_dir)

    save("enpulse_stats.json", json.dumps(summary, indent=2))
    return summary


def format_summary(summary):
    """One short, readable block for terminal output."""
    if summary.get("error"):
        return "  ⚠ %s" % summary["error"]
    b = summary.get("batterystats") or {}
    p = summary.get("procstats") or {}
    s = summary.get("storage") or {}
    m = summary.get("memory_log") or {}

    def fmt(value, unit=""):
        return "N/A" if value is None else "%s%s" % (value, unit)

    wl_s = round(b["partial_wakelock_ms"] / 1000.0, 1) if b.get("partial_wakelock_ms") is not None else None
    return "\n".join([
        "  Energy:  %s mAh estimated" % fmt(b.get("estimated_mah")),
        "  CPU:     %s s   wake locks: %s s (%s)   wake-up alarms: %s"
        % (fmt(b.get("cpu_total_s")), fmt(wl_s), fmt(b.get("partial_wakelock_count")), fmt(b.get("wakeup_alarms"))),
        "  Memory:  PSS avg %s / max %s   (%s samples)"
        % (fmt(p.get("pss_avg_mb"), " MB"), fmt(p.get("pss_max_mb"), " MB"), fmt(p.get("samples"))),
        "  Memory log: PSS avg %s / max %s, native heap max %s   (%s samples)"
        % (fmt(m.get("pss_avg_mb"), " MB"), fmt(m.get("pss_max_mb"), " MB"),
           fmt(m.get("native_heap_max_mb"), " MB"), fmt(m.get("samples"))),
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
