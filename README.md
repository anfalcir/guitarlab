# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

## Repository truth
The repository is the canonical source for scope, architecture, implementation state and homologation evidence. Chat history is supplementary only.

Read first:
- `docs/CURRENT_STATE.md` — exact live state and active gate;
- `docs/IMPLEMENTATION_ROADMAP.md` — milestone sequence and work pulled forward into M5;
- `docs/ARCHITECTURE.md` — current module/UI/media architecture;
- `docs/CODEC_SUPPORT_MATRIX.md` — implemented vs physically verified media capabilities;
- `docs/MANAGED_MEDIA_POLICY.md` — immutable-source/proxy/project-package contract;
- `docs/TIMELINE_INTERACTION_GUIDELINES.md` — timeline/drag/trim contract;
- `docs/STUDIO_OPTIONS_AND_MIXER.md` — Studio, routing, Mixer and Share-modal contract;
- `docs/TEST_AND_HOMOLOGATION_PLAN.md` — automated + physical gate policy;
- `docs/M5_ALPHA13_FINAL_HOMOLOGATION_CHECKLIST.md` — active physical checklist;
- `docs/HISTORICAL_CANDIDATES.md` — superseded candidates.

## Current development state
- stable `main`: signed `0.2.0-alpha03` baseline;
- active integration: `dev/parallel-m3-m5`, draft PR #1;
- active candidate: `0.2.0-alpha13`, versionCode 14;
- M2: PASS/CLOSED on Samsung SM-X230 + Pocket Amp USB;
- M3/M4: implemented and absorbed into the active branch;
- M5 capture/recording/coordinator, timeline, Mixer/Master and corrective UX: implemented;
- M5 media I/O pulled forward: multi-format import path, immutable native source + managed edit proxy, portable `.guitarlab` save/open, offline master render, WAV Float32/FLAC/MP3 output paths;
- M5 remains OPEN until alpha13 physical homologation explicitly passes on the target tablet.

Alpha11 was physically rejected for drag gesture cancellation. Alpha12 corrected drag lifetime and Track Settings but was superseded before physical closure when media I/O/project persistence/master export were intentionally pulled forward into the M5 final gate. Alpha13 is the consolidated candidate for those corrections plus the expanded M5 scope.

M6 must not begin before explicit M5 PASS/CLOSED. M6 starts with measured latency/synchronization/compensation/jitter/loopback, not with unfinished M5 media I/O.

## Build and signing
`.github/workflows/android-ci.yml` is the canonical executor. Every candidate runs `git diff --check`, unit tests, Android Lint and debug assembly. A commit containing `[sign-homologation]` additionally produces a signed release, verifies the locked signer certificate, emits SHA256SUMS/BUILD_IDENTITY and destroys temporary signing files.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts.
