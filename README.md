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

## Final signed RC
The active homologation candidate is `0.5.0-rc1` (versionCode 21), source commit `5a14e4d6522ab9cc53eb8e1dd80a03306fc9d248`. APK SHA-256: `5e343a9016cb5ea8fa9e381061ffb789667a6529a3b706fec7de9eb7bec9d1a5`. It passed the local software, Lint, debug/test-APK compilation, signed release, certificate and artifact-integrity gates. RC2/CI #593 remains the latest full API 36 and 1920×1200 emulator baseline.

## Current development state
- `main`: canonical branch promoted to the signed `0.5.0-rc1` homologation candidate after local digital validation;
- historical integration branch: `dev/parallel-m3-m5`; PR #1 merged into `main` on 2026-09-11;
- M5: PASS/CLOSED by explicit physical approval of `0.2.0-alpha14`;
- M6: PASS/CLOSED by explicit physical approval of `0.3.0-alpha1`;
- M7/M8 digital scope: PASS for the exact signed RC; residual physical homologation remains OPEN.
- Final candidate: `0.5.0-rc1`, versionCode 21, commit `5a14e4d6522ab9cc53eb8e1dd80a03306fc9d248`.

RC1 adds live recording waveform, strict selected-input confirmation, resilient transport stop/restart, take comparison, markers/sections, punch recording and level guidance. See `docs/RELEASE_NOTES_0.5.0-rc1.md`.

Post-alpha2 hardening covers managed-media loss prevention/recovery, process-death staging cleanup, lossless interrupted-recording preservation and WAV-header repair, safer SAF publication, deterministic codec timestamps, FLAC container correction, accessibility semantics, JVM performance evidence and Android API 36 instrumented regression.

## Build, regression and signing
`scripts/build_local.sh` is the default software gate and runs source materialization, JVM tests, Android Lint and debug assembly with pinned Gradle 9.6.1 / Android API 36 requirements. With explicit signing environment variables and `SIGNED_HOMOLOGATION=true`, it also builds the homologation release.

`.github/workflows/android-ci.yml` is retained as a manual emergency/full-emulator gate only. It has no `push` or `pull_request` trigger, so ordinary commits consume no GitHub Actions minutes. A signed manual run still requires both software and Android integration jobs to pass before signing.

## Physical validation policy
Automatable mathematics, persistence invariants, malformed-input handling, lifecycle recreation, codec structure and UI semantics are not delegated back to the user. The final physical gate is intentionally residual: Samsung/M-VAVE MK-300 USB routing and capture, target-specific MP3 encoder availability, subjective latency/listening quality, real-tablet stress and tactile/visual ergonomics.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts.

## RC2 Studio refinement
`0.4.0-rc2` introduced the current Studio layout: the complete transport/navigation group is centered on the full top bar; current time remains represented by the playhead; remaining time is removed; track count, clip count and total project duration live in the Pistas header; and Adicionar pista is an explicit footer action. These refinements remain in `0.5.0-rc1`.
