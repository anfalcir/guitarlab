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
Closed after user physical approval of `0.2.0-alpha14`. Recording, managed takes, media import, stereo L/R handling, portable project persistence, master export, drag/edit UX and M5 corrective gates are no longer pending.

## M6 — Measured latency and synchronization — ACTIVE PHYSICAL GATE
### Implemented
- reproducible loopback round-trip measurement harness;
- simultaneous AudioTrack/AudioRecord measurement with monotonic audio timestamp correlation;
- repeated-pass median latency, confidence, jitter and drift characterization;
- route + sample-rate scoped persistence of accepted calibration;
- measured frame-domain take-placement compensation with timeline-zero handling;
- safe zero-compensation fallback when calibration is missing/rejected;
- exact marker time labels and top-bar remaining/total transport indication;
- live Mute/Solo during PLAY and REC;
- persistent project renaming.

### Remaining M6 work
Only closure gates remain:
1. exact-candidate unit/Lint/debug/signing gates;
2. physical loopback calibration on Samsung SM-X230 + Pocket Amp route;
3. repeat calibration and verify jitter/drift acceptance behavior;
4. record known transient against backing and verify compensated take placement;
5. verify route change does not reuse an unrelated calibration;
6. regress PLAY/REC, loop, marker clocks, live Mute/Solo, rename, import/export and M5 core behavior;
7. corrective alpha only for repeatable P0/P1;
8. explicit user declaration M6 PASS/CLOSED.

## M7 — Production audio polish
Begins only after M6 closes:
- validated sample-rate conversion for mismatched source/project rates;
- fades/crossfades and advanced clip polish;
- export refinements when explicitly prioritized;
- performance/memory work for larger sessions.

## M8 — Release hardening
- migration/compatibility matrix;
- accessibility/device-size polish;
- crash/edge-case hardening;
- packaging, release notes and distribution gate.

## Gate discipline
Each milestone advances only after software and required physical gates pass. `CURRENT_STATE.md` and this roadmap are canonical.
