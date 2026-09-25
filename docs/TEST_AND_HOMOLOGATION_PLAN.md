# Test and Homologation Plan

Updated: 2026-09-24

## Purpose

Qualify GuitarLab in proportion to credible risk for its single owner. Test count is not a release objective; confidence in protected owner outcomes is.

Historical campaigns and exhaustive matrices are retained in `history/TEST_AND_HOMOLOGATION_PLAN_PRE_RC20.md` and related evidence files.

## Protected outcomes

The following are blocking when a candidate can affect them:

- no project, recording or managed-media loss/corruption;
- structurally valid and musically usable prepared audio;
- working search/acquisition → Prepare → Studio path;
- safe save/reopen and transactional publication;
- recovery, quota, idempotency and cloud cleanup correctness;
- no credential exposure or unauthorized access;
- exact APK package/version/provenance/signer identity;
- exact worker digest/engine/model/recipe identity;
- safe production promotion with rollback.

## Evidence selection rule

For each change:

1. identify components and owner flows it can materially affect;
2. run focused tests for the changed path;
3. run adjacent integration smoke for protected boundaries;
4. reuse accepted unrelated evidence;
5. add broad regression only when the affected surface is uncertain or prior evidence is invalidated.

Broad suites may continue to run. A specific unrelated flaky/cosmetic failure may be quarantined with recorded evidence and rationale. Integrity, audio, primary-flow, quota/cost, credential and signing failures may not be quarantined.

## RC20 automated minimum

### Worker/backend

- deterministic source materialization and shell/source checks;
- worker unit tests and exact engine/model/dependency identity;
- structure/finiteness/hard audio-quality gates;
- W3 exact contract plus independent quality acceptance;
- selected CPU8 W3/W4 runtime/cost/bundle evidence from one Cloud Run execution and one Demucs inference;
- U7 digest-pinned shadow qualification;
- post-cutover U4 transactional smoke;
- secret/credential hygiene and retained vulnerability evidence.

Stochastic numeric/hash differences are diagnostic under D-091. CPU4 and other matrices are optional. Warm-probe and standalone model-probe are diagnostic-only and default off in the normal RC20 gate.

### Android

- unit tests relevant to search terminal states and changed domain behavior;
- Android Lint;
- release assembly;
- representative API36 regression for search/acquisition and adjacent Prepare → Studio/save-reopen behavior;
- exact unsigned artifact provenance;
- no-recompile signing and package/version/certificate/SHA verification.

Debug assembly, full screenshot matrices and isolated geometry are required only when relevant to the candidate or when prior evidence is invalidated.

## Human and physical boundaries

Automation cannot establish musical usefulness or target-device behavior completely.

D-092 explicitly waives the additional pre-cutover W5 owner-listening gate. After Android signing, the owner performs final acceptance on the exact signed APK under `RC20_PHYSICAL_HOMOLOGATION.md`.

Do not manually repeat digitally proven claims unless physical behavior can differ materially.

## Vulnerability handling

Retain SBOM/scanner output. A finding blocks when it represents a credible applicable high-risk path in the shipped runtime or credentials. Severity labels without applicable code/path/fix context do not automatically veto the personal appliance.

RC20 keeps its current narrowly evidenced scanner exceptions; this policy does not require building a new generic reachability framework before release.

## Candidate invalidation

A change invalidates evidence only for paths it can materially affect. Examples:

- worker image/model/recipe change: repeat worker shadow and cloud integration; owner listening is repeated only if explicitly reinstated or required by a new observed audio-quality concern;
- separation lifecycle/backend change: repeat U7/U4 and relevant Android recovery smoke;
- search/acquisition change: repeat focused Android search and Prepare adjacency;
- signing/build change: repeat artifact provenance/signing identity;
- recording route/timing change: repeat affected digital and target USB checks;
- documentation-only change: no product qualification.

## Acceptance records

For a promoted candidate retain:

- producer SHA;
- relevant workflow/run identities and conclusions;
- unsigned and signed APK SHA-256;
- package/version/signer;
- worker digest and engine/model/recipe;
- owner-listening decision/waiver;
- physical acceptance decision and included capabilities;
- explicit quarantines or known non-blocking limitations.

`CURRENT_STATE.md` contains only the current record. Superseded evidence belongs under `history/` or immutable CI artifacts.

## Freeze

After final owner acceptance, do not run recurring qualification or rebuild for freshness alone. Reopen testing only with a maintenance trigger or owner-requested feature, and apply this same affected-path rule.


## U12bn RC20 Android completion focus
The next Android qualification must exercise the exact U12bn source and include focused coverage for persistent Prepare search terminal states/suggestion retry, local reference-binding repair, diagnostics navigation/export/redaction/checksums, accepted-manifest retention ordering, and adjacent Prepare → Studio behavior. The final signed artifact remains subject to the consolidated SM-X230/MK-300 physical campaign before freeze.


## RC20 physical candidate identity

The exact APK to install for final physical validation is `GuitarLabStudio-0.5.0-rc20-homologacao.apk`, signed APK SHA-256 `e82fdc75896564ea10c28e072fd186913320eca293b6fad9ea4b7c8fa0468468`, producer commit `fd63413ea440ed96a227b0203768b113bf256e98`. Do not rebuild between this digital pass and physical homologation.
