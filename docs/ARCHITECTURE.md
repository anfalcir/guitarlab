# GuitarLab Architecture

Updated: 2026-10-07

## Repository and release boundary
- `main` is canonical.
- Product behavior is promoted after exact-source affected-path gates and, where required, residual physical validation on the owner's environment.
- `.github/workflows/android-ci.yml` supports manual dispatch plus explicit `[run ci]` / `[run ci signed]` commit-message gates on `main`; ordinary `[skip ci]` commits remain inert. Temporary maintenance-branch trigger exceptions must be removed before merge/promotion.
- `CURRENT_STATE.md` records present-tense operational state and `RELEASE_BASELINE.md` records the immutable accepted artifact/backend identity. Any future physical acceptance occurs only on the exact signed APK intended for promotion.

## Module boundaries
- `core:model`: immutable project/track/clip/take metadata contracts.
- `core:project`: deterministic editors/history, repository, managed media, portable package handling and practice/recording/take policies.
- `core:codec`: WAV/format/waveform primitives and encoding-timeline rules.
- `core:audio`: pure audio/timing/calibration policies.
- `platform:codec-android`: Android compressed-format decode/encode adapters.
- `platform:audio-android`: playback/capture engines, routing, timing evidence and offline master renderer.
- `app`: Compose presentation, ViewModels, Android orchestration, direct Google Drive API v3 backup transport, retained historical migration code where harmless, and instrumentation surface.

## Managed media and portable projects
Imported external media is copied into project-controlled immutable source storage before becoming authoritative. Optional edit proxies, waveforms and renders are derivatives and may be regenerated. Non-destructive edits remain metadata operations.

The unified project schema uses explicit `ManagedAsset`, role, content hash, media facts, authoritative/derived classification, lifecycle and provenance. Preparation and Studio reference bindings are optional, so Studio-only projects remain valid. `UnifiedProjectRevision` provides deterministic canonical state/revision hashing and `ProjectAssetReachability` is the single cleanup authority for the graph. This domain is digitally closed in the unified line; historical U1/U1a compile-corrective context remains evidence only.

`.guitarlab` is a versioned portable package containing project metadata and referenced managed media. Import validates staging, traversal/resource bounds, manifest/media consistency and only publishes after successful validation. Duplication follows equivalent transactional invariants.

Prepared References v2 keeps Demucs' six source stems ephemeral inside the Cloud Run execution. The committed remote result and Android import contain exactly two aligned float32 WAV deliverables: backing without guitar and isolated guitar. Those two final references become project-managed canonical WAV assets; intermediate stems are neither uploaded nor stored locally for new v2 jobs. `activeStemAssetIds` and legacy media presentation remain only for backward-compatible recovery of older projects. Exporting WAV may therefore publish a final canonical reference directly without conversion; this does not imply that six stems are stored on the device.

Before publication, Cloud Run verifies output format, duration consistency, finite samples, and the post-render peak of each deliverable and their recombination; sampled source-versus-six-stem reconstruction SNR is logged as a diagnostic because Demucs stems are not guaranteed to sum sample-perfectly back to the input. Android independently verifies hash/container, performs strict float decoding, enforces the same recombination ceiling and rewrites accepted audio into GuitarLab's canonical float32 WAV. Reference bindings are healthy only when both the binding and matching timeline clip exist; empty prepared lanes can therefore be repaired automatically or via explicit Studio synchronization without new cloud work.

## Timeline/editing
Track/clip drag, trim, split, duplicate and deletion are deterministic project transactions. Split take lineage must remain valid when segments move or are deleted. Failed/stale drag is a no-op and may not partially mutate project/history/media state.

Stereo channel separation is a project transaction, not only a media transform. The splitter's actual output sample rate/frame count becomes the derived mono editing bound. Existing take metadata is cloned into per-track lineage when unambiguous; a take shared by multiple temporal clips fails closed rather than being guessed. Derived/proxy outputs are discarded if project publication does not commit.

## Recording and synchronization
Recording owns a dedicated `AudioRecord` input path and managed Float32 writer. Playback/backing and monitoring never feed the recording writer. Explicit input selection fails closed if Android cannot confirm the effective route.

Timing keeps distinct layers:
1. per-session capture/backing startup mapping;
2. accepted route+sample-rate physical calibration;
3. global route/rate residual adjustment captured at REC start and applied only to that new take;
4. take-specific post-recording synchronization stored on the recorded take and applied by exact delta across its lineage;
5. punch/pre-roll creative region logic.

