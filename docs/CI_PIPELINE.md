# CI and Release Pipeline

Updated: 2026-10-01

This document describes current execution controls. Historical tail hashes, run chronology and retired gate details are preserved in `history/CI_PIPELINE_PRE_RC20.md` and immutable workflow artifacts.

## Source materialization

- canonical entrypoint: `scripts/materialize_ci_sources.sh`;
- current candidate tail: RC22 dual-output/CUE via `scripts/materialize_ci_sources_rc22.py`, chained after the immutable RC21 tail;
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

A temporary maintenance branch may be admitted to the push filter only long enough to qualify work before integration. Such an exception is not part of the release architecture and must be removed before merging to `main`. The signed candidate is produced from canonical `main`, not from the temporary branch.

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
- one consolidated CPU8 W3/W4 shadow qualification: exactly one Cloud Run execution and one Demucs inference, collecting exact engine/model/config + structural/quality gates and performance/cost evidence;
- controlled shadow/production deployment and rollback information; production promotion uses the exact prequalified digest and never rebuilds the promoted worker.

CPU4 runs only when explicitly requested and is not part of the frozen RC20 qualification baseline. Warm-probe and standalone model-probe are off in the normal path. The full transactional U4 smoke runs after production cutover inside the controlled deploy so rollback remains automatic. The standalone U4 workflow remains pinned to the official frozen checkpoint identity unless a future qualified engine/model change explicitly replaces it.

## Real-cloud transactional gate

U4 is blocking when a change affects separation, backend lifecycle, recovery, quota, publication, ACK/purge or cleanup. It is not automatically repeated for an unrelated documentation or Android-only UI change.

The gate must prove representative source → job → output → import/ACK/purge behavior, idempotent recovery and absence of duplicate accepted quota/job.

## Vulnerability policy

Retain complete scanner/SBOM evidence. The frozen RC20 baseline keeps its narrow evidence-backed applicability decisions. A future finding blocks when it is materially applicable to the shipped runtime or credentials, not merely because a database assigns a severity label.

Do not replace evidence with a broad subjective reachability system. Review a newly blocking finding with package/path/vendor/fix/exploit context and encode only a narrow auditable decision when justified.

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

The accepted baseline is recorded in `RELEASE_BASELINE.md`. RC20 remains accepted while RC21 is an unpromoted maintenance candidate. The frozen accepted APK/worker does not require recurring CI. Run qualification again only after a real maintenance trigger or owner-requested feature, selecting gates by affected path under `TEST_AND_HOMOLOGATION_POLICY.md`.

The accepted RC20 runtime remains represented by the historical U12bx tail. The active RC21 maintenance source adds `scripts/materialize_ci_sources_rc21.py` as the current terminal materialization stage without rewriting old payloads. RC21 includes the legacy recording/stereo-integrity, transient-feedback and Drive catalog/cache changes documented in `CURRENT_STATE.md`.

The RC21 unsigned qualification authority is Android CI **#910 / run 36195905242** on source `ad182678cb2704dc9bbfc622124b4f2ac121fea1`: Unit/Lint/APK build PASS and API 36 regression PASS. Canonical integration is PR #7 / merge `2617fe1f1f2351a17389f165ed5d5a8e834e16a2`. The exact post-integration signed authority is Android CI **#912 / run 36197863467** on `main` producer `52b9d66f450fc597f8367f5778334280ceeb521e`; the workflow signed the tested unsigned APK without recompiling and verified package/version/certificate identity.
