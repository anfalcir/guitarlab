# H36 — Settings UX Polish

Updated: 2026-09-20
Status: **SOURCE PRE-GATE READY — H36b corrective line**

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

## H36a — CI #660 Android-test compile corrective
CI #660 passed H36 materialization, JVM/unit tests, Android Lint and debug/release assembly/provenance. The API36 job failed before instrumentation because the new Settings hierarchy test imported `androidx.compose.ui.test.onNode`, which is not a top-level import in the project's Compose Test API. The test already invokes the correct `composeRule.onNode(...)` method.

H36a removes only that invalid import:
- runtime/UI code unchanged;
- SettingsScreen blob remains `6c98b72b674743eeeebfe3991c0636e824ef8a09`;
- corrected test blob `ca2333c7f732212ded2cb06f027e3f5357ee1294`;
- H36a gzip SHA-256 `949c740dc67545016a27652630ed1e6504f76ae9322feaecd554191ee5753c22`;
- decoded patch SHA-256 `f5c8b3acdbd87ce4bbb3ffa8b95be3af018b3bb4df7acc27ff0fd0794a03686a`.

H36a remains SOURCE PRE-GATE until the complete API36/signing pipeline passes.


## H36b — CI #661 viewport-aware instrumentation corrective
CI #661 passed the entire software gate, compiled Android tests and executed 34 app instrumented tests. Result: 31 PASS / 3 FAIL / 0 errors.

All three failures were test-only viewport assumptions after the H36 layout change:
- External Control: HID toggle existed but was below the current Settings viewport immediately after enabling External Control;
- calibration modal: +1 ms existed but was below the current dialog viewport after previous assertions;
- Settings hierarchy: Diagnostics existed but the direct text-level `performScrollTo()` did not move the owning LazyColumn sufficiently.

H36b changes only instrumentation:
- scroll the Settings LazyColumn to `settings-external-hid-toggle` before asserting it;
- explicitly `performScrollTo()` each relevant fine-adjustment/reset target in the vertically scrollable calibration dialog;
- scroll the Settings LazyColumn with `performScrollToNode(hasText("Diagnóstico"))` before asserting Diagnostics.

No production source changes:
- `SettingsScreen.kt` remains blob `6c98b72b674743eeeebfe3991c0636e824ef8a09`.

H36b identities:
- gzip SHA-256: `ac06c0d951355e2d0885203e010509bbb1aade05af173e6ba848a47e1184f692`;
- decoded patch SHA-256: `a78b88bc6545a98fd109c353fe68910931897290f5feb93a0ebd648055dd3ee1`;
- External Control test blob: `2a59ede15b2f19579a5f720f1fc8fc2d5e779230`;
- calibration modal test blob: `6dbd1f42ed8476e34a9278f224e317feaec88718`;
- Settings hierarchy test blob: `eb8ee06396a0bba865f18bd49c42f356f3bec763`.

Direct H36a→H36b dry-run/application PASS; second direct application rejected as already applied; corrupted H36b archive rejected before source mutation with the H36a source hash preserved.
