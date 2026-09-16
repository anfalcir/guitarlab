# Android CI / release pipeline

Updated: 2026-09-16

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not automatically consume hosted CI. The assistant must not dispatch or rerun workflows/jobs.

Authority layers remain:
1. software gate — deterministic materialization, JVM/unit/audio/DSP/persistence/migration tests, Lint, debug/release assembly and unsigned provenance;
2. API36 gate — standard connected instrumentation plus isolated target-tablet geometry;
3. signed homologation — signs the exact tested unsigned artifact only after upstream gates pass.

## Source materialization
`.source-parts/` + `scripts/materialize_ci_sources.sh` are source-of-truth build inputs. Unexpected drift fails closed by exact Git blob hashes.

Canonical tail: `… → H23b → H24 → H24a → H25`.

H25 source part: `.source-parts/H25UiSettingsSafety.patch.gz.part00`.
Expected final message: `Source patch chain materialized through H25 with verified final hashes`.

H25 checks exact final blobs for `AppIconButton.kt`, `HomeScreen.kt`, `SettingsScreen.kt`, `StudioUserGuideDialog.kt` and the two new Android test files. The materializer recognizes already-materialized H25, upgrades H24a→H25 deterministically and fails closed on corrupted/unknown state.

## Last signed authority — CI #641
CI #641 / run `35105065689` / producer `b11769f340f7056c37dfb17d95b062909dad87bf` remains authoritative through H24a:
- JVM/unit **269/269 PASS**;
- Lint/build/unsigned provenance PASS;
- API36 **25/25 standard + 1/1 isolated geometry**;
- signed APK SHA-256 `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`;
- locked certificate match/signing cleanup PASS.

## H25 pre-gate evidence
Source-level validation passes for patch integrity, exact H24a→H25 materialization, idempotency, reverse/reapply round-trip, final Git blob hashes, corruption fail-closed, shell syntax, diff checks and Kotlin parser scan.

No local Android Gradle/Lint/API36/signing claim is made because this runtime lacks the required Android build environment.

The two new focused instrumentation files add delete-confirmation and calibration-modal coverage. Do not predict the next official standard API36 count before the workflow completes.

## Next signed gate
After H25 lands, the user manually dispatches `GuitarLab Android CI` on `main` with `signed_homologation=true`. All three authority layers must pass on one exact `head_sha` before H25 becomes the new physical candidate.
