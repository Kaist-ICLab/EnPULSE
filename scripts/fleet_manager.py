#!/usr/bin/env python3
"""
fleet_manager.py — EnPULSE Multi-Device Orchestrator

Manages multiple Android phones simultaneously via ADB over WiFi.
Supports batch status monitoring, data pulling, and report generation.

Usage:
  python scripts/fleet_manager.py setup
  python scripts/fleet_manager.py status
  python scripts/fleet_manager.py pull [--scenario NAME]
  python scripts/fleet_manager.py report [--scenario NAME]
"""

import os
import sys
import json
import argparse
import subprocess
import concurrent.futures
from datetime import datetime

CONFIG_FILE = os.path.join(os.path.dirname(__file__), "fleet_config.json")
PACKAGE_NAME = "kaist.iclab.mobiletracker"
BENCHMARK_PACKAGE = "kaist.iclab.benchmark"

# --- Utils ---

def load_config():
    if not os.path.exists(CONFIG_FILE):
        return {"devices": []}
    with open(CONFIG_FILE, "r") as f:
        return json.load(f)

def save_config(data):
    with open(CONFIG_FILE, "w") as f:
        json.dump(data, f, indent=2)

def run_adb(args, timeout=15):
    """Runs an ADB command and returns (success, stdout, stderr)."""
    try:
        cmd = ["adb"] + args
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        return res.returncode == 0, res.stdout.strip(), res.stderr.strip()
    except subprocess.TimeoutExpired:
        return False, "", "Timeout"
    except FileNotFoundError:
        print("❌ ADB not found. Please install Android Platform Tools.")
        sys.exit(1)

# --- Commands ---

def cmd_setup():
    print("=" * 60)
    print(" 📡 Fleet Setup - Wireless Debugging (Android 11+)")
    print("=" * 60)
    print("Ensure PC and all phones are on the same WiFi network.")
    print("On each phone:")
    print("  1. Go to Developer Options > Wireless Debugging")
    print("  2. Turn it ON")
    print("  3. Tap 'Pair device with pairing code'")
    print("-" * 60)

    config = load_config()
    devices = config.get("devices", [])

    while True:
        name = input("\nEnter device name (e.g. Phone-1) or 'q' to quit: ").strip()
        if name.lower() == 'q':
            break
        if not name:
            continue

        pair_ip_port = input(f"Enter Pairing IP address and port for {name} (e.g. 192.168.1.50:43211): ").strip()
        if not pair_ip_port:
            continue
            
        code = input(f"Enter 6-digit Wi-Fi pairing code: ").strip()
        
        print(f"⏳ Pairing with {pair_ip_port}...")
        success, out, err = run_adb(["pair", pair_ip_port, code])
        if success or "Successfully paired" in out:
            print("✅ Successfully paired!")
        else:
            print(f"❌ Pairing failed: {out} {err}")
            print("Please try again.")
            continue

        connect_ip_port = input(f"Enter IP address and port for Connection (shown on main Wireless Debugging screen, e.g. 192.168.1.50:39882): ").strip()
        print(f"⏳ Connecting to {connect_ip_port}...")
        c_success, c_out, c_err = run_adb(["connect", connect_ip_port])
        
        if "connected" in c_out.lower():
            print(f"✅ Successfully connected to {name}!")
            
            # Remove old entry if exists
            devices = [d for d in devices if d["name"] != name]
            
            devices.append({
                "name": name,
                "address": connect_ip_port
            })
            config["devices"] = devices
            save_config(config)
            print(f"💾 Saved {name} to fleet configuration.")
        else:
            print(f"❌ Connection failed: {c_out} {c_err}")

    print("\n✅ Setup complete. Current fleet:")
    for d in config["devices"]:
        print(f"  - {d['name']} ({d['address']})")

