# GuitarLab 0.5.0-rc19 — authoritative remote recovery corrective

Status: **SOURCE CANDIDATE / DIGITAL QUALIFICATION PENDING**.

## Identity
- versionName: `0.5.0-rc19`
- versionCode: `39`
- package: `studio.guitarlab.app`
- signer identity: unchanged; signed homologation remains intentionally deferred until the corrective gates pass.

## Corrective scope
RC19 closes the local ↔ remote reconciliation gap found during RC18 physical homologation.

- strong generation identity remains `uid + projectId + sourceAssetId + inputSha256`;
- `findRecoverableRemoteSeparation` discovers only the authenticated user's active same-generation job;
- `enqueueRemoteSeparation` returns `EXISTING_SAME_GENERATION` rather than consuming quota or dispatching another Cloud Run execution when matching work already exists;
- different active work is typed as `ACTIVE_JOB_CONFLICT`;
- monthly exhaustion is typed separately as `MONTHLY_QUOTA_REACHED`;
- unregistered retry uploads rejected synchronously are cleaned only after confirming no Firestore job exists for the requested jobId;
- Android persists the backend's effective job identity through an explicit adoption path instead of falsifying or rewriting jobId;
- manifest ownership and `RemoteResultManifest.validateFor()` remain strict;
- reopen/process restart schedules an authoritative generation-recovery probe before restoring a stale SEPARATING project to SOURCE_READY;
- Prepare copy distinguishes resumed cloud work, another active separation and monthly quota without exposing Firebase error jargon;
- U4 real-cloud now contains a completed-but-unacknowledged recovery/idempotency sub-gate before the existing ACK/PURGED proof.

## RC18 physical fixture
The production RC18 result below is intentionally preserved for RC19 physical acceptance:

`0860b0a7-6dda-439d-87af-b0f200c1a8b5`

Project:
`18cf294d-7a76-4f03-8abd-bc55d3344beb`

RC19 acceptance must install over RC18 without clearing app data and recover that exact result through the normal app path. No manual ACK, cancellation, Firestore mutation or Storage purge is permitted before the app proves recovery.

## Materialization
Canonical candidate source stage: **U12aj**.

`.source-parts/U12ajRemoteSameGenerationRecovery.patch` is blob-locked and `scripts/materialize_ci_sources_u12aj.py` enforces terminal blobs, semantic guards, `git diff --check` and reverse-apply.
