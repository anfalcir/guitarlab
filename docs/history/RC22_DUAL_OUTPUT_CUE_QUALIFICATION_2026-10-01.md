# RC22 Dual-Output/CUE Qualification — 2026-10-01

Status: **BRANCH DIGITAL PASS — CANONICAL MAIN INTEGRATED / REQUALIFICATION PENDING**

This record preserves the branch-level digital qualification evidence for GuitarLab `0.5.0-rc22` / versionCode `42`. RC20 remains the accepted physically homologated baseline until an exact signed RC22 artifact completes the remaining canonical-main and physical acceptance gates.

## Scope

RC22 adds explicit per-track MAIN/CUE monitoring with fail-closed secondary routing. The feature includes project persistence, Settings selection of MAIN/CUE endpoints, independent MAIN/CUE playback buses, recording-monitor routing, route verification, clock/drift guards, diagnostics, help copy and Mixer accessibility.

The initial Mixer CUE control toggles exclusive `MAIN <-> CUE`; the model/engine contract already supports `MAIN_AND_CUE` for future controlled UI use. CUE remains playback/monitoring metadata and does not exclude a track from Master Export.

## Exact branch qualification authority

- branch: `feature/dual-output-cue-routing`;
- exact qualified source SHA: `7524b37665a01af484f4297e4537d97f5a62a180`;
- Android CI: **#932 / run `36893687913`**;
- version: `0.5.0-rc22` / `42`;
- package: `studio.guitarlab.app`;
- protected RC22 payload blob at qualification: `2eb746e7b015b794deb025d361f834f97318464c`;
- RC22 materializer blob at qualification: `8d8a6ec5e36ef2a5f4d38ea043ae4cc085043785`.

## #932 result

- deterministic source materialization: PASS;
- unit/JVM tests: PASS;
- reproducible performance evidence: PASS;
- Android Lint: PASS;
- debug/release assembly: PASS;
- API 36 grouped instrumented regression: PASS;
- signed homologation job: intentionally skipped because #932 was a normal `[run ci]` branch qualification.

Artifacts retained by the run include the exact source snapshot, Android integration reports and the RC22 software-gate bundle.

## Accessibility corrective chronology

The final green branch result was reached without weakening the Mixer accessibility contract.

- #923/#925: exposed overlap between expanded CUE/Arm touch targets;
- #927/#928/#929: exposed the secondary CLIP interaction regression while preserving 48dp control targets;
- #930: proved the CLIP callback corrective and isolated remaining CUE/Arm geometry behavior;
- #931: showed that widening the strip caused a viewport artifact in the horizontal `LazyRow`, not a runtime overlap defect;
- #932: PASS after restoring the compact strip and making the instrumented checks viewport-safe via scrolling before interaction/measurement.

The final runtime preserves explicit real 48x48dp interactive targets where introduced, while the regression harness validates horizontally scrollable content instead of assuming every control is simultaneously inside a narrow test viewport.

## Canonical main integration

- PR #8 merged RC22 into canonical `main`;
- merge commit: `754fc4b8379843abd0494c5054bb856151db475f`;
- Android CI #933 / run `36895822376`: **FAIL before build/test execution** in the preflight `Diff sanity` step;
- root cause: the aggregate merge diff exposed structural single-space context lines inside the stored unified-diff payload `.source-parts/RC22DualOutputCueRouting.patch` to `git diff --check`, plus one Markdown hard-break in `README.md`;
- no Kotlin/Java/runtime source failed qualification in #933 because materialization, compilation and tests were never reached;
- canonical CI now excludes stored `.source-parts/*.patch` payload text from the generic whitespace preflight while retaining hash/`git apply --check`/idempotence validation in the materializer; the README trailing whitespace was removed.

## Remaining gates

Branch qualification is not release promotion. The remaining sequence is:

1. rerun the canonical-main software/API36 qualification after the #933 preflight corrective;
2. run signed exact-artifact qualification from canonical `main`;
3. physically validate the affected MAIN+CUE paths on the exact signed RC22 APK using the target Android 16 device and actual distinct physical outputs;
4. only then consider superseding RC20 in `RELEASE_BASELINE.md`.

CI cannot prove the Samsung/Android audio HAL behavior of two simultaneous physical outputs. RC20 therefore remains the accepted physical baseline until that residual validation is completed.
