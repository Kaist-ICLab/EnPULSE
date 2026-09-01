#!/usr/bin/env python3
"""
fleet_manager.py — EnPULSE Multi-Device Orchestrator

Manages multiple Android phones and watches simultaneously via ADB over WiFi.
Supports batch status monitoring, data pulling, APK installation, and report generation.

Usage:
  python scripts/fleet_manager.py setup
  python scripts/fleet_manager.py status
  python scripts/fleet_manager.py pull
  python scripts/fleet_manager.py install
"""

import os
import sys
import json
import argparse
import subprocess
import concurrent.futures
import re
from datetime import datetime

CONFIG_FILE = os.path.join(os.path.dirname(__file__), "fleet_config.json")
PACKAGE_NAME = "kaist.iclab.mobiletracker"
BENCHMARK_PACKAGE = "kaist.iclab.benchmark"

# --- Configuration & Utilities ---

def load_config() -> dict:
    """
    Loads the fleet configuration from fleet_config.json.
    
    Returns:
        dict: A dictionary containing the 'devices' list. Returns an empty structure if the file does not exist.
    """
    if not os.path.exists(CONFIG_FILE):
        return {"devices": []}
    with open(CONFIG_FILE, "r") as f:
        return json.load(f)

def save_config(data: dict):
    """
    Saves the fleet configuration to fleet_config.json.
    
    Args:
        data (dict): The configuration dictionary to save, typically containing a 'devices' list.
    """
    with open(CONFIG_FILE, "w") as f:
        json.dump(data, f, indent=2)

def run_adb(args: list, timeout: int = 15) -> tuple:
    """
    Executes an ADB command as a subprocess.
    
    Args:
        args (list): The list of arguments to pass to ADB (e.g., ["connect", "192.168.0.1:5555"]).
        timeout (int): The maximum time to wait for the command to complete, in seconds.
        
    Returns:
        tuple: (success (bool), stdout (str), stderr (str))
    """
    try:
        cmd = ["adb"] + args
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        return res.returncode == 0, res.stdout.strip(), res.stderr.strip()
    except subprocess.TimeoutExpired:
        return False, "", "Timeout"
    except FileNotFoundError:
        print("❌ ADB not found. Please install Android Platform Tools.")
        sys.exit(1)

def select_target_devices(devices: list) -> list:
    """
    Prompts the user to filter the list of devices by type or by manual selection.
    Automatically probes and saves the device type if it is unknown.
    
    Args:
        devices (list): A list of device dictionaries from the configuration.
        
    Returns:
        list: A filtered list of device dictionaries based on user selection.
    """
    print("\n🎯 Select Target Devices:")
    print("1. All devices")
    print("2. Phones only")
    print("3. Watches only")
    print("4. Select specific devices (comma-separated)")
    
    choice = input("Select an option (1-4) [1]: ").strip() or "1"
    
    if choice == "1":
        return devices
        
    if choice in ["2", "3"]:
        target_type = "Watch" if choice == "3" else "Phone"
        filtered = [d for d in devices if d.get("type") == target_type]
        return filtered
        
    if choice == "4":
        print("\nAvailable devices:")
        for i, d in enumerate(devices):
            dtype = d.get("type", "Unknown")
            print(f"{i+1}. {d['name']} ({d['address']}) - {dtype}")
        sel = input("Enter device numbers (e.g., 1,3,4): ").strip()
        try:
            indices = [int(x.strip()) - 1 for x in sel.split(",") if x.strip()]
            return [devices[i] for i in indices if 0 <= i < len(devices)]
        except ValueError:
            print("❌ Invalid input.")
            return []
            
    return devices

# --- Core Device Operations ---

