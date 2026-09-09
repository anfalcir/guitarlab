# GuitarLab architecture

Updated: 2026-09-09

## Repository/branch policy
- `main` is the stable signed baseline.
- `dev/parallel-m3-m5` is the active integration branch attached to draft PR #1.
- PR #1 stays draft through the M5 physical gate; its old M4/alpha07 description is historical and must not be interpreted as current architecture status.

## Core boundaries
- `core:model`: immutable project/track/clip metadata contracts.
- `core:project`: deterministic project editors, history, managed media, recording/session and timeline policies.
- `core:codec`: WAV metadata/decoder/waveform primitives.
- `platform:audio-android`: playback/capture engines and Android routing.
- `app`: Compose presentation, ViewModels and orchestration.

## Managed-media invariant
Imported/recorded source media is promoted into project-managed immutable storage. Trim, split, duplicate, reorder and cross-track clip migration are metadata operations and must not rewrite source bytes.

## Timeline interaction architecture (alpha11)
Drag state belongs to the workspace/timeline layer, not individual LazyColumn cells. It carries stable item identity, origin lane/index, workspace pointer coordinates, measured size, computed target and edge-scroll behavior. Track and clip flows share this coordinator.

The dragged ghost is rendered in the same Compose tree as a high-z overlay above all lanes. `Popup` is prohibited for drag ghosts. Pointer motion and destination snapping are separate concepts: the ghost follows the pointer continuously while insertion/target indicators may snap to lanes.

Destination calculation uses current `LazyListState.layoutInfo` bounds. Edge autoscroll runs continuously in a coroutine/frame loop and retargets after scroll. Drop performs exactly one project mutation; cancellation performs none. Core editors are exact no-ops when dropping at the original destination, preventing unnecessary history entries.

## Track settings architecture
`Configurar pista` is responsive: balanced identity/color and source-metadata columns in landscape/wide layouts, stacked sections in narrow layouts, with internal scroll only when required. Structural delete is separate from content clearing and remains disabled for non-empty tracks.

## M5/M6 boundary
M5 owns reliable capture and consolidated Studio behavior. M6 begins only after M5 PASS/CLOSED and starts with measured round-trip latency, synchronization, take compensation, jitter and loopback work.
