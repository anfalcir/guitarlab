# Current State

Last updated: 2026-09-08

## Stable baseline
- `main`: signed/homologation baseline `0.2.0-alpha03`, versionCode 4.
- Stable main remains untouched while the M2 Pocket Amp physical gate is open.

## Active development
- branch: `dev/parallel-m3-m5`
- draft PR: #1
- branch app version: `0.2.0-alpha04`, versionCode 5.

## Gates
- M2 software diagnostics: CI-green.
- M2 Pocket Amp physical homologation: OPEN.
- M3 WAV codec core/Android path: software-green; tested tablet evidence exists for PCM24/44.1 kHz/stereo direct seek.
- M4: IN PROGRESS.

## M4 completed checkpoints
- persisted Studio workspace and track lanes;
- clean adaptive design system and launcher identity;
- non-destructive `AudioClip` model;
- WAV import into chosen track;
- immutable project-managed source ingestion under `media/source/`;
- source metadata persistence;
- clip remove/mute/move and metadata-only trim core;
- waveform envelope generation/cache/rendering;
- explicit top marker-head UX for playhead/loop, with trim/record contract;
- semantic marker colors: playhead blue, loop green, trim restrained mustard, record red;
- transport state safety: edits only while `STOPPED`, no separate pause mode;
- real Android Studio playback engine over managed WAV media;
- hardware playback-head-driven playhead progression;
- Stop and start-from-playhead behavior;
- real loop wrapping using the existing loop markers;
- record remains disabled until M5.

## Latest verified CI before this playback checkpoint
Run #159 completed successfully: unit tests, Android Lint, debug APK assembly and artifact upload all passed. Signed homologation was intentionally skipped.

## Current playback checkpoint
The Studio now has a real M4 playback path using Android `AudioTrack` and the existing WAV decoder. Playback reads only immutable managed project sources. Multiple audible mono/stereo clips may be mixed by timeline overlap. The playhead follows `AudioTrack.playbackHeadPosition`, and loop playback maps the hardware-presented frame count through deterministic loop logic.

The first playback slice intentionally rejects audible clips that require sample-rate conversion. No implicit speed/pitch changes and no source rewriting are allowed. Resampling will create derived media after its own quality gate.

## Next checkpoints
1. CI validation of the playback engine checkpoint;
2. tablet validation of play/stop, start position, clock-following playhead and loop boundaries;
3. bind mustard trim marker heads to the existing non-destructive trim core;
4. validate marker locking and tablet ergonomics during playback;
5. continue toward the remaining M4 exit gate without closing the M2 Pocket Amp gate.

## Intentional limitations
- user-facing import currently begins with WAV;
- compressed formats remain planned/unverified;
- resampling is not yet production-enabled;
- M4 playback currently requires managed WAV clips at playback/project sample rate, mono or stereo;
- Studio recording remains M5 scope;
- M2 Pocket Amp physical gate remains OPEN.

## Branch policy
`main` remains stable. Parallel work stays in the draft PR. Routine commits do not trigger signed homologation. CI evidence never substitutes for physical-device homologation.