def check_device_status(device: dict) -> tuple:
    """
    Probes a single device for its battery level, benchmark service status, and local data folder count.
    
    Args:
        device (dict): The device dictionary containing 'name' and 'address'.
        
    Returns:
        tuple: (name, address, state, battery, benchmark_status, folder_count)
    """
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
    
    if "kaist.iclab.benchmark" in srv_out and "ServiceRecord{" in srv_out:
        status = "● Running"
        
        # Try getting elapsed time from 'ps' (Supported on Android 11+ Toybox)
        ps_success, ps_out, _ = run_adb(["-s", address, "shell", "ps", "-o", "NAME,ETIME"], timeout=5)
        found_time = False
        if ps_success:
            for line in ps_out.splitlines():
                if "kaist.iclab.benchmark" in line:
                    parts = line.strip().split()
                    if len(parts) >= 2 and ":" in parts[-1]:
                        status = f"● Running ({parts[-1]})"
                        found_time = True
                        break
                        
        # Fallback to dumpsys createTime if ps didn't work
        if not found_time:
            import re
            match = re.search(r"createTime=-?([0-9]+[a-zA-Z0-9]*)", srv_out)
            if match:
                status = f"● Running ({match.group(1)})"
    else:
        status = "■ Stopped"

    # 4. Count folders in EnPULSE (Phone) and Benchmarks (Watch)
    ls_success, ls_out, _ = run_adb(["-s", address, "shell", "ls", "-d", "/sdcard/Download/EnPULSE/*"], timeout=5)
    folders = [f for f in ls_out.splitlines() if "No such file" not in f and f.strip() and ("phone-" in f or "watch-" in f)]
    
    ls_watch_success, ls_watch_out, _ = run_adb(["-s", address, "shell", "ls", "-d", "/sdcard/Android/data/kaist.iclab.benchmark.wearable/files/Benchmarks/*"], timeout=5)
    watch_folders = [f for f in ls_watch_out.splitlines() if "No such file" not in f and f.strip() and "watch-" in f]
    
    folder_count = (len(folders) if ls_success and folders else 0) + (len(watch_folders) if ls_watch_success and watch_folders else 0)

    return name, address, "Online", battery, status, str(folder_count)

def pull_device_data(device: dict) -> tuple:
    """
    Extracts all benchmark data folders from a single device (Phone or Watch paths) to the local machine.
    
    Args:
        device (dict): The device dictionary containing 'name' and 'address'.
        
    Returns:
        tuple: (name, success_bool, status_message)
    """
    address = device["address"]
    name = device["name"]
    
    # Ensure connected
    run_adb(["connect", address], timeout=5)
    
    local_path = os.path.expanduser(os.path.join("~", "Desktop", "EnPULSE-Data", name))
    os.makedirs(local_path, exist_ok=True)
    
    # Pull the entire EnPULSE directory contents to ~/Desktop/EnPULSE-Data/<device_name>/ (For Phone)
    success_phone, out_p, err_p = run_adb(["-s", address, "pull", "/sdcard/Download/EnPULSE/.", local_path], timeout=300)
    
    # Pull the Benchmarks directory contents to ~/Desktop/EnPULSE-Data/<device_name>/ (For Watch)
    success_watch, out_w, err_w = run_adb(["-s", address, "pull", "/sdcard/Android/data/kaist.iclab.benchmark.wearable/files/Benchmarks/.", local_path], timeout=300)
    
    if success_phone or success_watch:
        return name, True, local_path
    else:
        err_msg = ""
        if err_p: err_msg += f"Phone: {err_p.strip()} "
        if err_w: err_msg += f"Watch: {err_w.strip()}"
        return name, False, err_msg.strip() if err_msg else "No data pulled"



# --- CLI Commands ---

def cmd_setup():
    """
    Interactive wizard to pair and connect new Android 11+ devices using Wireless Debugging.
    Saves paired devices to fleet_config.json.
    """
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

        conn_addr = input(f"Enter IP address and port for Connection (shown on main Wireless Debugging screen, e.g. 192.168.1.50:39882): ").strip()
        print(f"⏳ Connecting to {conn_addr}...")
        c_success, c_out, c_err = run_adb(["connect", conn_addr])
        
        if "connected" in c_out.lower():
            print(f"✅ Successfully connected to {name}!")
            
            # Remove old entry if exists
            devices = [d for d in devices if d["name"] != name]
        
            # Determine device type and save
            success, out, _ = run_adb(["-s", conn_addr, "shell", "getprop", "ro.build.characteristics"], timeout=5)
            device_type = "Watch" if (success and "watch" in out.lower()) else "Phone"
            
            devices.append({
                "name": name,
                "address": conn_addr,
                "type": device_type
            })
            config["devices"] = devices
            save_config(config)
            print(f"💾 Saved {name} ({device_type}) to fleet configuration.")
        else:
            print(f"❌ Connection failed: {c_out} {c_err}")

    print("\n✅ Setup complete. Current fleet:")
    for d in config["devices"]:
        print(f"  - {d['name']} ({d['address']})")

