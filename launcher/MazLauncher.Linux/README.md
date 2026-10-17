# MazLauncher Linux (early preview)

A native Linux launcher UI built with Avalonia and .NET 8. This is an **early foundation**, not yet a replacement for the Windows launcher.

## Implemented
- Resizable dark Maz-themed window with Play, Accounts and About tabs
- Create and delete local profiles, saved to `~/.config/MazLauncher/local-accounts.json` on typical Linux systems
- GitHub Actions Linux x64 self-contained preview artifact

## Not implemented yet
- Minecraft/Fabric installation and launch
- Microsoft account authentication
- Managed mod and resource-pack installation
- AppImage / distro packages

To run from source: `dotnet run --project launcher/MazLauncher.Linux/MazLauncher.Linux.csproj`

On a successful GitHub Actions run, download the `MazLauncher-Linux-x64-preview` artifact, extract it, mark `MazLauncher.Linux` executable and launch it.
