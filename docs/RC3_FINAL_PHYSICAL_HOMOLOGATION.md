# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-15

This checklist is intentionally residual. Do not repeat deterministic model/file/API36 checks already covered by CI.

## Canonical physical candidate
Use only the exact signed APK from CI #625:
- run ID `35010012582`;
- source SHA `476fa740408130adf6a4e9665d166e724a9184dd`;
- versionName `0.5.0-rc3`;
- versionCode `23`;
- package `studio.guitarlab.app`;
- unsigned APK SHA-256 `46750bb10e70c70d350011aa46411d2cafb7766746c20b6e83add100b6f8055a`;
- signed APK SHA-256 `107795f040ed18246bb130a9519044ba7e07f334a5835566b952cbc7fdb528e6`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact ID `10412879614`;
- target: Samsung SM-X230 + M-VAVE MK-300 over USB;
- MK-300 hardware loopback disabled.

CI #625 already passed software/Lint/build/provenance, API36 **22/22**, isolated 1920×1200 geometry and signed homologation. The checks below cover only facts CI cannot establish.

## A. H16 practice bar
- Open the Mixer so the docked practice bar is visible.
- Confirm the bar reads **Comparação | Ajustes | Timeline** in that order.
- Confirm the neutral comparison choice reads **Desativado**, while the separate top-bar panel toggle still reads **Mixer**.
- Confirm `Níveis` sits only in Ajustes.
- Judge the three blocks at the real tablet width: Ajustes should be compact, Timeline should have the most room, and the bar should look balanced.

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
- use only the exact #625 signed APK identified above;
- no repeatable P0/P1 in A–G;
- no unintended input fallback/backing leakage;
- no repeatable systematic guitar-vs-backing late placement;
- explicit user approval of this exact signed APK.

If A–G pass, M7 physical closure and the RC3 release decision may be finalized without another digital CI run, provided no source/product code changes are introduced afterward.