def cmd_status():
    """
    Queries all targeted devices in parallel to fetch their battery and benchmark service status.
    Prints the result in a formatted table.
    """
    config = load_config()
    devices = config.get("devices", [])
    if not devices:
        print("❌ No devices in fleet. Run 'fleet_manager.py setup' first.")
        return

    target_devices = select_target_devices(devices)
    if not target_devices:
        print("❌ No devices selected.")
        return

    print(f"\n📡 Querying status for {len(target_devices)} devices...")
    
    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(target_devices)) as executor:
        futures = {executor.submit(check_device_status, d): d for d in target_devices}
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())

    # Sort by name
    results.sort(key=lambda x: x[0])

    print("\n╔" + "═"*12 + "╤" + "═"*22 + "╤" + "═"*9 + "╤" + "═"*11 + "╤" + "═"*19 + "╤" + "═"*11 + "╗")
    print(f"║ {'Device':<10} │ {'Address':<20} │ {'State':<7} │ {'Battery':<9} │ {'Benchmark':<17} │ {'Folders':<9} ║")
    print("╠" + "═"*12 + "╪" + "═"*22 + "╪" + "═"*9 + "╪" + "═"*11 + "╪" + "═"*19 + "╪" + "═"*11 + "╣")
    
    for name, addr, state, bat, bench, f_count in results:
        print(f"║ {name:<10} │ {addr:<20} │ {state:<7} │ {bat:<9} │ {bench:<17} │ {f_count:<9} ║")
        
    print("╚" + "═"*12 + "╧" + "═"*22 + "╧" + "═"*9 + "╧" + "═"*11 + "╧" + "═"*19 + "╧" + "═"*11 + "╝")

def cmd_pull():
    """
    Pulls benchmark data folders from all targeted devices in parallel.
    Saves the data into ~/Desktop/EnPULSE-Data.
    """
    config = load_config()
    devices = config.get("devices", [])
    if not devices:
        print("❌ No devices in fleet.")
        return

    target_devices = select_target_devices(devices)
    if not target_devices:
        print("❌ No devices selected.")
        return

    print(f"\n📥 Pulling all benchmark data from {len(target_devices)} devices...")
    
    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(target_devices)) as executor:
        futures = {executor.submit(pull_device_data, d): d for d in target_devices}
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())

    results.sort(key=lambda x: x[0])
    for name, success, msg in results:
        if success:
            print(f"  ✅ {name}: Pulled all folders to {msg}")
        else:
            print(f"  ❌ {name}: Failed - {msg}")

