# GuitarLab Architecture

Updated: 2026-09-21

## Repository and release boundary
- `main` is canonical.
- Product behavior is promoted only after exact-source automated gates and, where required, residual physical validation.
- `.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`).
- CI #663 / run `35523442620` / producer `51d4098fa7b1b44a9fa315e939541020f594654d` is the current signed DIGITAL PASS through H36c.

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

The U1 source candidate evolves the project schema to version 2 with explicit `ManagedAsset`, role, content hash, media facts, authoritative/derived classification, lifecycle and provenance. Preparation and Studio reference bindings are optional, so migrated Studio-only projects remain valid. `UnifiedProjectRevision` provides deterministic canonical state/revision hashing and `ProjectAssetReachability` is the single cleanup authority for the new graph. U1a corrects Kotlin compile boundaries found by CI #667 without changing this architecture. The milestone remains SOURCE PRE-GATE until the corrected software gate executes.

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
Studio Share owns `Salvar e exportar`; editable `.guitarlab` persistence is distinct from WAV/FLAC/MP3 master delivery. Mixer state is persisted where applicable and playback/export must respect gain/pan/mute/solo/master behavior.

## Lifecycle and persistence
Durable creative state belongs in project persistence, not transient Composable state. Navigation/recreation and interrupted media operations are independently recoverable. Home library query state is presentation state and may be recreated without changing project data.

## Build/release architecture
`scripts/build_local.sh` is the local software gate when its environment is available. The GitHub workflow is the canonical full software/API36/geometry/signing executor.

Large deltas are materialized from `.source-parts` serially. The current source-candidate tail ends at U1b: accepted H28 → existing H29-H36c tail → H37/H37a/H37b Drive transport → U1 unified domain → U1a compile corrective → U1b test-fixture corrective. Each block verifies its patch/archive and exact terminal Git blobs. Unexplained drift blocks the build.

## Backup transport boundary — H37 source candidate
The protected H28 domain remains transport-agnostic: `ProjectBackupCoordinator` owns project/revision identity, deduplication, retention and restore semantics. H37 changes the Android remote-store edge from SAF to direct Drive API v3.

Primary path: Android + Google Identity Services OAuth `drive.file` → Drive v3 resumable upload/download. The Drive store uses private `appProperties` plus Drive `fileId`, byte size and SHA-256 for remote commit identity. Incomplete uploads are not catalogued as committed revisions. Persisted resumable session state is app-private and excluded from Android cloud/device backup. SAF remains only as a bounded one-time legacy migration source until all retained H26-H28 history has copied successfully.

No Firebase/Cloud Run/Functions hop, service account, client secret or refresh-token custody is part of this backup architecture.

## Current milestone boundary
M5 and M6 are closed. M7/M8 behavior through H36c is digitally approved at CI #663, and the H28 SAF backup corrective is physically accepted there. H37 is a SOURCE PRE-GATE backup-transport migration and requires its own exact-source digital gate plus real OAuth/Drive acceptance before becoming a release authority. Existing recording alignment, real USB disconnect/reconnect/capture preservation, the continuous 10-minute quality smoke and H35 target-device synchronization/calibration behavior remain physical residuals. H33 controller hardware acceptance remains a separate 1.1 gate.
