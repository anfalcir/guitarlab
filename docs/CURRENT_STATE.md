# Current State

Last updated: 2026-09-08

## Stable baseline
- `main`: signed/homologation baseline `0.2.0-alpha03`, versionCode 4.
- Stable main commit includes signed M2 software candidate; do not merge parallel work while required hardware gate remains open.

## Active development
- branch: `dev/parallel-m3-m5`
- draft PR: #1
- current app version on branch: `0.2.0-alpha04`, versionCode 5.

## Gates
- M2 software diagnostics: implemented and CI-green.
- M2 Pocket Amp physical homologation: OPEN.
- M3 WAV codec core/Android path: software-green.
- M3 real tablet evidence: PASS for tested WAV PCM24, 44.1 kHz, stereo, direct seek and exact midpoint seek.
- M4: IN PROGRESS.

## M4 completed checkpoints
- persisted Studio project workspace and track lanes;
- clean adaptive design system and app launcher identity;
- non-destructive `AudioClip` timeline model and validation;
- timeline preview foundation;
- WAV import vertical slice via Android Files/SAF into a chosen track;
- imported clip persistence through `FileProjectRepository`;
- technical source metadata persisted on clips;
- clip-management core with validated remove, mute/unmute and frame movement operations;
- Studio controls for persisted mute/unmute and removal.

## Latest verified CI before this checkpoint
Run #65 completed successfully for the documentation + M4 source-metadata checkpoint. Unit tests, Android Lint, debug APK assembly and artifact upload passed. Signed homologation was intentionally skipped for this routine development checkpoint.

## Current implementation checkpoint
M4 clip management is now isolated behind a pure `ProjectClipEditor`, allowing deterministic tests before UI wiring. Mute/unmute and removal are exposed in Studio and persisted atomically. Frame movement is implemented and tested in the core editor but intentionally not yet exposed as a gesture until waveform scale and playhead semantics are established.

## Next checkpoints
1. waveform envelope/cache contract and deterministic generation;
2. waveform rendering in timeline clips;
3. transport state/playhead foundation;
4. bind validated clip movement to timeline scale/gestures;
5. broader codec/import support only as each format passes its own software/Android gates.

## Important limitations that are intentional, not final scope
- user-facing Studio import currently begins with WAV only;
- compressed formats are still planned/unverified and must not be advertised yet;
- production resampling is not yet enabled;
- waveform and transport are not yet unlocked;
- recording into Studio is a later milestone;
- M2 Pocket Amp gate remains physically open.

## Branch policy
`main` remains stable. Parallel work stays in the draft PR. Do not trigger signed homologation for routine development commits. Do not close physical gates based only on CI.
