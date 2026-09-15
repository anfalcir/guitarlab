# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-15

This checklist is intentionally residual. Do not repeat deterministic checks already covered by CI.

## Candidate rule
Use only the exact signed candidate from CI #633:
- run `35033323990`;
- source `2204e0272f9e6e2f218bebd36889db424e006e03`;
- versionName `0.5.0-rc3`;
- versionCode `23`;
- package `studio.guitarlab.app`;
- signed APK SHA-256 `f40b24cb36b4cb1299efcb28a35b54e66b107af2ec3d578a870c70e2966ff52e`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- target Samsung SM-X230 + M-VAVE MK-300 over USB;
- MK-300 hardware loopback disabled.

## A. H20 physical output canonicalization
1. Open Options → Áudio with the tablet speakers available and inspect `Saída principal`.
2. Confirm the built-in speaker appears once as a physical `SM-X230` route; logical variants such as `SM-X230 • 0`, `• back` and `• bottom` must not be separate user choices.
3. Select the single SM-X230 route and play audio; confirm audible output.
4. Connect the MK-300, refresh and confirm it is represented once if Android exposes duplicate USB endpoints.
5. Select MK-300 and confirm audible playback.
6. Disconnect/reconnect the MK-300, refresh, reselect and repeat playback.

PASS: no dead duplicate user-facing outputs remain, each physical destination appears once, and explicit selection routes audibly before and after reconnect.

## B. H21 practice-bar hierarchy
- Open Mixer and inspect `Comparação | Ajustes | Timeline` at the real tablet width.
- Confirm each group has a clearly bounded chassis.
- Confirm the group title is visually a header, not a button.
- Confirm `Comparação` precedes comparison buttons, `Ajustes` precedes `Níveis`, and `Timeline` precedes timeline actions.
- Confirm functional accents remain consistent: blue / teal / amber.
- Confirm `Desativado`, `Referência`, `Minha`, `Ambas` remain fully visible and interactive.
- Confirm no group overlaps another and Timeline overflow stays local.

## C. H21 app-wide visual-system review
Review Home, Novo Projeto, Studio top bar/transport, timeline/track panels, Mixer, Options, Audio diagnostics, Codec diagnostics, track settings, dialogs/help/export.

PASS criteria:
- buttons are recognizable as controls rather than labels;
- section titles are recognizable as non-action headings;
- group/panel boundaries are obvious without excessive color noise;
- major shapes consistently read as squared rectangles with subtle rounding;
- no old oversized pill/soft-card geometry remains in ordinary interaction chrome;
- only semantically justified circular elements remain circular;
- touch targets remain comfortable and no text/control is clipped at the tablet font scale.

## D. Retained interaction smoke
- H17 CUT: no numeric T1/T2 boxes; only short yellow ticks in the time ruler;
- H12 Níveis: analyze/reanalyze/apply/Undo;
- H14 Mixer overflow both directions with fixed MASTER;
- H15 resident Studio return with no double-load/flicker and Undo retained.

## E. Recording / MK-300 residual hardware gate
- select MK-300 input/output with hardware loopback disabled;
- record against backing and confirm backing is not printed into the guitar take;
- verify live waveform and Peak/RMS;
- exercise Stop/REC transitions;
- listen for repeatable late placement, pops/dropouts, wrong speed or channel imbalance;
- disconnect selected input during a disposable take and verify fail-closed behavior.

## F. Focused smoke
- loop + live seek;
- Auto seções preview/application;
- two takes + active-take switch;
- short WAV/FLAC export and playback;
- Trim Apply → Undo → Redo → save/reopen.

## Final PASS criteria
- exact #633 candidate identity is verified;
- no duplicate/dead physical output choices remain;
- new visual system is coherent across all screens;
- no repeatable P0/P1 in A–F;
- no unintended input fallback/backing leakage;
- no repeatable systematic guitar-vs-backing late placement;
- explicit user approval of this exact signed APK.

If A–F pass and no source/product code changes follow, M7 physical closure and the RC3 release decision may be finalized without another digital CI run.
