# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

## Project truth
The repository is the canonical source for product scope, implementation order, capability status and homologation state. Chat history is supplementary only.

Read first:
- [`docs/PRODUCT_VISION.md`](docs/PRODUCT_VISION.md) — final product destination and principles
- [`docs/PRODUCT_REQUIREMENTS.md`](docs/PRODUCT_REQUIREMENTS.md) — functional/non-functional requirements
- [`docs/IMPLEMENTATION_ROADMAP.md`](docs/IMPLEMENTATION_ROADMAP.md) — milestone sequence and exit gates
- [`docs/CODEC_SUPPORT_MATRIX.md`](docs/CODEC_SUPPORT_MATRIX.md) — planned vs implemented vs verified audio formats
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — module boundaries and design rules
- [`docs/MANAGED_MEDIA_POLICY.md`](docs/MANAGED_MEDIA_POLICY.md) — immutable source/derived-media rules
- [`docs/STUDIO_OPTIONS_AND_MIXER.md`](docs/STUDIO_OPTIONS_AND_MIXER.md) — Options, routing and Mixer contract
- [`docs/M4_COMMERCIAL_POLISH_CHECKPOINT.md`](docs/M4_COMMERCIAL_POLISH_CHECKPOINT.md) — M4 product-facing Studio/Mixer checkpoint
- [`docs/M5_RECORDING_IMPLEMENTATION_PLAN.md`](docs/M5_RECORDING_IMPLEMENTATION_PLAN.md) — M5 recording architecture and gates
- [`docs/M5_CAPTURE_ENGINE_CHECKPOINT.md`](docs/M5_CAPTURE_ENGINE_CHECKPOINT.md) — Android capture-engine checkpoint
- [`docs/TEST_AND_HOMOLOGATION_PLAN.md`](docs/TEST_AND_HOMOLOGATION_PLAN.md) — CI/device/hardware validation
- [`docs/CURRENT_STATE.md`](docs/CURRENT_STATE.md) — exact development/gate status
- [`docs/M5C_ALPHA09_UX_CONSOLIDATION.md`](docs/M5C_ALPHA09_UX_CONSOLIDATION.md) — alpha09 Studio UX contract
- [`docs/M5_ALPHA09_HOMOLOGATION_CHECKLIST.md`](docs/M5_ALPHA09_HOMOLOGATION_CHECKLIST.md) — physical alpha09 acceptance gate
- [`docs/M5_ALPHA10_CORRECTIVE_CHECKPOINT.md`](docs/M5_ALPHA10_CORRECTIVE_CHECKPOINT.md) — post-homologation UX corrections

## Current development state
Stable `main` remains the signed `0.2.0-alpha03` baseline. Active work is isolated on `dev/parallel-m3-m5` in draft PR #1. The branch app version is `0.2.0-alpha10`, versionCode 11.

M2 Pocket Amp physical homologation is **PASS / CLOSED** for Samsung SM-X230 on Android 16/API 36. M3 established the WAV codec/import foundation. The signed M4 alpha07 candidate is currently under physical/UI validation while development continues on M5.

M5.A established project-managed recording transactions and the float-WAV writer. M5.B added the Android capture engine, input-route enforcement, live metering and monitoring. M5.C integrates permission, the mandatory visible/cancelable five-second countdown, exact-one-arm targeting, capture, safe partial takes, atomic promotion, automatic clip/waveform insertion and compatible backing playback. Alpha09 passed physical validation with minor UX findings; alpha10 is the corrective candidate for Trim readability and professional drag/drop feedback.

WAV-first does **not** mean WAV-only: multi-format import/export remains explicit product scope and is tracked in the codec matrix.

## Build
GitHub Actions in `.github/workflows/android-ci.yml` is the canonical remote executor. Routine CI performs source materialization, JDK/Android setup, unit tests, Android Lint, debug APK assembly and artifact upload.

Signed homologation is separate. Private signing material is CI-only, the signer fingerprint is checked, and secret material is never committed.

## Security
Never commit keystores, signing credentials, local SDK configuration, private caches or secret artifacts.
