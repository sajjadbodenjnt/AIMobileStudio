#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
if [ "$#" -lt 1 ]; then
  echo "Usage: bash termux-build.sh /path/to/AndroidProject [debug|release|aab]"
  exit 2
fi
PROJECT="$(realpath "$1")"
MODE="${2:-debug}"
if [ ! -f "$PROJECT/settings.gradle" ] && [ ! -f "$PROJECT/settings.gradle.kts" ]; then
  echo "Not a Gradle Android project: $PROJECT"; exit 2
fi
pkg update -y
pkg install -y openjdk-17 gradle aapt aapt2 apksigner zipalign
export JAVA_HOME="$PREFIX/opt/openjdk"
export PATH="$JAVA_HOME/bin:$PATH"
cd "$PROJECT"
case "$MODE" in
  debug) gradle --no-daemon assembleDebug ;;
  release) gradle --no-daemon assembleRelease ;;
  aab) gradle --no-daemon bundleRelease ;;
  *) echo "Mode must be debug, release, or aab"; exit 2 ;;
esac
echo "Build finished. Outputs:"
find app/build/outputs -type f \( -name '*.apk' -o -name '*.aab' \) -print
