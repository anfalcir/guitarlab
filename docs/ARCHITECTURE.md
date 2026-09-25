# GuitarLab Architecture

Updated: 2026-09-24

## Repository and release boundary
- `main` is canonical.
- Product behavior is promoted after exact-source affected-path gates and, where required, residual physical validation on the owner's environment.
- `.github/workflows/android-ci.yml` supports manual dispatch plus explicit `[run ci]` / `[run ci signed]` commit-message gates on `main`; ordinary `[skip ci]` commits remain inert.
- `CURRENT_STATE.md` alone records the active version/run boundary. Final physical acceptance occurs only on the exact signed APK intended for freeze.

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

## Lifecycle and persistence
Durable creative state belongs in project persistence, not transient Composable state. Navigation/recreation and interrupted media operations are independently recoverable. Home library query state is presentation state and may be recreated without changing project data.

## Build/release architecture
`scripts/build_local.sh` is the local software gate when its environment is available. GitHub workflows provide controlled Android/API36/signing and cloud qualification. Required jobs are selected proportionally under `TEST_AND_HOMOLOGATION_PLAN.md`.

Large deltas are materialized from `.source-parts` serially. The canonical entrypoint and current tail are identified in `CI_PIPELINE.md`. Protected blocks verify patch/archive identity and exact terminal Git blobs; the tail preserves `git diff --check`, semantic guards and reverse-apply/idempotence checks. Unexplained drift blocks the build.

## Backup transport boundary — direct Drive v3 production path
The protected backup domain remains transport-agnostic: project/revision identity, deduplication, retention and restore semantics are separated from transport. Direct Drive API v3 is the current backup transport.

Primary path: Android + Google Identity Services OAuth `drive.file` → Drive v3 resumable upload/download. The Drive store uses private `appProperties` plus Drive `fileId`, byte size and SHA-256 for remote commit identity. Incomplete uploads are not catalogued as committed revisions. Persisted resumable session state is app-private and excluded from Android cloud/device backup. Historical SAF/GBW/H37 migration is outside the supported release scope.

No Firebase/Cloud Run/Functions hop, service account, client secret or refresh-token custody is part of this backup architecture.

## Remote separation recovery

The local durable store is recovery intent, not proof that a cloud job exists. When reconciliation finds no remote job, replayable pre-dispatch states are idempotently re-enqueued, cancellation requests terminalize locally, and states requiring an already-created remote job expire safely. Cancellation/worker retry exhaustion must end in a terminal local state. Restoration of `SOURCE_READY` is source-generation-aware and may not overwrite newer active work. Per-project observation prefers active work over a late terminal completion from an older generation.

Current release work is defined only by `RC20_COMPLETION_PLAN.md`; historical milestones do not create architecture or homologation gates.
