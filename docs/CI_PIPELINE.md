# CI and Release Pipeline

Updated: 2026-09-24

This document describes current execution controls. Historical tail hashes, run chronology and retired gate details are preserved in `history/CI_PIPELINE_PRE_RC20.md` and immutable workflow artifacts.

## Source materialization

- canonical entrypoint: `scripts/materialize_ci_sources.sh`;
- current tail: U12bk via `scripts/materialize_ci_sources_u12bk.py`;
- protected deltas: `.source-parts/`;
- required properties: deterministic, hash/blob locked, idempotent, semantic guards, reverse-apply validation and fail-closed drift handling.

Never infer current source from an old H/U stage description. The entrypoint and terminal blob contracts define the build input.

## Controlled triggers

Ordinary commits use `[skip ci]` unless qualification is intentionally requested.

- `[run ci]`: Android software and API36 workflow;
- `[run ci signed]`: same qualification plus signing of the exact tested unsigned artifact;
- `[run u4 cloud]`: real-cloud transactional integration smoke;
- `[run u7 cloud]`: backend verification;
- `[run u7 shadow]`: controlled shadow publication/execution when paired with U7;
- `[run u7 matrix]`: optional alternative resource matrix;
- production mutation requires the explicit production confirmation marker defined by the workflow.

Manual `workflow_dispatch` remains available where configured. `main` is canonical.

## Android pipeline

### Software gate

- materialize exact source;
- run relevant JVM/unit/domain/audio/persistence tests;
- Android Lint;
- release assembly and unsigned artifact provenance;
- additional build/performance checks already encoded by the workflow.

### API36 gate

Run representative connected regression for the changed path and protected adjacent flow. Existing broad coverage may continue to execute; it does not turn an evidenced unrelated flaky/cosmetic assertion into a permanent personal-release veto.

### Signing gate

- credentials absent during compilation;
- download the exact qualified unsigned APK;
- verify checksum/package/version;
- align/sign without recompilation;
- verify locked certificate;
- record signed APK SHA-256 and workflow producer SHA.

Any identity mismatch blocks delivery.

## Backend pipeline

U7 retains:

- backend/worker tests and script/schema checks;
- keyless GitHub WIF authentication;
- secret hygiene;
- digest-pinned image build/publication;
- SBOM and full vulnerability report;
- exact engine/model/runtime contract;
- one consolidated CPU8 W3/W4 shadow qualification execution: exact engine/model/config + structural/quality gates and performance/cost evidence;
- controlled shadow/production deployment and rollback information.

CPU4 runs only when explicitly requested and is not an RC20 prerequisite while CPU8 remains acceptable. The normal shadow path does not run the full transactional U4 smoke or a separate model-probe; the full U4 gate runs after production cutover inside the controlled deploy so rollback remains automatic.

## Real-cloud transactional gate

U4 is blocking when a change affects separation, backend lifecycle, recovery, quota, publication, ACK/purge or cleanup. It is not automatically repeated for an unrelated documentation or Android-only UI change.

The gate must prove representative source → job → output → import/ACK/purge behavior, idempotent recovery and absence of duplicate accepted quota/job.

## Vulnerability policy

Retain complete scanner/SBOM evidence. RC20 keeps the existing narrow evidence-backed exceptions. A new finding blocks when it is materially applicable to the shipped runtime or credentials, not merely because a database assigns a severity label.

Do not build a broad subjective reachability system before RC20. Review a newly blocking finding with package/path/vendor/fix/exploit context and encode only a narrow auditable decision when justified.

## Proportional failure handling

Never quarantine:

- data/media corruption or loss;
- invalid/unusable audio;
- primary-flow failure;
- recovery/quota/cost duplication;
- credential/authorization failure;
- wrong APK/worker/signing identity.

A specific unrelated flaky/cosmetic assertion may be quarantined only with evidence, scope and rationale recorded in the candidate decision. Do not delete useful coverage merely because it is non-blocking for one candidate.

## Artifact retention

Retain only evidence needed to identify and reproduce the promoted candidate:

- producer SHA and run URL/number;
- exact-source/materialization result;
- unsigned/signed APK hashes and signer;
- worker digest/engine/model/recipe;
- relevant test summaries;
- consolidated W3/W4 qualification artifact for the finalist;
- deployment/rollback and U4 evidence;
- explicit quarantines.

Run IDs and current status belong in `CURRENT_STATE.md`, not here.

## Post-freeze

The frozen APK/worker does not require recurring CI. Run this pipeline again only after a real maintenance trigger or owner-requested feature, selecting gates by affected path under `TEST_AND_HOMOLOGATION_PLAN.md`.
