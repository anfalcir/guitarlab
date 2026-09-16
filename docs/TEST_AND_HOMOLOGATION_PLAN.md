# Test and Homologation Plan

Updated: 2026-09-16

## Active candidate line
`0.5.0-rc3` / versionCode `23` / package `studio.guitarlab.app`.

Last signed DIGITAL PASS: CI #641 / run `35105065689` / source `b11769f340f7056c37dfb17d95b062909dad87bf`, authoritative through H24a. H25 is **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**.

## Retained automated authority
#641 proves 269/269 JVM/unit, Lint/build/provenance, API36 **25/25 standard + 1/1 isolated geometry** and signed homologation through H24a.

## H25 required coverage
Source/contract checks:
- clickable icon surface uses the same rounded-square shape as the visible chassis;
- calibration details are removed from main Options and reachable through the dedicated modal;
- diagnostics are consolidated without duplicate entry points;
- project overflow delete opens confirmation instead of invoking repository deletion directly;
- Help text matches product behavior;
- H25 materialization is exact, idempotent and fail-closed.

Android instrumentation adds:
- delete cancel does not invoke destructive callback;
- delete confirm invokes it exactly once;
- calibration details are absent from the main Settings surface;
- `Calibração` opens the dedicated modal and the modal closes cleanly.

Do not predeclare the next official standard API36 count.

## Canonical manual gate
The user manually dispatches `.github/workflows/android-ci.yml` with `signed_homologation=true` after H25 lands. Promotion requires, on one exact `head_sha`: unit/JVM PASS, performance evidence, Lint PASS, debug/release + unsigned provenance PASS, standard API36 PASS, isolated 1920×1200 geometry PASS, signing/checksum/package/version/signer verification and signing cleanup.

## Residual physical checks after H25 DIGITAL PASS
- pointer/hover/press feedback follows the rounded-square button geometry on the target tablet/pointer setup;
- Options main page is visibly cleaner; calibration modal is readable/scrollable and exposes the expected controls;
- diagnostic actions are clear and non-duplicated;
- project delete requires confirmation; Cancel is safe; Confirm removes only the selected project;
- retained H24 Home search/filter/sort smoke;
- retained H22 route semantics;
- retained H23b repeated-take recording timing/backing-isolation/live-waveform checks on MK-300.

Use `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` after the new H25 signed candidate is promoted.
