# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

## Repository truth
The repository is the canonical source for scope, architecture, implementation state and homologation evidence. Chat history is supplementary only.

Read first:
- `docs/CURRENT_STATE.md` — exact live state and active gate;
- `docs/IMPLEMENTATION_ROADMAP.md` — milestone sequence;
- `docs/ARCHITECTURE.md` — current module/UI architecture;
- `docs/TIMELINE_INTERACTION_GUIDELINES.md` — timeline/drag/trim contract;
- `docs/STUDIO_OPTIONS_AND_MIXER.md` — Studio, routing and Mixer contract;
- `docs/TEST_AND_HOMOLOGATION_PLAN.md` — automated + physical gate policy;
- `docs/M5_ALPHA11_FINAL_HOMOLOGATION_CHECKLIST.md` — active physical checklist;
- `docs/HISTORICAL_CANDIDATES.md` — status of superseded alpha candidates.

## Current development state
- stable `main`: signed `0.2.0-alpha03` baseline;
- active integration: `dev/parallel-m3-m5`, draft PR #1;
- active candidate: `0.2.0-alpha11`, versionCode 12;
- M2: PASS/CLOSED on Samsung SM-X230 + Pocket Amp USB;
- M3/M4: implemented and absorbed into the active integration branch;
- M5 recording/coordinator: implemented; final physical closure is **not yet granted**;
- alpha09: physically approved with minor UX findings;
- alpha10: corrective candidate where Trim and Limpar/Excluir were approved, but track/clip drag and Track Settings layout were rejected;
- alpha11: final corrective candidate for shared drag coordinator, continuous autoscroll, waveform migration overlay and Track Settings layout.

M5 must remain OPEN until the alpha11 physical checklist is explicitly approved. M6 must not begin before that closure.

## Build and signing
`.github/workflows/android-ci.yml` is the canonical executor. Every candidate runs `git diff --check`, unit tests, Android Lint and debug assembly. A commit containing `[sign-homologation]` additionally produces a signed release, verifies the locked signer certificate, emits SHA256SUMS/BUILD_IDENTITY and destroys temporary signing files.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts.
