# Unified GuitarLab + GBW — Master Integration Roadmap

Updated: 2026-09-21
Status: **APPROVED SUCCESSOR PROGRAM / PLANNING AUTHORITY**  
Canonical repository: **anfalcir/guitarlab**  
Target Android package: **studio.guitarlab.app**  
Execution model: **serial, gate-driven, idempotent, AI/ChatGPT Work-friendly**  
Physical homologation objective: **one consolidated final target-device campaign whenever technically possible**

---

## 0. Authority and purpose

This document is the authoritative implementation guide for integrating the GBW Android product into GuitarLab as one coherent application.

It is intentionally self-contained. A future ChatGPT Work session must be able to continue the program by reading the repository and this file without relying on chat memory.

This roadmap does not erase or reinterpret historical evidence. The existing GuitarLab H29-H37 line and the existing GBW Android RC5 line remain evidence-bearing baselines. The integration program consumes them as inputs and replaces neither baseline retroactively.

The program changes the product direction from two cooperating Android applications to one product:

**GuitarLab = Prepare + Studio + Export + Cloud/Backup**

GBW ceases to be a separate end-user Android product after successful migration and becomes the preparation/separation capability of GuitarLab. The frozen GBW Linux baseline remains historical/functional reference and is not merged into the Android runtime.

### 0.1 Program naming

Integration milestones use the prefix **U** ("Unified") to avoid collision with the historical GuitarLab M/H numbering and GBW milestones.

Sequence:

U0 → U1 → U2 → U3 → U4 → U5 → U6 → U7 → U8 → U9 → U10 → U11 → U12

No U milestone may be marked complete unless its entry conditions, implementation obligations, automated evidence and exit gate are all satisfied.

### 0.2 Snapshot used to author this roadmap

GuitarLab:
- repository/branch: anfalcir/guitarlab / main;
- observed HEAD when this roadmap was authored: 01b2371310fb872eb1583231728941beb93c1a8e;
- package: studio.guitarlab.app;
- current signed digital authority before integration: CI #663 / H36c;
- H37b Drive v3 source candidate is SOURCE PRE-GATE at the authoring snapshot;
- main CI policy is manual-only workflow_dispatch.

GBW Android:
- repository/branch: anfalcir/gbw / dev/android-6.0;
- observed HEAD when this roadmap was authored: 4724b030eabe289a5c2645e379181c0f9602d25d;
- functional RC5 baseline documented at commit 48af553518c44b46b913aad73c22e3da04368ea5;
- package: com.gbw.android;
- cloud backend project: gbwapp-ef048;
- remote separation: Firebase/GCP + Cloud Run Job gbw-demucs;
- remote separation functional baseline is digitally closed in GBW documentation;
- standalone GBW backup is in migration toward direct Drive API v3 at the authoring snapshot.

These SHAs are provenance, not permanent assumptions. Every execution session must re-read the live repositories before modifying code.

---

# 1. Mission

Build one Android application in which a single immutable project identity covers the complete guitar-study workflow:

Source acquisition
→ source validation
→ Demucs separation
→ stems
→ backing/reference preparation
→ GuitarLab Studio
→ recordings/takes
→ timeline/mix
→ study exports / studio masters
→ transactional Google Drive backup and restore

The user must never need to "send a project from GBW to GuitarLab" in the final architecture. Preparation and Studio operate on the same project and the same managed asset graph.

---

# 2. Product decisions locked by this roadmap

## 2.1 GuitarLab is the surviving product shell

The final Android product remains GuitarLab.

Preserve:
- package studio.guitarlab.app;
- GuitarLab signing identity and upgrade continuity;
- GuitarLab current Studio/audio/timeline/take model unless explicitly migrated by this roadmap;
- GuitarLab repository as the canonical Android product repository.

GBW Android is an implementation source and migration source, not the final package identity.

## 2.2 GBW becomes "Prepare"

GBW capabilities are absorbed primarily into a GuitarLab **Prepare** workspace:
- source selection/import/search/download;
- source validation/preparation;
- remote Demucs orchestration;
- optional explicit local Demucs fallback while it remains technically supportable;
- six-stem management;
- backing/reference construction;
- job progress, cancellation and diagnostics.

## 2.3 One project identity

A project has one immutable projectId across:
- rename;
- source changes;
- separation;
- new backing versions;
- Studio edits;
- takes;
- exports;
- Drive sync.

Display name, artist, song title and folder names are never identity.

## 2.4 Drive and Firebase remain separate security/data planes

Google Drive:
- durable user project backup/restore;
- direct client-side Drive API;
- narrow drive.file authorization unless a future explicit decision changes it;
- no Firebase hop for backup bytes.

Firebase/Cloud Run:
- authentication/orchestration for temporary remote separation;
- transient source/stem transport only;
- ACK after durable local validation;
- purge after ACK;
- TTL fallback for orphaned temporary cloud objects.

Sharing one Google Cloud/Firebase project does not merge OAuth/token semantics.

## 2.5 No tuner / no pitch workflow

The unified Android product does not gain a guitar tuner, tuning detector, Rubber Band pitch workflow, BS-RoFormer or pitched exports merely because old GBW history contained such capabilities.

The active GBW Android scope is the source:
- original-key workflow;
- Demucs-only separation;
- no tuner;
- no pitch/tuning subsystem.

## 2.6 Physical validation is residual

Automated and programmatic verification must be pushed as far as realistically possible.

The desired manual strategy is:
- no repeated physical validation after every U milestone;
- preserve known physical residuals as explicit tracked items;
- accumulate only hardware/subjective claims that cannot be proven digitally;
- execute one consolidated final signed-candidate campaign on the target tablet/MK-300 after the complete unified line is digitally green.

A second physical campaign is allowed only if the first exposes a defect requiring source changes that invalidate physical evidence.

---

# 3. Explicit non-goals for the integration program

Do not use the integration as an excuse to add unrelated feature families.

Outside this roadmap unless separately approved:
- tuner;
- automatic pitch/tuning conversion;
- BS-RoFormer;
- multiple separation engines;
- tempo grid/metronome;
- time stretch;
- arbitrary DAW expansion;
- per-track independent physical outputs;
- cloud storage of temporary Demucs files as permanent backup;
- broad Drive scope;
- silent automatic conflict resolution;
- destructive migration of legacy user data;
- automatic deletion of old GBW/GuitarLab backups before verified migration.

Existing GuitarLab behavior that is already implemented remains regression scope even when it is not expanded.

---

# 4. Core engineering principles

Every U milestone must preserve these invariants.

1. **Fail closed on identity and integrity.**
   Unknown project, asset, job, revision, route or checksum state may not be guessed.

2. **Authoritative media is immutable.**
   Imported sources, accepted stems and finalized recordings are immutable managed assets. Edits are metadata/derived assets.

3. **Derived media is replaceable only by explicit provenance.**
   A new backing or reference does not silently replace one already used by a Studio session.

4. **No publication before validation.**
   Network download, Demucs stem import, restore, migration and export all stage first, validate, then publish atomically.

5. **Idempotency is a product requirement.**
   Retrying a completed operation after process death, network loss, WorkManager retry or reopen may not duplicate project state.

6. **Async results are ownership-scoped.**
   Every asynchronous result verifies projectId, operationId/jobId and expected source/revision before publication.

7. **Project rename is cosmetic.**
   Rename never forks cloud history or asset identity.

8. **No silent fallback.**
   Recording input, remote separation, Drive authorization and migration cannot silently use a different path when the requested one fails.

