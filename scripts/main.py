#!/usr/bin/env python3
"""
main.py — Unified EnPULSE Benchmarking CLI Tool

This orchestrator script allows you to choose and combine the different benchmarking operations:
1. Pull diagnostic system dumps (pull_benchmark.py)
2. Generate visual graphs and summaries (generate_report.py)
3. Calculate Supabase data reliability yield (yield_calculator.py)
4. Run all combined.
"""

import os
import sys
import subprocess

def select_adb_device():
    """Detects connected ADB devices and lets the user choose one if there are multiple."""
    try:
        output = subprocess.check_output(["adb", "devices"], text=True)
        lines = output.strip().split("\n")[1:] # Skip header
        devices = []
        for line in lines:
            if not line.strip():
                continue
            parts = line.split()
            if len(parts) >= 2 and parts[1] == "device":
                devices.append(parts[0])
                
        if not devices:
            print("⚠️ No connected ADB devices detected. Please connect your phone.")
            return "default"
        elif len(devices) == 1:
            return devices[0]
        else:
            print("\n📱 Multiple ADB devices detected:")
            for idx, serial in enumerate(devices, 1):
                print(f"  {idx}) {serial}")
            while True:
                choice = input(f"Select a device (1-{len(devices)}): ").strip()
                if choice.isdigit() and 1 <= int(choice) <= len(devices):
                    return devices[int(choice) - 1]
                print("❌ Invalid choice. Please enter a number in the list.")
    except Exception as e:
        print(f"⚠️ Error running ADB: {e}")
        return "default"

def run_script(script_name, args=[]):
    """Helper to run a sub-script using the current python interpreter."""
    script_path = os.path.join(os.path.dirname(__file__), script_name)
    if not os.path.exists(script_path):
        print(f"❌ Script not found: {script_path}")
        return False
        
    cmd = [sys.executable, script_path] + args
    print(f"\n🚀 Running: {' '.join(cmd)}")
    try:
        subprocess.check_call(cmd)
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ Script exited with error code {e.returncode}")
        return False
    except KeyboardInterrupt:
        print("\n🛑 Stopped by user.")
        return False

def interactive_menu():
    print("=" * 75)
    print("                     EnPULSE Benchmarking Orchestrator")
    print("=" * 75)
    print("1. [Report] Pull CSV & Generate Graphs (generate_report.py)")
    print("   -> Pulls the latest CSV from the phone and generates PDF/PNG charts")
    print("      of Battery level, CPU usage, CPU temp, and Java vs. Native Memory.")
    print("")
    print("2. [Yield]  Calculate Database Reliability Yield (yield_calculator.py)")
    print("   -> Queries the Supabase backend to calculate what percentage of")
    print("      sensor data successfully uploaded compared to expected sensor rates.")
    print("")
    print("3. [Logs]   Pull Deep Diagnostic Dump (pull_benchmark.py)")
    print("   -> Extracts raw Android system logs (dumpsys, memory maps, system-wide")
    print("      bugreports) from the phone for crash/optimization debugging.")
    print("")
    print("4. [All]    Run All Combined")
    print("   -> Downloads data, generates graphs, and runs the yield calculator.")
    print("")
    print("5. Exit")
    print("=" * 75)
    
    choice = input("Select an option (1-5): ").strip()
    
    if choice == "1":
        serial = select_adb_device()
        run_script("generate_report.py", ["--phone", serial])
    elif choice == "2":
        print("\n💡 Supabase credentials needed for Yield calculator.")
        url = input("Supabase URL (press enter for demo): ").strip()
        if not url:
            run_script("yield_calculator.py", ["--demo"])
        else:
            key = input("Supabase Key: ").strip()
            campaign = input("Campaign ID: ").strip()
            duration = input("Duration (seconds, default 3600): ").strip() or "3600"
            run_script("yield_calculator.py", ["--url", url, "--key", key, "--campaign", campaign, "--duration", duration])
    elif choice == "3":
        serial = select_adb_device()
        scenario = input("Scenario Name (e.g., A_Baseline): ").strip()
        if not scenario:
            print("❌ Scenario name is required to save log folders.")
            return
        args = ["--scenario", scenario, "--phone", serial]
        run_script("pull_benchmark.py", args)
    elif choice == "4":
        # Full end-to-end combination
        serial = select_adb_device()
        print("\n--- Phase 1: Pulling latest CSV and generating report graph ---")
        if run_script("generate_report.py", ["--phone", serial]):
            print("\n--- Phase 2: Calculating Yield ---")
            url = input("Supabase URL (press enter for demo): ").strip()
            if not url:
                run_script("yield_calculator.py", ["--demo"])
            else:
                key = input("Supabase Key: ").strip()
                campaign = input("Campaign ID: ").strip()
                duration = input("Duration (seconds, default 3600): ").strip() or "3600"
                run_script("yield_calculator.py", ["--url", url, "--key", key, "--campaign", campaign, "--duration", duration])
    elif choice == "5" or not choice:
        print("Goodbye!")
        sys.exit(0)
    else:
        print("❌ Invalid option.")

def main():
    # If arguments are passed, bypass interactive menu and show help
    if len(sys.argv) > 1:
        print("Usage: python scripts/main.py")
        print("Launches the interactive menu to run/combine benchmarking scripts.")
        sys.exit(0)
        
    while True:
        try:
            interactive_menu()
            print()
            input("Press Enter to return to menu...")
            print()
        except KeyboardInterrupt:
            print("\nGoodbye!")
            break

if __name__ == "__main__":
    main()