Timing evidence must progress monotonically, reject stale/backwards clocks, never mix incompatible evidence bases and protect placement arithmetic from overflow. Current behavior includes the ±500 ms global future-recording guardrail, persistent take-specific synchronization, PCM-zero silent route/clock verification, exact live-route confirmation before physical calibration stimulus and the explicit RECORD_AUDIO permission guard.

## Live recording waveform
Live waveform uses captured frame coverage as its timebase. Bounded envelope compaction preserves represented duration and transient peaks. UI publication is bounded/conflated; finalized file-derived waveform replaces transient state without changing clip placement.

## Home Project Library architecture
Home separates **repository state** from **library view state**.

Pipeline:
`repository.list() → immutable ProjectLibraryIndex → search → filters → deterministic sort → Compose presentation`.

Rules:
- repository I/O occurs on refresh/project mutation, never per keystroke;
- `ProjectLibraryIndex` caches normalized project names once per repository snapshot;
- name normalization uses Unicode decomposition, removes combining marks and lowercases with `Locale.ROOT`;
- search is therefore case- and accent-insensitive;
- filters are pure predicates over existing project metadata;
- sorting is deterministic and uses stable tie-breaks ending in project ID;
- default sort remains updated-descending, preserving prior Home behavior;
- search/filter/sort state lives in `HomeUiState` and survives repository refreshes;
- no project schema or `.guitarlab` migration is required;
- Compose owns only presentation/events; selection semantics live in `core:project` and are JVM-testable.

Filter dimensions:
- template: all / Guitar / Blank;
- content: all / with recordings / with audio/clips / no clips;
- sample rate: all / Auto / 44.1 / 48 / 88.2 / 96 kHz.

Sort dimensions:
- modified newest/oldest;
- name A–Z/Z–A;
- created newest/oldest.

## Home and Studio information architecture
Home is the project-library/navigation surface. Studio remains the creative editing/recording workspace. Both share one `StudioUserGuideDialog`, so user-visible workflow changes must update the same Help implementation in the same development block.

## Output and mixing
The canonical Export workspace owns external delivery; editable `.guitarlab` persistence is distinct from WAV/FLAC/MP3 master delivery. Home/Studio export entry points route to that single workspace. Mixer state is persisted where applicable and playback/export must respect gain/pan/mute/solo/master behavior.

Live Studio playback has two logical stereo buses:
`track render → per-track gain/pan/audibility → MAIN and/or CUE bus → Android output backend(s)`.

Track output routing is durable project metadata with `MAIN` as the compatibility default. Route mutations are stopped-state operations. CUE is optional and fail-closed: MAIN and CUE must resolve to explicit distinct physical endpoints, and preferred-device acceptance alone is never treated as proof of effective routing. Bluetooth remains excluded from synchronized CUE.

The Android backend supports two capability strategies:

- `COMMUNICATION_SPLIT`: when the requested CUE endpoint is exposed by `AudioManager.getAvailableCommunicationDevices()`, MAIN remains `USAGE_MEDIA + CONTENT_TYPE_MUSIC` and CUE uses `USAGE_VOICE_COMMUNICATION + CONTENT_TYPE_MUSIC`. This strategy is attempted before dual-MEDIA on eligible devices. The automatic path does **not** enter `MODE_IN_COMMUNICATION`.
- `MULTI_DEVICE`: conventional independent media sinks using explicit preferred devices. It remains a compatibility fallback for devices/topologies that genuinely support two independent media routes.

`CueRouteController` owns selection-time admission, cancellation/generation safety, process-session profile caching, topology invalidation, retry and diagnostic journaling. A successful preflight persists the exact strategy, physical rates and strategy parameters that runtime must reproduce.

Physical-route safety and synchronization quality are separate concerns. Both strategies require exact canonical MAIN/CUE route separation and fail closed on missing, converged, wrong or mirrored routes. Conventional `MULTI_DEVICE` retains the tight 12 ms initial presentation-origin bound. `COMMUNICATION_SPLIT` allows a bounded 60 ms initial static offset because MEDIA and COMMUNICATION can have different fixed pipelines; the measured offset is exported diagnostically. Runtime communication drift uses a 60 ms envelope and must remain outside it for 750 ms before CUE is suppressed. These wider communication-specific timing bounds never relax physical-route identity.

