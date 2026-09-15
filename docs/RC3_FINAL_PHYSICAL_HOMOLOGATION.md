# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-15

This checklist is intentionally residual. Do not repeat deterministic checks already covered by CI.

## Canonical physical candidate
Use only the exact signed APK from CI #626:
- run ID `35017084625`;
- source SHA `f187ab2ba7596c4aa04d223f007409b2fb39f490`;
- versionName `0.5.0-rc3`;
- versionCode `23`;
- package `studio.guitarlab.app`;
- unsigned APK SHA-256 `28578bab8b76a611aa1b0cd92f8aa0b428826526779e1757ab45b5b34ce254b9`;
- signed APK SHA-256 `93a7ed1ebfdedf7421cf21183db84a529d2d095c9564caafc87952506cb5426b`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact ID `10415074932`;
- target: Samsung SM-X230 + M-VAVE MK-300 over USB;
- MK-300 hardware loopback disabled.

CI #626 already passed software/Lint/build/provenance, API36 **22/22**, isolated 1920×1200 geometry **1/1** and signed homologation. The checks below cover only facts CI cannot establish.

## A. H17 practice bar
- Open the Mixer and inspect **Comparação | Ajustes | Timeline**.
- Confirm `Níveis` is fully inside Ajustes and no longer visually collides with the comparison buttons.
- Confirm the combined `Ajustes + Níveis` cluster looks centered inside its block, with comparable breathing room toward Comparação and Timeline.

PASS: no overlap, no clipping and balanced center spacing at the real tablet width.

## B. H17 CUT ruler
- Enter `Cortar` on a clip.
- Confirm the numeric T1/T2 label boxes are gone.
- Confirm T1/T2 appear only as short yellow ticks on the **time ruler**.
- Confirm the ticks do not extend upward into the sections/playhead rail.
- Drag both waveform Trim handles and compare each tick to the expected timeline point.

PASS: exact horizontal alignment, short ticks confined to the time ruler, no extra label boxes and no visual contamination of the sections/playhead rail.

## C. H12 level analysis retained
Open `Ajustes → Níveis`, analyze/reanalyze, apply individual and batch suggestions and Undo the batch once.

PASS: global apply remains one Undo transaction and silent/no-audio tracks remain safe.

## D. H14/H14a Mixer overflow retained
Overflow the Mixer, swipe both directions and observe MASTER.

PASS: all strips reachable and MASTER remains fixed without jitter.

## E. H15 resident Studio return retained
Studio → Options → same Studio and Studio → Home → same project, preserving one reversible edit.

PASS: no visible double-load/flicker and Undo remains available.

## F. Recording / MK-300 residual hardware gate
- explicitly select MK-300 input/output with hardware loopback disabled;
- record against backing and confirm backing is not printed into the guitar take;
- verify live waveform and Peak/RMS behavior;
- exercise Stop/REC transitions;
- listen for repeatable late placement, pops/dropouts, wrong speed or channel imbalance;
- disconnect selected input during a disposable take and verify fail-closed behavior.

## G. Focused smoke
- loop + live seek;
- Auto seções preview/application;
- two takes + active-take switch;
- short WAV/FLAC export and playback;
- Trim Apply → Undo → Redo → save/reopen.

## Final PASS criteria
- use only the exact #626 signed APK identified above;
- no repeatable P0/P1 in A–G;
- no unintended input fallback/backing leakage;
- no repeatable systematic guitar-vs-backing late placement;
- explicit user approval of this exact signed APK.

If A–G pass, M7 physical closure and the RC3 release decision may be finalized without another digital CI run, provided no source/product code changes are introduced afterward.
