# GuitarLab Studio

Android-first guitar study, recording and audio-diagnostics project.

Current validated development baseline: **M2 0.2.0-alpha03 (versionCode 4)**.

## Current gate

M2 is validating the Android audio path before timeline/audio-production features are unlocked. The alpha03 diagnostic hardens microphone capture by preferring media sample rates for the built-in mic, explicitly testing the microphone source, reporting Android microphone privacy/mute state, and refusing to pass a capture that contains only digital zero.

## Build

The canonical remote executor is GitHub Actions in `.github/workflows/android-ci.yml`.

Every push to `main` runs:

- JDK 17
- Android SDK platform 37 / target 36
- Build Tools 36.0.0
- Gradle 9.6.1 with persistent GitHub Actions cache
- unit tests
- Android Lint
- debug APK assembly

A signed homologation APK is available through manual workflow dispatch after the private signing secret is configured. The signing material is never committed to this repository, and the produced APK certificate is checked against the locked M2 homologation fingerprint before publication.

## Security

Keystores, credentials, local Android configuration, APK outputs and portable caches are excluded from Git. Do not commit private signing material.
