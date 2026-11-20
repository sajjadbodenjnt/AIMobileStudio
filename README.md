# AI Mobile Studio

AI Mobile Studio is a ChatGPT-powered native Android app-building workspace. It uses ChatGPT for AI code generation, with a local Android project workspace and a guarded Gradle build controller. It does not include a local Ollama AI assistant.

## Current implementation

- Native Android application (Java, Android SDK).
- ChatGPT sign-in flow using OAuth/PKCE and encrypted Android Keystore credential storage.
- ChatGPT plan-usage permission check before calling the Responses API. This is not access to the user's private ChatGPT chat history.
- AI-generated project files are previewed and require user approval before being written.
- Project workspace path validation and backups for replaced files.
- Local project creation and source editing.
- ChatGPT is the app's AI provider; no Ollama installation, local model download, or local AI server is required.
- Persistent reference-image queue with no fixed app-imposed item count, multi-select picker, duplicate removal, and saved document permissions where supported.
- Reference images are decoded one at a time and downscaled to a maximum 2048-pixel edge before upload to reduce memory pressure.
- Termux bridge exports a project ZIP through Android's Storage Access Framework, excludes build caches and signing secrets, bundles a Gradle build script, and imports the resulting APK through a user-selected document.\n- ARM64 Termux bootstrap installs/checks JDK 21, Gradle and native Android utilities, detects the SDK, attempts API 35 platform provisioning through an existing sdkmanager, and reports missing components.
- GitHub Actions workflow builds the AI Mobile Studio app itself and uploads its debug APK as a workflow artifact.

## Important capability boundaries

This repository is an actively developed builder, not yet a verified one-click professional APK factory. Image queue capacity is still bounded by device storage/RAM, network transport, request size, and the connected ChatGPT model/account/service limits; the app does not bypass those provider limits. Current generation sends the selected queue in a request, so very large queues still need the planned resumable batch-analysis pipeline.

The Android app does not execute Termux's private binaries (Android sandbox isolation prevents that). It now includes an explicit SAF bridge: export a selected project as a ZIP, run its bundled `AIMobileStudio-Termux-Build.sh` inside Termux, then import the resulting APK back into the app for installation. This is a user-confirmed handoff, not a silent background build.

The GitHub Actions workflow builds **AI Mobile Studio itself**. Arbitrary generated projects are built locally through the explicit Termux bridge, not by the GitHub workflow.

A debug APK produced by CI is for testing. A production release requires a user-controlled signing key and a release-signing flow.

## Build AI Mobile Studio

Requirements: JDK 17, Android SDK Platform 35, Android Build Tools 35.0.0, Gradle 8.9.

    gradle --no-daemon clean assembleDebug

The APK is produced at:

    app/build/outputs/apk/debug/app-debug.apk

GitHub Actions uploads the same APK as `aimobilestudio-debug-apk`.

## ChatGPT connection

Use the in-app **Continue with ChatGPT** action. The user must approve the requested OAuth scopes and must be eligible for direct ChatGPT plan usage under OpenAI's current rules. If the required plan-usage permission is not granted, AI generation remains disabled. The app does not scrape browser cookies or read private ChatGPT conversations.

## Termux

The optional script `scripts/install-termux-toolchain.sh` prepares a Termux-side toolchain. See [Termux Toolchain Setup](docs/TERMUX_TOOLCHAIN.md). Android app sandbox isolation means a dedicated, explicit bridge is still required before the app can invoke Termux binaries.

## Security notes

- Credentials are encrypted using Android Keystore.
- Generated file paths are constrained to the selected project workspace.
- Existing files are backed up before replacement.
- Builds require explicit user approval.
- Never commit OAuth tokens, API keys, signing keystores, or passwords.
