# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-20

## Candidate binding
Current signed candidate:
- CI #659 / run `35512894518`;
- producer `a6a53e8ba9e75b32565e451870758c7c65ad687f`;
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned tested APK SHA-256 `4351458825b7a07c53ff2827e69477414896b497b0a77e3aa7e681c29d97c5da`;
- signed APK SHA-256 `e7ddce638a83d92af0ef2a01e46a10152f8c0eb4c535f0ddbc8671d9b94a1295`;
- signed APK size `13,835,802` bytes;
- signer certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

CI #659 is DIGITAL PASS through H35a: 330/330 JVM/unit tests, Android Lint with 0 errors, 33/33 standard API36 tests, 1/1 isolated target geometry, debug/release/provenance and exact-artifact signing all passed.

## H28 backup physical result
**PASS for the defect that triggered H28.** Target-device retest confirmed backup functions without the previously reproduced false confirmation/selective-duplicate behavior.

Backup/restore remains under normal regression protection; it is no longer the current release blocker.

## Remaining recording/audio physical closure
Use the exact #659 signed APK unless a later exact-source candidate explicitly supersedes it.

### Route and recording isolation
- [ ] intended MK-300 input/output are the effective routes during REC;
- [ ] hardware loopback is OFF and backing is not printed into the guitar take;
- [ ] live waveform/meters remain coherent;
- [ ] selected route fails closed rather than silently falling back to the tablet microphone.

### Timing and H35 calibration behavior
- [ ] repeated guitar-against-backing recordings show no repeatable systematic early/late placement;
- [ ] changing the **global** Settings fine adjustment affects only recordings made afterward and does not move an existing take;
- [ ] a recorded take exposes its take-specific synchronization control in the track/take edit UI;
- [ ] changing that take-specific value moves the take/split lineage coherently, persists after save/reopen and returns predictably toward zero;
- [ ] route/sample-rate-specific automatic/global calibration is not reused across incompatible tuples;
- [ ] **Verificação digital silenciosa** emits no intentional non-zero calibration stimulus;
- [ ] physical round-trip calibration aborts silently if the selected input/output are not the actual routed devices;
- [ ] when a valid physical calibration path exists, the new short adaptive chirp is materially less aggressive than the former harsh burst.

### Resilience and quality
- [ ] USB hot-unplug/reconnect preserves valid capture safely, never silently switches to the tablet microphone and never auto-resumes REC;
- [ ] one continuous 10-minute recording/playback exercise has no growing offset, dropout, wrong pitch/speed or runaway waveform behavior;
- [ ] the 10-minute project survives save/reopen;
- [ ] representative WAV/FLAC export succeeds after the stress run;
- [ ] transport including `|<`, representative edit → save → reopen and Undo/Redo remain coherent;
- [ ] no repeatable P0/P1 regression.

H33 MIDI/HID controller hardware acceptance belongs to 1.1 and is not a stable-1.0 blocker.

## Final PASS
RC3 FINAL / stable-1.0 readiness requires this exact signed candidate to retain DIGITAL PASS, complete the applicable hardware-only residual above, contain no repeatable P0/P1 and receive explicit approval of signed APK SHA-256 `e7ddce638a83d92af0ef2a01e46a10152f8c0eb4c535f0ddbc8671d9b94a1295`.