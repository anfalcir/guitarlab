# M4 Metering and Master Persistence Checkpoint

## Goal
Complete the non-decorative Mixer foundation by making meters useful and Master gain durable without breaking existing projects.

## Scope
- per-track playback meters derived from each audible track bus after clip + track gain/pan and before Master;
- Master meter after audible-track summing + Master gain and before output clamp;
- immediate meter attack, deterministic 750 ms peak hold and time-based release/decay;
- visible held-peak marker and clipping evidence;
- `GuitarProject.masterGainDb` persisted with supported range -60..+12 dB;
- legacy project JSON with no `masterGainDb` loads at 0 dB;
- meter state remains transient and is never serialized;
- meters reset on Stop/error;
- no source audio mutation.

## Engine design
The Android playback engine builds reusable stereo buffers per audible track for each render chunk. Clips mix into their track buffer first. Track meters are measured from that buffer, then track buffers are summed into the Master buffer. Master gain and Master meter follow. Output samples are clamped only after metering, so over-0 dBFS conditions remain observable.

## Compatibility
The project schema stays additive-compatible: `masterGainDb` has a serializer default of 0 dB. Existing schema-v1 JSON therefore remains readable without a destructive migration. Codec tests cover legacy decode and non-zero Master round-trip.

## Software validation
Required before this checkpoint is software-green:
- `MeterBallisticsPolicy` unit tests for attack/hold/decay/reset;
- project codec compatibility tests;
- project validation for Master/track gain range;
- Android unit tests/lint/debug assembly through canonical CI.

## Physical validation later
On the new signed M4 consolidation candidate:
- track meters respond only for audible tracks and follow Mute/Solo;
- Master responds to summed audio and to saved Master gain;
- peak hold behaves visibly without flicker;
- clipping indication is observable with intentionally hot levels;
- saved Master gain survives project close/reopen;
- existing older projects open at 0 dB Master;
- repeated play/stop clears meter state without crash or stale values.

Recording remains M5 and live mixer automation remains outside this checkpoint.
