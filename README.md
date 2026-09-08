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
- [`docs/TEST_AND_HOMOLOGATION_PLAN.md`](docs/TEST_AND_HOMOLOGATION_PLAN.md) — CI/device/hardware validation
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — durable product/architecture decisions
- [`docs/CURRENT_STATE.md`](docs/CURRENT_STATE.md) — exact development/gate status

## Current development state
Stable `main` remains the signed M2 `0.2.0-alpha03` baseline. Active work is isolated on `dev/parallel-m3-m5` in draft PR #1. The branch is currently `0.2.0-alpha04` and M4 is in progress.

Important: the M2 Pocket Amp physical hardware gate remains OPEN. Parallel M3/M4 progress does not close it.

M3 established the codec architecture and WAV vertical slice. WAV-first does **not** mean WAV-only: multi-format import/export is an explicit final-product requirement and is tracked in the codec matrix.

## Build
GitHub Actions in `.github/workflows/android-ci.yml` is the canonical remote executor. Routine development CI performs source materialization, JDK/Android setup, unit tests, Android Lint, debug APK assembly and diagnostics/artifact upload.

Signed homologation is a separate controlled path. Private signing material is supplied only through CI secrets, the output signer is checked against the locked expected fingerprint, and secret material is never committed.

## Security
Never commit keystores, signing credentials, local SDK configuration, generated APKs intended as secrets, or private cache bundles. `.gitignore` and the CI pipeline are designed around that rule.
