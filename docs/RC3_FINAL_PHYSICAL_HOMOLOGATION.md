# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-15

This checklist is intentionally residual. Do not repeat deterministic checks already covered by CI.

## Exact candidate under test
Use only the signed candidate from CI #631 / run `35025012392`:
- source SHA `33fb05a504be2d047259b1d967e6ab1a7e48a68c`;
- package `studio.guitarlab.app`;
- versionName `0.5.0-rc3`;
- versionCode `23`;
- unsigned APK SHA-256 `895ed8ccc957bf0bb17addfdd98806fd3425cc695443f234e27bbae62607cfd8`;
- signed APK SHA-256 `61441b92e3065ba845d9f3e0ed6791d35d41975180a01bb21b612427b493c02d`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- target Samsung SM-X230 + M-VAVE MK-300 over USB;
- MK-300 hardware loopback disabled.

CI #631 is already DIGITAL PASS: standard API36 **23/23 PASS**, isolated 1920×1200 geometry **1/1 PASS**, software/Lint/build/provenance PASS and signed homologation PASS. The checks below cover only residual hardware/visual facts.

## A. H18/H18a adaptive practice bar
- Open the Mixer and inspect **Comparação | Ajustes | Timeline** at the real tablet width.
- Confirm `Desativado`, `Referência`, `Minha` and `Ambas` are all fully visible and interactive.
- Confirm `Ambas` is not clipped/covered at the Ajustes boundary.
- Confirm `Ajustes + Níveis` remains centered, contained and visually balanced.
- Exercise Timeline controls and confirm Timeline overflow does not move/cut Comparação or Ajustes.

PASS: all four comparison controls are completely visible, no overlap/clipping exists, and the three blocks remain coherent at the physical tablet width.

## B. H19 MK-300 output canonicalization
- Connect the MK-300 before opening/refreshing Audio I/O.
- Confirm the physical MK-300 appears **once** in the output selector even if Android exposes multiple logical USB endpoints internally.
- Select that single MK-300 output and play backing/audio.
- Confirm audible output works without a second duplicate option.
- Disconnect and reconnect the MK-300, refresh devices, select it again and repeat playback.
- Confirm no stale/dead duplicate entry survives reconnect.

PASS: one user-facing MK-300 output represents the physical interface and routes audible playback correctly before and after reconnect.

## C. H17 CUT retained
- Enter `Cortar` on a clip.
- Confirm numeric T1/T2 boxes remain absent.
- Confirm only short yellow CUT ticks appear inside the time ruler and remain aligned with waveform Trim handles.

## D. H12 level analysis retained
Open `Ajustes → Níveis`, analyze/reanalyze, apply individual and batch suggestions and Undo the batch once.

PASS: global apply remains one Undo transaction and silent/no-audio tracks remain safe.

## E. H14/H14a Mixer overflow retained
Overflow the Mixer, swipe both directions and observe MASTER.

PASS: all strips are reachable and MASTER remains fixed without jitter.

## F. H15 resident Studio return retained
Studio → Options → same Studio and Studio → Home → same project, preserving one reversible edit.

PASS: no visible double-load/flicker and Undo remains available.

## G. Recording / MK-300 residual hardware gate
- explicitly select MK-300 input/output with hardware loopback disabled;
- record against backing and confirm backing is not printed into the guitar take;
- verify live waveform and Peak/RMS behavior;
- exercise Stop/REC transitions;
- listen for repeatable late placement, pops/dropouts, wrong speed or channel imbalance;
- disconnect selected input during a disposable take and verify fail-closed behavior.

## H. Focused smoke
- loop + live seek;
- Auto seções preview/application;
- two takes + active-take switch;
- short WAV/FLAC export and playback;
- Trim Apply → Undo → Redo → save/reopen.

## Final PASS criteria
- the exact #631 candidate above is the installed/tested APK;
- all four comparison controls are visible without clipping;
- exactly one user-facing MK-300 output choice exists and audible playback works through it;
- reconnect/reselection does not resurrect a stale/dead duplicate;
- no repeatable P0/P1 in A–H;
- no unintended input fallback/backing leakage;
- no repeatable systematic guitar-vs-backing late placement;
- explicit user approval of this exact signed APK.

If A–H pass, M7 physical closure and the RC3 release decision may be finalized without another digital CI run, provided no product/source code changes are introduced afterward.
