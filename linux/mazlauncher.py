#!/usr/bin/env python3
"""MazLauncher Linux foundation: safe, dependency-free release discovery.

This does not download or execute unverified release assets.
"""
import argparse
import json
import os
from pathlib import Path
import platform
import sys
import urllib.error
import urllib.request

API = "https://api.github.com/repos/awnkr-par/maz-client/releases/latest"

def data_dir():
    base = os.environ.get("XDG_DATA_HOME")
    return Path(base).expanduser() / "mazlauncher" if base else Path.home() / ".local/share/mazlauncher"

def config_dir():
    base = os.environ.get("XDG_CONFIG_HOME")
    return Path(base).expanduser() / "mazlauncher" if base else Path.home() / ".config/mazlauncher"

def release_info(timeout=12):
    req = urllib.request.Request(API, headers={
        "Accept": "application/vnd.github+json",
        "User-Agent": "MazLauncher-Linux/0.1",
    })
    with urllib.request.urlopen(req, timeout=timeout) as response:
        payload = json.load(response)
    return {
        "tag": payload.get("tag_name", ""),
        "url": payload.get("html_url", ""),
        "assets": [
            {"name": asset.get("name", ""), "size": asset.get("size", 0)}
            for asset in payload.get("assets", [])
        ],
    }

def main():
    parser = argparse.ArgumentParser(description="MazLauncher Linux foundation")
    parser.add_argument("command", choices=["doctor", "releases", "paths"])
    args = parser.parse_args()
    if args.command == "doctor":
        print(json.dumps({
            "system": platform.system(),
            "architecture": platform.machine(),
            "python": platform.python_version(),
            "linux_supported": sys.platform.startswith("linux"),
            "data_directory": str(data_dir()),
            "config_directory": str(config_dir()),
        }, indent=2))
        return 0 if sys.platform.startswith("linux") else 2
    if args.command == "paths":
        print(json.dumps({"data": str(data_dir()), "config": str(config_dir())}, indent=2))
        return 0
    try:
        print(json.dumps(release_info(), indent=2))
        return 0
    except (urllib.error.URLError, TimeoutError, ValueError, OSError) as exc:
        print(f"Release lookup failed: {exc}", file=sys.stderr)
        return 1

if __name__ == "__main__":
    raise SystemExit(main())
