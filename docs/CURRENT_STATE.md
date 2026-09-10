# Current State — GuitarLab Studio

Updated: 2026-09-09

## Active branch and gate
- Repository: `anfalcir/guitarlab`
- Branch: `dev/parallel-m3-m5` (historical branch name retained to avoid destabilizing the active PR)
- Draft PR: #1
- Candidate under construction: `0.3.0-alpha1`, versionCode 16
- M2: PASS/CLOSED on Samsung SM-X230 Android 16/API 36 + Pocket Amp USB
- M3/M4: absorbed
- M5: **PASS/CLOSED by user physical approval of alpha14**
- M6: implementation complete in software; **OPEN pending M6 physical homologation**

## M6 scope implemented
1. frame-derived exact time labels on playhead and loop markers;
2. top-bar transport summary with `Restante` and `Total`;
3. route-scoped round-trip latency calibration harness using simultaneous AudioTrack/AudioRecord loopback and Android monotonic audio timestamps;
4. repeated measurements, median latency, confidence, jitter and drift characterization;
5. only stable/accepted calibrations are eligible for automatic take-placement compensation;
6. compensation is applied in the frame domain at take finalization, including source trimming when an earlier shift would cross timeline zero;
7. calibration persistence is keyed to input route + output route + sample rate;
8. Mute/Solo are live mixer controls during PLAY and REC; structural edits and REC-arm changes remain guarded;
9. project rename is persistent and immediately reflected by Studio/Home/export naming.

## M6 physical gate
The software must not invent a latency value. Physical homologation must run `Calibrar latência` with a real loopback/return path on the target route, verify the reported stability, then record a known transient against backing and confirm compensated placement. Repeatability, route changes, mute/solo during PLAY/REC, marker time labels and project rename are part of the M6 checklist.

## Safety rule
An absent or rejected calibration produces **zero automatic compensation**. A stored calibration is route/sample-rate specific and is not reused for a different route.

Canonical roadmap: `docs/IMPLEMENTATION_ROADMAP.md`.
Active physical checklist: `docs/M6_ALPHA1_HOMOLOGATION_CHECKLIST.md`.
