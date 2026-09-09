# Current State

Last updated: 2026-09-09

## Stable baseline
- `main`: signed baseline `0.2.0-alpha03`, versionCode 4.
- `main` remains intentionally unchanged until the active consolidation line is physically validated.

## Active development
- branch: `dev/parallel-m3-m5`
- draft PR: #1
- branch app version: `0.2.0-alpha08`, versionCode 9.
- signed M4 candidate: `0.2.0-alpha07`, versionCode 8, commit `3ec18f20bfe910fd88bd3f64027b5176c4e97807`; physical/UI validation is in progress on Samsung SM-X230.

## Gates
- M2 Pocket Amp physical homologation on Samsung SM-X230 / Android 16 API 36: **PASS / HOMOLOGATED**.
- M3 WAV codec core/Android path: software-green; tested tablet evidence exists for PCM24/44.1 kHz/stereo direct seek.
- M4 alpha07: software/signing gates **PASS**; physical consolidation pending user validation.
- M5.A recording media/core: software-green (run #220).
- M5.B Android capture engine: implementation checkpoint; branch-head CI required before calling it software-green.

## M4 line under physical validation
Alpha07 includes the commercial-polish/Mixer V3 pass: pt-BR UI, icon-first navigation, top-bar transport, persistent Mixer pinning, fixed Master + scrolling track strips, live gain/pan/Master, latched CLIP indicators, synchronized 20-color track identity, track management, simplified clip actions and corrected timeline-marker alignment.

Any additional M4 layout/theme corrections found during alpha07 validation will be folded into the alpha08/M5 line and revalidated during the M5 physical gate.

## M5 recording line
M5 turns the homologated M2 input/duplex foundation plus M4 managed media/timeline/mixer into real Studio recording.

Established in M5.A:
- `0.2.0-alpha08` / versionCode 9;
- mandatory deterministic 5-second Record countdown policy (`5 → 4 → 3 → 2 → 1`);
- streaming 32-bit IEEE-float WAV writer;
- project-owned temporary recording transaction;
- atomic promotion into `media/source/` only after a valid take;
- interrupted `.recording.part.wav` cleanup;
- unit-tested WAV round-trip and recording-store behavior.

M5.B adds:
- Android `AudioRecord` capture engine;
- FLOAT32 preferred with PCM16 fallback converted to normalized float;
- mono-first input negotiation with stereo fallback;
- exact project sample-rate requirement when provided;
- global preferred input from Options with stable-signature re-resolution;
- explicitly selected missing input fails instead of silently recording another device;
- route-change detection during capture;
- live input Peak/RMS reporting;
- valid partial-take result when interruption happens after frames exist;
- `Desligado / Automático / Ligado` software-monitoring preference;
- conservative AUTO policy to avoid USB double-monitoring and Bluetooth live-monitor latency.

## Safety boundaries
- M5.B does not yet advertise complete user recording support: the Studio coordinator still has to bind permission, Arm, countdown, transport, backing playback, take commit, clip insertion and waveform generation.
- finalized project-managed sources remain immutable.
- empty/invalid captures must not become project clips.
- fine round-trip latency placement/compensation remains M6.
- export and compressed codecs remain gated separately.

## Next checkpoint
M5.C — Studio recording coordinator:
- permission flow from Studio;
- armed-track validation;
- 5-second countdown/cancel;
- capture + optional backing playback coordination;
- input meter/clip latch in Studio;
- Stop/finalize/validate/commit take;
- create `AudioClip` metadata + waveform for armed track(s);
- safe partial recovery and cleanup.

## Merge policy
PR #1 remains draft. Do not merge to `main` until the applicable signed physical gate is green with no P0/P1 regression and evidence is persisted in the repository/PR.
