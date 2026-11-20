# AI Mobile Studio — Engineering Roadmap

This roadmap tracks verifiable capabilities. “100% professional” is not a testable guarantee; every milestone must be proven by builds, tests and real-device validation.

## M1 — Native project generation
- [x] Generate a standard Java Android Gradle project.
- [x] Create manifest, app module, Gradle configuration and starter Activity.
- [ ] Add Kotlin and reusable Android resource/layout templates.
- [ ] Add recursive project explorer, rename/delete and safe file operations.

## M2 — ChatGPT-powered app creation
- [x] Official system-browser OAuth/PKCE sign-in flow for eligible ChatGPT plan usage.
- [x] Encrypted Android Keystore credential storage and permission gating.
- [x] Responses API code-generation path and authorized model discovery.
- [x] Bengali/English app instructions and reference-image queue.
- [x] Preview generated file proposals and require approval before applying.
- [ ] Add durable, resumable image batches with per-batch summaries and retry.
- [ ] Add full diff preview, backup restore UI, audit log and safe rollback.
- [ ] Device-test OAuth, image uploads, long generations, cancellation and token refresh.

## M3 — Professional editor
- [ ] Syntax highlighting, line numbers, tabs, search/replace and undo/redo.
- [ ] Diagnostics panel and Java/Kotlin language tooling where feasible.
- [ ] XML/resource editor, asset manager and UI preview.
- [ ] Project import/export using Android Storage Access Framework.

## M4 — Real Android build pipeline
- [x] Add guarded Gradle build controller with explicit approval and offline-only execution.
- [x] Add Termux ARM64 host-tool bootstrap and diagnostics documentation.
- [ ] Choose and implement a supported build runtime: verified app-private toolchain or permissioned Termux bridge.
- [ ] Provision compatible JDK, Gradle, Android SDK platform/build-tools and Maven/Gradle dependencies.
- [ ] Add verified downloads, checksums, resumable provisioning and storage estimates.
- [ ] Run generated projects through Gradle and show live logs, progress, cancellation and actionable errors.
- [ ] Verify APK with apksigner and install through Android Package Installer.
- [ ] Add user-owned keystore setup, release APK and AAB export.

## M5 — Quality and security
- [ ] Automated unit and instrumentation tests.
- [ ] Lint, dependency/license report and secure credential handling.
- [ ] Crash recovery, autosave, project backup/restore.
- [ ] Build, install and smoke-test generated apps on real Android devices.
- [ ] Validate permissions, network security, data storage and accessibility.
- [ ] End-to-end acceptance: prompt → project → review → build → sign → install.

## Explicit product decisions
- ChatGPT is the only AI provider. Do not add Ollama, local models or another AI backend.
- ChatGPT generation requires internet, user authorization and eligibility under the applicable OpenAI service rules.
- Local project files and the Android build process may operate offline only after all required toolchain/dependency packages are provisioned.
- No app-imposed image-count limit is a promise of unlimited provider usage; request size, memory, network and service limits still apply.
- CI builds of AI Mobile Studio itself do not prove that arbitrary generated projects can be built on-device.

## Current verified boundary

The app contains a native Android shell, ChatGPT OAuth/API integration, project generation proposals, workspace safeguards, a persistent reference-image queue and a guarded build controller. The app-private Android build toolchain is not yet provisioned, and the Termux bootstrap does not itself bridge Termux's private files into the app sandbox. The generated-project build, signing, installation and end-to-end workflows remain unverified.
