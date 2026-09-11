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
The digitally validated homologation candidate is `0.4.0-rc1` (versionCode 19), built from commit `66108182d9a733930139a84e4f6b9525172bb9aa` by GitHub Actions run [#590](https://github.com/anfalcir/guitarlab/actions/runs/34593159502). SHA-256: `604f13b83e27021201101fd663dad5c61bca600829ab9565109f48ae23f8e0ad`. Only the residual physical Samsung/Pocket Amp checklist remains.

## Current development state
- `main`: canonical branch containing the digitally validated signed `0.4.0-rc1` homologation candidate;
- historical integration branch: `dev/parallel-m3-m5`; PR #1 merged into `main` on 2026-09-11;
- M5: PASS/CLOSED by explicit physical approval of `0.2.0-alpha14`;
- M6: PASS/CLOSED by explicit physical approval of `0.3.0-alpha1`;
- M7/M8 digital scope: PASS for the exact signed RC; residual physical homologation remains OPEN.
- Final candidate: `0.4.0-rc1`, versionCode 19, commit `66108182d9a733930139a84e4f6b9525172bb9aa`, CI #590.

Post-alpha2 hardening covers managed-media loss prevention/recovery, process-death staging cleanup, lossless interrupted-recording preservation and WAV-header repair, safer SAF publication, deterministic codec timestamps, FLAC container correction, accessibility semantics, JVM performance evidence and Android API 36 instrumented regression.

## Build, regression and signing
`scripts/build_local.sh` is the default software gate and runs source materialization, JVM tests, Android Lint and debug assembly with pinned Gradle 9.6.1 / Android API 36 requirements. With explicit signing environment variables and `SIGNED_HOMOLOGATION=true`, it also builds the homologation release.

`.github/workflows/android-ci.yml` is retained as a manual emergency/full-emulator gate only. It has no `push` or `pull_request` trigger, so ordinary commits consume no GitHub Actions minutes. A signed manual run still requires both software and Android integration jobs to pass before signing.

## Physical validation policy
Automatable mathematics, persistence invariants, malformed-input handling, lifecycle recreation, codec structure and UI semantics are not delegated back to the user. The final physical gate is intentionally residual: Samsung/Pocket Amp USB routing and capture, target-specific MP3 encoder availability, subjective latency/listening quality, real-tablet stress and tactile/visual ergonomics.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts.
