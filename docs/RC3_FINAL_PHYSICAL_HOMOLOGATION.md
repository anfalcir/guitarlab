# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-16

This checklist is intentionally residual. Do not repeat deterministic checks already proven by exact-source CI #642 unless a physical symptom points back to them.

## Candidate rule
Use **only** the exact signed APK from CI #642 / run `35121955150`, producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632`.

Locked identity:
- versionName `0.5.0-rc3`;
- versionCode `23`;
- package `studio.guitarlab.app`;
- signed APK SHA-256 `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- target Samsung SM-X230 + M-VAVE MK-300 over USB;
- MK-300 hardware loopback OFF for normal REC validation.

Digital evidence already closed at #642: 269/269 JVM/unit, Lint/build/provenance, API36 **28/28 standard + 1/1 isolated geometry**, signed homologation and signer/checksum identity.

## H25 physical smoke
- rounded-square icon controls show hover/press feedback conforming to the rounded-square chassis, with no circular highlight protruding;
- main Options page is cleaner and no longer displays the full calibration detail stack;
- `Calibração` opens a dedicated readable/scrollable modal with route, rate, status, measurement/stability, compensation, residual adjustment and calibration action;
- closing/canceling the modal does not mutate calibration;
- an uncalibrated status by itself does not prevent normal REC;
- `Diagnóstico` exposes clear Audio/devices and Codecs/files actions without duplicated diagnostic buttons elsewhere;
- project overflow `Excluir` opens confirmation naming the project and permanence; Cancel leaves it intact; Confirm removes the selected project only.

## Retained H24 Home smoke
Search, accent/case handling, combined filters, `X de Y`, stable sorting, clear-filter behavior, no-results vs empty-library distinction and refresh after rename/duplicate remain coherent.

## Retained H22/H23b critical smoke
- semantic physical routes remain correct after MK-300 reconnect;
- transient success operations do not spam Snackbars;
- normal REC loopback OFF and fine adjustment starts at 0.0 ms;
- include at least three repeated 44.1 kHz takes plus non-zero playhead and one loop/punch take;
- no repeatable systematic early/late displacement;
- backing is not printed into the guitar take;
- live waveform/Peak/RMS and selected-input fail-closed behavior remain correct;
- use calibration only if a repeatable route-specific residual exists; unstable calibration must not apply;
- representative edit/save/reopen/WAV-FLAC export smoke passes.

## Final PASS
Requires exact #642 candidate identity, all retained behaviors above, no repeatable P0/P1 and explicit user approval of signed APK SHA `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`.
