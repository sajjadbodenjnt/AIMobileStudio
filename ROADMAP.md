# Professional Offline IDE — Engineering Roadmap

This roadmap defines measurable milestones. “100% professional” is not a testable engineering claim, so each milestone must be proven by builds, tests and offline verification.

## M1 — Native project generation (in progress)
- [x] Generate a standard Java Android Gradle project.
- [x] Create manifest, app module, Gradle configuration and starter Activity.
- [ ] Add Kotlin project template and resource/layout templates.
- [ ] Add recursive project explorer, rename/delete and safe file operations.

## M2 — Editor experience
- [ ] Syntax highlighting, line numbers, tabs, search/replace and undo/redo.
- [ ] Diagnostics panel and Java/Kotlin language-server integration where feasible.
- [ ] XML layout editor, asset manager and live preview.
- [ ] Project import/export using Android Storage Access Framework.

## M3 — Real on-device build engine
- [ ] Define supported device ABI and storage/RAM requirements.
- [ ] Provision compatible JDK, Gradle, Android SDK platform/build-tools and AGP.
- [ ] Build a toolchain installer that downloads and verifies packages once.
- [ ] Execute Gradle builds with --offline; never silently fall back to network.
- [ ] Show complete build output, actionable errors and cancellation.
- [ ] Verify APK with apksigner and install via Android package installer.

## M4 — Embedded offline AI
- [ ] Select and document a redistributable code model and license.
- [ ] Integrate llama.cpp or another Android-compatible local inference runtime.
- [ ] Add model download/import, checksum verification, storage management and model settings.
- [ ] Stream generation, context budgeting, code patch review and explicit apply/revert.
- [ ] Test model inference in airplane mode.

## M5 — Professional quality gates
- [ ] Automated unit/instrumentation tests.
- [ ] Lint, dependency/license report and secure-keystore handling.
- [ ] Crash recovery, autosave, project backup/restore.
- [ ] Reproducible release build and install test on real Android devices.
- [ ] End-to-end airplane-mode test: create → edit → AI assist → build → sign → install.


## M6 — ChatGPT-connected coding agent (planned; not yet implemented)

- [x] Add initial Android system-browser Sign in with ChatGPT OAuth/PKCE flow for eligible open-source/local-app usage; no password collection or cookie scraping.
- [x] Persist per-installation host ID and implement browser OAuth with state, nonce and PKCE.
- [x] Verify RS256/JWKS signature, issuer, audience, nonce and expiry; gate inference on granted `chatgpt.tokens.use.direct`.
- [x] Add Android Keystore AES-GCM credential storage, refresh and disconnect. Multi-account switching and revocation UX remain.
- [x] Add authorized model discovery and Responses API coding-agent request path; live OAuth/inference still needs device testing.
- [x] Add workspace-scoped project context, generated-file proposals, path confinement and explicit apply approval.
- [ ] Add full diff preview, backup restore UI and audit log; current implementation makes backups and requires approval.
- [x] Add a guarded offline Gradle build controller; it remains inactive until the on-device toolchain is provisioned.
- [x] Retain the separate local Ollama provider; embedded local inference is still pending.
- [ ] Pass all acceptance tests in `docs/CHATGPT_AGENT_DESIGN.md`.

See [ChatGPT Agent Design](docs/CHATGPT_AGENT_DESIGN.md) for security boundaries, architecture and test criteria.

## Offline definition

After a one-time provisioning step, all editing, AI inference, dependency resolution, compilation, packaging, signing and installation must work with network access disabled. First-time downloads cannot be honestly described as offline. The installer must show which toolchain/model packages are missing and their storage footprint.

## Current limitation

The repository currently has a native Java shell, a basic editor, an Ollama loopback connector and the first Android project generator. It does not yet contain M2–M5. A successful CI build proves only that the IDE source compiles; it does not prove that the on-device build engine or embedded AI works.
