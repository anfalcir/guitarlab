# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-16

This checklist is intentionally residual. Do not repeat deterministic checks already covered by the exact-source CI.

## Candidate rule
The current physical candidate remains the exact signed APK from the latest successful full signed workflow. CI #639 is the last signed authority through H23b. H24 is newer and must receive a new full signed CI PASS before its APK is used for H24 physical acceptance.

Locked identity unless a later promoted run intentionally changes versioning:
- versionName `0.5.0-rc3`;
- versionCode `23`;
- package `studio.guitarlab.app`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- target Samsung SM-X230 + M-VAVE MK-300 over USB;
- MK-300 hardware loopback disabled for normal REC validation.

## A. H24 Home Project Library smoke
Run only on the signed H24 candidate after its digital gate passes.
- Search by a project name fragment; case differences must not matter.
- If practical, use a name containing an accent and confirm accent-insensitive matching.
- Clear search with the dedicated clear action.
- Open filters and combine at least two dimensions, e.g. template + sample rate.
- Confirm visible count changes to `X de Y` when narrowed.
- Change ordering between Modified recently and Name A–Z; list order must update immediately and remain stable.
- Clear filters without unexpectedly resetting the chosen sort order.
- Confirm true-empty library and no-results states are visually distinct.
- Open a project from the filtered/sorted list, return Home and confirm the query state remains coherent.
- Rename or duplicate a project and confirm the current search/filter/sort view refreshes correctly.
- Controls must remain readable/tappable on the SM-X230 and usable with larger font scale during a quick smoke.

## B. Retained H22 route UX smoke
- Input/output show semantic physical choices only; no raw `remote-submix`, `0`, `back`, `bottom`, `hsp:` duplicates.
- MK-300 appears once per direction and routes correctly after reconnect.

## C. H23b transient-feedback acceptance
- Play/Stop/Pause success produces no Snackbar.
- Enter/exit CUT/Trim produces no Snackbar.
- REC state/countdown/finalization stays visible in owning UI without Snackbar spam.
- Mute/Solo/Arm/selection/seek do not generate redundant success messages.
- No normal transient message exposes low-level Android route identifiers.
- Real errors remain concise and visible; meaningful degraded state warns once; async export completion may show one concise completion message.

## D. H23b zero-adjustment recording timing — critical gate
Start with residual fine adjustment at **0.0 ms**.
1. Select MK-300 input/output explicitly.
2. Use a transient-rich backing and the real project sample rate.
3. Include **44.1 kHz**; smoke 48 kHz if practical. Digital coverage includes 44.1/48/88.2/96 kHz.
4. Record at least three independent takes of the same obvious transient/riff.
5. Inspect/listen to every take against the backing.
6. PASS only if no repeatable systematic late/early displacement is shared across takes.
7. Exercise timeline zero and a non-zero playhead.
8. Exercise one loop-punch take and confirm crop/alignment.
9. Stop and record again; later takes must not inherit prior timing state.

Human performance variation is not a timing defect; the target is repeatable systematic displacement.

## E. Plan-B analyzer/calibration
Only if section D reveals a stable route-specific residual:
- keep the same input/output/sample rate;
- enable a valid loopback path specifically for calibration;
- inspect route, rate, attempts, median, jitter, drift, confidence and status;
- unstable result must not auto-apply;
- disable test loopback before normal recording;
- rerun section D at fine adjustment zero;
- route/rate changes must not reuse unrelated calibration.

H23b intentionally ignores the old lossy hashed calibration entry; recalibrate once on H23b+ before relying on measured route compensation.

## F. Fine residual adjustment
Only if a repeatable residual remains after Plan A + accepted calibration.
- start at ±1 ms; use ±5 ms only when clearly justified;
- positive advances the take, negative delays it;
- repeat three takes;
- `Zerar` returns the exact route/rate residual to 0;
- changing input/output/rate must not silently reuse unrelated fine adjustment.

## G. Retained production smoke
- backing is not printed into the guitar take with loopback disabled;
- live waveform + Peak/RMS during REC;
- selected-input disconnect fails closed;
- loop + live seek;
- Auto sections preview/application;
- short WAV/FLAC export and playback;
- Trim Apply → Undo → Redo → save/reopen.

## Final PASS criteria
- exact signed candidate identity is verified;
- H24 Home-library UX works on target tablet without functional/accessibility regression;
- H22 semantic routes remain correct;
- transient-feedback contract passes;
- no repeatable P0/P1 recording timing defect remains;
- no unintended input fallback/backing leakage;
- explicit user approval of the exact signed APK.
