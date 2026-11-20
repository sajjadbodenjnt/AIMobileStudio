# ChatGPT-powered AI Agent for AIMobileStudio

## Goal

Allow the user to authorize AIMobileStudio with their own ChatGPT account and, when eligible and explicitly approved, use ChatGPT plan usage for coding-agent requests. The app must also retain a separate local-model mode for airplane-mode work.

This is an engineering design, not a claim that the integration is already implemented.

## Important product boundaries

- Use the official **Sign in with ChatGPT** open-source-app flow. Do not imitate ChatGPT's website login, scrape cookies, collect passwords, or embed a ChatGPT web session.
- ChatGPT identity and permission to use ChatGPT plan usage are separate grants. AI inference is enabled only if the OAuth result includes the required `chatgpt.tokens.use.direct` permission.
- This integration does not grant access to the user's ChatGPT conversations.
- Eligibility and rollout rules are controlled by OpenAI. The documented ChatGPT-plan-usage path is for open-source tools and personal projects running locally; paid or remotely hosted distribution requires separate approval.
- ChatGPT plan usage is not the same as API-key billing. Keep an optional API provider separate and never place a developer API key in the APK.
- The Android implementation must follow the current OpenAI open-source registration/sign-in contract and terms. The published reference implementation is Electron/Node-oriented; porting it to Android requires independent review and testing, not blind reuse of desktop code.

## Proposed architecture

1. **Android UI** — Continue with ChatGPT, connection status, account selection, model picker, usage-management link, disconnect/revoke controls.
2. **Auth coordinator** — Generate and persist a per-installation `ext_agent_host_id`; use system browser, authorization state, nonce and PKCE; receive the loopback callback; exchange the one-time code; validate the identity token and granted scopes.
3. **Credential vault** — Keep tokens out of WebView/JavaScript, logs, project files and Git. Encrypt persisted credentials with Android Keystore-backed encryption. Support refresh, expiry, account switching and logout.
4. **Inference client** — Discover models for the selected authorized profile and call the supported Responses API flow only when the required permission is present. Handle cancellation, streaming, usage limits, revoked consent and network errors.
5. **Agent orchestrator** — Convert user requests into a plan and typed tool calls. Do not expose arbitrary shell execution.
6. **Scoped tools** — List projects, read files, propose file writes/patches, inspect build output, run approved builds, and show APK metadata. Enforce canonical-path checks, project-root confinement, file-size limits, backups and audit records.
7. **Build controller** — Initially report toolchain status and build logs. Add build/sign/install actions only after the on-device toolchain exists; require user confirmation before signing or installing.
8. **Offline provider** — Retain the local model connector and later embed a supported local runtime. ChatGPT-connected mode requires network; offline mode uses the local model and cached toolchain.

## Security requirements

- Never ask for the user's ChatGPT password or accept pasted browser cookies.
- Never ship a client secret or developer API key inside the Android APK.
- Use fresh state, nonce and PKCE for every authorization transaction; validate issuer, audience, nonce, expiry and the exact granted scopes.
- Bind the OAuth callback listener to loopback only, use a one-time callback and close it after completion.
- Store credentials only in app-private storage protected with Android Keystore-backed encryption.
- Do not log access/refresh tokens, authorization codes, prompts containing secrets, or private source files.
- Require explicit user approval for destructive edits, build execution, keystore use, APK installation and any external network action.
- Restrict all project tools to the selected workspace; reject absolute paths, traversal, symlinks escaping the workspace and oversized payloads.
- Provide disconnect and local credential deletion controls.

## Acceptance tests

1. Fresh install creates and persists one stable host ID.
2. Sign-in opens the system browser and returns to the app through the registered loopback callback.
3. Invalid state, nonce, issuer, audience, expired code and malformed token are rejected.
4. Identity-only consent does not enable model requests.
5. Inference works only when the required plan-usage scope is granted and an authorized model is available.
6. Refresh, expiry, account switching, logout, revoked consent and usage-limit errors are handled.
7. Tokens never appear in logs, exported projects or app backups.
8. Agent file operations cannot escape the selected project root and can be reviewed/reverted.
9. Airplane mode disables ChatGPT mode cleanly while local AI and cached build tools remain usable.
10. End-to-end tests verify generated project → edit → build → sign → install on a real supported Android device.

## Implementation sequence

- A: verify current OpenAI open-source sign-in contract, SDK licensing and Android-compatible callback/token requirements.
- B: implement and test sign-in/profile/credential lifecycle without agent file tools.
- C: add model discovery and a minimal inference test.
- D: add scoped project tools with review/undo and audit log.
- E: connect build controller only after a real on-device build engine is available.
- F: complete airplane-mode and real-device acceptance tests.

## References

- https://developers.openai.com/siwc/quickstart
- https://developers.openai.com/siwc/token-sharing-open-source
- https://developers.openai.com/siwc/token-sharing-open-source/sign-in
- https://developers.openai.com/siwc/token-sharing-open-source/errors-and-recovery
- https://developers.openai.com/cookbook/articles/sign-in-with-chatgpt
