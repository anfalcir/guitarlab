# U12 — Remote Prepared References v2 hardening plan

Updated: 2026-09-23
Status: RC14 signed DIGITAL PASS; RC15 physical corrective active
Target release candidate: 0.5.0-rc15 / versionCode 35

RC14 is the latest signed authority on producer `13c6f36e5e3c2bcf82cf21f885e8d7aa6c41ed34` (Android `35937621047`), with U4 `35935229248` and U7 `35935229232` passing on its cloud-identical predecessor. RC15 preserves that protocol and corrects the physical import findings documented in `RELEASE_NOTES_0.5.0-rc15.md`; `2601220` is the parser/reentry baseline, and the final source remains pending exact-source CI plus target-device acceptance.

## 1. Problem statement

Physical rc11 evidence proved the Cloud Run Demucs execution completed successfully and produced all six 44.1 kHz stereo stems, but the Android client failed after remote completion while downloading/validating/importing the result set. The affected job had six large WAV objects of roughly 63 MB each, so the old client contract moved roughly 380 MB of intermediate data to the tablet before local reference rendering.

Rc12 already hardens the legacy v1 path with file-backed streaming, resumable import semantics and IMPORT_FAILED rather than misreporting post-processing failures as EXPIRED. That is necessary backward-compatibility/recovery behavior, but it does not remove the architectural inefficiency.

## 2. Decision

New jobs use a versioned remote result contract v2.

Cloud Run remains responsible for Demucs six-stem inference, but the six stems are ephemeral implementation details. Before committing a remote result, the same worker renders the two product deliverables that GuitarLab actually needs:

- prepared Backing — stereo WAV float32, 44.1 kHz;
- prepared Guitar — stereo WAV float32, 44.1 kHz.

The reference recipe must preserve the existing deterministic U5 behavior:
- backing = drums + bass + other + vocals + piano;
- guitar = guitar stem;
- measure the loudest peak across backing, guitar and backing+guitar;
- apply one shared gain to both outputs;
- target ceiling = -1 dBFS;
- preserve exact frame alignment and common sample rate/channel contract.

The worker uploads only the two deliverables and the manifest. Raw six-stem WAVs are never uploaded for v2 jobs.

## 3. Why WAV remains the v2 transport

The Android/core codec matrix does not yet mark FLAC as a verified Studio editing format. Introducing FLAC here would couple this release-critical fix to a second codec qualification campaign.

Therefore v2 uses canonical float32 WAV. This already cuts remote egress from six stereo WAVs to two stereo WAVs (about 67% less for equal duration) and removes the six-stem memory/local-publication path. A later transport-only FLAC revision may be introduced independently after codec validation.

## 4. Manifest v2

The commit marker remains result-manifest.json, uploaded last.

Required v2 fields:
- schemaVersion = 2;
- immutable job/user/project/source ownership;
- Demucs engine/revision/model/model SHA;
- sample rate/channels/frames/duration;
- timing/thread/inference evidence;
- referenceRecipe with recipe version, targetPeakDbfs, sharedGainDb, backing stem membership and guitar source;
- exactly two deliverables:
  - name=backing, role=REFERENCE_BACKING;
  - name=guitar, role=REFERENCE_GUITAR;
- each deliverable carries path, bytes, SHA-256, sampleRate, channels, frames and encoding=FLOAT32_LE.

The Android parser remains backward compatible with schemaVersion 1 so an already-completed legacy job can still be rescued without another Demucs run.

## 5. Android publication

For v2:
1. download manifest;
2. verify manifest SHA and ownership;
3. download Backing and Guitar as file-backed staged payloads;
4. verify byte count + SHA-256 + WAV metadata without loading the whole file into memory;
5. atomically publish Backing and Guitar as managed derived reference assets;
6. split Guitar locally to L/R with the existing bounded-memory splitter;
7. create provenance linking both deliverables to the authoritative source, remote job, model and recipe;
8. set PreparationStatus.READY, activeBackingAssetId and activeGuitarAssetId;
9. bind references using the existing PreparedReferenceBindingPolicy;
10. only after local commit succeeds, acknowledge the remote import.

For v1:
- retain the rc12 streaming six-stem recovery path;
- IMPORT_FAILED remains resumable;
- never rerun Demucs merely because local import failed while remote results still exist.

## 6. Remote cleanup lifecycle

The result is transactionally owned by the remote job until acknowledgement.

### Normal success
- worker deletes uploaded source after the manifest is durably committed;
- app publishes local references and calls acknowledgeRemoteImport;
- acknowledge marks IMPORTED/PURGING;
- backend deletes the entire remote job prefix, not only output/;
- backend records PURGED + remotePurgedAt;
- acknowledgement is idempotent.

### Cancellation
- backend cancels the Cloud Run execution when possible;
- entire remote job prefix is deleted;
- job becomes CANCELLED;
- repeated cancellation remains idempotent.

### Purge failure
- local import remains successful;
- Firestore keeps remoteCleanupState=FAILED/PENDING;
- later backend calls may retry cleanup;
- the job must never be reverted from IMPORTED because cleanup failed.

### Orphan safety net
A prefix-scoped retention mechanism must delete remote/v1 job objects that outlive the recovery window. It must never affect Drive backups or unrelated Firebase Storage content. Deployment/verification must explicitly prove the retention rule rather than assuming it exists.

## 7. State semantics

- RUNNING/SEPARATING: Demucs inference;
- RUNNING/PREPARING_REFERENCES: server rendering Backing/Guitar;
- RUNNING/PUBLISHING_RESULTS: uploading the two final deliverables;
- COMPLETED: manifest + both deliverables committed remotely;
- IMPORTING: Android is downloading/validating/publishing;
- IMPORT_FAILED: remote result exists or local staged payload exists; user can resume import;
- IMPORTED: local references committed and ACK accepted;
- FAILED: processing contract failed;
- EXPIRED: reserved for genuine remote result expiry/removal, never generic local import failure.

