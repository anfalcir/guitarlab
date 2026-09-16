# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-16

This checklist is intentionally residual. Do not repeat deterministic checks already covered by CI.

## Candidate rule
H22/H22a physical route UX was validated on CI #636. H23 changes product source, so do **not** use #636 to approve recording timing. The next physical timing candidate must be the exact signed APK produced by a successful full CI on the H23 source SHA.

Locked identity that must remain unchanged:
- versionName `0.5.0-rc3`;
- versionCode `23`;
- package `studio.guitarlab.app`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- target Samsung SM-X230 + M-VAVE MK-300 over USB;
- MK-300 hardware loopback disabled for normal REC validation.

## A. Retained H22 route UX smoke
- Entrada: semantic physical choices only; no `remote-submix`, `0`, `back`, `bottom`, `hsp:...` duplicates.
- Saída: semantic physical choices only.
- MK-300 appears once per direction and routes correctly after reconnect.

## B. H23 transient-feedback acceptance
- Play/Stop success produces no Snackbar.
- Enter/exit CUT produces no Snackbar.
- REC countdown/capturing/finalizing state is visible in the owning UI but does not spam Snackbar.
- No normal Snackbar exposes `SM-X230 • bottom/back/0`, `remote-submix`, `hsp:`, `route2:` or `route3:`.
- A real error still produces a concise user-facing message.
- A meaningful degraded recording state still warns once and does not repeat continuously.

## C. H23 zero-adjustment recording timing — critical gate
Start with fine adjustment at **0.0 ms**.

1. Select MK-300 input/output explicitly.
2. Use a transient-rich backing and the project's real sample rate. Include 44.1 kHz because the reported evidence was at 44.1 kHz.
3. Record at least three independent takes while playing to the same obvious transient/riff.
4. Stop and inspect/listen to each take against the backing.
5. PASS only if no repeatable systematic late/early offset is evident across takes.
6. Exercise recording from timeline zero and from a non-zero playhead.
7. Exercise loop-punch once to verify compensated crop remains correct.

A single human performance miss is not a timing failure; the concern is a repeatable systematic displacement shared across takes.

## D. Plan-B analyzer/calibration
Only if section C still reveals a stable route-specific residual:
1. Keep the same MK-300 input/output and sample rate.
2. Connect/enable a valid loopback path specifically for calibration.
3. Run `Calibrar latência` and require an accepted stable result; unstable measurements must not be applied.
4. Disable the test loopback again for normal guitar recording.
5. Repeat section C with fine adjustment still at zero.

## E. Fine residual adjustment
Use only if a repeatable residual remains after Plan A + accepted calibration.
- Apply the smallest correction necessary using ±1 ms first.
- Positive adjustment must advance the take; negative must delay it.
- Repeat three takes and confirm the correction is stable.
- Change route or sample rate and confirm the previous adjustment is not silently reused.

## F. Retained production smoke
- backing is not printed into the guitar take with MK-300 loopback disabled;
- live waveform + Peak/RMS during REC;
- selected-input disconnect fails closed;
- loop + live seek;
- Auto seções preview/application;
- short WAV/FLAC export and playback;
- Trim Apply → Undo → Redo → save/reopen.

## Final PASS criteria
- exact H23 signed candidate identity is verified;
- H22 semantic-route behavior remains correct;
- transient-feedback contract passes;
- no repeatable P0/P1 recording timing defect remains;
- no unintended input fallback/backing leakage;
- fine adjustment is zero if Plan A fully resolves the route, otherwise any non-zero value is route/rate-specific and physically verified;
- explicit user approval of the exact signed APK.
