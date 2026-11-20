#!/data/data/com.termux/files/usr/bin/bash
# AIMobileStudio ARM64 build-toolchain bootstrap for Termux.
# Run: bash scripts/install-termux-toolchain.sh
set -Eeuo pipefail

if [[ "${PREFIX:-}" != "/data/data/com.termux/files/usr" ]]; then
  echo "ERROR: Run this script inside Termux (not inside Ubuntu/proot)." >&2
  exit 1
fi

echo "== AIMobileStudio Termux toolchain bootstrap =="
echo "This installs host-side tools in Termux. It does not copy Linux binaries into the Android app sandbox."
pkg update -y

for package in openjdk-17 gradle android-tools aapt; do
  echo
  echo "Installing/checking: $package"
  if ! pkg install -y "$package"; then
    echo "WARNING: Package '$package' was unavailable or failed. Continuing; the final doctor will report missing tools." >&2
  fi
done

# Optional package name differs across Termux repository snapshots.
if ! command -v apksigner >/dev/null 2>&1; then
  pkg install -y apksigner || true
fi

echo
echo "== Detected tools =="
for tool in java javac gradle adb aapt aapt2 apksigner; do
  if command -v "$tool" >/dev/null 2>&1; then
    printf '%-12s %s\n' "$tool" "$(command -v "$tool")"
  else
    printf '%-12s MISSING\n' "$tool"
  fi
done

if command -v java >/dev/null 2>&1; then java -version 2>&1 | head -n 3; fi
if command -v gradle >/dev/null 2>&1; then gradle --version | head -n 8; fi

# Discover an existing SDK; do not download or overwrite one automatically.
SDK=""
for candidate in "${ANDROID_SDK_ROOT:-}" "${ANDROID_HOME:-}" "$HOME/lib/android-sdk" "$HOME/Android/Sdk"; do
  if [[ -n "$candidate" && -d "$candidate/platform-tools" || -n "$candidate" && -d "$candidate/build-tools" ]]; then
    SDK="$candidate"
    break
  fi
done

if [[ -n "$SDK" ]]; then
  echo
  echo "Android SDK detected: $SDK"
  export ANDROID_HOME="$SDK"
  export ANDROID_SDK_ROOT="$SDK"
  [[ -d "$SDK/platforms/android-35" ]] && echo "Android API 35: FOUND" || echo "Android API 35: MISSING"
  [[ -d "$SDK/build-tools/35.0.0" || -d "$SDK/build-tools/37.0.0" ]] && echo "Android Build Tools: FOUND" || echo "Android Build Tools: MISSING"
  [[ -x "$SDK/platform-tools/adb" ]] && echo "SDK adb: FOUND" || true
else
  echo
  echo "Android SDK: NOT DETECTED"
  echo "Set ANDROID_HOME/ANDROID_SDK_ROOT to your installed SDK, then rerun."
fi

echo
echo "== Bootstrap finished =="
echo "This script prepares the Termux host toolchain only."
echo "AIMobileStudio's in-app BuildController still needs a secure Termux bridge before it can invoke these host tools."
echo "Do not assume an offline build is ready until JDK, Gradle, API 35, Build Tools, and all Maven artifacts are present."