def check_device_status(device):
    address = device["address"]
    name = device["name"]
    
    # 1. Connect if not connected
    run_adb(["connect", address], timeout=5)
    
    # 2. Check battery
    bat_success, bat_out, _ = run_adb(["-s", address, "shell", "dumpsys", "battery"], timeout=5)
    if not bat_success:
        return name, address, "Offline", "-", "-", "-"

    battery = "-"
    for line in bat_out.splitlines():
        if "level:" in line:
            battery = line.split(":")[1].strip() + "%"
            break

    # 3. Check Benchmark Service status
    srv_success, srv_out, _ = run_adb(["-s", address, "shell", "dumpsys", "activity", "services", BENCHMARK_PACKAGE], timeout=5)
    
    if "kaist.iclab.benchmark/.BenchmarkService" in srv_out:
        status = "● Running"
    else:
        status = "■ Stopped"

    # 4. Count folders in EnPULSE (Phone) and Benchmarks (Watch)
    ls_success, ls_out, _ = run_adb(["-s", address, "shell", "ls", "-d", "/sdcard/Download/EnPULSE/*"], timeout=5)
    folders = [f for f in ls_out.splitlines() if "No such file" not in f and f.strip() and ("phone-" in f or "watch-" in f)]
    
    ls_watch_success, ls_watch_out, _ = run_adb(["-s", address, "shell", "ls", "-d", "/sdcard/Android/data/kaist.iclab.benchmark.wearable/files/Benchmarks/*"], timeout=5)
    watch_folders = [f for f in ls_watch_out.splitlines() if "No such file" not in f and f.strip() and "watch-" in f]
    
    folder_count = (len(folders) if ls_success and folders else 0) + (len(watch_folders) if ls_watch_success and watch_folders else 0)

    return name, address, "Online", battery, status, str(folder_count)

def cmd_status():
    config = load_config()
    devices = config.get("devices", [])
    if not devices:
        print("❌ No devices in fleet. Run 'fleet_manager.py setup' first.")
        return

    print("⏳ Querying fleet status...")
    
    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(devices)) as executor:
        futures = {executor.submit(check_device_status, d): d for d in devices}
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())

    # Sort by name
    results.sort(key=lambda x: x[0])

    print("\n╔" + "═"*12 + "╤" + "═"*22 + "╤" + "═"*9 + "╤" + "═"*11 + "╤" + "═"*11 + "╤" + "═"*14 + "╗")
    print(f"║ {'Device':<10} │ {'Address':<20} │ {'State':<7} │ {'Battery':<9} │ {'Benchmark':<9} │ {'Folders':<12} ║")
    print("╠" + "═"*12 + "╪" + "═"*22 + "╪" + "═"*9 + "╪" + "═"*11 + "╪" + "═"*11 + "╪" + "═"*14 + "╣")
    
    for name, addr, state, bat, bench, f_count in results:
        print(f"║ {name:<10} │ {addr:<20} │ {state:<7} │ {bat:<9} │ {bench:<9} │ {f_count:<12} ║")
        
    print("╚" + "═"*12 + "╧" + "═"*22 + "╧" + "═"*9 + "╧" + "═"*11 + "╧" + "═"*11 + "╧" + "═"*14 + "╝")


def pull_device_data(device):
    address = device["address"]
    name = device["name"]
    
    # Ensure connected
    run_adb(["connect", address], timeout=5)
    
    local_path = os.path.join(os.path.dirname(__file__), "outputs", name)
    os.makedirs(local_path, exist_ok=True)
    
    # Pull the entire EnPULSE directory contents to outputs/<device_name>/ (For Phone)
    success_phone, out_p, err_p = run_adb(["-s", address, "pull", "/sdcard/Download/EnPULSE/.", local_path], timeout=300)
    
    # Pull the Benchmarks directory contents to outputs/<device_name>/ (For Watch)
    success_watch, out_w, err_w = run_adb(["-s", address, "pull", "/sdcard/Android/data/kaist.iclab.benchmark.wearable/files/Benchmarks/.", local_path], timeout=300)
    
    if success_phone or success_watch:
        return name, True, local_path
    else:
        err_msg = ""
        if err_p: err_msg += f"Phone: {err_p.strip()} "
        if err_w: err_msg += f"Watch: {err_w.strip()}"
        return name, False, err_msg.strip() if err_msg else "No data pulled"

