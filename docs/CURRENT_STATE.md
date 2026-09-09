# Current State

Last updated: 2026-09-09

## Stable baseline
- `main`: signed baseline `0.2.0-alpha03`, versionCode 4.
- `main` remains intentionally unchanged until the current M4 consolidation checkpoint is physically validated.

## Active development
- branch: `dev/parallel-m3-m5`
- draft PR: #1
- branch app version: `0.2.0-alpha06`, versionCode 7.
- alpha06 is the current M4 consolidation candidate line. It supersedes the historical alpha05 package for validating the current Studio redesign, routing and Mixer/Master work.

## Gates
- M2 software diagnostics: CI-green.
- M2 Pocket Amp physical homologation on Samsung SM-X230 / Android 16 API 36: **PASS / HOMOLOGATED**.
- M3 WAV codec core/Android path: software-green; tested tablet evidence exists for PCM24/44.1 kHz/stereo direct seek.
- M4: **IN PROGRESS**. Current software feature checkpoint is green; signed alpha06 build/signing validation and physical consolidation are the remaining gates.

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
- bottom Mixer Dock with hidden/visible and temporary/pinned states;
- persistent project Master gain;
- real per-track and Master Peak/RMS metering with deterministic peak hold/decay.

## Latest completed software gate
GitHub Actions run #211 completed **SUCCESS** for commit `b96b0fb846c2ba89732bed3dabe500734880e156`, covering the persistent Master + per-track metering/ballistics checkpoint.

## Current M4 candidate checkpoint
The branch is now prepared as `0.2.0-alpha06` / versionCode 7 for signed M4 consolidation. The candidate scope intentionally includes the modern single-workspace Studio, immersive fullscreen, Options/routing, track mix, Master and meters in addition to the earlier playback/loop/trim foundation.

## Signed candidate status
The existing signed `0.2.0-alpha05` package is historical and must not be used to approve the current branch UI/Mixer scope. A signed alpha06 package must be produced by the controlled CI signing path, signer fingerprint verified, package identity/hashes inspected, and only then supplied for physical tablet homologation.

## Remaining M4 consolidation gate
Before PR #1 can merge to `main`:
1. alpha06 software + signed homologation jobs must be green;
2. inspect artifact identity, versionCode/versionName, APK hash and signer verification evidence;
3. physically validate `M4_ALPHA06_HOMOLOGATION_CHECKLIST.md` on Samsung SM-X230;
4. preserve immutable-source behavior and verify no P0/P1 regression;
5. persist physical evidence in repository/PR;
6. only then mark PR #1 ready and merge to `main`, followed by post-merge CI.

## Intentional limitations
- user-facing import currently begins with WAV;
- compressed formats remain planned/unverified;
- resampling is not production-enabled;
- playback requires managed mono/stereo WAV clips at the project/playback sample rate;
- record/arm/monitoring remain M5 scope;
- export command placement exists in Options, but export engine remains unimplemented/gated;
- no live mixer automation while playback is active.

## Branch policy
Routine development uses software CI only. Signed homologation is explicit. `main` stays stable until the current M4 physical consolidation gate is green.
