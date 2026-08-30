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
    print("1. [Report] Pull CSV & Generate Graphs")
    print("   -> Pulls the latest CSV from the phone and generates PDF/PNG charts")
    print("      of Battery level, CPU usage, CPU temp, and Java vs. Native Memory.")
    print("")
    print("2. [Yield]  Calculate Database Reliability Yield")
    print("   -> Queries the Supabase backend to calculate what percentage of")
    print("      sensor data successfully uploaded compared to expected sensor rates.")
    print("")
    print("3. [Logs]   Pull Deep Diagnostic Dumps")
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
        run_script("generate_report.py")
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
        serial = input("Phone ADB Serial (leave blank if only 1 device connected): ").strip()
        scenario = input("Scenario Name (e.g., A_Baseline): ").strip()
        if not scenario:
            print("❌ Scenario name is required to save log folders.")
            return
        args = ["--scenario", scenario]
        if serial:
            args += ["--phone", serial]
        else:
            args += ["--phone", "default"] # pull_benchmark.py requires phone arg
        run_script("pull_benchmark.py", args)
    elif choice == "4":
        # Full end-to-end combination
        print("\n--- Phase 1: Pulling latest CSV and generating report graph ---")
        if run_script("generate_report.py"):
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
