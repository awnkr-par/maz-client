#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="${1:-0.1.0}"
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Version must be major.minor.patch" >&2
  exit 2
fi
command -v dpkg-deb >/dev/null || { echo "dpkg-deb is required" >&2; exit 1; }
command -v python3 >/dev/null || { echo "python3 is required" >&2; exit 1; }
BUILD="$(mktemp -d)"
trap 'rm -rf "$BUILD"' EXIT
mkdir -p "$BUILD/pkg/DEBIAN" "$BUILD/pkg/usr/share/mazlauncher" "$BUILD/pkg/usr/bin" "$BUILD/pkg/usr/share/applications" "$ROOT/dist"
cp "$ROOT/linux/desktop.py" "$ROOT/linux/mazlauncher.py" "$BUILD/pkg/usr/share/mazlauncher/"
cat > "$BUILD/pkg/DEBIAN/control" <<EOF
Package: mazlauncher-linux
Version: $VERSION
Section: games
Priority: optional
Architecture: all
Maintainer: MazClient Contributors
Depends: python3 (>= 3.10), python3-pyside6.qtwidgets
Description: MazLauncher Linux development preview
 Qt desktop preview of the MazClient launcher.
EOF
cat > "$BUILD/pkg/usr/bin/mazlauncher" <<'EOF'
#!/bin/sh
exec /usr/bin/python3 /usr/share/mazlauncher/desktop.py "$@"
EOF
chmod 755 "$BUILD/pkg/usr/bin/mazlauncher"
cat > "$BUILD/pkg/usr/share/applications/mazlauncher.desktop" <<'EOF'
[Desktop Entry]
Name=MazLauncher
Comment=MazClient Linux development preview
Exec=mazlauncher
Terminal=false
Type=Application
Categories=Game;
EOF
dpkg-deb --build --root-owner-group "$BUILD/pkg" "$ROOT/dist/mazlauncher-linux_${VERSION}_all.deb"
echo "Created dist/mazlauncher-linux_${VERSION}_all.deb"
