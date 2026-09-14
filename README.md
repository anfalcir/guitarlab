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

## Active RC3 state
The active candidate remains `0.5.0-rc3` (versionCode 23). Manual workflow run **#613** fully passed software, API 36 instrumentation, isolated 1920×1200 geometry and signed homologation at source `db5a4208848e4b6ca2163ce715d0c5bb464cfe37`; that signed APK has SHA-256 `4b62d38c1caf3f449c94b4f9111263dacd87d8cf5e9116caa26245ecb716f341`.

Source has since advanced with final physical-homologation UX corrections (Auto seções geometry/end-boundary hardening, shared Home/Studio guide access and a non-layout-shifting 3-second REC countdown overlay). Therefore #613 is the latest fully green **baseline**, not proof for the new HEAD; the next signed candidate requires another explicit manual workflow dispatch.

## Current development state
- `main`: canonical branch; ordinary commits remain `[skip ci]`;
- historical integration branch `dev/parallel-m3-m5` is merged and no longer the source of truth;
- current hardware target for residual physical validation: Samsung SM-X230 + M-VAVE MK-300;
- active candidate identity: `0.5.0-rc3`, versionCode 23;
- active residual checklist: `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md`.

## Build, regression and signing
`scripts/build_local.sh` is the default software gate and runs source materialization, JVM tests, Android Lint and debug assembly with pinned Gradle 9.6.1 / Android API 36 requirements. With explicit signing environment variables and `SIGNED_HOMOLOGATION=true`, it also builds the homologation release.

`.github/workflows/android-ci.yml` is retained as a manual emergency/full-emulator gate only. It has no `push` or `pull_request` trigger, so ordinary commits consume no GitHub Actions minutes. A signed manual run still requires both software and Android integration jobs to pass before signing.

## Physical validation policy
Automatable mathematics, persistence invariants, malformed-input handling, lifecycle recreation, codec structure and UI semantics are not delegated back to the user. The final physical gate is intentionally residual: Samsung/M-VAVE MK-300 USB routing and capture, target-specific MP3 encoder availability, subjective latency/listening quality, real-tablet stress and tactile/visual ergonomics.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts.

## RC2 Studio refinement
`0.4.0-rc2` introduced the current Studio layout: the complete transport/navigation group is centered on the full top bar; current time remains represented by the playhead; remaining time is removed; track count, clip count and total project duration live in the Pistas header; and Adicionar pista is an explicit footer action. These refinements remain in `0.5.0-rc1`.