def cmd_pull():
    config = load_config()
    devices = config.get("devices", [])
    if not devices:
        print("❌ No devices in fleet.")
        return

    print(f"📥 Pulling all benchmark data from {len(devices)} devices...")
    
    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(devices)) as executor:
        futures = {executor.submit(pull_device_data, d): d for d in devices}
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())

    results.sort(key=lambda x: x[0])
    for name, success, msg in results:
        if success:
            print(f"  ✅ {name}: Pulled all folders to {msg}")
        else:
            print(f"  ❌ {name}: Failed - {msg}")


def generate_device_report(name, path):
    script_path = os.path.join(os.path.dirname(__file__), "generate_report.py")
    try:
        subprocess.run([sys.executable, script_path, "--folder", path], capture_output=True, text=True, check=True)
        return name, True, "Report generated"
    except subprocess.CalledProcessError as e:
        return name, False, e.stderr

def cmd_report(scenario):
    # Find all downloaded folders
    base_out = os.path.join(os.path.dirname(__file__), "outputs")
    if not os.path.exists(base_out):
        print("❌ No outputs folder found. Run 'pull' first.")
        return
        
    tasks = []
    
    for device_name in os.listdir(base_out):
        dev_dir = os.path.join(base_out, device_name)
        if not os.path.isdir(dev_dir): continue
        
        # Find latest matching scenario
        folders = []
        for f in os.listdir(dev_dir):
            if scenario and f"Benchmark_{scenario}" not in f:
                continue
            folders.append(f)
            
        if not folders:
            continue
            
        folders.sort(reverse=True) # basic string sort by date assuming standard format
        latest = folders[0]
        
        tasks.append((device_name, os.path.join(dev_dir, latest)))
        
    if not tasks:
        print(f"❌ No pulled data found for scenario: {scenario or 'Any'}")
        return
        
    print(f"📊 Generating reports for {len(tasks)} devices...")
    
    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=min(len(tasks), 4)) as executor:
        futures = {executor.submit(generate_device_report, name, path): name for name, path in tasks}
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())

    results.sort(key=lambda x: x[0])
    for name, success, msg in results:
        if success:
            print(f"  ✅ {name}: Success")
        else:
            print(f"  ❌ {name}: Failed - {msg}")


def interactive_menu():
    while True:
        print("\n" + "=" * 60)
        print("              EnPULSE Fleet Manager Interactive Menu")
        print("=" * 60)
        print("1. Setup (Pair and connect new wireless devices)")
        print("2. Status (Check battery and benchmark status)")
        print("3. Pull (Extract latest benchmark data)")
        print("4. Report (Generate graphs for pulled data)")
        print("5. Exit")
        print("=" * 60)
        
        choice = input("Select an option (1-5): ").strip()
        if choice == "1":
            cmd_setup()
        elif choice == "2":
            cmd_status()
        elif choice == "3":
            cmd_pull()
        elif choice == "4":
            scenario = input("Filter by scenario name (press Enter for all): ").strip()
            cmd_report(scenario if scenario else None)
        elif choice == "5" or choice.lower() == 'q':
            print("Exiting Fleet Manager.")
            break
        else:
            print("❌ Invalid option.")

def main():
    if len(sys.argv) == 1:
        try:
            interactive_menu()
        except KeyboardInterrupt:
            print("\nGoodbye!")
        return

    parser = argparse.ArgumentParser(description="EnPULSE Fleet Manager")
    subparsers = parser.add_subparsers(dest="command", help="Command to run")
    
    subparsers.add_parser("setup", help="Pair and connect to new wireless devices")
    subparsers.add_parser("status", help="Check battery and benchmark status of all devices")
    subparsers.add_parser("pull", help="Pull all benchmark data from all devices")
    
    report_parser = subparsers.add_parser("report", help="Generate reports for all pulled devices")
    report_parser.add_argument("--scenario", help="Filter by scenario name", default=None)
    
    args = parser.parse_args()
    
    if args.command == "setup":
        cmd_setup()
    elif args.command == "status":
        cmd_status()
    elif args.command == "pull":
        cmd_pull()
    elif args.command == "report":
        cmd_report(args.scenario)
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
