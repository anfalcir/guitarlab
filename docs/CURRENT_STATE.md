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
- WAV import into a chosen track;
- imported media copied into immutable project-managed `media/source/` storage;
- external original retained only as provenance and never used as mutable working media;
- technical source metadata persisted on clips;
- clip-management core with remove, mute/unmute, move and non-destructive trim;
- waveform envelope generation, derived cache and Studio rendering;
- deterministic timeline frame/fraction mapping and bounded playhead/loop state;
- explicit top marker-head component with 48 dp interaction targets;
- canonical transport state policy STOPPED / PLAYING / RECORDING;
- canonical symbol-only transport bar: return-to-start, play→stop, record, loop;
- stopped-only edit lock contract for markers/import/clip edits;
- trim marker color changed to muted mustard `#9C741F`, distinct from record red.

## Latest verified CI
Run #129 completed successfully for the prior marker-head checkpoint. Unit tests, Android Lint, debug APK assembly and artifact upload passed. Signed homologation was intentionally skipped for routine development.

## Current implementation checkpoint
The current M4 checkpoint is transport UX/state safety. Marker heads are only user-editable while transport is STOPPED. The pure `TransportPolicy` defines active-state locking and loop/return-to-start behavior. The Studio has the final intended transport control arrangement and icon semantics.

Play/record remain intentionally disabled because the production transport/record engine is not yet implemented. This follows the product rule that unimplemented features must not appear as working controls. Once the real engine is connected, the same policy will make Play become Stop, lock timeline edits during PLAYING/RECORDING and let the engine own playhead/record-head progression.

## Next checkpoint
1. production playback engine over immutable managed media;
2. audio-clock-driven playhead progression;
3. stop and seek synchronization;
4. loop execution using the existing loop markers;
5. only after those pass, enable the Play control;
6. recording remains a later M5 engine gate even though its UX/state semantics are already defined.

## Important limitations that are intentional, not final scope
- user-facing Studio import currently begins with WAV only;
- compressed formats are still planned/unverified and must not be advertised yet;
- production resampling is not yet enabled;
- transport UI/state policy exists, but production playback is still gated;
- record control remains disabled until recording engine implementation;
- M2 Pocket Amp gate remains physically open.

## Branch policy
`main` remains stable. Parallel work stays in the draft PR. Do not trigger signed homologation for routine development commits. Do not close physical gates based only on CI.
