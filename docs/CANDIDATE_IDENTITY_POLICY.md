# Candidate identity policy

Updated: 2026-09-25

This file defines the stable identity/provenance contract for every promoted GuitarLab candidate. It intentionally does **not** duplicate the volatile “current candidate” snapshot; read `CURRENT_STATE.md` for that.

## Locked product identity

- canonical branch: `main`;
- package: `studio.guitarlab.app`;
- expected homologation certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Historical alpha/RC identities remain evidence only.

## Exact-source rule

A promoted candidate is bound to the exact `github.sha` whose source was materialized and qualified. Documentation prepared before the run must not guess or predeclare the producer SHA.

The release evidence must agree on exact producer SHA, package, versionName, versionCode, unsigned APK SHA-256, signed APK SHA-256, signer certificate and workflow/run identity.

Any mismatch blocks delivery. A successful build from a different SHA, version or signer is a different candidate.

## Build-once / sign-exactly rule

The release package is compiled while signing credentials are absent. After the required qualification gates pass, the signing job must download the **exact tested unsigned APK**, verify its checksum and package/version identity, align/sign it without recompiling source, verify the locked certificate and record the final signed APK SHA-256.

## Candidate lifecycle

**source/backend qualification → exact unsigned Android candidate qualification → signing of that exact artifact → physical homologation of the signed APK → freeze**

Worker/audio listening may occur before Android signing. Final application physical acceptance must be performed on the exact signed APK intended to become the frozen baseline.

## Proportional qualification rule

A promoted candidate must pass all gates relevant to the changed code and representative supported path, including protected data/media integrity, musical validity, primary flow, quota/cost, credential and signing checks.

Existing broad Android regression may continue to execute unchanged. A specific unrelated flaky/cosmetic failure may be quarantined only with explicit evidence and rationale. No data-loss, audio-integrity, primary-flow, quota/cost, credential or signing failure may be quarantined.

The frozen RC20 Trivy/SBOM evidence is retained. For future maintenance candidates, applicability under D-090/D-096 guides decisions; no generic severity-only waiver or broad subjective reachability framework replaces evidence-backed review.

## Cloud/backend identity

When a candidate changes the separation backend/worker, the finalist is qualified by immutable digest; shadow precedes production mutation; the rollback target is retained; and transactional U4 evidence is required after the affected production change.

Unrelated Android-only changes do not automatically require a redundant provider-real campaign when prior applicable evidence remains valid.

## Frozen baseline and future candidates

The accepted RC20 artifact/backend identity is recorded in `RELEASE_BASELINE.md`. That baseline is frozen and is not redefined by later documentation commits.

A future runtime/source/backend change creates a new candidate with a new exact producer identity and proportional qualification. A later docs-only commit never changes the producer identity of an already-built candidate.
