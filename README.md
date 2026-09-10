# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

## Repository truth
The repository is the canonical source for scope, architecture, implementation state and homologation evidence. Chat history is supplementary only.

Read first:
- `docs/CURRENT_STATE.md` — live milestone/gate state;
- `docs/IMPLEMENTATION_ROADMAP.md` — milestone sequence and remaining release work;
- `docs/ARCHITECTURE.md` — current module, media, lifecycle and CI architecture;
- `docs/CODEC_SUPPORT_MATRIX.md` — implemented vs JVM/emulator/target-verified codec capabilities;
- `docs/MANAGED_MEDIA_POLICY.md` — immutable-source/proxy/project-package contract;
- `docs/TIMELINE_INTERACTION_GUIDELINES.md` — timeline/drag/trim contract;
- `docs/STUDIO_OPTIONS_AND_MIXER.md` — Studio, routing, Mixer and Share-modal contract;
- `docs/TEST_AND_HOMOLOGATION_PLAN.md` — automated + residual physical gate policy;
- `docs/M7_ALPHA1_HOMOLOGATION_CHECKLIST.md` — historical filename retained for the current residual M7/final-RC physical checklist;
- `docs/M8_GLOBAL_DIGITAL_REGRESSION.md` — global regression matrix and corrected-defect evidence;
- `docs/HISTORICAL_CANDIDATES.md` — superseded candidates.

## Current development state
- stable `main`: unchanged stable signed baseline;
- active integration: `dev/parallel-m3-m5`, draft PR #1;
- M5: PASS/CLOSED by explicit physical approval of `0.2.0-alpha14`;
- M6: PASS/CLOSED by explicit physical approval of `0.3.0-alpha1`;
- M7 implementation candidate: `0.4.0-alpha2`, versionCode 18; M7 remains OPEN only for the final residual physical gate;
- signed expanded-regression checkpoint: `0.4.0-alpha2`, commit `ca5b9d57ed07bb9cbd27a5da61386cc764209fd0`, push CI #465;
- latest functional/CI hardening baseline before this documentation synchronization: `922c1c499248800ecce2ddf447c5201d95bbe9cb`, canonical CI #545;
- no new signed RC is promoted until the consolidated digital gates are green and the final physical candidate is intentionally cut.

Post-alpha2 hardening covers managed-media loss prevention/recovery, process-death staging cleanup, lossless interrupted-recording preservation and WAV-header repair, safer SAF publication, deterministic codec timestamps, FLAC container correction, accessibility semantics, JVM performance evidence and Android API 36 instrumented regression.

## Build, regression and signing
`.github/workflows/android-ci.yml` is the canonical executor. Each active-branch candidate runs two independent required gates in parallel:
1. software gate — diff sanity, unit tests, reproducible performance evidence, Android Lint and debug APK assembly;
2. Android integration gate — API 36 emulator instrumentation, including lifecycle recreation, Mixer accessibility behavior and Android codec integration.

The emulator AVD is snapshot-cached to reduce repeated CI setup cost. The signed homologation job has `needs` on both gates and remains skipped unless explicitly requested by `[sign-homologation]` or authorized manual dispatch. Signing material exists only inside the runner, the certificate identity is checked cryptographically, and temporary signing files are destroyed after the job.

## Physical validation policy
Automatable mathematics, persistence invariants, malformed-input handling, lifecycle recreation, codec structure and UI semantics are not delegated back to the user. The final physical gate is intentionally residual: Samsung/Pocket Amp USB routing and capture, target-specific MP3 encoder availability, subjective latency/listening quality, real-tablet stress and tactile/visual ergonomics.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts.
