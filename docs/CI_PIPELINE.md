# Android CI / release pipeline

Updated: 2026-09-15

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not auto-consume hosted CI, and the assistant must not dispatch or rerun the workflow.

The pipeline retains three authority layers:
1. software gate — materialization, JVM/unit/audio/DSP/persistence/migration, performance, Lint, debug/release and unsigned provenance;
2. API36 gate — connected instrumentation plus isolated 1920×1200 geometry;
3. signed homologation — signs the exact tested unsigned artifact only after both mandatory gates pass.

## Source materialization
`.source-parts/` plus `scripts/materialize_ci_sources.sh` are part of the build contract. Unexpected source drift fails closed.

Canonical tail after H11b:
`H12 engine → H12 UI → H13 → H14 → H14a → H15 → H16 → H17 → H18 → H19 → H18a`.

New tail files:
- `.source-parts/H18AdaptivePracticeBar.patch.gz`
- `.source-parts/H19UsbOutputRouteCanonicalization.patch.gz`
- `.source-parts/H18aAdaptivePracticeBarNarrowFallback.patch`

The updated materializer passes `bash -n` and the H18/H19 pair passed forward application, reverse validation and clean round-trip against the exact post-H17 materialized #626 source.

## Retained signed authority — CI #626
CI #626 / run `35017084625` / source `f187ab2ba7596c4aa04d223f007409b2fb39f490` remains the authoritative signed DIGITAL PASS through H17:
- API36 **22/22 PASS**;
- isolated geometry **1/1 PASS**;
- signed APK SHA-256 `93a7ed1ebfdedf7421cf21183db84a529d2d095c9564caafc87952506cb5426b`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## CI #628 diagnostic evidence
Run `35021968990`, source `ec05eec58397dc09237d163d6537eb49cfbd3650`:
- software/unit/performance/Lint/build/provenance: PASS;
- H19 route-policy unit tests: PASS;
- API36: 21/22 PASS;
- sole failure: H18 narrow-viewport layout hid `Ajustes` beyond the whole-strip scroll viewport;
- signing: SKIPPED because the mandatory Android gate failed.

H18a replaces only that narrow fallback with vertically stacked semantic groups and adds distinct narrow + simulated target-tablet layout regressions. The wide/tablet H18 algorithm and H19 routing implementation are unchanged.

## H18/H19 next-gate requirements
The next manual run must additionally prove:
- H18 comparison controls are fully contained and do not cross into Ajustes;
- H18 retained Ajustes/Níveis centering and segment non-overlap;
- H19 route-policy JVM tests pass;
- Android app/Lint/build remain valid with the runtime route resolver;
- the existing complete API36 suite and isolated tablet geometry remain green;
- signed homologation uses the exact H18/H19 source SHA.

The emulator cannot establish real MK-300 USB endpoint behavior, so the duplicate-output route probe remains a final residual hardware assertion after the digital gate.

## Artifact identity discipline
Documentation-only commits never replace the exact application/source SHA that produced a signed candidate. H18/H19 are PRE-GATE until a new user-dispatched exact-source workflow is green.
