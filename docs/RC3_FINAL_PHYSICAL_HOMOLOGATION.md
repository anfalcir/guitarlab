# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-16

This checklist is intentionally residual. Do not repeat deterministic checks already proven by exact-source CI #641 unless a physical symptom points back to them.

## Candidate rule
Use **only** the exact signed APK from CI #641 / run `35105065689`, producer source `b11769f340f7056c37dfb17d95b062909dad87bf`.

Locked identity:
- versionName `0.5.0-rc3`;
- versionCode `23`;
- package `studio.guitarlab.app`;
- signed APK SHA-256 `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- target Samsung SM-X230 + M-VAVE MK-300 over USB;
- MK-300 hardware loopback OFF for normal REC validation.

Digital evidence already closed at #641: 269/269 JVM/unit, Lint/build/provenance, API36 **25/25 standard + 1/1 isolated geometry**, signed homologation and signer/checksum identity.

## A. H24 Home Project Library smoke
- Search by a project-name fragment; case differences must not matter.
- If practical, use a name containing an accent and confirm accent-insensitive matching.
- Clear search with the dedicated clear action.
- Combine at least two filter dimensions, e.g. template + sample rate.
- Confirm narrowed count becomes `X de Y`.
- Switch ordering between `Modificados recentemente` and `Nome A–Z`; order must update immediately and remain stable.
- Clear filters without resetting the chosen sort unexpectedly.
- Confirm real empty-library and no-results states are visually distinct.
- Open a project from the filtered/sorted list, return Home and confirm query state remains coherent.
- Rename or duplicate a project and confirm the current view refreshes correctly.
- Controls must remain readable/tappable on SM-X230; quick larger-font smoke is desirable.

## B. Retained H22 route UX smoke
- Input/output show semantic physical choices only; no raw `remote-submix`, `0`, `back`, `bottom`, `hsp:` duplicates.
- Friendly built-in labels remain `Microfone do tablet` / `Alto-falante do tablet`.
- MK-300 appears once per direction and routes correctly after reconnect.

## C. H23b transient-feedback acceptance
- Play/Stop/Pause success produces no Snackbar.
- Enter/exit CUT/Trim produces no Snackbar.
- REC state/countdown/finalization stays visible in owning UI without Snackbar spam.
- Mute/Solo/Arm/selection/seek do not generate redundant success messages.
- No normal transient message exposes low-level Android route identifiers.
- Real errors remain concise and visible; meaningful degraded state warns once; async export completion may show one concise completion.

## D. H23b zero-adjustment recording timing — critical gate
Start with residual fine adjustment at **0.0 ms**.
1. Select MK-300 input/output explicitly.
2. Keep MK-300 normal-recording loopback OFF.
3. Use a transient-rich backing and the actual project editing/recording rate.
4. Include **44.1 kHz** specifically; add 48 kHz smoke if practical.
5. Record at least **three independent takes** of the same obvious transient/riff.
6. Inspect/listen to every take against the backing.
7. PASS if there is no repeatable systematic early/late displacement shared across takes; one human miss is not a timing defect.
8. Exercise timeline zero and a non-zero playhead.
9. Exercise one loop/punch take and confirm crop/alignment.
10. Stop and record again; later takes must not inherit prior timing state.

## E. Plan-B analyzer/calibration
Use only if section D reveals a stable route-specific residual:
- keep the same MK-300 input/output/sample rate;
- enable a valid loopback path specifically for calibration;
- run `Calibrar latência` and inspect route, rate, attempts, median, jitter, drift, confidence and status;
- unstable result must not auto-apply;
- disable calibration loopback before normal guitar recording;
- rerun section D at fine adjustment zero;
- route/rate changes must not reuse unrelated calibration.

H23b intentionally ignores the old lossy hashed calibration entry; recalibrate once before relying on measured route compensation.

## F. Fine residual adjustment
Only if a repeatable residual remains after Plan A + accepted calibration.
- start at ±1 ms; increase only when objectively justified;
- positive advances the take; negative delays it;
- confirm with three takes;
- `Zerar` returns that exact route/rate adjustment to 0;
- changing route/rate must not reuse unrelated fine adjustment.

## G. Retained production smoke
- backing is not printed into guitar take with loopback OFF;
- live waveform + Peak/RMS during REC;
- selected-input disconnect fails closed;
- loop + live seek;
- Auto sections preview/application;
- short WAV/FLAC export and playback;
- Trim Apply → Undo → Redo → save/reopen.

## Final PASS criteria
- exact CI #641 identity verified;
- H24 Home-library UX works on target tablet without functional/accessibility regression;
- H22 semantic routes remain correct;
- transient-feedback contract passes;
- no repeatable P0/P1 recording-timing defect remains;
- no unintended input fallback/backing leakage;
- explicit user approval of signed APK SHA `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`.