def cmd_install():
    """
    Interactive wizard to build APKs via Gradle and deploy them in parallel to targeted devices.
    Intelligently routes Watch APKs to Watches and Phone APKs to Phones.
    """
    config = load_config()
    devices = config.get("devices", [])
    if not devices:
        print("❌ No devices in fleet.")
        return

    proj_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    ALL_APPS = [
        {"id": "app-mobile-benchmark", "type": "Benchmark", "device": "Phone", "task": ":app-mobile-benchmark:assembleDebug", "path": os.path.join(proj_dir, "app-mobile-benchmark", "build", "outputs", "apk", "debug", "EnPULSE-Mobile-Benchmark.apk")},
        {"id": "app-wearable-benchmark", "type": "Benchmark", "device": "Watch", "task": ":app-wearable-benchmark:assembleDebug", "path": os.path.join(proj_dir, "app-wearable-benchmark", "build", "outputs", "apk", "debug", "EnPULSE-Watch-Benchmark.apk")},
        {"id": "app-mobile-tracker", "type": "Tracker", "device": "Phone", "task": ":app-mobile-tracker:assembleDebug", "path": os.path.join(proj_dir, "app-mobile-tracker", "build", "outputs", "apk", "debug", "EnPULSE-Mobile.apk")},
        {"id": "app-wearable-tracker", "type": "Tracker", "device": "Watch", "task": ":app-wearable-tracker:assembleDebug", "path": os.path.join(proj_dir, "app-wearable-tracker", "build", "outputs", "apk", "debug", "EnPULSE-Watch.apk")}
    ]

    print("\n📦 Which apps do you want to install?")
    print("1. All Apps (4 apps)")
    print("2. Benchmark Apps (Phone & Watch Benchmark)")
    print("3. Tracker Apps (Phone & Watch Tracker)")
    print("4. Phone Apps (Tracker & Benchmark)")
    print("5. Watch Apps (Tracker & Benchmark)")
    print("6. Select a specific single app")
    app_choice = input("Select an option (1-6) [1]: ").strip() or "1"
    
    selected_apps = []
    if app_choice == "1":
        selected_apps = ALL_APPS
    elif app_choice == "2":
        selected_apps = [a for a in ALL_APPS if a["type"] == "Benchmark"]
    elif app_choice == "3":
        selected_apps = [a for a in ALL_APPS if a["type"] == "Tracker"]
    elif app_choice == "4":
        selected_apps = [a for a in ALL_APPS if a["device"] == "Phone"]
    elif app_choice == "5":
        selected_apps = [a for a in ALL_APPS if a["device"] == "Watch"]
    elif app_choice == "6":
        for i, a in enumerate(ALL_APPS):
            print(f"{i+1}. {a['id']}")
        single_choice = input(f"Select app (1-4): ").strip()
        try:
            selected_apps = [ALL_APPS[int(single_choice) - 1]]
        except (ValueError, IndexError):
            pass

    if not selected_apps:
        print("❌ Invalid app choice.")
        return

    target_devices = select_target_devices(devices)
    if not target_devices:
        print("❌ No devices selected.")
        return

    gradle_tasks = [a["task"] for a in selected_apps]
    print(f"\n🔨 Building APKs via Gradle: {' '.join(gradle_tasks)}")
    gradle_cmd = ["./gradlew"] + gradle_tasks
    
    try:
        subprocess.run(gradle_cmd, cwd=proj_dir, check=True)
    except subprocess.CalledProcessError:
        print("❌ Gradle build failed!")
        return

    # Check APK existence & fallback resolution
    for app in selected_apps:
        if not os.path.exists(app["path"]):
            debug_apk_dir = os.path.join(proj_dir, app["id"], "build", "outputs", "apk", "debug")
            if os.path.exists(debug_apk_dir):
                apks = [os.path.join(debug_apk_dir, f) for f in os.listdir(debug_apk_dir) if f.endswith(".apk")]
                if apks:
                    app["path"] = apks[0]

        if not os.path.exists(app["path"]):
            print(f"❌ Built APK not found: {app['id']}")
            return

    print(f"🚀 Deploying to {len(target_devices)} device(s) in parallel...")

    def install_to_device(device):
        name = device["name"]
        address = device["address"]
        run_adb(["connect", address], timeout=5)
        
        device_type = device.get("type", "Phone")
        
        # Filter apps that match this device type
        apps_for_this_device = [a for a in selected_apps if a["device"] == device_type]
        
        if not apps_for_this_device:
            return name, True, "Skipped (No apps selected for this device type)"
        
        results_msg = []
        all_success = True
        
        for app in apps_for_this_device:
            success, out, err = run_adb(["-s", address, "install", "-r", "-d", "-t", app["path"]], timeout=120)
            if success and "Success" in out:
                results_msg.append(f"{app['id']} ✅")
            else:
                err_clean = (err or out).replace('\n', ' ').strip()
                results_msg.append(f"{app['id']} ❌")
                all_success = False
                
        return name, all_success, " | ".join(results_msg)

    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(target_devices)) as executor:
        futures = {executor.submit(install_to_device, d): d for d in target_devices}
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())

    results.sort(key=lambda x: x[0])
    print("\n📋 Deployment Results:")
    for name, success, msg in results:
        if success:
            print(f"  ✅ {name}: {msg}")
        else:
            print(f"  ⚠️ {name}: {msg}")



# --- CLI Entrypoint ---

def interactive_menu():
    """
    Displays an interactive continuous terminal menu if the script is run without arguments.
    Allows the user to select and repeatedly run fleet commands.
    """
    while True:
        print("\n" + "=" * 60)
        print("              EnPULSE Fleet Manager Interactive Menu")
        print("=" * 60)
        print("1. Setup (Pair and connect new wireless devices)")
        print("2. Status (Check battery and benchmark status)")
        print("3. Pull (Extract latest benchmark data)")
        print("4. Install (Build & Deploy APKs to all devices)")
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
            cmd_install()
        elif choice == "5" or choice.lower() == 'q':
            print("Exiting Fleet Manager.")
            break
        else:
            print("❌ Invalid option.")

def main():
    """
    Main entry point. Parses command line arguments if provided, 
    otherwise falls back to the interactive terminal menu.
    """
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
    subparsers.add_parser("install", help="Build and install benchmark APKs to all devices")
    
    args = parser.parse_args()
    
    if args.command == "setup":
        cmd_setup()
    elif args.command == "status":
        cmd_status()
    elif args.command == "pull":
        cmd_pull()
    elif args.command == "install":
        cmd_install()
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