9. **Security boundaries remain explicit.**
   Drive token != Firebase session != App Check token != Cloud Run authorization.

10. **Evidence is exact-source.**
    Candidate claims bind to an exact Git producer, build identity, tests and hashes.

11. **No automatic hosted CI consumption without policy authorization.**
    The current GuitarLab CI policy remains manual-only until explicitly changed. Local/emulator tests should be run autonomously where possible; canonical hosted gates are never assumed.

12. **Documentation changes with behavior.**
    A milestone is incomplete if code and live documentation disagree.

---

# 5. Target information architecture and UX

## 5.1 Home becomes the unified project library

Each Home card represents one project, not an app subsystem.

Recommended compact card state:
- artist / song or project name;
- Prepare status: not started / source ready / separating / ready / error;
- Studio status: not started / session exists / recordings present;
- cloud status: synced / pending / error / auth required;
- last meaningful update;
- active job progress when present.

Do not overload the card with raw technical identifiers. projectId/revision IDs belong in details/diagnostics.

## 5.2 New Project entry points

The single **+ New Project** flow exposes:

1. **Search song**
   - opens Prepare source search;
   - creates the project only when enough metadata/source intent exists to do so safely.

2. **Import audio**
   - local source via Android document picker;
   - source is copied into project-managed immutable storage.

3. **Blank project**
   - creates a Studio-ready project with no Prepare dependency.

A blank project must remain first-class. GuitarLab may be used purely for recording without GBW processing.

## 5.3 Project shell

Primary project workspaces:

- **Prepare**
- **Studio**
- **Export**

A project summary/status surface may be shown before or alongside those workspaces, but it must not become a fourth complex workflow.

Tablet:
- persistent or adaptive project navigation;
- wide layouts use available space without duplicating controls.

Phone:
- adaptive tab/navigation implementation;
- same capabilities, not a reduced product.

## 5.4 Prepare workspace

Flow:

Source
→ Separation
→ Prepared references
→ Ready for Studio

### Source
Supports the accepted GBW Android source families:
- local;
- Bandcamp;
- SoundCloud;
- YouTube;
- any additional provider only if already accepted by the live GBW baseline at implementation time.

Search ranking and source-provider details remain behind a domain/service abstraction. UI must show source identity and validation status without leaking implementation jargon.

### Separation
Shows:
- local/remote execution mode;
- state;
- progress;
- cancellation;
- queue/start time where useful;
- monthly cloud usage/policy state where supplied;
- actionable failure;
- sanitized diagnostics.

### Prepared references
At minimum:
- source original;
- six stems;
- default backing;
- guitar reference.

Default backing recipe remains the current GBW rule unless later explicitly changed:

drums + bass + other + vocals + piano

guitar is excluded from default backing.

## 5.5 Studio handoff is zero-copy

Opening Studio after Prepare must not export/reimport files.

Studio references managed assets already owned by the project.

Default guitar-study template may map:
- prepared backing → Backing Track;
- prepared guitar stereo → Guitar L / Guitar R derived mono references when the template requires them;
- My Guitar L / My Guitar R remain recording tracks.

The exact template must preserve existing GuitarLab role and stereo-separation invariants.

## 5.6 Prepared-reference version changes

A re-separation or different backing recipe creates a new prepared asset/version.

It may not silently mutate an existing Studio reference.

If Studio already references an older backing/reference, UI presents a reviewable state such as:
- new prepared reference available;
- Update Studio reference;
- Keep current.

Updating reference must preserve:
- recordings;
- takes;
- clip edits;
- markers/sections;
- loop;
- mixer;
- project identity.

## 5.7 Project Assets surface

Provide a compact asset browser/details surface, not six permanent extra timeline tracks.

At minimum it can expose:
- Original;
- Backing;
- Guitar;
- Drums;
- Bass;
- Vocals;
- Piano;
- Other;
- recordings;
- exports.

Future drag/use actions may consume these assets, but first integration must avoid cluttering the Studio automatically.

## 5.8 Export workspace

Separate two concepts.

**Study exports**
- prepared backing;
- guitar reference;
- any explicitly supported Prepare export variant.

**Studio master**
- GuitarLab offline render of the current session;
- existing WAV/FLAC/MP3 support rules remain capability-gated.

Never label both workflows ambiguously as one generic export if the result semantics differ.

## 5.9 Activity / Jobs Center

One global activity surface owns long-running work:
- source preparation/download;
- remote separation;
- stem import;
- backup;
- restore;
- project migration;
- master export where useful.

Each item carries:
- operationId/jobId;
- projectId;
- semantic type;
- state;
- progress;
- retry/cancel policy;
- terminal result;
- sanitized error.

Leaving a project must not cancel a valid long-running operation unless the user explicitly cancels it.

## 5.10 Settings

Recommended groups:

**Account & Cloud**
- separation sign-in/session;
- cloud usage/quota state;
- Google Drive connection;
- automatic backup;
- sync diagnostics.

**Audio & Recording**
- input/output;
- monitoring;
- sample rate;
- latency/sync/calibration;
- existing recording safety.

**Control**
- existing external MIDI/HID support if present/enabled.

**Application**
- appearance;
- storage;
- diagnostics;
- about/help.

Drive and Firebase sessions must be presented as separate capabilities even if the same Google account is used.

---

# 6. Target modular architecture

The exact module split may be adapted to the live repository, but dependency direction must follow this model.

Suggested target:

app

core:model  
core:project  
core:media  
core:codec  
core:audio  
core:jobs

feature:library  
feature:prepare  
feature:studio  
feature:export  
feature:settings  
feature:activity

platform:audio-android  
platform:codec-android  
platform:drive  
platform:firebase  
platform:source-android  
platform:separation

cloud/separation

### Dependency rule

Feature modules depend on core contracts.

Platform modules implement core ports.

Studio must not depend on Prepare implementation.

Prepare must not depend on Studio implementation.

Both consume the shared project/media domain.

The app module wires navigation, dependency composition and Android lifecycle only.

### Repository strategy

The final Android and cloud source should become reproducible from anfalcir/guitarlab.

Do not mechanically copy GBW wholesale.

For each imported GBW component:
1. record original path/repository/commit;
2. identify its contract;
3. port or move the minimal implementation;
4. adapt package/module boundaries;
5. reproduce its tests;
6. add integration tests;
7. only then stop depending on the old location.

The GBW repository remains a frozen historical/migration reference after cutover.

---

# 7. Unified domain contract

## 7.1 UnifiedProject

A project must have stable persisted fields equivalent to:

- schemaVersion;
- projectId;
- displayName;
- artist optional;
- title optional;
- createdAt;
- updatedAt;
- sample-rate/project-audio settings;
- preparation state references;
- Studio state;
- export metadata;
- migration provenance.

Avoid placing transient job/network status inside the creative project document unless it is semantically durable. Recoverable operation state belongs to a separate job store.

## 7.2 Asset

All authoritative/derived media becomes explicit project media metadata.

Minimum conceptual fields:

- assetId;
- projectId or shareable ownership scope;
- role;
- relative managed-media location;
- content SHA-256;
- byte size;
- format/container;
- codec/encoding;
- sample rate;
- channels;
- frame count/duration;
- createdAt;
- source/provenance;
- authoritative vs derived classification;
- lifecycle state.

Recommended roles include:

SOURCE_ORIGINAL  
STEM_DRUMS  
STEM_BASS  
STEM_OTHER  
STEM_VOCALS  
STEM_GUITAR  
STEM_PIANO  
REFERENCE_BACKING  
REFERENCE_GUITAR  
RECORDING_TAKE  
STUDIO_MASTER  
PRACTICE_EXPORT

