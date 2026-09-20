# H36 — Settings UX Polish

Updated: 2026-09-20
Status: **SOURCE PRE-GATE READY**

## Problem
Physical review found two related presentation defects:
1. the main Settings screen felt visually polluted because wide full-screen cards and large full-width buttons gave routine actions the same visual weight as primary Studio actions;
2. the latency fine-adjustment controls outgrew the calibration modal and required horizontal dragging to discover all ±ms choices.

## UX plan implemented
### Main Settings
- Center the Settings workspace and cap its useful width at 920 dp on wide/tablet screens.
- Keep narrow layouts responsive rather than forcing desktop geometry.
- Lighten section chrome and remove redundant nested surfaces around read-only option rows.
- Introduce one responsive `SettingsActionRow`:
  - wide layouts: text/status left, compact action right;
  - narrow layouts: action stacks below and may fill available width;
  - minimum action height: 48 dp.
- Apply the pattern to calibration entry, device refresh, backup management, External Control/HID and diagnostics.
- Keep External Control mappings compact in-row on wide layouts while retaining Learn/Clear behavior and all existing semantic test tags.
- Preserve functional Settings behavior and navigation.

### Calibration modal
- Cap dialog width at 640 dp.
- Remove the manual-adjustment `horizontalScroll`.
- Render the complete fine-adjustment set as:
  - Antecipar: −25 / −5 / −1 ms;
  - Atrasar: +1 / +5 / +25 ms;
  - separate `Zerar ajuste`.
- Keep the current value/status visible through the existing global-adjustment summary.
- Render silent verification and physical calibration through the same responsive action-row language.

## Non-goals
H36 does not change:
- automatic latency calculation;
- H35 global-vs-take adjustment semantics;
- audio routing or fail-closed route checks;
- backup, MIDI/HID or project persistence behavior;
- signed version/package identity.

## Automated/source coverage
- existing `SettingsCalibrationModalInstrumentedTest` expanded to assert −25, −1, +1, +25 and reset visibility;
- new `SettingsVisualHierarchyInstrumentedTest` protects the Settings root and discoverability of compact Audio/External Control actions;
- existing External Control instrumented tests retain their tags/semantics.

## Pre-publication evidence
- diff whitespace sanity PASS;
- source-contract assertions PASS: no Settings horizontal carousel, 920 dp content cap, compact action component and all six fine-adjustment choices;
- H35a→H36 first materialization PASS;
- H35a→H36 second materialization PASS/idempotent;
- full H28→H36 first materialization PASS;
- full H28→H36 second materialization PASS/idempotent;
- deliberate H36 source corruption rejected with exit 1 before source mutation.

H36 source identities:
- gzip SHA-256: `22895aa6e1d39a3c6988f503467e763893b0448ca1b087ba23614a1a2b162eaf`;
- decoded patch SHA-256: `9489fe121cbbc6c43bf9675701cd74c2556c7d8c1461c48030745711c1b54852`;
- `SettingsScreen.kt` Git blob: `6c98b72b674743eeeebfe3991c0636e824ef8a09`;
- calibration modal test blob: `df31fc2dcc05ef619dc79062e9b29666ed176dd4`;
- Settings hierarchy test blob: `5f8560042219f51216e02637e3b48f22ca947623`.

## Evidence boundary
No Android compile/Lint/API36/signing claim is made for H36 yet. CI #659 / producer `a6a53e8ba9e75b32565e451870758c7c65ad687f` remains the latest signed DIGITAL PASS until a manual exact-source workflow closes H36.
