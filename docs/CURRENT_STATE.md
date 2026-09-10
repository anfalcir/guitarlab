# Current State — GuitarLab Studio

Updated: 2026-09-09

## Active branch and gate
- Repository: `anfalcir/guitarlab`
- Branch: `dev/parallel-m3-m5` (historical branch name retained to avoid destabilizing the active PR)
- Draft PR: #1
- Current M6 physical candidate: `0.3.0-alpha1`, versionCode 16
- Exact signed candidate commit: `89654674764c2152d0fe8b1b6d8857f5b8bac966`
- GitHub Actions signed candidate run: #388 / ID `34422221979`
- APK SHA-256: `241aa1ea9c130ae78f55db3d6a568f416594b48bd26ee9171b59f7e83a4c335c`
- Homologation artifact ZIP SHA-256: `7a2e901f2bdade9b3e9ce0abfb42cbe46c8059bb8df83e59b93f3bf46bfa9fa0`
- Signing certificate SHA-256: `0762D4F3ECB8E1A9AAA4BDB1E098A666C9BA9BF8B71C4F14B0E7A7065404E181`
- BUILD_IDENTITY milestone: `M6-physical-homologation-candidate`
- BUILD_IDENTITY gate: `alpha1-awaiting-physical-validation`
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
7. calibration persistence is keyed to explicitly selected input route + output route + sample rate; automatic/unidentified routes are not accepted as reusable calibration identities;
8. Mute/Solo are live mixer controls during PLAY and REC; structural edits and REC-arm changes remain guarded;
9. project rename is persistent and immediately reflected by Studio/Home/export naming.

## Software/signing gate
The exact candidate commit passed unit tests, Android Lint and debug APK assembly. The push workflow also passed the signed homologation job, verified the official signing certificate, packaged the deliverable, and destroyed the restored signing bundle after use. A separate PR-triggered CI run (#389 / ID `34422224173`) also passed the software gate on the same candidate commit.

## M6 physical gate
The software must not invent a latency value. Physical homologation must run `Calibrar latência` with a real loopback/return path on the target route, verify the reported stability, then record a known transient against backing and confirm compensated placement. Repeatability, route changes, Mute/Solo during PLAY/REC, marker time labels and project rename are part of the M6 checklist.

## Safety rule
An absent or rejected calibration produces **zero automatic compensation**. A stored calibration is route/sample-rate specific and is not reused for a different route.

## Closure rule
M6 remains OPEN until the signed alpha1 candidate is physically validated on the target setup with zero repeatable P0/P1 and explicit user approval. Do not begin the next milestone before that gate closes.

Canonical roadmap: `docs/IMPLEMENTATION_ROADMAP.md`.
Active physical checklist: `docs/M6_ALPHA1_HOMOLOGATION_CHECKLIST.md`.