Role is not identity. Two assets may have the same role when versions exist.

## 7.3 Provenance

Every derived asset must state enough deterministic provenance to explain and validate its origin.

Examples:

Stem:
- sourceAssetId;
- source SHA-256;
- engine family;
- model/checkpoint identity/hash;
- separation contract version;
- relevant processing parameters.

Backing:
- input stem asset IDs/hashes;
- recipe version;
- included/excluded stems;
- shared-gain rule/version;
- render implementation version.

Derived Guitar L/R:
- parent stereo asset;
- selected channel;
- conversion policy/version.

Master:
- project revision;
- render settings;
- format.

Provenance must never contain secrets/tokens.

## 7.4 Project revision

The unified revision identifier must derive from canonical durable project state, not volatile fields such as:
- current sync progress;
- last retry time;
- notification state;
- access token;
- transient route ID.

Canonicalization must be deterministic and covered by golden/property tests.

A rename may create a new logical revision if display metadata is part of user state, but it must never change projectId or fork project history.

## 7.5 Reference bindings

Studio does not "own copies" of Prepare assets.

A Studio reference binding points to an asset/version.

When a replacement asset is created:
- the old binding remains valid;
- UI can offer an explicit rebind;
- rebind is a transactional project edit;
- Undo/Redo semantics are defined;
- stale asset cleanup cannot delete still-referenced media.

---

# 8. Managed-media lifecycle

Lifecycle states should be explicit enough to prevent cleanup bugs:

staging  
→ validated  
→ published/managed  
→ referenced or unreferenced  
→ eligible for safe cleanup only under proven rules

Never delete:
- authoritative source;
- finalized recording;
- accepted stem;
- any asset reachable from current project state;
- any asset reachable from retained Undo/history if current GuitarLab policy protects it;
- any asset required by pending cloud backup;
- any asset involved in in-progress migration.

Derived caches such as waveform/proxy may follow existing regenerable-media policy.

A single reachability service should answer whether managed media can be deleted. Feature code must not invent independent cleanup heuristics.

---

# 9. Separation integration contract

## 9.1 Remote path

Preserve the accepted GBW RC5 flow semantically:

managed source
→ upload to temporary cloud transport
→ Firebase job state
→ Cloud Run Demucs
→ six stems
→ Android download staging
→ structural/audio validation
→ durable local publish
→ ACK
→ remote purge

Remote Storage is never Drive backup.

## 9.2 Required stem acceptance

Before ACK/publish, validate as objectively possible:
- exactly expected stem set;
- no duplicate semantic stem;
- non-zero payload;
- decodable audio;
- expected channel policy;
- expected sample-rate policy;
- duration within defined tolerance of source;
- no NaN/invalid PCM after decode;
- hashes recorded;
- jobId/projectId/source identity match.

If one stem fails, do not publish a half-valid prepared set as complete.

## 9.3 Remote state monotonicity

Remote/local reconciliation must prevent terminal regression.

Example accepted state order:

UPLOADING  
READY/QUEUED  
RUNNING  
COMPLETED  
IMPORTING  
IMPORTED

FAILED/CANCELLED/EXPIRED are terminal for the relevant attempt.

Late polling may not turn IMPORTED back into RUNNING.

## 9.4 Idempotency

A completed job reconciliation may not:
- duplicate stems;
- create a second backing unintentionally;
- ACK twice with conflicting state;
- recreate a dismissed terminal notification.

Local publish is keyed by operation/job identity plus source/provenance identity.

## 9.5 Explicit local fallback

If local Demucs remains in scope:
- it is explicit, never silently selected after remote failure;
- model/checkpoint is hash-verified;
- execution survives safe Android lifecycle rules as far as platform permits;
- result enters the exact same stem validation/publication pipeline;
- provenance records LOCAL vs REMOTE;
- parity tests verify role/format/duration contracts.

If later evidence justifies removing local fallback, that requires an explicit decision and migration of UX/docs/tests; it may not disappear accidentally during integration.

---

# 10. Firebase / Cloud Run integration boundary

## 10.1 GuitarLab Firebase app identity

Because the unified GuitarLab app will consume the GBW separation backend, the Firebase project may be shared but the Android app identity is GuitarLab-specific.

The implementation must use the Firebase Android app configuration for:
- package studio.guitarlab.app;
- the correct signing identities/environments.

A GBW package configuration must never be copied blindly.

Firebase SDK/configuration is introduced for remote separation only. This does not change the direct Drive backup architecture.

## 10.2 Backend authorization

Before cutover:
- audit Firestore/Storage/Functions/Cloud Run authorization assumptions;
- ensure user-scoped job isolation;
- preserve maximum concurrent job policy;
- preserve monthly logical quota policy unless explicitly revised;
- prevent cross-user/project access;
- sanitize diagnostics.

## 10.3 App Check

Treat App Check independently from Firebase Auth.

If Play Integrity enforcement cannot be proven for sideloaded homologation builds, do not enable enforcement merely to satisfy architecture aesthetics.

Required:
- code path covered;
- enforcement state explicit;
- fail modes tested;
- real-device evidence before enabling a mode that could lock out the installed app.

## 10.4 Cloud source migration to GuitarLab repository

Use a staged cutover.

Phase A:
- GuitarLab client consumes the existing proven GBW backend contract;
- no production cloud move required.

Phase B:
- copy/port cloud source into GuitarLab with provenance;
- reproduce worker/backend/security tests;
- build immutable image;
- deploy a staging/shadow job;
- compare contract/output behavior.

Phase C:
- switch production deployment authority/OIDC conditions to GuitarLab only after staging parity;
- pin last-known-good worker image digest;
- preserve rollback command/config;
- do not delete old GBW cloud source/deploy evidence until rollback window closes.

The Cloud Run job may keep its existing external name during transition if that reduces risk. Product branding does not require a risky infrastructure rename.

---

# 11. Drive backup architecture for the unified product

## 11.1 Why the cloud format must evolve

A unified project can contain:
- original source;
- six large stems;
- backing/reference assets;
- multiple recordings/takes;
- Studio data;
- exports.

Re-uploading a monolithic full snapshot after a small timeline edit can become unnecessarily large.

Therefore the final unified cloud backup must support content-addressed immutable media deduplication while preserving transactional project revisions.

Portable .guitarlab export remains a separate self-contained user package and does not need to equal the cloud representation.

## 11.2 Unified Drive schema

Introduce a new explicit backup schema version for the unified system. Do not reinterpret H37 history in place.

Conceptual remote records:

**Asset objects**
- immutable;
- identity by content SHA-256 plus validated size;
- metadata identifies schema/role-independent content facts;
- resumable upload;
- server-confirmed integrity before eligible for a project commit.

**Project revision manifests**
- immutable;
- contain canonical project state and exact referenced asset hashes/IDs;
- projectId;
- revisionId;
- baseRevisionId where applicable;
- schema;
- timestamps;
- manifest hash;
- migration provenance.

**Current project descriptor/head**
- identifies currently committed revision;
- update occurs only after every referenced asset and revision manifest is verified;
- conflict/reconciliation logic must be explicit.

Exact Drive concurrency/precondition mechanics must be verified against current Drive API behavior before coding. Do not invent an unsupported compare-and-swap primitive.

## 11.3 Commit protocol

Minimum transaction order:

