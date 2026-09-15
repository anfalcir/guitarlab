# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-15

This checklist is intentionally residual. Do not repeat deterministic checks already covered by CI.

## Evidence boundary
CI #626 / source `f187ab2ba7596c4aa04d223f007409b2fb39f490` remains the last signed DIGITAL PASS through H17. **Do not use the #626 APK as final evidence for H18/H19**, because both change product source.

Run this checklist only on the next signed APK after the exact-source H18/H19 manual CI passes.

Expected constants remain:
- versionName `0.5.0-rc3`;
- versionCode `23`;
- package `studio.guitarlab.app`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- target Samsung SM-X230 + M-VAVE MK-300 over USB;
- MK-300 hardware loopback disabled.

## A. H18 adaptive practice bar
- Open the Mixer and inspect **Comparação | Ajustes | Timeline** at the real tablet width.
- Confirm `Desativado`, `Referência`, `Minha` and `Ambas` are all fully visible and interactive.
- Confirm no comparison control is clipped at the Ajustes divider.
- Confirm `Ajustes + Níveis` remains centered and fully contained.
- Exercise Timeline controls and confirm their own overflow does not move/cut Comparação or Ajustes.

PASS: all comparison controls are completely visible, no overlap/clipping exists, and the three blocks remain visually balanced.

## B. H19 MK-300 output canonicalization
- Connect the MK-300 before opening/refreshing Audio I/O.
- Confirm the physical MK-300 appears **once** in the output selector even if Android exposes multiple logical USB endpoints internally.
- Select that one MK-300 output and play backing/audio.
- Confirm audible output works without choosing a second duplicate entry.
- Disconnect and reconnect the MK-300, refresh devices and repeat selection/playback.
- Confirm stale selections do not survive as invalid routes.

PASS: one logical MK-300 output choice represents the physical device and that choice routes audible playback correctly before and after reconnect.

## C. H17 CUT retained
- Enter `Cortar` on a clip.
- Confirm numeric T1/T2 boxes remain absent.
- Confirm only short yellow CUT ticks appear inside the time ruler and remain aligned with the waveform Trim handles.

## D. H12 level analysis retained
Open `Ajustes → Níveis`, analyze/reanalyze, apply individual and batch suggestions and Undo the batch once.

PASS: global apply remains one Undo transaction and silent/no-audio tracks remain safe.

## E. H14/H14a Mixer overflow retained
Overflow the Mixer, swipe both directions and observe MASTER.

PASS: all strips reachable and MASTER remains fixed without jitter.

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
- exact-source H18/H19 workflow fully green with signed homologation;
- package/version/source/checksum/signer verified;
- all four comparison controls visible without clipping;
- exactly one user-facing MK-300 output choice for the connected physical interface and audible playback through it;
- no repeatable P0/P1 in A–H;
- no unintended input fallback/backing leakage;
- no repeatable systematic guitar-vs-backing late placement;
- explicit user approval of that exact signed APK.

If A–H pass, M7 physical closure and the RC3 release decision may be finalized without another digital CI run, provided no source/product code changes are introduced afterward.
