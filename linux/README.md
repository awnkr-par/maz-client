# MazLauncher for Linux (foundation)

This is an **early development scaffold**, not a playable launcher or a release candidate.

## Why separate?
The existing Windows launcher uses WPF, which is Windows-specific. The Linux
launcher is being developed independently while retaining the same MazClient
repository and GitHub release source.

## Run
Requires Python 3.10+; no external Python packages.

```bash
python3 linux/mazlauncher.py doctor
python3 linux/mazlauncher.py paths
python3 linux/mazlauncher.py releases
```

- `doctor` checks OS, architecture and runtime.
- `paths` shows XDG-compliant config and data locations.
- `releases` reads release metadata from GitHub, without downloading or
  executing release assets.

## Safety and next steps
- No root privileges, system-wide installation, disk formatting or USB access.
- Do not run this from or install it onto the user's important thumb drive.
- Implement a Linux UI, Microsoft sign-in, optional offline profiles, isolated
  Minecraft 26.2 / 1.21.11 instances, Fabric dependency management, verified
  downloads, and an OS-specific update asset selector.
- Windows and Linux builds may share a release but **must select different
  platform-specific assets**; never try to install a Windows EXE on Linux.
- Add Linux CI builds and tests before publishing any installer.
- A successful metadata lookup is **not** evidence that Minecraft launches.

Nothing in this directory changes the Windows launcher.
