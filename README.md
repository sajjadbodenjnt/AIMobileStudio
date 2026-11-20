# AIMobileStudio — Offline Android App Builder

A native Android IDE project designed to grow into an on-device, offline-first app development environment.

## What works in this revision

- Native Android IDE shell and private project workspace.
- Basic source editor with UTF-8 open/save.
- **New Android App Project** creates a real Java + Gradle Android project (settings, root/app Gradle files, manifest, Java activity and README).
- Local AI connector targeting the loopback Ollama API (default model: qwen2.5-coder:3b).
- GitHub Actions debug APK build for testing the IDE itself.

## Important: honest capability status

This is an actively developed foundation, **not yet a complete professional offline Android Studio replacement**. The IDE APK compiles successfully in CI, but that does not mean generated apps can already be built inside the IDE. The current app does not yet bundle or execute the Android SDK, Gradle distribution, Kotlin compiler, a full APK signing pipeline, or an embedded local LLM runtime.

## Offline-first target architecture

1. **Project authoring:** project templates, file explorer, editor, syntax highlighting, diagnostics, UI preview and asset manager.
2. **On-device build engine:** a compatible JDK, Gradle distribution, Android SDK platform/build-tools, Android Gradle Plugin and every Maven artifact must be installed/cached on the device. The build runner must invoke Gradle in offline mode and clearly report missing components.
3. **APK pipeline:** compile, package, align, sign with a user-owned keystore, verify signature, then offer Android's package installer.
4. **Local AI:** integrate a local inference runtime (for example llama.cpp through JNI) and a user-selected model stored on-device. No cloud endpoint is required. The current Ollama connector is only an optional development bridge.
5. **Quality and safety:** build logs, lint, tests, crash-safe project writes, backups, dependency/license inventory and reproducible builds.

## Build the IDE

Use JDK 17, Android SDK Platform 35, Android Gradle Plugin 8.7.3 and Gradle 8.9. The GitHub Actions workflow builds the debug APK and uploads it as an artifact. First dependency resolution requires internet; after all SDK and Gradle/Maven artifacts are cached, Gradle can run with --offline.

## Prepare the Termux ARM64 toolchain\n\nRun `bash scripts/install-termux-toolchain.sh` inside Termux. It checks/installs available host tools and detects an existing Android SDK. Read [Termux Toolchain Setup](docs/TERMUX_TOOLCHAIN.md). This prepares the Termux environment only; the in-app Build Controller still needs a secure Termux bridge because Android apps cannot directly execute another app's private binaries.\n\n## Build a generated project

A generated project is a standard Android Gradle project. It needs JDK 17, Android SDK Platform 35, Build Tools and the matching Gradle/Android Gradle Plugin dependencies. After those are cached, run:

    gradle --offline assembleDebug

The APK is produced at:

    app/build/outputs/apk/debug/app-debug.apk

The current IDE app generates this project but does **not** yet run that build command itself.

## Local AI

The optional connector calls only http://127.0.0.1:11434/api/generate. A local Ollama server and downloaded model must already be running. This is not yet an embedded model runtime; the future offline target is an in-app local inference engine.

## Compatibility

The IDE currently targets Android API 26+ and uses Java. Generated projects target minSdk 24 and compileSdk 35.