1. freeze canonical local snapshot for desiredRevisionId;
2. enumerate referenced authoritative assets;
3. validate every local asset;
4. upload only missing remote asset objects;
5. verify remote object size/checksum/properties;
6. upload immutable revision manifest;
7. verify revision manifest;
8. reconcile remote current descriptor against the last-known/base revision;
9. if no conflict, publish new current descriptor;
10. verify server state;
11. only then mark local revision server-confirmed;
12. run best-effort garbage collection only after reachability proof.

No path may display "synced" after step 5 or 6 only.

## 11.4 Deduplication

Deduplicate by content identity, not filename.

At minimum:
- same asset content in repeated project revisions uploads once;
- project rename causes no media reupload;
- metadata-only Studio edit causes no media reupload;
- a new take uploads only that new asset plus new revision metadata;
- a new separation uploads only new stem/backing assets not already present.

Cross-project dedup may be supported if the reachability/GC model safely accounts for it. If not, use project-scoped hash objects first; correctness outranks maximum dedupe.

## 11.5 Garbage collection

Never delete a remote asset because it disappeared from the newest revision alone.

GC requires:
- complete committed-manifest reachability analysis;
- retention policy;
- no pending local revision referencing the object;
- no migration in progress;
- safety grace period;
- best-effort deletion after new state is fully committed.

GC failure never invalidates a successful backup.

## 11.6 Conflict semantics

Never choose newest timestamp automatically when both sides changed.

At minimum distinguish:
- NO_OP;
- UPLOAD_LOCAL;
- DOWNLOAD_REMOTE;
- LOCAL_ONLY;
- REMOTE_ONLY;
- CONFLICT.

Conflict UX:
- keep local;
- use Drive;
- duplicate/import as separate copy where safer;
- technical details available.

Resolve operations create new explicit state; they never rewrite history invisibly.

## 11.7 Restore

Restore pipeline:
1. discover eligible committed project revision;
2. stage manifest;
3. validate schema/project/revision;
4. resolve exact asset set;
5. download missing assets;
6. validate bytes/hash/format;
7. build project staging directory;
8. run full project invariants;
9. atomic publish;
10. rollback staging on any failure.

Existing local project must remain intact on restore failure.

## 11.8 OAuth

Keep narrow drive.file scope as the default architecture.

Current Google guidance describes drive.file as per-file access for files created/opened/shared with the app. Therefore legacy cross-app GBW backup discovery must not be assumed merely because GBW and GuitarLab share a Cloud project. Migration must be designed around files the unified app can legitimately access, explicit user selection/share, or local GBW export.

Never broaden to full Drive access merely to simplify migration.

## 11.9 Legacy H37 compatibility

H37 .guitarlab backup remains a legacy reader/migration source.

Unified backup migration:
- read committed H37 history;
- restore/import to staging;
- ingest into unified project/asset model;
- commit new unified backup;
- verify round trip;
- keep old H37 remote files until migration has proven successful and a later explicit cleanup action is authorized.

Do not mutate old H37 files into the new schema.

---

# 12. Local project migration

## 12.1 Existing GuitarLab projects

Migration must be backward compatible and lossless.

Existing project becomes:

Preparation = absent/not started  
Studio = exact existing behavior/state preserved

Preserve:
- projectId;
- tracks;
- clips;
- takes;
- active takes;
- markers/sections/loop;
- mix state;
- source media;
- waveform/proxy semantics;
- recording sync metadata;
- Undo/history policy as applicable;
- timestamps.

A migrated project may not suddenly create GBW assets.

## 12.2 Existing GBW Android projects

Android sandbox isolation means GuitarLab must not assume it can read private GBW files directly.

Supported migration channels should include at least one robust user-controlled path:
- import latest supported .gbwbackup archive;
- "Open/Share with GuitarLab" handoff from GBW if a final bridge build is warranted;
- explicit local project export/import where technically cleaner.

Importer:
- validates schema;
- validates projectId;
- validates source/stems/exports;
- verifies hashes/manifest;
- maps GBW metadata to UnifiedProject;
- never trusts paths from archive;
- blocks traversal/zip bombs/resource abuse;
- stages before publish;
- records migration provenance.

Do not delete GBW local data after import.

## 12.3 Project ID collision

If importing a GBW project whose projectId already exists locally:
- compare provenance/content/revision;
- exact same imported state may be a no-op;
- divergent state requires explicit merge/conflict/duplicate policy;
- never overwrite merely because IDs match.

## 12.4 Duplicate/Copy

User duplication must create a new projectId and preserve or copy asset references according to managed-media policy without creating unsafe shared mutable state.

---

# 13. Search/source acquisition migration

Port GBW source acquisition behind stable interfaces.

Required boundaries:
- source search provider;
- result normalization;
- scoring/ranking;
- source download;
- content validation;
- project source publication.

Tests must cover:
- provider returns empty;
- malformed metadata;
- wrong duration;
- preview/partial media rejection;
- duplicate result candidates;
- cancellation;
- process recreation;
- network loss;
- download success but validation failure;
- source publication after project switched;
- project renamed during operation.

The provider network implementation must never directly mutate project state.

---

# 14. Backing/reference renderer

Create a single deterministic preparation renderer.

Inputs:
- exact accepted stem asset IDs;
- recipe;
- shared-gain policy;
- output format policy.

Outputs:
- REFERENCE_BACKING;
- REFERENCE_GUITAR or direct accepted guitar stem binding according to domain decision.

Tests:
- channel/frame alignment;
- output duration;
- no independent normalization that breaks recombination relationship;
- clipping/headroom policy;
- repeat render determinism within codec/float tolerance;
- missing stem fail-closed;
- mismatched sample rate/channel fail-closed or explicit validated conversion;
- cancellation cleanup;
- no mutation of stem assets.

---

# 15. Audio invariants across Prepare and Studio

The integration may not regress GuitarLab audio safety.

Required:
- Prepare playback/preview cannot feed the recording writer;
- source/stem decoding uses existing validated codec abstractions where possible;
- Studio recording remains dedicated AudioRecord input;
- backing/reference playback remains isolated from recorded content;
- selected recording input stays fail-closed;
- sample-rate conversions are explicit and provenance-bearing;
- prepared references preserve timeline-zero alignment;
- stereo channel identity is preserved;
- no implicit speed/pitch change;
- final export and playback preserve existing mixer semantics.

Golden audio tests must cover at least:
- impulse/transient timing;
- mono/stereo;
- different L/R content;
- silence;
- full-scale/near-clipping signal;
- 44.1 and 48 kHz primary paths;
- any additional rates still claimed by GuitarLab;
- duration/frame-count invariants;
- backing recipe recombination.

---

# 16. Jobs, concurrency and lifecycle

## 16.1 Operation ownership

Every long operation uses an immutable operationId and records:
- projectId;
- operation type;
- input revision/source asset;
- creation time;
- durable phase;
- progress;
- retry count;
- terminal state;
- output publication identity.

## 16.2 Concurrency policy

Define explicit locks/scopes.

At minimum:
- one destructive project migration per project;
- one separation job per project source generation;
- backup can snapshot while Studio is usable, but snapshot state is frozen;
- restore/install cannot race project mutation;
- deletion cannot race active backup/migration;
- export operates on frozen project revision.

Do not use one global mutex for everything unless profiling proves it acceptable; use semantic operation scopes.

## 16.3 Process death/reboot

Test:
- source download;
- remote separation polling;
- stem download/import;
- backup upload;
- restore download;
- migration;
- export;
- recording recovery.

Each operation must either:
- safely resume;
- reconcile;
- or fail into an actionable recoverable state.

It may not invent success.

