# GuitarLab 0.5.0-rc19 — authoritative remote recovery corrective

Status: **DIGITAL PASS / SIGNED · PHYSICAL ACCEPTANCE BLOCKED BY AUDIO-QUALITY INCIDENT**.

## Identity
- versionName: `0.5.0-rc19`
- versionCode: `39`
- package: `studio.guitarlab.app`
- signer identity: unchanged; signed homologation produced by Android CI #852 / run `36026458960` on frozen source `e0a8a2218accbe9c8eec413a40f4fedf7d9da9bc`.

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
Canonical candidate source stage: **U12an**. U12aj contains the production/test/version/U4 recovery delta, U12ak corrects the process-death test double, U12al reconstructs `functions/src/index.ts` from the clean RC18 source plus the intended RC19 callable contract, U12am isolates the callable-recovery proof from the worker-only shadow while leaving it mandatory by default and for production/U4, and U12an makes exact same-generation adoption win safely over unrelated preserved active work without dispatching or consuming quota. Each source stage remains blob-locked with terminal/semantic guards, `git diff --check` and reverse-apply. The U7 workflow additionally accepts explicit push markers on canonical `main`: `[run u7 shadow]` for shadow and `[run u7 production] [confirm production]` for production, while preserving the existing manual `workflow_dispatch` confirmations.

## Production qualification

- Shadow: U7 #129 / run `36021641467` PASS on `96c27a03b838878a14fc767a05811e95300ab074`, including real `guitarlab-demucs-shadow-7v74l` prepared-reference v2 smoke; production Functions/rules were not mutated in shadow.
- Production: U7 #130 / run `36024185442` PASS on `f0f743c3c311a4332175b15737cf6a661be07f55`.
- Production recovery gate: same-generation COMPLETED job adoption returned the existing remote job, did not increment quota, did not dispatch a second Cloud Run execution, and cleaned the rejected retry upload before normal ACK/purge.
- Production real-cloud smoke: `gbw-demucs-lcslf` PASS; prepared-reference v2 manifest/integrity, source cleanup, ACK, Firestore IMPORTED/PURGED and whole-prefix purge idempotency all passed.
- U7 deploy evidence artifact id `10819698734`, digest `sha256:783af8ec21c201ac7b388c61bf08711454cd00c29edefdcff28866522984d2a4`.
- Signed homologation APK: `GuitarLabStudio-0.5.0-rc19-homologacao`, artifact id `10820021943`, APK SHA-256 `20c6a49efa69828653862c29c7907cd734bd2e3f62235295cd53b7f7c571c50b`. Android CI #852 passed unit, Lint, build, API 36 and signing.

## Physical audio-quality finding

RC19 is not approved for final physical acceptance.

A target-device Prepare run for project `WATG - Deadbolt` completed under processing/job `c3ba40ec-6153-483a-801f-9e838b3e3b60`, but the resulting references remained unusable by listening.

Supplied active-reference evidence:
- backing SHA-256 `d9f206f25f9eb28d02c7fb4a4a3e1d667077429e91520eb64763cd0251fe2a8e`;
- guitar SHA-256 `8bbb4b5eb6b4ef8758669c1734b8d095893aadc9792d12a6330d9318eae725fa`;
- both are 44.1 kHz stereo float32 WAV, same duration, no NaN/Inf;
- their recombination peaks at approximately -1 dBFS, consistent with the v2 shared-gain ceiling;
- a notable backing mean/DC component is recorded as diagnostic evidence but is not yet established as the root cause.

The incident does not invalidate RC19's recovery/idempotency digital proof. It does block product acceptance because numerical integrity alone did not prove useful musical separation.

The authoritative follow-up is `RC20_COMPLETION_PLAN.md`. Read-only Cloud Run/Firebase lineage forensics must precede any audio-algorithm change. Any source fix requires a new candidate; planned target is `0.5.0-rc20` / versionCode `40`.
