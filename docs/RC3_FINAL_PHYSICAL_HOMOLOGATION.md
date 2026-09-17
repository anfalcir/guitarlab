# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-17

## Candidate binding
Current signed candidate:
- CI #653 / run `35207902169`;
- producer `d09fc003e2ae2d699238eb39ba699f75a746fea4`;
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- signed APK SHA-256 `1a36efcbe24d5995dd3609889237ca112670e52554e187d1b93ed5a8649263c0`;
- signed APK size `13,737,498` bytes;
- signer certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

CI #653 is H28 DIGITAL PASS.

## H28 backup physical result
**PASS for the defect that triggered H28.** Target-device retest confirms backup is now functioning correctly without the previously reproduced false confirmation/selective-duplicate behavior.

Backup/restore remains under normal regression protection; it is no longer the current release blocker.

## Remaining recording/audio physical closure
On the exact signed candidate or a later candidate that preserves the same behavior:
- [ ] normal REC with intended USB audio device and hardware loopback OFF;
- [ ] backing is not printed into the guitar take;
- [ ] live waveform/meters update coherently;
- [ ] selected route fails closed rather than silently falling back;
- [ ] no repeatable systematic late/early placement remains between played guitar and backing;
- [ ] route/sample-rate-specific calibration is not reused across incompatible tuples;
- [ ] representative disconnect/reconnect remains safe if exercised;
- [ ] transport including `|<` remains coherent;
- [ ] representative edit → save → reopen survives;
- [ ] representative WAV/FLAC export succeeds;
- [ ] no repeatable P0/P1 regression.

## Final PASS
RC3 FINAL requires the exact signed candidate to retain DIGITAL PASS, complete the remaining recording/audio physical closure, contain no repeatable P0/P1 and receive explicit approval of that exact signed APK SHA-256.

The post-H28 hardening program beyond RC3 is tracked separately in `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.
