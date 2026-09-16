# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-16

## Candidate rule
CI #641 remains the last signed authority through H24a, but H25 changes current product source. Therefore **do not use the #641 APK to approve H25**. Resume final physical homologation only after a new full signed H25 workflow is promoted and its exact producer SHA/APK checksum are recorded in `CURRENT_STATE.md`.

Target remains Samsung SM-X230 + M-VAVE MK-300 over USB; hardware loopback OFF for normal REC validation.

## H25 physical smoke
- rounded-square icon controls show hover/press feedback conforming to the rounded-square chassis, with no circular highlight protruding around the control;
- main Options page is cleaner and no longer shows the full calibration detail stack;
- `Calibração` opens a dedicated readable/scrollable modal with route, rate, status, measurement/stability, compensation, residual adjustment and calibration action;
- closing/canceling the modal does not mutate calibration;
- `Diagnóstico` exposes clear Audio/devices and Codecs/files actions without duplicated diagnostic buttons elsewhere;
- project overflow `Excluir` opens a confirmation naming the project; Cancel leaves it intact; Confirm removes the selected project.

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
Requires exact signed H25 candidate identity, all retained behaviors above, no repeatable P0/P1 and explicit user approval of that exact APK.
