# Android CI / release pipeline

Updated: 2026-09-15

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits never auto-consume hosted CI. The assistant must not dispatch or rerun the workflow.

The pipeline has three authority layers:
1. software gate — materialization, JVM/unit/audio/DSP/persistence/migration, performance, Lint, debug/release and unsigned provenance;
2. API36 gate — complete connected instrumentation plus isolated 1920×1200 geometry;
3. signed homologation — signs the exact tested unsigned artifact only after both mandatory gates pass.

## Source materialization
`.source-parts/` plus `scripts/materialize_ci_sources.sh` are part of the build contract. Unexpected source drift fails closed.

Canonical hardening order:
`H1 → H2 → H3 → H4 → H5 → H6 → H7 → H8 → H9 → H10 → H11 → H11a → H11b → H12 engine → H12 UI → H13 → H14 → H14a → H15 → H16`.

Physical Review IV / final-polish tail:
- `.source-parts/H12LevelEngine.patch`
- `.source-parts/H12LevelUi.patch`
- `.source-parts/H13TrimRuler.patch`
- `.source-parts/H14MixerHorizontalScroll.patch`
- `.source-parts/H14aMixerScrollViewportRegression.patch`
- `.source-parts/H15ResidentStudioReturn.patch`
- `.source-parts/H16FinalUiTrimOverlay.patch.gz`

`apply_patch_once` accepts either a clean forward application or an exact reverse dry-run indicating the patch is already present. Overlapping historical stages retain explicit final-blob guards.

## Canonical evidence
### CI #624 — signed DIGITAL PASS through H15/H14a
Run `35005147318`, source `7858dca021a51e0e08835e3fa3f86e6d3b657215`:
- materialization PASS;
- complete software/performance/Lint/build/provenance PASS;
- API36 **22/22 PASS**;
- isolated 1920×1200 geometry PASS;
- signed homologation PASS;
- signed APK SHA-256 `82da7c41591c01e304e44e57031ebac6263a2175866ecd1b438d65da0539462b`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Historical diagnostics #621–#623 remain useful root-cause evidence for H14/H14a and the repaired materializer, but #624 supersedes them as the digitally homologated baseline.

## H16 pre-gate evidence
H16 is newer than #624. Source validation on the exact #624 materialized source:
- patch SHA-256 `40a4056644707c57291dfd876fde8487d8dbe9c436ba0409ccb7c6a60c31a0bb`;
- `patch --dry-run -p1`: PASS;
- forward application: PASS;
- reverse dry-run after application: PASS;
- `git apply --check`: PASS;
- `git diff --check`: PASS;
- changed Kotlin files show no parser-level syntax errors in the available local Kotlin parser pass.

H16 extends connected regressions to require:
- `Comparação | Ajustes | Timeline` docked practice-bar structure;
- neutral comparison label `Desativado`;
- Níveis action in Ajustes;
- compact proportional segment sizing;
- no dedicated Trim lane;
- direct marker-rail/time-ruler adjacency;
- T1 X projection matching the marker rail's canonical timeline fraction after a real handle drag.

## Next manual execution
After H16 is consolidated on `main`:
1. GitHub → Actions → `GuitarLab Android CI`.
2. Select `main`.
3. Set `signed_homologation=true`.
4. Confirm the selected workflow SHA is the exact final `main` SHA.
5. Require all three gate layers to PASS.
6. Verify signed APK, `BUILD_IDENTITY.txt` and `SHA256SUMS.txt` all agree on package/version/source/checksum/signer.

Until that run succeeds, #624 remains the canonical signed baseline and H16 remains PRE-GATE.
