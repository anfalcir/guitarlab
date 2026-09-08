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
- explicit top marker-head component with 48 dp interaction targets for playhead/loop and shared semantic contract for trim/record markers.

## Latest verified CI before this marker checkpoint
Run #115 completed successfully for the managed-media + waveform checkpoint. Unit tests, Android Lint, debug APK assembly and artifact upload passed. Signed homologation was intentionally skipped for routine development.

## Current implementation checkpoint
The Studio now has a marker-head timeline interaction foundation. The playhead uses restrained blue and loop in/out markers use restrained green. Dragging is initiated from the clearly visible marker head at the top; the thin vertical line is orientation only. The reusable marker contract already defines trim and record-head semantics so later implementations cannot regress to line-only precision dragging.

The marker state is currently session/UI state and does not yet claim live transport playback. Production play/pause/stop, audio-clock-driven playhead movement and actual loop playback remain the next isolated M4 transport checkpoint.

## Next checkpoints
1. validate the current marker-head code in CI;
2. production transport state/playback over immutable managed media;
3. audio-clock-driven playhead progression and seek;
4. bind trim marker heads to non-destructive clip trim mode;
5. tablet UX validation for marker size, drag precision and light/dark contrast;
6. broader codec/import support only as each format passes its own software/Android gates.

## Important limitations that are intentional, not final scope
- user-facing Studio import currently begins with WAV only;
- compressed formats are still planned/unverified and must not be advertised yet;
- production resampling is not yet enabled;
- marker heads currently manipulate timeline state, not a live playback engine;
- recording into Studio is a later milestone;
- M2 Pocket Amp gate remains physically open.

## Branch policy
`main` remains stable. Parallel work stays in the draft PR. Do not trigger signed homologation for routine development commits. Do not close physical gates based only on CI.