## 16.4 Cancellation

Cancellation must be phase-aware.

After cancellation:
- no partial project publication;
- validated already-managed assets are not deleted accidentally;
- staging/temp files are cleaned when provably disposable;
- remote job cancellation semantics are explicit;
- terminal state is idempotent.

---

# 17. Security and privacy

Mandatory gates:

- no client secret in APK;
- no service account key in APK/repository;
- no access/refresh/App Check/Firebase auth token in logs;
- no resumable session URL in exported diagnostics;
- no Authorization headers in logs;
- drive.file only unless a future recorded decision changes it;
- Firebase rules isolate user/project data;
- Storage temporary object paths are unguessable/authorized;
- backend validates user/job ownership;
- Cloud Run dispatch cannot be abused to run arbitrary command/model;
- job request fields are allowlisted/validated;
- source filenames/artist/title are treated as untrusted strings;
- ZIP/project import blocks path traversal and resource bombs;
- diagnostic export contains no audio unless the user explicitly chooses a separate export feature.

Static CI scans should fail on known credential patterns and broad Drive scopes.

---

# 18. Cost/quota robustness

## 18.1 Separation

Preserve a server-enforced usage policy.

Client display is informative only; backend remains authoritative.

Test:
- limit reached;
- stale policy revision;
- month rollover;
- duplicate dispatch attempt;
- job accepted but client loses response;
- cancellation/expiry;
- same user on multiple devices if supported.

No client retry may accidentally consume multiple cloud jobs for one logical operation.

## 18.2 Drive

Use local safety budgets only as a protective UX mechanism, not as the source of Google billing truth.

Backup engine should minimize:
- repeated uploads;
- repeated full-project downloads;
- list polling;
- unnecessary metadata writes.

Large asset upload uses resumable protocol.

---

# 19. Observability and diagnostics

One sanitized support report should be able to explain, without audio content:

- app version/build/source SHA;
- projectId truncated/full only in advanced details;
- project schema;
- unified revision;
- asset inventory and hashes truncated as needed;
- managed-media integrity summary;
- current job states;
- separation job id/state/policy revision;
- backend timing fields;
- Drive connection state;
- desired/confirmed revision;
- pending object counts/bytes;
- last retry classification;
- recording route/session health;
- storage free space;
- last migration result;
- no secrets.

Logs should use bounded ring/history or lifecycle-appropriate retention, not unbounded growth.

---

# 20. Digital test strategy

No milestone is complete with "build succeeds" only.

## 20.1 Unit tests

Cover:
- project identity;
- asset identity;
- canonical revision digest;
- provenance;
- reference bindings;
- backing recipe;
- job state machines;
- conflict resolver;
- migration mapping;
- Drive manifest/object metadata;
- retry classification;
- cleanup reachability;
- storage budget policy;
- cloud quota/policy parsing;
- source ranking/normalization.

## 20.2 Property-based / randomized invariant tests

Generate project graphs with randomized:
- tracks;
- clips;
- takes;
- prepared assets;
- revisions;
- bindings;
- deletes/rebinds;
- migrations.

Assert:
- no dangling refs;
- projectId stability;
- active-take invariants;
- asset reachability;
- deterministic canonical serialization;
- no NaN/Infinity/negative frames;
- round-trip equality within documented canonical rules.

Use deterministic seeds and retain failing seeds.

## 20.3 Fuzz/malformed input

Corpus:
- corrupt .guitarlab;
- corrupt .gbwbackup;
- truncated ZIP;
- path traversal;
- duplicate entry;
- zip bomb/resource-limit case;
- invalid JSON;
- extreme strings;
- zero-byte media;
- truncated WAV;
- false codec headers;
- malformed remote job JSON;
- malformed Drive metadata;
- incomplete manifest;
- hash mismatch.

Expected outcome: explicit failure, cleanup of staging, no crash, no partial project mutation.

## 20.4 HTTP/network fault injection

Use injectable transport/fake server.

Drive:
- disconnect mid-chunk;
- 401;
- 408;
- 429;
- 5xx;
- lost final response;
- expired resumable session;
- wrong offset;
- checksum mismatch;
- duplicate/reordered response;
- slow stream;
- cancellation.

Separation backend:
- upload interruption;
- job-create response lost;
- queue delay;
- status repeat;
- out-of-order status;
- COMPLETED but missing stem;
- stem download truncation;
- ACK response lost;
- purge delayed;
- auth expired;
- policy mismatch.

## 20.5 Persistence/process-death tests

Kill/recreate around every transaction boundary:
- before staging;
- after staging;
- before validation;
- after validation;
- before publish;
- after publish but before local status update;
- after remote commit but before local confirmation.

Repeated recovery must converge to one valid state.

## 20.6 Android instrumentation

Cover:
- Home/new project flows;
- Prepare navigation;
- separation progress/errors;
- Studio handoff;
- reference update prompt;
- Export split;
- Settings account/cloud separation;
- Activity center;
- rotation/recreation;
- back navigation;
- immersive mode;
- dialogs;
- accessibility tags;
- responsive geometry.

Device profiles:
- target tablet geometry;
- representative phone;
- portrait/landscape where product supports both;
- font scale stress;
- density variation.

## 20.7 Accessibility

Automated plus inspection:
- TalkBack labels;
- semantic role/state;
- focus order;
- touch target >= product standard;
- destructive confirmation;
- progress announcements not spammy;
- color not sole status channel;
- large font no clipping;
- keyboard/accessibility action for important gestures.

## 20.8 Audio golden tests

Use generated deterministic signals.

Validate:
- decode;
- frame counts;
- sample-rate conversion where claimed;
- stereo channel separation;
- backing sum/shared gain;
- silence;
- transient alignment;
- output peak/RMS;
- offline vs realtime behavior where contract requires parity;
- no backing leakage path into recording writer in software.

## 20.9 Stress

Digital stress matrix should include:

Project durations:
- 1 min;
- 3 min;
- 5 min;
- 10 min;
- a longer synthetic boundary only for algorithmic overflow/storage testing, not as a product quality promise.

Content:
- source + six stems;
- multiple recordings/takes;
- 24-track/100+ clip existing digital stress where compatible with current GuitarLab regression assets;
- repeated prepared-reference versions;
- repeated metadata edits;
- many backup revisions.

Operations:
- 50+ reopen/save cycles;
- 100+ deterministic edit/Undo/Redo operations;
- repeated backup no-op;
- rapid edits while backup dirty;
- backup during normal project use;
- restore/cancel loops;
- low-storage simulation;
- slow I/O;
- background/foreground cycles.

Measure:
- memory;
- leaked jobs/coroutines;
- temp file growth;
- asset orphan count;
- waveform/cache growth;
- backup bytes uploaded;
- duplicate cloud jobs;
- elapsed operation metrics.

## 20.10 Remote integration smoke

Before final release candidate, run a controlled real cloud digital smoke using a small deterministic test asset:
- auth;
- one remote Demucs job;
- six outputs;
- download/validation;
- ACK/purge;
- no orphan state.

This is a digital integration gate and should not require the user to listen manually.

Avoid consuming cloud quota on every ordinary commit.

---

# 21. Performance budgets

Record baseline before porting and prevent gross regression.

At minimum track:
- cold start;
- project library load;
- project open;
- Studio first render;
- source import;
- waveform generation;
- memory after opening prepared project;
- memory during separation import;
- local storage amplification;
- backup upload bytes for metadata-only edit;
- backup upload bytes for one new take.

Regression thresholds must be declared from measured baseline rather than arbitrary numbers. A significant regression requires explanation or correction before milestone close.