MAIN owns render-loop timing. CUE never blocks MAIN. CUE writes use `WRITE_NON_BLOCKING` through a bounded FIFO: partial or zero writes are normal transient backpressure and remain queued in order, while negative writes, sustained backlog beyond the bounded queue, route loss or sustained drift disable CUE. The queue is cleared on seek/route loss and is not a hidden delay mechanism for MAIN.

Runtime Communication Split does not repeat the complete selection-time silent clock preflight on the same freshly opened tracks. Instead, the negotiated profile recreates the proven strategy, playback performs fresh physical-route qualification before musical content proceeds, and route/drift guards remain active throughout the session. Runtime suppression exports the specific cause, pending CUE samples, presented-frame delta, drift limit and effective route keys.

CUE never requests automatic ducking, never rewrites MAIN gain and never manipulates Android system volume to manufacture a mix. Track/Master mixer gain remains the product authority; Android device/volume-group controls remain external multipliers.

Normal Play and backing playback during REC use the same dual-bus engine. Software monitoring follows the armed track's output route when enabled; losing a required CUE path disables CUE while capture remains input-only. CUE assignment is monitoring/playback metadata and never excludes a track from offline Master Export.

## Lifecycle and persistence
Durable creative state belongs in project persistence, not transient Composable state. Navigation/recreation and interrupted media operations are independently recoverable. Home library query state is presentation state and may be recreated without changing project data.

Compatibility recovery is deliberately separated from persisted-byte identity. Cryptographic/revision validation decodes the persisted canonical project state first; only after that digest is accepted may narrow compatibility repair (such as strict legacy recorded-take recovery) alter the in-memory representation. Compatibility repair must be deterministic and idempotent.

## Build/release architecture
`scripts/build_local.sh` is the local software gate when its environment is available. GitHub workflows provide controlled Android/API36/signing and cloud qualification. Required jobs are selected proportionally under `TEST_AND_HOMOLOGATION_POLICY.md`.

The canonical Android source-validation entrypoint is `scripts/materialize_ci_sources.sh`. For the current checked-in source line it executes semantic/source guards, including CUE capability and zero-duck invariants. Older staged materializers and `.source-parts` remain historical/reproducibility inputs only; `CI_PIPELINE.md` is the live execution authority. Unexplained source drift blocks qualification.

## Backup transport boundary — direct Drive v3 production path
The protected backup domain remains transport-agnostic: project/revision identity, deduplication, retention and restore semantics are separated from transport. Direct Drive API v3 is the current backup transport.

Primary path: Android + Google Identity Services OAuth `drive.file` → Drive v3 resumable upload/download. The Drive store uses private `appProperties` plus Drive `fileId`, byte size and SHA-256 for remote commit identity. Incomplete uploads are not catalogued as committed revisions. Persisted resumable session state is app-private and excluded from Android cloud/device backup. Historical SAF/GBW/H37 migration is outside the supported release scope.

No Firebase/Cloud Run/Functions hop, service account, client secret or refresh-token custody is part of this backup architecture.

### Backup catalog read model

Drive heads/manifests remain the source of truth. The Android catalog path takes one remote head snapshot and derives retained version history, per-project reconciliation and remote tips from that same snapshot. Immutable descriptors and manifest timestamps already validated in a prior successful catalog may be reused to avoid downloading unchanged project-state objects.

A bounded app-private metadata cache may accelerate screen entry, but it is not authority. It is scoped to Drive account + retention policy, bound to the current local project revision map, schema/integrity checked and discarded on mismatch/corruption. Every visible Backup-screen entry forces remote head verification; explicit user refresh also forces it.

Automatic backup is owned by WorkManager and the production Drive service, not by the Backup Composable/ViewModel lifetime. Completion causes an immediate catalog reread only when the Backup screen is visible. Off-screen completion leaves the durable Drive state untouched and avoids a redundant network read; the next Backup-screen entry performs the required forced remote verification.

## Remote separation recovery

The local durable store is recovery intent, not proof that a cloud job exists. When reconciliation finds no remote job, replayable pre-dispatch states are idempotently re-enqueued, cancellation requests terminalize locally, and states requiring an already-created remote job expire safely. Cancellation/worker retry exhaustion must end in a terminal local state. Restoration of `SOURCE_READY` is source-generation-aware and may not overwrite newer active work. Per-project observation prefers active work over a late terminal completion from an older generation.

The accepted release identity is defined only by `RELEASE_BASELINE.md`; the current successor candidate and residual physical gates are defined only by `CURRENT_STATE.md`. Completed candidate plans and milestone narratives remain historical evidence under `docs/history/`.
