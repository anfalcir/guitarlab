# GuitarLab Architecture

Updated: 2026-09-16

## Repository and release boundary
- `main` is canonical.
- Product behavior is promoted only after exact-source automated gates and, where required, residual physical validation.
- `.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`).
- CI #639 is the last signed DIGITAL PASS through H23b; H24 is newer and PRE-GATE.

## Module boundaries
- `core:model`: immutable project/track/clip/take metadata contracts.
- `core:project`: deterministic editors/history, repository, managed media, portable package handling, practice/recording policies and the H24 project-library selection policy.
- `core:codec`: WAV/format/waveform primitives and encoding-timeline rules.
- `core:audio`: pure audio/timing/calibration policies.
- `platform:codec-android`: Android compressed-format decode/encode adapters.
- `platform:audio-android`: playback/capture engines, routing, timing evidence and offline master renderer.
- `app`: Compose presentation, ViewModels, Android orchestration, SAF and instrumentation surface.

## Managed media and portable projects
Imported external media is copied into project-controlled immutable source storage before becoming authoritative. Optional edit proxies, waveforms and renders are derivatives and may be regenerated. Non-destructive edits remain metadata operations.

`.guitarlab` is a versioned portable package containing project metadata and referenced managed media. Import validates staging, traversal/resource bounds, manifest/media consistency and only publishes after successful validation. Duplication follows equivalent transactional invariants.

## Timeline/editing
Track/clip drag, trim, split, duplicate and deletion are deterministic project transactions. Split take lineage must remain valid when segments move or are deleted. Failed/stale drag is a no-op and may not partially mutate project/history/media state.

## Recording and synchronization
Recording owns a dedicated `AudioRecord` input path and managed Float32 writer. Playback/backing and monitoring never feed the recording writer. Explicit input selection fails closed if Android cannot confirm the effective route.

Timing keeps distinct layers:
1. per-session capture/backing startup mapping;
2. accepted route+sample-rate calibration;
3. bounded residual fine adjustment;
4. punch/pre-roll creative region logic.

H23b requires progressing monotonic timestamp evidence, rejects stale/backwards clocks, never mixes incompatible evidence bases, protects placement arithmetic from overflow and scopes calibration/fine adjustment to exact input+output+sample-rate identity.

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

Large RC3 deltas are materialized from `.source-parts` serially. The current canonical tail ends at H24. Every patch must apply cleanly or match an already-materialized final hash state; any unexplained drift blocks the build.

## Current milestone boundary
M5 and M6 are closed. M7/M8 release hardening is digitally approved through H23b at CI #639. H24 is implemented/source-validated and awaits exact-source full CI. Final RC3 closure still requires residual target-device approval, chiefly H24 Home ergonomics and H23b recording synchronization/routing on SM-X230 + MK-300.