---

# 22. Release engineering and provenance

## 22.1 Branch authority

GuitarLab main remains canonical unless the live repository policy changes.

Before every write:
- re-fetch HEAD;
- verify expected baseline;
- rebase/re-read if advanced;
- never overwrite concurrent work from stale file SHAs.

## 22.2 CI

Current GuitarLab canonical CI remains manual-only.

Work may autonomously:
- run available local JVM tests;
- run Gradle tasks when toolchain exists;
- run lint;
- build debug/release when signing material is securely available;
- run emulator/instrumentation if environment supports it;
- run deterministic static/security/materialization checks.

Work must not claim DIGITAL PASS if the canonical required gate has not executed.

A hosted CI dispatch requires explicit owner authorization under current policy.

## 22.3 Materialization

If the repository still uses .source-parts when integration implementation begins:
- do not bypass it casually;
- either extend it deterministically with hash-verified U blocks;
- or execute a dedicated, reviewed migration away from materialized source.

Do not maintain two contradictory build sources.

## 22.4 Candidate identity

Every release candidate records:
- versionName;
- versionCode;
- exact producer SHA;
- package;
- signer certificate SHA-256;
- APK SHA-256;
- test counts;
- lint result;
- API/emulator result;
- cloud integration result;
- migration result;
- known physical residual.

---

# 23. AI / ChatGPT Work autonomous execution protocol

Every Work session must perform this protocol before implementation.

## 23.1 Session bootstrap

1. Read docs/CURRENT_STATE.md.
2. Read this roadmap completely.
3. Read docs/DOCUMENTATION_MAP.md.
4. Read docs/DECISIONS.md.
5. Read relevant subsystem contracts.
6. Fetch current GuitarLab main HEAD.
7. If GBW source is still needed, fetch current GBW dev/android-6.0 HEAD and CURRENT_STATE.
8. Inspect working tree/repository state.
9. Identify the first incomplete U milestone.
10. Verify its entry gate from repository evidence; do not rely on prior chat claims.

## 23.2 Idempotency rule

Before changing a milestone:
- inspect whether equivalent code/tests/docs already exist;
- if complete and evidence is valid, do not reimplement;
- run the milestone's validation suite;
- update status/evidence only.

A rerun of the same Work prompt on the same repository state should converge to no-op except for genuinely new evidence.

## 23.3 Implementation rule

Work should proceed autonomously through all digitally possible substeps.

Do not stop merely to ask how to name an internal class, file or test when this roadmap defines the behavior.

Ask the owner only when:
- an irreversible product decision is missing;
- external authorization/credential/account action is unavoidable;
- a destructive cloud action needs consent;
- a physical observation is required;
- current repository evidence contradicts this roadmap in a way that cannot be safely reconciled.

## 23.4 Gate rule

For each milestone:
- implement;
- test targeted behavior;
- run regression;
- run static/lint/build as available;
- perform fault injection relevant to changed state machines;
- update docs;
- record exact evidence;
- only then mark milestone PASS.

Do not advance through a red gate.

## 23.5 Failure handling

On failure:
1. capture the exact failing command/test;
2. determine whether defect is source, test, infrastructure or unavailable external dependency;
3. fix source/test defects;
4. rerun the smallest proving test;
5. rerun milestone regression;
6. record any external blocker explicitly.

Do not delete a failing test to make the gate green unless the test is proven invalid and replaced with equivalent or stronger evidence.

## 23.6 Documentation update discipline

At milestone close update at minimum:
- this roadmap status/checklist;
- CURRENT_STATE;
- IMPLEMENTATION_ROADMAP when global sequence changes;
- ARCHITECTURE if runtime architecture changed;
- DECISIONS for durable decisions;
- TEST_AND_HOMOLOGATION_PLAN for evidence boundary;
- release notes/candidate identity when appropriate.

## 23.7 Commit discipline

Prefer coherent milestone/sub-milestone commits.

Docs-only/planning changes follow current [skip ci] policy.

Do not trigger GitHub Actions automatically under the current policy.

---

# 24. Serial implementation milestones

---

## U0 — Baseline freeze, inventory and integration contract

### Objective
Create a reproducible integration baseline before moving code.

### Entry
- GuitarLab live documentation readable;
- GBW live documentation/source accessible.

### Work
- re-audit both repository HEADs;
- identify exact latest digitally valid product baselines;
- inventory GBW Android files by capability: source, project, separation client, local separation, export, Firebase, cloud;
- inventory GuitarLab project/media/backup/Studio seams;
- document mapping GBW → target GuitarLab module;
- identify duplicated concepts and select one target implementation;
- freeze golden project/audio fixtures from both products;
- record current schemas and migration examples;
- confirm GuitarLab H37b or successor Drive source has passed its required digital compile/build gate before its code becomes the base for unified backup work;
- do not require a separate physical acceptance here if the objective is one final physical campaign; carry known physical residual forward explicitly.

### Tests/evidence
- both baseline projects build/test to their currently documented digital standard where tooling permits;
- fixture hashes recorded;
- no user data modified.

### Exit
A machine-readable/human-readable integration inventory exists and every imported subsystem has a target owner/module.

---

## U1 — Unified project and asset domain

### Objective
Introduce UnifiedProject/Asset/Provenance contracts without changing end-user behavior.

### Work
- evolve schema backward-compatibly;
- asset IDs/hashes/roles;
- preparation state optional;
- reference bindings;
- provenance;
- canonical revision serialization;
- reachability service;
- migration reader for current GuitarLab schema.

### Required tests
- legacy GuitarLab project round trip;
- current project round trip;
- canonical revision determinism;
- rename identity;
- duplicate new projectId;
- randomized graph invariants;
- malformed schema;
- save/reopen;
- portable .guitarlab compatibility.

### Exit
Existing GuitarLab behavior is bit/semantically equivalent where expected and no UI requires Prepare yet.

Rollback: schema reader remains backward compatible; writer change is not promoted if old project corpus fails.

---

## U2 — Unified project shell / Home / navigation

### Objective
Expose the new information architecture with feature flags or safe empty states.

### Work
- unified Home card model;
- + New Project: Search / Import / Blank;
- Prepare / Studio / Export project navigation;
- Activity entry;
- status badges from domain state;
- blank/Guitar template preservation.

### Tests
- navigation/recreation;
- current Home search/filter/sort regression;
- empty preparation;
- existing project opens directly/appropriately without data loss;
- tablet/phone/font-scale/accessibility geometry.

### Exit
Old projects are fully usable even though Prepare implementation may still be placeholder/disabled.

---

## U3 — Source acquisition port

### Objective
Bring GBW source workflow into feature:prepare.

### Work
- local import;
- remote search/providers accepted by baseline;
- ranking/selection;
- download;
- validation;
- managed SOURCE_ORIGINAL publication;
- progress/cancel/retry;
- Activity Center integration.

### Tests
All source fault cases in sections 13 and 20.

### Exit
A GuitarLab project can acquire a validated managed source without invoking separation.

No source provider may write directly into Studio state.

---

## U4 — Separation client + local publication pipeline

### Objective
Connect GuitarLab to the proven GBW remote separation contract.

### Work
- GuitarLab Firebase app configuration;
- auth/session integration;
- backend client port;
- durable job store;
- remote status reconciliation;
- six-stem staging/validation;
- publication as managed assets;
- ACK/purge;
- notifications;
- sanitized diagnostics;
- local fallback port if retained.

