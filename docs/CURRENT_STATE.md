# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed DIGITAL PASS: CI #626 / run `35017084625` / exact source `f187ab2ba7596c4aa04d223f007409b2fb39f490`.
- #626 signed APK SHA-256: `93a7ed1ebfdedf7421cf21183db84a529d2d095c9564caafc87952506cb5426b`.
- #626 unsigned APK SHA-256: `28578bab8b76a611aa1b0cd92f8aa0b428826526779e1757ab45b5b34ce254b9`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Workflow remains manual-only: `.github/workflows/android-ci.yml` uses `workflow_dispatch`.

## Evidence boundary
CI #626 remains authoritative through H17. Physical review of the #626 APK confirmed the final CUT presentation and Ajustes/Níveis placement, then exposed two remaining issues: the last comparison button could be clipped by the fixed docked-bar proportions, and the MK-300 output selector could expose duplicate Android logical endpoints where only one endpoint actually produced sound.

H18 and H19 were first gated at CI #628. The software gate passed, but one H18 narrow-viewport instrumentation assertion failed; H19 compiled and its unit-policy coverage passed. H18a is the focused correction after #628. H18/H18a/H19 remain **PRE-GATE** until a new exact-source run passes. CI #626 therefore remains the last signed DIGITAL PASS.

## H18 — Adaptive practice bar — SOURCE-VALIDATED / PRE-GATE
Cause: the docked bar used fixed `0.34 / 0.16 / 0.50` weights. At the physical tablet geometry the real intrinsic width of `Comparação + Desativado + Referência + Minha + Ambas` could exceed the allocated comparison segment and the final control was clipped at the Ajustes divider.

Correction:
- docked wide layout is content-first rather than percentage-first;
- Comparação and Ajustes receive their measured content width;
- Timeline receives the flexible remaining viewport and owns horizontal overflow for its longer action set;
- narrow docked viewports scroll the complete segmented strip rather than clipping one section;
- Ajustes retains symmetric internal padding and Níveis remains fully contained;
- fixed proportional weights remain only in the separate non-docked layout where they are already guarded by the existing responsive branch.

Regression hardening:
- all four comparison controls (`Desativado`, `Referência`, `Minha`, `Ambas`) must be fully contained by the Comparação segment;
- Ajustes/Níveis containment and centered insets remain enforced;
- segment overlap remains forbidden.

Source part: `.source-parts/H18AdaptivePracticeBar.patch.gz`

Patch SHA-256: `fba48ae2b0eedd2c87c269738197542f84a2772bac1aa55e0646ee722ef3627e`.

Expected materialized blobs:
- `StudioPlaceholderScreen.kt`: `cf6521e1f53a829a66f3a9404fc3553c05f0573a`
- `AutoSectionsSlotInstrumentedTest.kt`: `0d1614b7bbe474d11a3a1885c4bd913fdfb0d769`

## H19 — USB output route canonicalization — SOURCE-VALIDATED / PRE-GATE
Root cause: `StudioAudioRoutingStore.outputChoices()` previously exposed every raw `AudioDeviceInfo` returned by `AudioManager.GET_DEVICES_OUTPUTS`. Android can represent one physical USB interface through multiple logical endpoint types. The UI therefore could show duplicate-looking MK-300 outputs, while selection correctness depended on a raw endpoint. Android's preferred-device API also does not guarantee that the preferred endpoint is the endpoint actually routed.

Correction:
- USB endpoints with the same physical identity are collapsed into one user-facing logical route;
- non-USB profiles remain distinct;
- persisted legacy endpoint signatures are migrated to the canonical physical-route signature when the route is present;
- duplicate candidate endpoints are ranked only as an optimization, never by enumeration order;
- the selected duplicate group is resolved with an inaudible short stereo `AudioTrack` probe;
- correctness is confirmed from `AudioTrack.routedDevice` while the silent probe is playing;
- the confirmed endpoint is cached for the current process and stale IDs are rejected after reconnect;
- if no duplicate endpoint can be confirmed, explicit resolution fails closed and normal playback can use the existing automatic-route fallback rather than selecting a known-unconfirmed endpoint;
- disconned selections are now cleared from both Compose state and persistent routing preferences on refresh.

