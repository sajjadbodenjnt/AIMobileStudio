# AI Mobile Studio

A fresh Android-native foundation for an AI-assisted Android project builder.

## Current implementation
- Native Java Android app (no AndroidX dependency).
- Create and browse local Android project folders.
- Prompt-based code generation through the OpenAI Responses API.
- API key stored in Android app-private SharedPreferences (not committed to Git).
- Export/build workflow documented for Termux.

## Important authentication note
A ChatGPT consumer-account sign-in is not the same as OpenAI API access. This version uses an OpenAI API key supplied by the user. It does not claim to sign in with a ChatGPT subscription or provide API credits.

## Requirements
- Android Studio / Android SDK Platform 35
- JDK 17+
- Gradle 8.9 (wrapper can be generated locally)

## Build
Open this repository in Android Studio, allow Gradle sync, then build the `:app:assembleDebug` task.

## Termux build
See `scripts/termux-build.sh`. The script installs/checks the Termux-side build packages and builds a Gradle project directory passed as its first argument. Export/copy the project into Termux storage first.

## Security
Never commit API keys, signing keys, passwords, or local.properties. The app sends prompts to the OpenAI API only after the user chooses Generate.
