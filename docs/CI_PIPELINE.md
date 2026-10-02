# CI and Release Pipeline

Updated: 2026-10-02

This document describes current execution controls. Historical tail hashes, run chronology and retired gate details are preserved in `history/CI_PIPELINE_PRE_RC20.md` and immutable workflow artifacts.

## Source materialization

- canonical entrypoint: `scripts/materialize_ci_sources.sh`;
- current candidate tail: RC27 Mixer card hierarchy via `scripts/materialize_ci_sources_rc27.py`, chained after RC26b signed-documentation closure and immutable RC26a/RC26/RC25 predecessors;
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

The accepted baseline remains RC20 until exact signed successor physical acceptance. RC26 is retained as signed digital authority (`0.5.0-rc26` / `46`, Android CI #950/#951) but was not promoted before RC27 was requested. RC27 `0.5.0-rc27` / `47` is the active Android-only presentation candidate.

The current deterministic chain closes RC26's post-sign documentation with `scripts/materialize_ci_sources_rc26b.py`, then applies RC27 through `scripts/materialize_ci_sources_rc27.py`. Prior source payloads are immutable. An RC27 `[run ci]` execution must materialize the exact source, pass Unit/Lint/build and API36 regression, and retain the affected Mixer screenshot matrix. `[run ci signed]` is permitted only after that exact visual candidate has passed its digital gate and screenshot review.

Because RC27 changes only Compose presentation/tests/docs, U4/U7/Drive/provider-real campaigns are not reopened. Existing routing, recording, Demucs and backup evidence remains applicable unless a later source change touches those paths.

## Historical RC23 Studio UI qualification

The terminal source stage is `scripts/materialize_ci_sources_rc23.py`, after RC22. Its payload/terminal blobs seal the navbar, narrow Mixer, preferences, guide and affected tests without rewriting prior stages. RC23 is versionName `0.5.0-rc23`, versionCode `43`. Focused API36 additions run in Studio/practice and target-tablet geometry groups; the existing software and broader adjacent regressions remain enabled. The temporary `feature/studio-space-layout` push trigger was retired after successful branch qualification (#938) and before canonical integration/signing.

## Active RC24 Studio density qualification

Terminal stage: `scripts/materialize_ci_sources_rc24.py`, after immutable RC23. Candidate `0.5.0-rc24` / `44`. Qualification branch `feature/studio-density-rc24` temporarily participates in push CI; remove it before canonical signing. Existing focused Studio/practice/tablet groups execute updated tests, including populated audio lanes and complete/minimum screenshot artifacts. Local shell/source reconstruction checks do not substitute Android compile, Lint or API36 execution. The owner reports workflow completion; no polling/monitoring.

## RC25 output/CUE startup qualification

RC25 follows immutable RC24 with `.source-parts/RC25OutputCueCorrection.patch` and `scripts/materialize_ci_sources_rc25.py`. Temporary `feature/audio-routes-rc25` qualification trigger is independent of RC24's branch; both are retired before canonical signing. Existing software gate includes core-audio and app unit tests; existing practice regression includes unavailable-CUE history preservation. No workflow execution monitoring or result predeclaration.

RC25 branch qualification PASS: Android CI #946 / run 36934260224 on 854aeee0da730d2011e1141fc50a69d59bb13d58. Terminal source stage RC25f includes bounded geometry tests, compact Slider interaction and separate navbar viewport/content layouts. Retire RC24/RC25 temporary push exceptions before integration. Signed producer must run on canonical main and its exact signed identity is recorded only after successful completion. No workflow monitoring.

## RC26 signed digital authority

RC26 terminal runtime stage is `scripts/materialize_ci_sources_rc26a.py`. Android CI #950 / run `36944380748` passed software/Lint/build/API36 on source `148d7aacfe2793b2d4d10305940d838654ec83d9`; release producer `df5791c2e4984a7fbdf4141df460be20e151bcf8` retained the same Git tree and Android CI #951 / run `36946189701` requalified and signed the exact tested unsigned artifact. RC26 remains predecessor digital evidence, not the accepted physical baseline.

## Active RC27 Mixer visual-hierarchy qualification

RC27 follows a documentation-closure stage (`materialize_ci_sources_rc26b.py`) so the chain reproduces the signed RC26 ledger before applying new runtime changes. RC27 changes `MixerDock`, focused Mixer/tablet tests, in-app guide, version identity and all affected live UI/qualification documentation. The required first gate is `[run ci]`; complete/minimum five-channel screenshots are part of the qualification evidence. No result or signed identity is predeclared.
