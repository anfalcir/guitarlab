# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-15

This checklist is intentionally residual. Do not repeat deterministic model/file/API36 checks already covered by CI.

## Candidate identity rule
Current last signed digital baseline:
- CI #624 / source `7858dca021a51e0e08835e3fa3f86e6d3b657215`;
- signed APK SHA-256 `82da7c41591c01e304e44e57031ebac6263a2175866ecd1b438d65da0539462b`.

H16 is newer and PRE-GATE. **Do not use #624 as physical approval evidence for H16.** Run this final checklist only on the next signed APK after its exact-source manual CI passes.

Expected candidate constants:
- versionName `0.5.0-rc3`;
- versionCode `23`;
- package `studio.guitarlab.app`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- target Samsung SM-X230 + M-VAVE MK-300 over USB;
- MK-300 hardware loopback disabled.

## A. H16 practice bar
- Open the Mixer so the docked practice bar is visible.
- Confirm the bar reads **Comparação | Ajustes | Timeline** in that order.
- Confirm the neutral comparison choice reads **Desativado**, while the separate top-bar panel toggle still reads **Mixer**.
- Confirm `Níveis` sits only in Ajustes.
- Judge the three blocks at the real tablet width: Ajustes should be compact, Timeline should have the most room, and the whole bar should look balanced rather than artificially equal.

PASS: labels are unambiguous, Níveis is visually separated from comparison modes, and no control feels cramped or disproportionately empty.

## B. H16 Trim overlay
- Enter `Cortar` on a clip.
- Confirm there is **no dedicated T1/T2 lane** between the playhead/loop rail and the time ruler.
- Confirm the playhead rail remains directly above the normal compact time ruler.
- Move T1 and T2 to several positions, including near/over playhead and loop markers and near each other.
- Compare the yellow line to the actual handle/expected timeline point.

PASS: T1/T2 are horizontally exact, the yellow line crosses the existing rail/ruler rather than living in a separate strip, precise labels overlay competing markers when needed, close labels remain readable, and no extra vertical space is wasted.

## C. H12 level analysis retained
- Open `Ajustes → Níveis`.
- Analyze all eligible tracks, reanalyze one track, apply one suggestion, then apply multiple suggestions and Undo once.

PASS: modal remains practical; global apply remains one Undo transaction; silent/no-audio tracks are safe.

## D. H14/H14a Mixer overflow retained
- Use enough tracks to overflow the Mixer.
- Swipe both directions and access hidden strips.
- Observe MASTER throughout.

PASS: natural horizontal swipe; all strips reachable; MASTER fixed with no jitter.

## E. H15 resident Studio return retained
- Studio → Options → same Studio repeatedly.
- Studio → Home → same project.
- Preserve one reversible edit before a round trip.

PASS: no visible double-load/flicker and Undo remains available.

## F. Recording / MK-300 residual hardware gate
- Explicitly select MK-300 input/output with hardware loopback disabled.
- Record rhythm against backing and confirm backing is not printed into the guitar take.
- Verify Peak/RMS responds on the recording target.
- Exercise Stop during countdown and during capture, then REC-to-stop.
- Listen for repeatable late placement, pops/dropouts, wrong speed or channel imbalance.
- Disconnect the selected input during a disposable take and verify fail-closed behavior.

## G. Focused smoke
- one loop pass and live playhead seek;
- Auto seções preview/application;
- two takes and active-take switch;
- short WAV or FLAC export and playback;
- Trim Apply → Undo → Redo → save/reopen.

## Final PASS criteria
- exact-source H16 workflow fully green with signed homologation;
- package/version/source/checksum/signer verified;
- no repeatable P0/P1 in A–G;
- no unintended input fallback/backing leakage;
- no repeatable systematic guitar-vs-backing late placement;
- explicit user approval of that exact signed APK.
