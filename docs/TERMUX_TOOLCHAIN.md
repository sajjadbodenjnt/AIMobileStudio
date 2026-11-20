# Termux ARM64 Build Toolchain

AIMobileStudio needs a real Android build toolchain, not only a Build button.

## Supported preparation path

For Android phones running Termux on ARM64, this repository provides:

```bash
bash scripts/install-termux-toolchain.sh
```

The bootstrap installs/checks available Termux host packages (JDK 17, Gradle, Android platform utilities) and detects an existing Android SDK. It deliberately does not overwrite or silently download an SDK into an arbitrary directory.

Set the SDK path before running it if needed:

```bash
export ANDROID_HOME="$HOME/lib/android-sdk-9123335"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
bash scripts/install-termux-toolchain.sh
```

The app now provides a Termux handoff: select a project in APK Build Controller, export its ZIP to Downloads, and copy the displayed commands into Termux. The ZIP contains a build script that downloads Gradle 8.9 if needed, uses the detected Android SDK, resolves dependencies online, builds `assembleDebug`, and copies the APK to `~/storage/downloads/AIMobileStudio-Builds/`. In the app, choose **Import APK built in Termux** and select that APK. This requires internet on the first build unless all Gradle/Maven artifacts are already cached.

## Required components

- JDK 17 (Java and javac)
- Gradle compatible with the project's Android Gradle Plugin
- Android SDK Platform 35
- Android Build Tools (35.0.0 or a project-compatible version)
- Platform tools where needed
- Android Gradle Plugin and every transitive Maven artifact cached locally
- Project dependencies and Gradle wrapper distribution cached locally

After the first successful online dependency resolution, test with:

```bash
gradle --offline --no-daemon assembleDebug
```

A genuine offline test must be performed with network access disabled. If Gradle reports a missing artifact, the toolchain is not yet fully provisioned.

## Important Android sandbox boundary

Termux and AIMobileStudio are separate Android applications. AIMobileStudio cannot simply execute Termux's private binaries or read its private SDK directory. The current in-app BuildController expects an app-private toolchain at `files/toolchain`; the Termux bootstrap above does not populate that directory.

The bridge is implemented as an explicit SAF export/import workflow. It does not cross the Android app sandbox or execute Termux commands silently; the user runs the exported script in Termux and then selects the APK to import.

## Build pipeline and limitations

1. Provision and validate the toolchain.
2. Resolve/cache Gradle wrapper, AGP and Maven dependencies.
3. Run Gradle offline with cancellation and bounded log capture.
4. Locate and validate the resulting APK.
5. Sign with a user-controlled signing key, verify with apksigner.
6. Hand the APK to Android's package installer.
7. Test the entire flow in airplane mode.

Never put signing passwords, keystore files, OAuth tokens, or other credentials into AI prompts or project context.
