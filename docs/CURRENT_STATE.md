# Current State

Last updated: 2026-09-08

## Stable baseline
- `main`: signed baseline `0.2.0-alpha03`, versionCode 4.
- `main` remains intentionally unchanged until the current M4 consolidation checkpoint is physically validated.

## Active development
- branch: `dev/parallel-m3-m5`
- draft PR: #1
- branch app version: `0.2.0-alpha04`, versionCode 5.

## Gates
- M2 software diagnostics: CI-green.
- M2 Pocket Amp physical homologation on Samsung SM-X230 / Android 16 API 36: **PASS / HOMOLOGATED**.
- M3 WAV codec core/Android path: software-green; tested tablet evidence exists for PCM24/44.1 kHz/stereo direct seek.
- M4: IN PROGRESS.

## M2 homologation closure
Physical Pocket Amp evidence now covers the complete planned M2 checklist for the target tablet/interface combination:
- correct USB input/output enumeration and routing;
- Play PASS;
- Record PASS for silence/frames and real guitar/non-zero signal;
- Duplex PASS at 44.1 kHz, stereo FLOAT32 with 220500 captured/output frames and `outputUnderruns=0` in the reported run;
- hot-unplug during Play, Record and Duplex handled as controlled failure without app crash;
- idle disconnect/reconnect PASS, including Android USB re-enumeration with new device IDs;
- microphone permission-denied path PASS without crash;
- repeated stability sequence PASS as reported by the tester.

See `M2_HOMOLOGATION_EVIDENCE.md`. M2 is no longer an open merge blocker.

## M4 completed checkpoints
- persisted Studio workspace and track lanes;
- clean adaptive design system and launcher identity;
- non-destructive `AudioClip` model;
- WAV import into chosen track;
- immutable project-managed source ingestion under `media/source/`;
- source metadata persistence;
- clip remove/mute/move and metadata-only trim core;
- waveform envelope generation/cache/rendering;
- explicit top marker-head UX for playhead/loop/trim, with record contract;
- semantic marker colors: playhead blue, loop green, trim restrained mustard, record red;
- transport state safety: edits only while `STOPPED`, no separate pause mode;
- real Android Studio playback engine over managed WAV media;
- hardware playback-head-driven playhead progression;
- Stop and start-from-playhead behavior;
- real loop wrapping using loop markers;
- DAW-style mustard trim marker draft with Apply/Cancel and immutable-source bounds;
- record remains disabled until M5.

## Latest verified CI before trim checkpoint
Run #161 completed successfully for commit `e455c63258b63dc70ad3daea70feea8bcd2d1b84`: unit tests, Android Lint, debug APK assembly and artifact upload passed. Signed homologation was intentionally skipped.

## Current M4 checkpoint
The next software checkpoint combines real playback with user-facing non-destructive trim markers. Trim is staged as a draft: T◀/T▶ move while STOPPED, playback/import/other clip edits are blocked until Apply or Cancel, and Apply changes only timeline/source-offset metadata. The managed source file is never rewritten.

## Next gates before merging PR #1 to main
1. CI-green validation of the trim-marker checkpoint;
2. physical tablet validation of M4 Play/Stop, start-from-playhead, hardware-clock playhead progression and loop boundaries;
3. physical tablet validation of mustard trim marker ergonomics, Apply/Cancel, source bounds and edit locking while playing;
4. create a signed consolidated alpha05 candidate only after the software checkpoint is green;
5. perform the short M4 consolidation homologation; if green, merge PR #1 into `main`.

## Intentional limitations
- user-facing import currently begins with WAV;
- compressed formats remain planned/unverified;
- resampling is not yet production-enabled;
- M4 playback currently requires managed WAV clips at playback/project sample rate, mono or stereo;
- Studio recording remains M5 scope.

## Branch policy
M2 no longer requires branch isolation. The draft PR remains isolated only because M4 playback/trim still needs its physical tablet consolidation gate. Routine commits do not trigger signed homologation until an explicit candidate is prepared.
