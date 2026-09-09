# Implementation Roadmap

Updated: 2026-09-09

This roadmap reflects the actual dependency order after media I/O, portable project persistence and master export were intentionally pulled forward into the final M5 gate.

## M1 — Project/model foundation — CLOSED
Project model, templates, persistence baseline and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Samsung SM-X230 Android 16/API36 + Pocket Amp USB physical gate established. Evidence: `M2_HOMOLOGATION_EVIDENCE.md`.

## M3 — Codec/import foundation — ABSORBED
WAV reader/decoder/waveform primitives, codec matrix, sample-rate policy and Android document access. Later media-format work is now part of the M5 consolidated gate rather than a separate unfinished milestone.

## M4 — Studio playback/edit/mix foundation — ABSORBED
Timeline, playback, non-destructive clip operations, Track Settings, Mixer/Master, Loop, Trim and project interaction foundations are integrated into the active branch.

## M5 — Reliable recording + Studio consolidation + Media I/O — ACTIVE FINAL GATE
### Completed implementation blocks
- recording/capture engine, countdown, REC Arm targeting and managed take finalization;
- playback/backing during recording and established monitoring/routing policies;
- Mixer/Master, meters, pan, gain, mute/solo and UI persistence;
- Trim, split/duplicate/remove/clear/delete, Undo/Redo and timeline controls;
- workspace-owned track/clip drag with overlay, destination calculation and edge autoscroll;
- responsive Track Settings, source metadata and edit-pencil affordance;
- import policy for WAV PCM, FLAC, AIFF/AIFC PCM, MP3, AAC/M4A, OGG/Vorbis and Opus;
- immutable native managed source + regenerable PCM WAV editing proxy;
- portable `.guitarlab` save/open with versioned manifest and safe extraction;
- offline float master rendering;
- output paths for WAV 32-bit float, FLAC and MP3 320 kbps;
- dedicated Share icon/modal for project persistence and final audio export;
- visible import-processing feedback after SAF selection;
- real per-channel stereo waveform display;
- role-aware stereo import decision and synchronized guitar L/R distribution;
- explicit non-destructive `Separar estéreo em 2 pistas mono`, distinct from temporal split.

### Remaining M5 work
Only closure gates remain:
1. alpha14 software gate on the exact candidate commit;
2. signed APK certificate/hash/identity validation;
3. physical alpha14 checklist on target Samsung, including representative media import, portable save/open and all three requested master formats;
4. corrective alpha only if a repeatable P0/P1 is found;
5. explicit user declaration of M5 PASS/CLOSED.

No new product feature should be added between alpha14 signing and physical homologation unless required to fix a gate failure.

## M6 — Measured latency and synchronization — BLOCKED
Begins only after M5 closure. Planned order:
1. reproducible loopback/round-trip measurement harness;
2. capture/playback clock correlation;
3. measured take-placement compensation;
4. jitter/drift characterization and policy;
5. repeatable physical validation across supported routing cases;
6. persistence of validated latency calibration where justified.

M6 does **not** own unfinished import/export/project-save work; that was pulled forward and must be closed in M5.

## M7 — Production audio polish
After M6 synchronization is proven:
- validated sample-rate conversion for mismatched source/project rates;
- fades/crossfades and advanced clip polish (stereo L/R separation is no longer pending here; it was pulled into M5);
- export refinements such as additional formats/bit-depth options only when explicitly prioritized;
- performance/memory work for larger sessions.

## M8 — Release hardening
- migration/compatibility matrix;
- accessibility and device-size polish;
- crash/edge-case hardening;
- packaging, release notes and distribution gate.

## Gate discipline
Each milestone advances only after its software and required physical gates pass. Historical alpha/checkpoint documents are evidence, not live planning. `CURRENT_STATE.md` and this roadmap are canonical for current work.
