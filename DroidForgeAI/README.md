# DroidForge AI IDE

An Android-native, offline-first AI app-builder prototype. This project is isolated under `DroidForgeAI/` so the existing AI Mobile Studio app remains unchanged.

## Current status

- [x] Landscape developer IDE shell (Compose Material 3)
- [x] Qwen GGUF downloader with progress and app-private storage
- [x] JNI interface and llama.cpp CMake integration (ARM64)
- [x] Prompt/code-generation service and Kotlin fence extraction
- [x] Generated Android project scaffolding
- [ ] Verify native build against the pinned llama.cpp revision on a real ARM64 device
- [ ] Complete and validate APK compilation on-device
- [ ] Automated compiler-error repair loop and installer handoff

**Important:** Android does not provide a supported in-app API for running the full Android Gradle Plugin toolchain. This first source drop scaffolds projects and generates code; it does not claim that the Build APK button already produces installable APKs. The supported next step is a Gradle build runner (for example, a local Termux/PC build or a controlled build service), then wire its result into FileProvider installation.

## Model

Default model URL:
`https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF/resolve/main/qwen2.5-coder-3b-instruct-q4_k_m.gguf`

The model is approximately 2 GB. Download it on Wi-Fi and ensure sufficient free storage and RAM. Inference is local after download.

## Build in Android Studio

- Android Studio with JDK 17
- Android SDK Platform 35
- Android NDK 27.2.12479018
- CMake 3.22.1+

Open the `DroidForgeAI` directory as a standalone Gradle project and build `:app:assembleDebug`.

## Safety and limitations

- Model files are downloaded to app-private storage; no model is bundled in the APK.
- No API key is required for local inference.
- Qwen output is untrusted source code. Review generated files before building or installing.
- A 3B Q4 model needs substantial memory; devices with 8 GB RAM may still experience memory pressure.
