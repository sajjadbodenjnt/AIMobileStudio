#!/data/data/com.termux/files/usr/bin/bash
# AI Mobile Studio: ARM64 Termux toolchain bootstrap.
# Installs host-native Termux tools, provisions Android platform metadata, and verifies ARM64 executables.
set -Eeuo pipefail

fail(){ printf 'ERROR: %s\n' "$*" >&2; exit 1; }
info(){ printf '\n== %s ==\n' "$*"; }
ok(){ printf '  [OK] %s\n' "$*"; }
warn(){ printf '  [WARN] %s\n' "$*" >&2; }

[[ "${PREFIX:-}" == "/data/data/com.termux/files/usr" ]] || fail "Run this in the main Termux app, not Ubuntu/proot."
[[ "$(uname -m)" == "aarch64" ]] || warn "This bootstrap is primarily configured for ARM64; detected $(uname -m)."

SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/lib/android-sdk-9123335}}"
SDK="${SDK%/}"
export ANDROID_HOME="$SDK" ANDROID_SDK_ROOT="$SDK"
mkdir -p "$SDK"

info "Updating Termux package indexes"
pkg update -y

# These packages provide Android-host-native tools. Google Linux build-tools executables
# are x86_64 and must not be blindly executed on an ARM64 Android phone.
PACKAGES=(openjdk-21 gradle android-tools aapt aapt2 aidl d8 apksigner zipalign file curl wget unzip zip git)
for package in "${PACKAGES[@]}"; do
  if dpkg -s "$package" >/dev/null 2>&1; then
    ok "$package already installed"
  else
    info "Installing $package"
    pkg install -y "$package" || warn "Package $package is unavailable in the configured Termux repositories."
  fi
done

info "Checking Java and Gradle"
command -v java >/dev/null 2>&1 || fail "JDK is missing. Install openjdk-21 in Termux."
java -version 2>&1 | head -n 3
command -v gradle >/dev/null 2>&1 || fail "Gradle is missing. Install the Termux gradle package."
gradle --version | grep -E '^Gradle ' | head -n 1

info "Provisioning Android SDK platform 35"
if [[ ! -f "$SDK/platforms/android-35/android.jar" ]]; then
  SDKMANAGER=""
  for candidate in "$SDK/cmdline-tools/latest/bin/sdkmanager" "$SDK/cmdline-tools/bin/sdkmanager" "$HOME/lib/android-sdk/cmdline-tools/latest/bin/sdkmanager"; do
    if [[ -f "$candidate" ]]; then SDKMANAGER="$candidate"; break; fi
  done
  if [[ -n "$SDKMANAGER" ]]; then
    chmod +x "$SDKMANAGER" 2>/dev/null || true
    if command -v termux-fix-shebang >/dev/null 2>&1; then termux-fix-shebang "$SDKMANAGER" || true; fi
    export JAVA_OPTS="${JAVA_OPTS:-} -Djava.net.preferIPv4Stack=true"
    yes | "$SDKMANAGER" --sdk_root="$SDK" --licenses >/dev/null 2>&1 || true
    "$SDKMANAGER" --sdk_root="$SDK" "platforms;android-35" || fail "Could not install Android API 35 platform."
  else
    warn "sdkmanager not found. Android SDK platform 35 was not downloaded."
    warn "Google command-line tools can be placed at: $SDK/cmdline-tools/latest/bin/sdkmanager"
  fi
fi
[[ -f "$SDK/platforms/android-35/android.jar" ]] && ok "Android API 35 platform" || warn "Android API 35 platform is still missing."

info "Checking native ARM64 build utilities"
for tool in aapt aapt2 d8 apksigner zipalign adb; do
  if command -v "$tool" >/dev/null 2>&1; then
    path="$(command -v "$tool")"
    if file "$path" 2>/dev/null | grep -qi 'x86-64'; then
      warn "$tool resolves to x86_64; not safe to execute on ARM64."
    else
      ok "$tool: $path"
    fi
  else
    warn "$tool is missing."
  fi
done

if command -v aapt2 >/dev/null 2>&1; then
  AAPT2="$(command -v aapt2)"
  cat > "$HOME/.aimobilestudio-toolchain.env" <<EOF
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
export JAVA_HOME="$PREFIX"
export PATH="$PREFIX/bin:\$PATH"
export AAPT2_OVERRIDE="$AAPT2"
EOF
  ok "Environment saved to ~/.aimobilestudio-toolchain.env"
  ok "Use: source ~/.aimobilestudio-toolchain.env"
  ok "For Gradle Android builds, pass -Pandroid.aapt2FromMavenOverride=$AAPT2 when supported by the project."
fi

info "Final toolchain report"
printf 'Architecture: %s\n' "$(uname -m)"
printf 'Java: %s\n' "$(java -version 2>&1 | head -n 1)"
printf 'Gradle: %s\n' "$(gradle --version | grep '^Gradle ' | head -n 1)"
printf 'SDK root: %s\n' "$SDK"
[[ -f "$SDK/platforms/android-35/android.jar" ]] && echo "API 35: READY" || echo "API 35: MISSING"
[[ -x "$PREFIX/bin/aapt2" ]] && echo "Termux aapt2: READY" || echo "Termux aapt2: MISSING"
[[ -x "$PREFIX/bin/apksigner" ]] && echo "Termux apksigner: READY" || echo "Termux apksigner: MISSING"
echo
echo "Bootstrap completed. This is a toolchain readiness check, not proof that every Android Gradle project builds."
