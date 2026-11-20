# ARM64 Android build toolchain for AI Mobile Studio

AI Mobile Studio uses ChatGPT for code generation. Android builds run in a separate, user-approved Termux process because Android applications cannot execute another app's private binaries or SDK files.

## Automated bootstrap

In the main Termux app (not Ubuntu/proot), run:

```bash
bash scripts/install-termux-toolchain.sh
```

The bootstrap now:
- updates Termux package indexes;
- installs/checks OpenJDK 21, Gradle, Android platform utilities, AAPT/AAPT2, D8, apksigner, curl and archive tools;
- detects the SDK path (defaults to `$HOME/lib/android-sdk-9123335`);
- installs Android API 35 when a usable `sdkmanager` is already present;
- reports missing tools and writes a small environment file at `~/.aimobilestudio-toolchain.env`.

Load the detected environment in Termux:

```bash
source ~/.aimobilestudio-toolchain.env
```

If the SDK is elsewhere, set it before running the bootstrap:

```bash
export ANDROID_HOME="$HOME/lib/android-sdk-9123335"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
bash scripts/install-termux-toolchain.sh
```

The bootstrap deliberately does not blindly run Google's Linux SDK Build Tools binaries on ARM64. Several official binaries are x86_64-only; Termux-native utilities must be used where needed. It also does not download unofficial prebuilt binaries from unverified mirrors.

## What must be present for a build

- ARM64-compatible JDK 17 or 21;
- Gradle or a project Gradle Wrapper distribution;
- Android SDK Platform 35 (or the platform required by the project);
- Android Gradle Plugin and all required Maven dependencies;
- ARM64-compatible AAPT2 and other build utilities required by the project's AGP version;
- sufficient free storage and memory.

The app's current private `BuildController` expects a separate app-private toolchain at `files/toolchain`. The Termux bootstrap does not populate that private directory. The currently supported on-device route is the explicit project ZIP export to Termux, build there, then import the produced APK.

## Build and verify

The exported Termux build script supports debug and release APK tasks. It checks the SDK and Gradle, builds the Android application module(s), copies non-empty APKs to:

```
~/storage/downloads/AIMobileStudio-Builds/
```

It verifies APK signatures with `apksigner` when available. A release build is not production-ready unless the project is configured with the user's own signing key. Never put a keystore, signing password, OAuth token, or API key in a project ZIP or AI prompt.

AAB generation and automated error-repair/rebuild are still separate implementation tasks; do not assume they are available until the app exposes those actions and a real build test passes.

## Validation

A successful bootstrap is only a tool-presence check. A genuine readiness test must build a real Android project, verify the APK with apksigner, and install it on the target device. Offline readiness must be tested separately with network access disabled after all Gradle/Maven artifacts are cached.

## Security boundaries

- Android app sandboxing prevents AIMobileStudio from directly reading Termux's private SDK directory.
- Termux command execution must be explicitly permission-gated by the user.
- Project ZIP export excludes build caches, local.properties, environment files, keystores and credential-like files.
- Android's package installer still requires user confirmation.
