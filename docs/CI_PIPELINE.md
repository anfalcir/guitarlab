# CI and Release Pipeline

Updated: 2026-10-03

This document describes current execution controls. Historical tail hashes, run chronology and retired gate details are preserved in `history/CI_PIPELINE_PRE_RC20.md` and immutable workflow artifacts.

## Source materialization

- canonical entrypoint: `scripts/materialize_ci_sources.sh`;
- current candidate tail: RC29 CUE route-settlement correction via `scripts/materialize_ci_sources_rc29.py`, chained after immutable RC28a/RC28 and earlier predecessors;
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


## RC29 CUE route-settlement qualification

Owner physical evidence `142418.mp4` rejects signed RC28 despite CI #958/#959 PASS: the target device still returns `ROUTE_UNCONFIRMED` for MK-300 MAIN + wired CUE. RC29 does not weaken route identity or clock safety. It splits admission into a bounded 5 s physical-route settlement phase and a separate bounded 2 s clock-qualification phase, requires four consecutive distinct-route polls before clock collection, and reasserts both explicit preferred devices after `AudioTrack.play()`. Any convergence after qualification, missing effective route, unstable clock, >12 ms initial offset, runtime drift or secondary backpressure remains blocking/fail-closed. Diagnostic export now carries the last preflight route trace.

## Post-freeze

The accepted baseline remains RC20 until exact signed successor physical acceptance. RC28 `0.5.0-rc28` / `48` is the latest signed digital authority after Android CI #958/#959 but is physically rejected for the target synchronized CUE pair. RC29 `0.5.0-rc29` / `49` is the active unsigned corrective candidate and must pass fresh software/API36 before any signing trigger.

The deterministic runtime chain closes RC26's post-sign documentation with `scripts/materialize_ci_sources_rc26b.py`, then applies RC27, RC27a loaded-frame evidence synchronization, RC27b compact clipping-badge correction and RC27c edge anchoring. Prior source payloads are immutable. CI #955 passed the terminal runtime source and visual evidence; release producer `222e66616e2eceba3e2781785c69c72020c40d04` preserved that exact tree and CI #956 signed it successfully.

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

## RC27a visual-evidence correction

CI #952 / run `37034736534` passed software and API36 on RC27 source `720ace0c76de20205ca6adde53d145fe4d867c8d`. Visual review accepted the minimum-mode/card screenshots but rejected the complete-mode target-tablet artifact because it showed the loading spinner. RC27a does not change runtime UI; it strengthens `StudioMixerTabletInstrumentedTest` so capture is gated by `StudioViewModel.loading == false`, representative waveform/Mixer visibility and Compose/window idle. A fresh `[run ci]` is required before signing.

## RC27b focused clipping visual correction

CI #953 / run `37037749921` passed RC27a software/API36 and produced valid complete/minimum target-tablet captures. Focused clipping/large-font review showed the textual CLIP face can cover centered header identity. RC27b changes only the visible clipping face to a compact warning badge while preserving its 48 dp action target/callback and full-width meters. A fresh `[run ci]` plus focused screenshot review is required before signing.

## RC27c focused clipping placement correction

CI #954 / run `37040291049` passed Unit/Lint/build and failed API36 only on the new `Master CLIP indicator must not cover MASTER` assertion. RC27c does not alter size or behavior: it moves the 28×24 dp warning face from the center of its unchanged 48×48 dp action target to the target's outer/right edge. A new `[run ci]` must pass the same non-overlap and clear-clipping interaction before signing.

## RC27 signed digital authority

RC27 terminal runtime stage is `scripts/materialize_ci_sources_rc27c.py`. Android CI #955 / run `37045089530` passed software/Lint/build/API36 and reviewed visual evidence on source `a506f3f81744f5de19e6a4fedb232a64503e2f50`. Release producer `222e66616e2eceba3e2781785c69c72020c40d04` retained the identical Git tree; Android CI #956 / run `37047225250` requalified and signed the exact tested artifact. Signed APK SHA-256 is `d5186972bda0efb48652656d310c3e45f70702df3e0dff070828e11890fb827f`; certificate SHA-256 is `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`. Physical owner validation remains separate.


## RC28 CUE physical-route identity gate

RC28 is triggered by owner target-device evidence, not by emulator speculation. The gate must prove that logical endpoint aliases cannot cause false CUE rejection while preserving physical isolation: API36 routed-device sets may contain several logical endpoints only when every endpoint canonicalizes to the selected physical output. Empty route evidence, an extra physical destination, MAIN/CUE physical convergence, unstable clocks, >12 ms initial offset and CUE backpressure remain blocking. After digital PASS, physical acceptance is required on the exact signed RC28 APK with MK-300 MAIN + wired CUE.


## RC28a qualification-only correction

CI #957 / run `37122989286` proved the RC28 API36 emulator regression PASS but failed the software gate before Lint/build because `AndroidOutputRouteIdentityTest` imported `kotlin.test.Test` in a module whose working tests use JUnit4 `org.junit.Test`. RC28a changes only that test annotation import plus truthful qualification/source-chain documentation. Runtime audio files, app identity `0.5.0-rc28` / `48`, CUE physical-route logic and release requirements are unchanged.