### Tests
- fake backend exhaustive state/failure matrix;
- Firebase emulator/security tests where supported;
- deterministic state monotonicity;
- process death;
- cancellation;
- duplicate/lost response;
- wrong project/job;
- malformed stems.

### Digital external gate
One controlled real-cloud test may be run when owner/environment authorization exists; otherwise milestone remains integration-pre-gate.

### Exit
Six validated stems exist in the unified project with provenance and no GBW app handoff.

---

## U5 — Prepared backing/reference + zero-copy Studio integration

### Objective
Make Prepare output directly useful in Studio.

### Work
- deterministic backing recipe renderer;
- reference-guitar binding;
- default template bindings;
- optional derived guitar L/R using existing stereo policy;
- explicit new-reference available flow;
- update/keep old binding;
- Asset surface.

### Tests
- golden audio;
- alignment;
- shared gain;
- Studio open;
- rebind Undo/Redo;
- recordings/takes remain unchanged after preparation update;
- missing/corrupt stem;
- save/reopen;
- export after rebind.

### Exit
End-to-end local flow Source → Stems → Backing → Studio is digitally proven.

---

## U6 — Unified exports

### Objective
Separate Prepare study exports from Studio masters cleanly.

### Work
- Study Exports section;
- Studio Master section;
- preserve codec capability gates;
- provenance in outputs;
- staging/publication/cancellation;
- avoid duplicate old GBW export UI.

### Tests
- WAV/FLAC and MP3 only where device capability claims permit;
- duration/channel/rate;
- cancellation/rollback;
- low storage;
- filename/path sanitization;
- export after project rename;
- no project mutation from export failure.

### Exit
No user needs the standalone GBW app for backing/guitar exports.

---

## U7 — Cloud backend source consolidation

### Objective
Make the unified product reproducible from GuitarLab without destabilizing proven production separation.

### Work
- port cloud source/tests into GuitarLab;
- reproduce backend rules/policy;
- update deploy docs;
- stage/shadow deploy;
- compare behavior;
- update WIF/OIDC/repo conditions only at cutover;
- preserve last-known-good image digest and rollback.

### Tests
- backend unit/integration/security;
- container build;
- model/checkpoint verification;
- job contract;
- quota;
- isolation;
- temporary Storage purge;
- shadow integration.

### Exit
Production separation can be maintained/deployed from GuitarLab repository with proven rollback.

Do not delete old GBW cloud evidence yet.

---

## U8 — Unified Drive backup vNext

### Objective
Replace standalone app backup concepts with one scalable transactional project backup.

### Work
- content-addressed asset store;
- immutable revision manifests;
- current descriptor/conflict model;
- resumable object upload;
- server-confirmed integrity;
- dirty desired/confirmed semantics;
- restore staging;
- GC reachability;
- diagnostic state;
- automatic WorkManager scheduling;
- migrate H37 reader into legacy adapter.

### Tests
- full section 11 and section 20 Drive/fault matrix;
- metadata-only edit uploads no media;
- one new take uploads only new asset + metadata;
- rapid edits coalesce;
- process death;
- conflict;
- storage full;
- corrupted remote asset;
- GC safety;
- multi-project reachability if cross-project dedupe enabled.

### Exit
One unified project backup/restore path is digitally green and does not require monolithic GB-scale reupload for ordinary metadata edits.

---

## U9 — Legacy migration

### Objective
Prove safe migration from both former products.

### Work
- GuitarLab local legacy migration;
- H37 Drive legacy migration;
- GBW .gbwbackup importer;
- optional GBW Share/Open bridge;
- conflict/collision handling;
- migration report;
- legacy data retention policy.

### Corpus
- pre-M5 GuitarLab;
- M5/M6/M7/H28/H37-era fixtures as available;
- partial metadata;
- current project;
- current GBW RC5 project backup fixtures;
- corrupt archives.

### Tests
- byte/content integrity;
- projectId rules;
- repeat import idempotency;
- duplicate/conflict;
- process death;
- no delete of legacy source;
- migrated project full backup/restore round trip.

### Exit
The user can move existing work into unified GuitarLab without destructive assumptions.

---

## U10 — Cross-product regression, stress, security and performance hardening

### Objective
Attack the complete integrated system digitally before release-candidate work.

### Work
Run the largest regression campaign of the project.

Required suites:
- all GuitarLab existing unit/instrumented/audio/persistence tests;
- all ported GBW source/separation/backend tests;
- unified domain property tests;
- malformed/fuzz;
- network faults;
- process death;
- storage pressure;
- lifecycle;
- accessibility;
- responsive geometry;
- security scans;
- cloud contract;
- Drive transactional suite;
- migration corpus;
- long-song stress;
- job concurrency;
- leak/temp/orphan audit;
- performance comparison.

### Exit
No repeatable P0/P1 digital defect; all accepted P2/P3 documented with rationale.

---

## U11 — Final digital release gate

### Objective
Produce the one exact signed candidate intended for the consolidated physical campaign.

### Required evidence
- deterministic source/materialization PASS;
- all unit/integration/property/fuzz suites PASS;
- Android Lint 0 errors;
- debug/release build PASS;
- API36 instrumented PASS;
- isolated target-tablet geometry PASS;
- signing/zipalign/package/version/certificate PASS;
- APK SHA-256 recorded;
- real-cloud digital separation smoke PASS;
- Drive backup/restore integration PASS where it does not require physical judgment;
- migration corpus PASS;
- no credential leakage;
- docs consistency audit PASS;
- exact remaining physical checklist generated from unproven claims only.

### Exit
Signed candidate frozen. No source change after freeze except through a new candidate.

---

## U12 — Consolidated physical homologation and cutover

### Objective
Validate only what digital systems cannot prove, ideally in one manual campaign.

Target hardware:
- Samsung SM-X230 / current target tablet;
- M-VAVE MK-300;
- actual intended USB hub/power topology if that topology is part of normal use;
- external controller only if enabled as a release claim.

### Manual campaign

**Installation/upgrade**
- install over supported prior GuitarLab build when applicable;
- verify user projects/migration;
- no forced uninstall/data loss.

**USB audio**
- intended MK-300 input/output selected and effective;
- loopback off;
- no backing printed into guitar recording;
- no silent tablet-mic fallback;
- monitoring behavior;
- disconnect/reconnect preserves valid capture and does not auto-resume REC.

**Timing/listening**
- repeated transient take alignment;
- global vs take-specific sync behavior;
- no repeatable systematic late/early placement;
- subjective latency acceptable for intended workflow;
- no audible wrong speed/pitch/corruption.

**Long session**
- one continuous 10-minute representative play/record session;
- live waveform/meters;
- no growing sync drift/dropout;
- stop/finalize;
- save/reopen;
- representative Studio master.

**Prepare**
- acquire one real source;
- remote Demucs job;
- observe progress/background/reopen;
- six stems downloaded;
- listen enough to detect obvious missing/corrupt/wrong-role output;
- backing/reference opens in Studio without manual import.

**Drive**
- connect intended account;
- backup unified prepared+Studio project;
- modify project;
- confirm incremental sync;
- restore into safe test context;
- validate playable project and recordings;
- disconnect/reconnect behavior.

**UX**
- tablet ergonomics;
- major navigation;
- readable states;
- no system-bar obstruction;
- no obvious touch/focus issue.

### Pass criteria
- no repeatable P0/P1;
- no data loss;
- no wrong input capture;
- no severe audible corruption;
- no blocking OAuth/Firebase/Drive flow defect;
- exact signed candidate approved by owner.

