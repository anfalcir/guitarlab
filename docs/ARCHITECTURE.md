# GuitarLab Architecture

Updated: 2026-09-21

## Repository and release boundary
- `main` is canonical.
- Product behavior is promoted only after exact-source automated gates and, where required, residual physical validation.
- `.github/workflows/android-ci.yml` supports manual dispatch plus explicit `[run ci]` / `[run ci signed]` commit-message gates on `main`; ordinary `[skip ci]` commits remain inert.
- U11 signed authority is Android CI #783 / run `35793456972` at exact producer `4218e4343746932a4de61c5abaa29ba5769a30ed`, package `studio.guitarlab.app`, `0.5.0-rc5` / 25. U10/C8 technical authority remains preserved as #781 evidence.

## Module boundaries
- `core:model`: immutable project/track/clip/take metadata contracts.
- `core:project`: deterministic editors/history, repository, managed media, portable package handling and practice/recording/take policies.
- `core:codec`: WAV/format/waveform primitives and encoding-timeline rules.
- `core:audio`: pure audio/timing/calibration policies.
- `platform:codec-android`: Android compressed-format decode/encode adapters.
- `platform:audio-android`: playback/capture engines, routing, timing evidence and offline master renderer.
- `app`: Compose presentation, ViewModels, Android orchestration, direct Google Drive API v3 backup transport, legacy SAF migration and instrumentation surface.

## Managed media and portable projects
Imported external media is copied into project-controlled immutable source storage before becoming authoritative. Optional edit proxies, waveforms and renders are derivatives and may be regenerated. Non-destructive edits remain metadata operations.

The unified project schema uses explicit `ManagedAsset`, role, content hash, media facts, authoritative/derived classification, lifecycle and provenance. Preparation and Studio reference bindings are optional, so Studio-only projects remain valid. `UnifiedProjectRevision` provides deterministic canonical state/revision hashing and `ProjectAssetReachability` is the single cleanup authority for the graph. This domain is digitally closed in the unified line; historical U1/U1a compile-corrective context remains evidence only.

`.guitarlab` is a versioned portable package containing project metadata and referenced managed media. Import validates staging, traversal/resource bounds, manifest/media consistency and only publishes after successful validation. Duplication follows equivalent transactional invariants.

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

H23b requires progressing monotonic timestamp evidence, rejects stale/backwards clocks, never mixes incompatible evidence bases and protects placement arithmetic from overflow. H34/H35/H35a add the ±500 ms global future-recording guardrail, persistent take-specific synchronization, PCM-zero silent route/clock verification, exact live-route confirmation before physical calibration stimulus, and the explicit RECORD_AUDIO permission guard.

## Live recording waveform
Live waveform uses captured frame coverage as its timebase. Bounded envelope compaction preserves represented duration and transient peaks. UI publication is bounded/conflated; finalized file-derived waveform replaces transient state without changing clip placement.

## H24 Home Project Library architecture
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
`scripts/build_local.sh` is the local software gate when its environment is available. The GitHub workflow is the canonical full software/API36/geometry/signing executor.

Large deltas are materialized from `.source-parts` serially. The current canonical tail ends at U10zb and composes the accepted H-series, Drive v3, unified-domain/shell/cloud/backup and U10 cohesion-hardening blocks. Each protected block verifies patch/archive identity and exact terminal Git blobs; the current tail additionally preserves `git diff --check`, semantic guards and reverse-apply/idempotence checks. Unexplained drift blocks the build.

## Backup transport boundary — direct Drive v3 production path
The protected backup domain remains transport-agnostic: project/revision identity, deduplication, retention and restore semantics are separated from transport. H37 introduced the direct Drive API v3 edge; U8 production cutover and U8m provider-real acceptance made that path the current unified backup authority.

Primary path: Android + Google Identity Services OAuth `drive.file` → Drive v3 resumable upload/download. The Drive store uses private `appProperties` plus Drive `fileId`, byte size and SHA-256 for remote commit identity. Incomplete uploads are not catalogued as committed revisions. Persisted resumable session state is app-private and excluded from Android cloud/device backup. SAF remains only as a bounded one-time legacy migration source until all retained H26-H28 history has copied successfully.

No Firebase/Cloud Run/Functions hop, service account, client secret or refresh-token custody is part of this backup architecture.

## Current milestone boundary

U10/C8 and U11 are CLOSED / DIGITAL PASS. The exact signed candidate is `0.5.0-rc5` / versionCode `25`, producer `4218e4343746932a4de61c5abaa29ba5769a30ed`. U12 is active and owns only the consolidated physical acceptance.

Drive provider-real acceptance is already closed by U8m and remains supporting evidence because U10 did not alter the production Drive transport/store contract. U12 is the remaining consolidated physical boundary for real MK-300 routing/capture/isolation, monitoring, timing/listening, USB reconnect, continuous 10-minute quality and target-device ergonomics. External-controller physical acceptance remains a separate 1.1 boundary unless explicitly promoted as a release claim.
