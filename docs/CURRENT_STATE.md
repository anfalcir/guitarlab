# Current State

Last updated: 2026-09-08

## Stable baseline
- `main`: signed baseline `0.2.0-alpha03`, versionCode 4.
- `main` remains intentionally unchanged until the current M4 consolidation checkpoint is physically validated.

## Active development
- branch: `dev/parallel-m3-m5`
- draft PR: #1
- branch app version: `0.2.0-alpha05`, versionCode 6.
- current branch has moved materially beyond the signed alpha05 artifact: Studio single-screen redesign, immersive fullscreen, Options Center, global audio routing, Mixer Dock, real track mix controls, master gain and live metering are newer branch work.

## Gates
- M2 software diagnostics: CI-green.
- M2 Pocket Amp physical homologation on Samsung SM-X230 / Android 16 API 36: **PASS / HOMOLOGATED**.
- M3 WAV codec core/Android path: software-green; tested tablet evidence exists for PCM24/44.1 kHz/stereo direct seek.
- M4: **IN PROGRESS**. Software checkpoints continue; physical consolidation remains pending.

## M2 homologation closure
Physical Pocket Amp evidence covers the complete planned M2 checklist for the target tablet/interface combination: correct USB routing, Play, Record, Duplex, hot-unplug handling, idle reconnect, permission-denied safety and repeated stability. See `M2_HOMOLOGATION_EVIDENCE.md`. M2 is not an open merge blocker.

## M4 completed software foundations
- persisted Studio workspace and track lanes;
- single-screen, timeline-first Studio layout with no whole-screen vertical scroll;
- Graphite Studio visual direction and immersive fullscreen shell;
- immutable project-managed WAV import and technical metadata;
- non-destructive `AudioClip`, waveform cache/rendering and trim core;
- explicit marker heads; loop markers render only while loop is enabled;
- trim draft with visible Apply/Cancel controls and immutable-source bounds;
- STOPPED-only editing and deterministic transport ownership;
- real Android managed-WAV playback driven by `AudioTrack.playbackHeadPosition`;
- play-from-playhead, Stop and loop wrapping;
- synchronized timeline/mixer track selection;
- persisted track gain, pan, mute and solo with real playback behavior;
- Options Center for global audio I/O, export workflow placement and diagnostics;
- persisted audio device preferences using stable signatures rather than raw Android device IDs;
- selected main output applied to playback with controlled fallback to Android Auto routing;
- real master gain and master peak/RMS metering;
- current checkpoint adds per-track post-track-mix meters, deterministic peak hold/decay and persistent project master gain with legacy-project compatibility.

## Latest completed software gate before the current checkpoint
GitHub Actions run #203 completed **SUCCESS** for commit `db1cf825fac15aa5efb66365345008bb8a84e08a` (master gain + master metering checkpoint).

## Current M4 checkpoint
The active checkpoint hardens the Mixer rather than adding decorative controls:
- per-track meters are measured from each audible track bus after clip+track gain/pan and before master gain;
- Master remains measured after summing tracks and applying master gain, before output clamp;
- meter presentation uses deterministic attack, peak hold and time-based decay;
- master gain is moved from session-only state into durable `GuitarProject` metadata;
- legacy project JSON without `masterGainDb` loads at unity (`0 dB`) through the existing additive/default-compatible serializer behavior;
- project validation enforces track/master gain bounds.

## Signed candidate status
The existing `0.2.0-alpha05` signed package is a valid historical M4 playback/trim candidate, but it predates the subsequent Studio redesign, Options/routing and Mixer/Master work. It must not be treated as the final physical candidate for the current branch head. A newer signed consolidation build will be required after the current software checkpoint is green.

## Remaining M4 consolidation gate
Before PR #1 can merge to `main`:
1. current software checkpoint must be CI-green;
2. produce a new signed M4 consolidation candidate from the then-current branch head;
3. physically validate on the Samsung SM-X230: workspace ergonomics/fullscreen, managed import, Play/Stop/seek/loop, trim Apply/Cancel, edit locking, mixer gain/pan/mute/solo, output routing/fallback, master gain/meters and stability;
4. preserve immutable-source behavior and verify no P0/P1 regression;
5. only then mark PR #1 ready and merge to `main`.

## Intentional limitations
- user-facing import currently begins with WAV;
- compressed formats remain planned/unverified;
- resampling is not production-enabled;
- playback requires managed mono/stereo WAV clips at the project/playback sample rate;
- record/arm/monitoring remain M5 scope;
- no live mixer automation while playback is active.

## Branch policy
Routine development uses software CI only. Signed homologation is explicit. `main` stays stable until the current M4 physical consolidation gate is green.
