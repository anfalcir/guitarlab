# M4 Playback Checkpoint

This checkpoint promotes Studio transport from UX-only state to real Android playback over immutable project-managed WAV media.

## Implemented
- Android `AudioTrack` streaming engine using PCM float stereo output.
- Managed WAV sources are decoded through the existing core WAV decoder; external origin files are never read during normal playback.
- Mono sources are duplicated to stereo; stereo sources preserve L/R order.
- Multiple audible clips are mixed by timeline overlap with clip gain applied and final mix clamped safely.
- Playback begins from the stopped playhead position.
- Stop interrupts streaming and returns transport to `STOPPED` without mutating media.
- Playhead updates are derived from `AudioTrack.playbackHeadPosition`, not from an arbitrary UI timer.
- Loop playback wraps through the existing loop in/out markers.
- Timeline markers, import and clip edits remain locked while playback is active.
- Record remains disabled: recording belongs to M5 and is not implicitly enabled by M4 playback readiness.

## Deliberate gate
The first production-playback slice requires audible managed WAV clips to match the selected project playback sample rate and to be mono or stereo. A sample-rate mismatch fails explicitly instead of changing speed/pitch or mutating the source. Resampling will be introduced as separate derived media after its own quality gate.

## Clock semantics
`PlaybackClockPolicy` maps hardware-presented frame counts to timeline frames, including deterministic loop wrapping. Unit tests protect end clamping, pre-loop travel, loop wrapping and starts at/after loop end.

## M4 next checkpoint
Physical tablet validation of playback, seek/start position, stop position, loop boundaries and marker locking; then bind trim marker heads to the existing metadata-only trim core.
