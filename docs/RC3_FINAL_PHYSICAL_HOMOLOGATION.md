# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-16

This checklist is intentionally residual. Do not repeat deterministic checks already covered by the exact-source CI.

## Exact candidate — CI #639
Use only the signed APK produced by run `35096711936` from exact product source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`.

Locked identity:
- versionName `0.5.0-rc3`;
- versionCode `23`;
- package `studio.guitarlab.app`;
- unsigned APK SHA-256 `195aa82a581bbcc30890b278cab03bc029eb5d3376fa99130b67e24f6213e21a`;
- signed APK SHA-256 `ffac9da48c17fe2bd28172d357c2f45e906c15b20a216443c1b8b78a9a893696`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- target Samsung SM-X230 + M-VAVE MK-300 over USB;
- MK-300 hardware loopback disabled for normal REC validation.

CI #639 already passed unit/JVM **260/260**, Lint/build/provenance, API36 **23/23**, isolated target geometry **1/1**, and signed homologation. Do not repeat these digitally covered checks unless a physical symptom specifically points back to them.

## A. Retained H22 route UX smoke
- Input shows semantic physical choices only; no low-level duplicate endpoints.
- Output shows semantic physical choices only.
- Built-in labels remain user-friendly (`Microfone do tablet`, `Alto-falante do tablet`).
- MK-300 appears once per physical direction and routes correctly after reconnect.
- No normal UI exposes route internals such as `remote-submix`, `hsp:`, `route2:`, `route3:`, raw endpoint/device indices or address/product tokens.

## B. H23b transient-feedback acceptance
- Play/Stop/Pause success produces no Snackbar.
- Enter/exit CUT/Trim produces no Snackbar.
- REC countdown/capturing/finalizing is visible in the owning UI but does not spam Snackbar.
- Mute/Solo/Arm/selection/seek do not generate redundant transient success messages.
- A real error still produces a concise user-facing message.
- A meaningful degraded recording state warns once and respects warning cooldown.
- Async export completion may show one concise completion Snackbar.

## C. H23b zero-adjustment recording timing — critical gate
Start with residual fine adjustment at **0.0 ms**.

1. Select MK-300 input/output explicitly.
2. Keep hardware loopback disabled for normal REC.
3. Use a transient-rich backing and the project's actual editing/recording sample rate.
4. Include a **44.1 kHz** project; 48 kHz is a useful additional smoke. Digital policy coverage already includes 44.1/48/88.2/96 kHz.
5. Record at least **three independent takes** while playing the same obvious transient/riff.
6. Inspect/listen to every take against the backing.
7. PASS only if no repeatable systematic late/early displacement is shared across takes.
8. Exercise recording from timeline zero and from a non-zero playhead.
9. Exercise one loop/punch take and confirm crop/alignment remains correct.
10. Stop and record again; confirm later takes do not inherit timing state from earlier takes.

A single human performance miss is not a timing failure. The concern is a repeatable systematic displacement.

## D. Plan-B analyzer/calibration
Use only if section C reveals a stable route-specific residual.

1. Keep the exact same MK-300 input/output and sample rate.
2. Connect/enable a valid loopback path specifically for calibration.
3. Run `Calibrar latência` and inspect input, output, sample rate, attempts, median latency, jitter, drift, confidence and status.
4. A result marked unstable must **not** become active route compensation.
5. Disable the test loopback again before normal guitar recording.
6. Repeat section C with fine adjustment still at zero.
7. Change route or sample rate and confirm the previous calibration is not reused.

H23b intentionally does not auto-apply the old H23 32-bit hashed calibration entry. Calibrate once again on H23b before relying on route compensation.

## E. Fine residual adjustment
Use only if a repeatable residual remains after Plan A + accepted calibration.
- Start with ±1 ms; use larger values only when measured/audibly justified.
- Positive adjustment advances the take; negative adjustment delays it.
- Repeat three takes and confirm the correction is stable.
- `Zerar` must return the exact route/rate residual to `0.0 ms`.
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
- exact CI #639 source/artifact/signing identity verified;
- H22 semantic-route behavior remains correct;
- H23b transient-feedback contract passes physically;
- no repeatable P0/P1 recording timing defect remains;
- no unintended input fallback or backing leakage;
- fine adjustment remains zero if Plan A fully resolves the route, otherwise any non-zero value is route/rate-specific and physically verified;
- explicit user approval of the exact signed APK SHA-256 `ffac9da48c17fe2bd28172d357c2f45e906c15b20a216443c1b8b78a9a893696`.
