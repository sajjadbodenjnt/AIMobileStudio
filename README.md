# Offline Android IDE

Native Android IDE foundation for building Java/Kotlin Android apps on an Android phone.

## Phase 1 included
- Native Java Android application shell
- App-private project workspace
- Basic Java source editor with save/open
- Local-only AI connector for an Ollama server at 127.0.0.1:11434
- Build toolchain readiness screen

## Not yet included
This is the first working foundation, not yet a complete on-device IDE. The full Android SDK/Gradle build runner, Kotlin compilation, syntax highlighting, APK signing/install pipeline, and bundled llama.cpp runtime/model are Phase 2+ tasks.

## Build
Open in Android Studio with JDK 17, Android SDK Platform 35, Android Gradle Plugin 8.7.3 and Gradle 8.9+. First dependency resolution requires network access; after dependencies and SDK components are cached, builds can run offline.

## Local AI
Install and download an Ollama model once, then run Ollama locally. Default model: qwen2.5-coder:3b. The app calls only the loopback endpoint and does not use a cloud AI API.

## Offline requirements
For truly offline operation, Gradle, Android Gradle Plugin, SDK platform/build-tools, all dependency artifacts, and model weights must already be available on-device.
