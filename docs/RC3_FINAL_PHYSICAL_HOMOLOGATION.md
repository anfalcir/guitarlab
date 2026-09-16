# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-16

This checklist is intentionally residual. Do not repeat deterministic checks already covered by the exact-source CI.

## Candidate rule
H22 route UX was physically approved and H23/H23a passed CI #638. H23b changes product source after #638, so the next physical timing candidate must be the exact signed APK produced by a **new successful full CI on the H23b SHA**.

Locked identity that must remain unchanged:
- versionName `0.5.0-rc3`;
- versionCode `23`;
- package `studio.guitarlab.app`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- target Samsung SM-X230 + M-VAVE MK-300 over USB;
- MK-300 hardware loopback disabled for normal REC validation.

## A. Retained H22 route UX smoke
- Input: semantic physical choices only; no `remote-submix`, `0`, `back`, `bottom`, `hsp:...` duplicates.
- Output: semantic physical choices only.
- MK-300 appears once per direction and routes correctly after reconnect.

## B. H23b transient-feedback acceptance
- Play/Stop/Pause success produces no Snackbar.
- Enter/exit CUT/Trim produces no Snackbar.
- REC countdown/capturing/finalizing is visible in the owning UI but does not spam Snackbar.
- Mute/Solo/Arm/selection/seek do not generate redundant transient success messages.
- No normal Snackbar/error exposes `deviceId`, `productName=`, address, endpoint index, `SM-X230 • bottom/back/0`, `remote-submix`, `hsp:`, `route2:` or `route3:`.
- A real error still produces a concise user-facing message.
- A meaningful degraded recording state warns once and respects warning cooldown.
- Async export completion may show one concise completion Snackbar.

## C. H23b zero-adjustment recording timing — critical gate
Start with residual fine adjustment at **0.0 ms**.

1. Select MK-300 input/output explicitly.
2. Use a transient-rich backing and the project's real sample rate.
3. Include **44.1 kHz** because the physical evidence was 44.1 kHz; if practical, also smoke 48 kHz. Digital tests cover 44.1/48/88.2/96 kHz.
4. Record at least three independent takes while playing the same obvious transient/riff.
5. Inspect/listen to every take against the backing.
6. PASS only if no repeatable systematic late/early displacement is shared across takes.
7. Exercise recording from timeline zero and a non-zero playhead.
8. Exercise one loop-punch take and confirm crop/alignment remains correct.
9. Stop, record again and verify the second/third take does not inherit timing state from the previous take.

A single human performance miss is not a timing failure; the concern is a repeatable systematic displacement.

## D. Plan-B analyzer/calibration
Only if section C reveals a stable route-specific residual:
1. Keep the exact same MK-300 input/output and sample rate.
2. Connect/enable a valid loopback path specifically for calibration.
3. Run `Calibrar latência` and inspect input, output, sample rate, attempts, median latency, jitter, drift, confidence and status.
4. A result marked unstable must **not** become active route compensation.
5. Disable the test loopback again before normal guitar recording.
6. Repeat section C with fine adjustment still at zero.
7. Change route or sample rate and confirm the previous calibration is not reused.

H23b intentionally does not auto-apply the old H23 32-bit hashed calibration entry. If this device/rate was calibrated before H23b, calibrate it again once on H23b.

## E. Fine residual adjustment
Use only if a repeatable residual remains after Plan A + accepted calibration.
- Start with ±1 ms; use ±5 ms only when clearly necessary.
- Positive adjustment advances the take; negative delays it.
- Repeat three takes and confirm the correction is stable.
- `Zerar` must return the exact route/rate residual to 0.
- Change input, output or sample rate and confirm the previous fine adjustment is not silently reused.

## F. Retained production smoke
- backing is not printed into the guitar take with MK-300 loopback disabled;
- live waveform + Peak/RMS during REC;
- selected-input disconnect fails closed;
- loop + live seek;
- Auto sections preview/application;
- short WAV/FLAC export and playback;
- Trim Apply → Undo → Redo → save/reopen.

## Final PASS criteria
- exact H23b signed candidate source/artifact identity is verified;
- H22 semantic-route behavior remains correct;
- transient-feedback contract passes;
- no repeatable P0/P1 recording timing defect remains;
- no unintended input fallback/backing leakage;
- fine adjustment remains zero if Plan A fully resolves the route, otherwise any non-zero value is route/rate-specific and physically verified;
- explicit user approval of the exact signed APK.