## 8. Integrity and security

- stable Firebase Email/Password identity and GBW_ALLOWED_UIDS remain mandatory;
- no weakening of Storage/Firestore ownership boundaries;
- every object path remains namespaced by uid/jobId;
- SHA-256 is checked at remote render, manifest, staged download and managed publication boundaries;
- result paths reject traversal and foreign prefixes;
- manifest is the sole remote commit marker;
- no ACK before local durable project publication;
- no remote purge before ACK;
- no password persistence in GuitarLab.

## 9. Reliability

- all result downloads use files, never aggregate ByteArray payloads;
- staging is idempotent by byte count + SHA-256;
- process death/reboot can resume from cached final deliverables;
- publishing is all-or-nothing from the project model perspective;
- partial local assets are cleaned on failed publication;
- previous Studio bindings are not silently overwritten;
- legacy v1 completed jobs remain recoverable;
- reference rendering failure in Cloud Run becomes a typed terminal worker error, not COMPLETED.

## 10. Cost/storage controls

New v2 jobs do not upload raw stems at all.

Expected remote steady state:
- during processing: source object;
- after processing/before ACK: backing.wav + guitar.wav + result-manifest.json;
- after ACK: no job audio objects;
- after cancellation: no job audio objects;
- abandoned jobs: removed by the retention safety net.

This prevents indefinite accumulation of six-stem result sets.

## 11. Mandatory automated coverage

### Worker
- six aligned stems -> deterministic Backing/Guitar;
- backing membership exact;
- guitar identity exact;
- shared gain equivalence to U5 algorithm;
- peak ceiling <= -1 dBFS within tolerance;
- reconstruction/balance preservation;
- mismatched stem contract rejected;
- missing stem rejected;
- render/upload cancellation boundaries;
- manifest uploaded last;
- input removed only after committed result;
- no six stem upload in v2;
- manifest schema validation.

### Core/Android
- v1 manifest backward compatibility;
- v2 manifest ownership/path/SHA validation;
- exactly two v2 deliverables;
- file-backed download/cache/reopen;
- local publication + L/R split;
- project save/reopen;
- no activeStemAssetIds required for a v2-ready project;
- PreparedReferenceBindingPolicy works with v2 assets;
- IMPORT_FAILED resume without new enqueue/Demucs;
- remote missing while local staged deliverables exist;
- cancellation and project deletion semantics;
- notification ongoing -> terminal dismissible;
- Home sort persistence;
- Activity Limpar histórico preserves only active operations;
- inline Prepare cloud login.

### Backend
- ACK idempotency;
- whole-prefix purge on ACK;
- cleanup failure keeps IMPORTED;
- cancellation whole-prefix cleanup;
- authorized UID boundary;
- v2 schema/revision exposure;
- orphan-retention configuration verification.
- Firebase Storage rules compile and explicitly authorize authenticated owners to read only
  `output/prepared/backing.wav` and `output/prepared/guitar.wav`; the gate rejects a recursive
  `output/{path=**}` grant so intermediate or future artifacts are not exposed accidentally.

### RC14 physical-homologation corrective (2026-09-23)

Physical evidence from job `99d3099f-618d-42ca-afe6-a24471553460` proved that Cloud Run had
successfully committed both v2 references and the manifest, while Android deterministically
cycled from `IMPORT_FAILED` to `IMPORTING` and back before transferring either 63 MB WAV. The
root cause was a contract mismatch in Firebase Storage rules: the legacy direct-output matcher
authorized `output/result-manifest.json`, but no matcher authorized the v2 nested
`output/prepared/*.wav` paths.

Production ruleset `525c392a-2aeb-48f3-8685-e55f13c9d528` corrects the boundary with an exact,
owner-authenticated read grant for `backing.wav` and `guitar.wav` only. Client writes and all
other nested output paths remain denied. Existing completed jobs remain recoverable through
Retomar importação and must not rerun Demucs. The Android copy also distinguishes authorization,
session and integrity failures while retaining the invariant that remote results are preserved.

The subsequent Android bug reports captured the post-download failure precisely at
`WavStructure.read`: valid float32 deliverables used the standard `WAVE_FORMAT_EXTENSIBLE`
container and IEEE-float subtype, while RC14 recognized only the legacy top-level tags 1 and 3.
RC15 validates and accepts classic PCM/float plus extensible PCM/float with the complete canonical
subtype GUID. Unknown and malformed subtypes remain fail-closed. Client enqueue/resume is also
serialized across the durable check/save boundary to make rapid repeated taps idempotent locally;
the backend continues to enforce the single active job authoritatively.

### Gates
- unit/integration/lint/build;
- complete API 36 regression;
- U7 backend/container/schema/security gate;
- U4 real-cloud v2 smoke proving:
  source -> Demucs -> two prepared references -> manifest v2 -> download/hash -> ACK -> prefix purge;
- signed APK only after all digital gates pass.

## 12. Physical acceptance

The final physical candidate is not accepted until the target tablet proves:
- login/authorization;
- source upload;
- Cloud Run Demucs + server reference preparation;
- only Backing/Guitar transferred to the tablet;
- Studio opens with the prepared references;
- reboot/background resume;
- import retry without re-running Demucs;
- cancellation;
- ongoing/dismissible notification semantics;
- Activity history cleanup;
- Home sort persistence;
- remaining MK-300 recording/routing/10-minute acceptance.

No rc12 physical campaign is required if rc13 closes these digital gates first.
