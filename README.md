# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

## Repository truth
The repository is the canonical source for scope, architecture, implementation state and homologation evidence. Chat history is supplementary only.

Read first:
- `docs/CURRENT_STATE.md` — authoritative live candidate/gate state;
- `docs/IMPLEMENTATION_ROADMAP.md` — milestone sequence and remaining release work;
- `docs/PHYSICAL_EDITING_RECORDING_HARDENING_PLAN.md` — H0–H6 trim/clip/drag/latency/live-waveform hardening record;
- `docs/ARCHITECTURE.md` — current module, media, recording and CI architecture;
- `docs/TIMELINE_INTERACTION_GUIDELINES.md` — timeline/drag/trim/delete contract;
- `docs/TEST_AND_HOMOLOGATION_PLAN.md` — automated + residual physical gate policy;
- `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — active residual physical checklist after exact-source automated PASS;
- `docs/CANDIDATE_IDENTITY_POLICY.md` — version/source/signer/checksum identity contract;
- `docs/DOCUMENTATION_MAP.md` — active vs historical document map.

## Active RC3 state
The active candidate remains `0.5.0-rc3` (versionCode 23).

Manual workflow **#615** fully passed software, API 36 instrumentation, isolated 1920×1200 geometry and signed homologation at source `74bf86efbec94d249c4968c3284bf1985cd66b44`. Its signed APK SHA-256 is `d443cb33a010d2d21dad43e9ff12554d7ada4f2bb783be0c80e3ac11bfe2d6c9`, with the locked homologation certificate `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

After #615, physical use exposed a connected editing/recording workflow that required structural hardening. The current `main` therefore contains the serial H0–H6 program:
- independent ergonomic trim handles instead of opaque slider acquisition;
- safe split-take lineage and explicit clip deletion;
- drag-to-trash plus transaction-safe clip movement;
- measured recording startup skew/route-latency compensation instead of a hard-coded timing offset;
- frame-based, bounded/conflated live REC waveform;
- integrated persistence, timing, waveform and instrumented regression additions.

Therefore #615 is the latest fully green **baseline**, not proof for the current HEAD. The next promoted APK must come from one new explicit manual workflow dispatch against the final current `main` SHA.

## Current development state
- `main`: canonical branch; ordinary commits remain `[skip ci]`;
- historical integration branch `dev/parallel-m3-m5` is merged and is not source of truth;
- active hardware target: Samsung SM-X230 + M-VAVE MK-300;
- active candidate identity: `0.5.0-rc3`, versionCode 23;
- active residual checklist: `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md`.

## Build, regression and signing
`scripts/build_local.sh` is the local software gate and runs source materialization, JVM tests, Android Lint and debug assembly with pinned Gradle/Android requirements. With explicit signing environment variables and `SIGNED_HOMOLOGATION=true`, it can also build the homologation release.

`.github/workflows/android-ci.yml` is the canonical manually dispatched full software/API36/geometry/signing executor. It has no `push` or `pull_request` trigger. A signed run requires both software and Android integration jobs to pass before the exact unsigned release artifact is signed and identity-checked.

Large source deltas are versioned under `.source-parts` and applied serially by `scripts/materialize_ci_sources.sh`. The H1→H6 sequence is intentionally ordered so interaction, clip lineage, drag transaction, recording timing, live waveform and integrated regression remain separately diagnosable. Patch drift must fail the build rather than silently materialize a partial candidate.

## Physical validation policy
Automatable mathematics, persistence invariants, clip-lineage rules, timing policy, malformed-input handling, lifecycle recreation, codec structure, UI semantics and generic geometry are not delegated back to the user. After the next exact-source automated PASS, the physical gate is intentionally short: trim-handle ergonomics, split/move/delete/trash flow, real MK-300 routing/isolation, guitar-vs-backing synchronization, multi-minute live waveform behavior, subjective monitoring/listening and one representative export/stress smoke.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts.