If a source fix is required after this campaign, produce a new exact candidate and repeat only the physical scenarios whose evidence was invalidated plus a short smoke of adjacent risk. Do not blindly repeat unrelated manual QA.

---

# 25. Decommission strategy

Standalone GBW Android is deprecated only after U12 PASS.

After pass:
- freeze GBW Android branch with final migration instructions;
- keep Linux baseline untouched;
- keep old cloud image/source evidence for defined rollback window;
- remove duplicate active Android development instructions from GBW docs or clearly mark frozen;
- do not delete user Drive backup folders automatically;
- do not revoke cloud services until unified production path is confirmed.

Rollback window should preserve:
- last standalone GBW APK/build identity;
- last standalone GuitarLab pre-unification APK/build identity;
- last-known-good Demucs image digest;
- legacy importers/readers.

---

# 26. Documentation hierarchy during the program

Read in this order:

1. docs/CURRENT_STATE.md — live operational status.
2. docs/UNIFIED_GUITARLAB_GBW_IMPLEMENTATION_ROADMAP.md — this integration program.
3. docs/DOCUMENTATION_MAP.md — authority map.
4. docs/DECISIONS.md — durable decisions.
5. docs/ARCHITECTURE.md — currently implemented architecture.
6. docs/TEST_AND_HOMOLOGATION_PLAN.md — current evidence boundary.
7. subsystem contracts relevant to the current U milestone.
8. GBW docs/CURRENT_STATE.md and source only while a GBW subsystem is still being ported.

When current implementation differs from target design, ARCHITECTURE describes what exists and this roadmap describes where it is going. Do not rewrite ARCHITECTURE as if future work already exists.

---

# 27. Milestone status table

| Milestone | Status at roadmap creation | Owner/manual dependency |
|---|---|---|
| U0 Baseline/inventory | PASS — `docs/U0_BASELINE_INVENTORY.md` | no physical |
| U1 Unified domain | SOURCE PRE-GATE — implementation complete, software gate pending | manual CI authorization because local pinned toolchain is unavailable |
| U2 Unified shell UX | NOT STARTED | none |
| U3 Source acquisition | NOT STARTED | provider/network credentials only if required |
| U4 Separation integration | NOT STARTED | cloud auth/config may require owner account action |
| U5 Prepare → Studio | NOT STARTED | none |
| U6 Unified exports | NOT STARTED | none |
| U7 Cloud source consolidation | NOT STARTED | deploy authorization may require owner |
| U8 Unified Drive backup | NOT STARTED | OAuth setup may require owner |
| U9 Legacy migration | NOT STARTED | legacy fixtures/user export if not in repo |
| U10 Global hardening | NOT STARTED | none beyond external service smoke |
| U11 Final digital gate | NOT STARTED | canonical CI dispatch under current policy |
| U12 Final physical homologation | NOT STARTED | owner/manual target hardware |

A Work session updates this table only after objective evidence.

### U0 closure evidence — 2026-09-21

- live heads frozen at GuitarLab `dc3cb95093110270c494804aa03ef625dc3ceef2` and GBW `4724b030eabe289a5c2645e379181c0f9602d25d`;
- H37b producer `01b2371310fb872eb1583231728941beb93c1a8e` has CI #665 unit/Lint/build/API36 PASS evidence; its signing-provenance failure occurred before signing and is corrected at HEAD by H37c without promoting a signed candidate;
- human and machine inventories: `docs/U0_BASELINE_INVENTORY.md` and `integration/u0/inventory.json`;
- synthetic schema fixtures and the existing deterministic WAV fixture are hash-locked by `integration/u0/fixtures/manifest.json`;
- `scripts/verify_u0_inventory.py` PASS: 13 mapped capabilities and 3 hash-verified fixtures;
- H37b materialization first run and repeat/idempotent run PASS at the frozen HEAD;
- full local Gradle/Android reproduction was unavailable in the Work environment because Gradle 9.6.1 and Android SDK 36 were absent; no hosted CI was dispatched and no new DIGITAL PASS is claimed.

### U1 source checkpoint — 2026-09-21

- schema 2 adds explicit immutable managed assets, asset roles/classification/lifecycle, deterministic provenance, optional preparation state, reference bindings and migration provenance while schema-1 projects upgrade with empty preparation/assets;
- canonical project serialization and SHA-256 revision identity normalize asset/map ordering while retaining durable creative state;
- one reachability service protects authoritative assets, active preparation/reference roots and their provenance parents;
- project validation rejects duplicate/dangling/unsafe/staging asset state;
- portable package and duplicate-project media enumeration now includes unified asset paths;
- unit coverage adds legacy migration, revision/rename identity, reachability, malformed assets, portable round trip and 100 deterministic randomized inventory-order seeds;
- `.source-parts/U1UnifiedProjectDomain.patch` is SHA-256 locked; clean application, repeat materialization and deliberate corrupted-patch rejection PASS;
- compilation/JVM regression remains pending because the Work environment has no Gradle 9.6.1/Android SDK 36. Under the manual-only policy, U1 remains SOURCE PRE-GATE and U2 must not start across this red/unexecuted gate.

---

# 28. Program-level Definition of Done

The integration is complete only when all are true:

1. GuitarLab is the single Android product.
2. Existing GuitarLab projects remain usable.
3. Supported GBW projects can be migrated safely.
4. One projectId spans Prepare, Studio, Export and backup.
5. Source acquisition works from the unified app.
6. Remote Demucs produces six validated managed stems.
7. Prepared backing/reference opens in Studio without file handoff.
8. Re-preparation never silently destroys Studio reference history.
9. Studio recording/takes/timeline behavior remains regression-green.
10. Study export and Studio master are distinct and functional.
11. Firebase/Cloud Run temporary processing is isolated from Drive durable backup.
12. Unified Drive backup is transactional, resumable, incremental/deduplicated and conflict-aware.
13. Restore is staging-first, hash-validated and rollback-safe.
14. Legacy backup/project data is never deleted before verified migration.
15. No broad Drive scope or embedded backend credential is introduced.
16. Process death/network loss/cancellation converge safely.
17. Fuzz/malformed data cannot crash or partially publish a project.
18. Stress tests show bounded memory/temp/orphan behavior.
19. Exact-source digital gate is green.
20. Final signed candidate passes the residual physical campaign.
21. GBW Android is frozen/deprecated only after successful cutover.
22. Repository documentation is internally consistent and sufficient to resume the project without chat history.

---

# 29. Change-control rule

This roadmap may evolve when implementation evidence reveals a better technical mechanism, but changes must preserve the mission and invariants.

Any change to:
- project identity;
- asset immutability;
- backup transaction model;
- Drive scope;
- cloud data retention;
- destructive migration policy;
- package/signing identity;
- physical homologation boundary

requires:
1. explicit decision entry in DECISIONS.md;
2. roadmap update;
3. affected test-plan update;
4. migration/backward-compatibility analysis.

No implementation shortcut may silently weaken those contracts.

---

# 30. External references relevant to implementation

Drive authorization and per-file scope:
- https://developers.google.com/workspace/drive/api/guides/api-specific-auth
- https://developers.google.com/identity/protocols/oauth2/scopes

Drive resumable upload and limits:
- https://developers.google.com/workspace/drive/api/guides/manage-uploads
- https://developers.google.com/workspace/drive/api/guides/limits

Drive custom/app properties:
- https://developers.google.com/workspace/drive/api/guides/properties

These external references are implementation references only. Repository contracts remain the product source of truth, and external API behavior must be re-verified at implementation time because provider APIs can change.
