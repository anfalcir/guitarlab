# Implementation Roadmap

Updated: 2026-09-09

## M1 — Project/model foundation — CLOSED
Project model, templates, persistence baseline and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Samsung SM-X230 Android 16/API36 + Pocket Amp USB physical gate established.

## M3 — Codec/import foundation — ABSORBED
Codec/import work was consolidated into M5.

## M4 — Studio playback/edit/mix foundation — ABSORBED
Timeline, playback, editing, Mixer/Master and project interaction foundations are integrated.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after user physical approval of `0.2.0-alpha14`.

## M6 — Measured latency and synchronization — PASS/CLOSED
Closed after explicit user physical approval of `0.3.0-alpha1`. Loopback calibration, route-scoped compensation, clocks, live Mute/Solo and project rename are no longer pending.

## M7 — Production audio polish — ACTIVE PHYSICAL GATE
### Implemented for alpha1
- validated offline band-limited sample-rate conversion for mismatched imported source/project rates;
- immutable original retained while converted audio is stored only as managed editing proxy;
- additive editing-rate metadata for backwards-compatible project persistence;
- non-destructive clip fade-in/fade-out controls;
- crossfade over overlapping clips on the same track;
- identical fade envelope in realtime playback and offline master rendering;
- bounded-memory resampling and reusable render scratch buffers for larger-session stability;
- Home project menu: Rename plus the same shared `Salvar e exportar` modal used in Studio;
- Home export covers `.guitarlab`, WAV 32-bit float, FLAC and MP3 320 kbps through the established managed-media/render pipeline.

### M7 closure gates
1. exact-candidate unit tests, Android Lint, debug build and signed release;
2. import mismatched 44.1/48 kHz representatives and verify duration/pitch/playback/export;
3. verify fades and overlapping crossfade audibly and in exported master;
4. stress a larger multi-track session for responsiveness/memory regressions;
5. verify Home rename and Home `Salvar e exportar` round trip;
6. regression of M5/M6 audio, recording, latency compensation and project persistence;
7. zero repeatable P0/P1 plus explicit user M7 PASS/CLOSED.

## M8 — Release hardening
- migration/compatibility matrix;
- accessibility/device-size polish;
- crash/edge-case hardening;
- packaging, release notes and distribution gate.

## Gate discipline
Each milestone advances only after software and required physical gates pass. `CURRENT_STATE.md` and this roadmap are canonical.
