#!/usr/bin/env python3
"""
System information collector for benchmark metadata.

Captures CPU, memory, OS, Python, JVM, and Mill version details,
writing them to sysinfo.json alongside benchmark results.

Usage: python3 benchmarks/collect_sysinfo.py [output_path]
Default output: benchmarks/outputs/sysinfo.json
"""

import json
import multiprocessing
import platform
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path


def _get_cpu_model():
    """Get CPU model name. Uses sysctl on macOS, /proc/cpuinfo on Linux."""
    cpu = platform.processor()
    if cpu and cpu != "":
        # On macOS, platform.processor() may return 'arm' or 'i386' rather than full name
        if platform.system() == "Darwin":
            try:
                result = subprocess.run(
                    ["sysctl", "-n", "machdep.cpu.brand_string"],
                    capture_output=True, text=True, timeout=5,
                )
                if result.returncode == 0 and result.stdout.strip():
                    return result.stdout.strip()
            except (subprocess.TimeoutExpired, FileNotFoundError):
                pass
        return cpu

    # Fallback: try sysctl on macOS
    if platform.system() == "Darwin":
        try:
            result = subprocess.run(
                ["sysctl", "-n", "machdep.cpu.brand_string"],
                capture_output=True, text=True, timeout=5,
            )
            if result.returncode == 0:
                return result.stdout.strip()
        except (subprocess.TimeoutExpired, FileNotFoundError):
            pass

    return "unknown"


def _get_ram_gb():
    """Get total RAM in GB. Uses sysctl on macOS, os.sysconf on Linux."""
    if platform.system() == "Darwin":
        try:
            result = subprocess.run(
                ["sysctl", "-n", "hw.memsize"],
                capture_output=True, text=True, timeout=5,
            )
            if result.returncode == 0:
                return round(int(result.stdout.strip()) / (1024 ** 3), 1)
        except (subprocess.TimeoutExpired, FileNotFoundError, ValueError):
            pass

    # Linux fallback via os.sysconf
    try:
        import os
        page_size = os.sysconf("SC_PAGE_SIZE")
        page_count = os.sysconf("SC_PHYS_PAGES")
        return round(page_size * page_count / (1024 ** 3), 1)
    except (ValueError, OSError, AttributeError):
        pass

    return -1.0


def _get_jvm_version():
    """Get JVM version from 'java -version' stderr output."""
    try:
        result = subprocess.run(
            ["java", "-version"],
            capture_output=True, text=True, timeout=10,
        )
        # java -version outputs to stderr
        lines = result.stderr.strip().split("\n")
        if lines:
            return lines[0]
    except (subprocess.TimeoutExpired, FileNotFoundError):
        pass
    return "unknown"


def _get_mill_version():
    """Get Mill version from './mill --version' output."""
    # Try from the project root (benchmarks parent)
    project_root = Path(__file__).parent.parent
    mill_path = project_root / "mill"

    try:
        result = subprocess.run(
            [str(mill_path), "--version"],
            capture_output=True, text=True, timeout=30,
            cwd=str(project_root),
        )
        if result.returncode == 0 and result.stdout.strip():
            return result.stdout.strip().split("\n")[0]
    except (subprocess.TimeoutExpired, FileNotFoundError, OSError):
        pass

    return "unknown"


def collect_sysinfo():
    """Collect system information and return as a dictionary."""
    return {
        "cpu_model": _get_cpu_model(),
        "cpu_cores": multiprocessing.cpu_count(),
        "ram_gb": _get_ram_gb(),
        "os": f"{platform.system()} {platform.release()}",
        "os_version": platform.version(),
        "python_version": platform.python_version(),
        "jvm_version": _get_jvm_version(),
        "mill_version": _get_mill_version(),
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }


if __name__ == "__main__":
    # Output path from CLI arg or default
    if len(sys.argv) > 1:
        output_path = Path(sys.argv[1])
    else:
        output_path = Path(__file__).parent / "outputs" / "sysinfo.json"

    output_path.parent.mkdir(parents=True, exist_ok=True)

    info = collect_sysinfo()

    with output_path.open("w") as f:
        json.dump(info, f, indent=2)

    print(f"System info written to {output_path}")
    for key, value in info.items():
        print(f"  {key}: {value}")