Tests:
- duplicate USB logical endpoints collapse to one physical choice;
- both legacy duplicate signatures migrate to one canonical signature;
- different USB addresses remain distinct;
- non-USB profiles are never collapsed merely because labels match;
- compatibility ranking is deterministic and independent from Android enumeration order.

Source part: `.source-parts/H19UsbOutputRouteCanonicalization.patch.gz`

Patch SHA-256: `21373fa0d85d55ee180fed29308e5677e7bde90b1b555e366c61a872a388eb06`.

Expected materialized blobs:
- `StudioAudioRoutingStore.kt`: `b19f5b9ae0ec584168723f6cfce7cb66707fd154`
- `StudioAudioRoutePolicy.kt`: `f7306776ba185098157929a936f8921465c85dea`
- `SettingsScreen.kt`: `f5d7ab99c143981a83e75dde4c1ad27d7f2dacb6`
- `StudioUserGuideDialog.kt`: `0f40df36f6c72c2604ce995c05c24d938de73357`
- `StudioAudioRoutePolicyTest.kt`: `525f97ffa5563858179745edecf8394217ff4105`

## Local/source validation
Against the exact post-H17 materialized source emitted by CI #626:
- H18 forward `git apply --check`: PASS;
- H18 application: PASS;
- H19 forward `git apply --check` after H18: PASS;
- H19 application: PASS;
- reverse checks and complete clean round-trip: PASS;
- `git diff --check`: PASS;
- pure `StudioAudioRoutePolicy` compiled locally with `kotlinc` and its canonicalization/ranking smoke assertions: PASS;
- updated materializer `bash -n`: PASS.

No Android runtime/USB hardware PASS is claimed locally.

## CI #628 — diagnostic result, not a candidate
Run `35021968990`, exact source `ec05eec58397dc09237d163d6537eb49cfbd3650`:
- materialization through H19: PASS;
- unit/core/audio/DSP/persistence/migration: PASS;
- H19 route-policy JVM tests: PASS;
- performance, Android Lint, debug/release assembly and unsigned provenance: PASS;
- API36 connected regression: **21/22 PASS**;
- sole failure: `AutoSectionsSlotInstrumentedTest.dockedPracticeControlsRenderAsBalancedComparisonAdjustmentsAndTimelineSegments`;
- failure point: `Ajustes` was outside the visible viewport in the phone-shaped default emulator because the narrow H18 fallback scrolled the entire segmented strip;
- signed homologation: correctly SKIPPED because the Android integration gate failed.

This is layout-test/product fallback evidence only; it is not evidence against H19 USB canonicalization.

## H18a — narrow adaptive fallback correction — SOURCE-VALIDATED / PRE-GATE
- wide/tablet H18 remains content-first: Comparação and Ajustes measure to content, Timeline owns the flexible remainder;
- below the docked single-row breakpoint, the three semantic groups stack vertically instead of scrolling the entire strip;
- Comparação and Timeline may scroll internally, but Ajustes/Timeline can no longer be pushed completely off-screen by earlier content;
- instrumentation now separately covers narrow discoverability and simulated ~1280dp target-tablet containment of all four comparison buttons.

Source part: `.source-parts/H18aAdaptivePracticeBarNarrowFallback.patch`

Patch SHA-256: `cbb483c11fdc9166f8e23fb104378a81a4df98a0067164c8d5cd79d3ea84358b`.

Expected post-H18a blobs:
- `StudioPlaceholderScreen.kt`: `27240634d16724c1985d3a853c9de1886d3a2c68`
- `AutoSectionsSlotInstrumentedTest.kt`: `e68bcd102258c6a42d1f75d8678b4170d8c8bb68`

Source validation against the exact post-H19 #628 source: forward patch application PASS, reverse dry-run PASS and `git diff --check` PASS.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7/M8 through H17: **DIGITAL PASS** at #626.
- H18/H18a/H19: **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE** after diagnostic CI #628.

## Next authoritative gate
The user manually dispatches `GuitarLab Android CI` on the then-current `main` with signed homologation enabled. Required PASS: software/unit/Lint/build/provenance including the new route-policy unit tests, full API36 regression including the H18 containment assertions, isolated 1920×1200 geometry, and signed homologation on the exact same source SHA.

The assistant must not dispatch or rerun Actions.
