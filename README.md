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
- [`docs/TIMELINE_INTERACTION_GUIDELINES.md`](docs/TIMELINE_INTERACTION_GUIDELINES.md) — marker/transport UX contract
- [`docs/STUDIO_WORKSPACE_GUIDELINES.md`](docs/STUDIO_WORKSPACE_GUIDELINES.md) — single-screen timeline-first Studio contract
- [`docs/STUDIO_OPTIONS_AND_MIXER.md`](docs/STUDIO_OPTIONS_AND_MIXER.md) — Options, routing and Mixer contract
- [`docs/TEST_AND_HOMOLOGATION_PLAN.md`](docs/TEST_AND_HOMOLOGATION_PLAN.md) — CI/device/hardware validation
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — durable decisions
- [`docs/CURRENT_STATE.md`](docs/CURRENT_STATE.md) — exact development/gate status

## Current development state
Stable `main` remains the signed `0.2.0-alpha03` baseline. Active work is isolated on `dev/parallel-m3-m5` in draft PR #1. The branch app version is `0.2.0-alpha05`, versionCode 6, but branch code has moved beyond the historical signed alpha05 artifact.

M2 Pocket Amp physical homologation is **PASS / CLOSED** for Samsung SM-X230 on Android 16/API 36. M3 established the WAV codec/import foundation. M4 is still in progress and now includes the real playback/trim path plus a substantially redesigned single-screen Studio, immersive fullscreen, Options Center, output routing, functional track mixer and Master.

The current M4 software checkpoint adds per-track metering, peak hold/decay and durable project master gain. Run #203 was green for the immediately preceding master-gain/master-meter checkpoint. The current branch head still requires its own CI gate and later a new signed physical consolidation candidate.

WAV-first does **not** mean WAV-only: multi-format import/export remains explicit product scope and is tracked in the codec matrix. Recording remains M5 and must not be inferred from the presence of arm metadata or the red transport affordance.

## Build
GitHub Actions in `.github/workflows/android-ci.yml` is the canonical remote executor. Routine CI performs source materialization, JDK/Android setup, unit tests, Android Lint, debug APK assembly and artifact upload.

Signed homologation is separate. Private signing material is CI-only, the signer fingerprint is checked, and secret material is never committed.

## Security
Never commit keystores, signing credentials, local SDK configuration, private caches or secret artifacts.
