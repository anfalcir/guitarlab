# Android CI / release pipeline

Updated: 2026-09-15

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not auto-consume hosted CI, and the assistant must not dispatch or rerun the workflow.

The pipeline has three authority layers:
1. software gate — materialization, JVM/unit/audio/DSP/persistence/migration, performance, Lint, debug/release and unsigned provenance;
2. API36 gate — complete connected instrumentation plus isolated 1920×1200 geometry;
3. signed homologation — signs the exact tested unsigned artifact only after both mandatory gates pass.

## Source materialization
`.source-parts/` plus `scripts/materialize_ci_sources.sh` are part of the build contract. Unexpected source drift fails closed.

Canonical hardening order:
`H1 → H2 → H3 → H4 → H5 → H6 → H7 → H8 → H9 → H10 → H11 → H11a → H11b → H12 engine → H12 UI → H13 → H14 → H14a → H15 → H16 → H17`.

Final source tail:
- `.source-parts/H12LevelEngine.patch`
- `.source-parts/H12LevelUi.patch`
- `.source-parts/H13TrimRuler.patch`
- `.source-parts/H14MixerHorizontalScroll.patch`
- `.source-parts/H14aMixerScrollViewportRegression.patch`
- `.source-parts/H15ResidentStudioReturn.patch`
- `.source-parts/H16FinalUiTrimOverlay.patch.gz`
- `.source-parts/H17CutRulerPracticeSpacing.patch`

## Last signed authority — CI #625
CI #625 / run `35010012582` / source `476fa740408130adf6a4e9665d166e724a9184dd` is the authoritative signed DIGITAL PASS through H16:
- software/performance/Lint/build/provenance: PASS;
- API36: **22/22 PASS**;
- isolated 1920×1200 geometry: PASS;
- signed homologation: PASS;
- signed APK SHA-256: `107795f040ed18246bb130a9519044ba7e07f334a5835566b952cbc7fdb528e6`.

## H17 pre-gate
H17 is newer than #625 and therefore requires a new manual gate. Its patch is appended strictly after H16 and changes only:
- final Studio practice/CUT presentation;
- the synchronized user guide wording;
- the two focused instrumentation regressions.

H17 patch SHA-256: `38b3f494cf528fcc9fc818e6ef38ed0647389e106ec1bcc821e00ee2e65278dc`.

Source validation against the exact #625 post-H16 materialized snapshot: forward/reverse patch checks, `git apply --check`, `git diff --check`, Kotlin parser scan and materializer `bash -n` all PASS.

## Next execution rule
The next workflow must be manually dispatched by the user on the exact then-current `main` with signed homologation enabled. All three authority layers must pass before H17 can be promoted to DIGITAL PASS.
