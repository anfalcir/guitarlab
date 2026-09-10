# GuitarLab architecture

Updated: 2026-09-10

## Repository/branch policy
- `main` is the stable signed baseline.
- `dev/parallel-m3-m5` is the active integration branch attached to draft PR #1.
- PR #1 remains draft through the M5 physical gate.

## Core boundaries
- `core:model`: immutable project/track/clip metadata contracts, including managed source/proxy references.
- `core:project`: deterministic editors/history, project repository, managed media, portable project bundle writer/reader, recording/session and timeline policies.
- `core:codec`: WAV metadata/decoder/waveform primitives, import-format policy and Float32 WAV writer.
- `platform:codec-android`: Android compressed-format decode-to-proxy and master encoder adapters.
- `platform:audio-android`: playback/capture engines, routing and offline master renderer.
- `app`: Compose presentation, ViewModels, SAF launchers and orchestration.

## Managed-media architecture
An imported document has two distinct identities:
1. **authoritative source** — byte-preserved project-managed native original, immutable after commit;
2. **editing representation** — optional managed PCM WAV proxy, derived/regenerable and never authoritative.

`AudioClip.managedSourcePath` refers to the source; `managedEditProxyPath` optionally refers to the editing representation. Playback/waveform/render paths resolve proxy first when present, otherwise source. Non-destructive edits stay in project metadata.

## Portable project architecture
The portable `.guitarlab` package is a versioned ZIP container with manifest, `project.json`, referenced sources and referenced proxies. The reader stages extraction into a temporary project directory, blocks traversal/out-of-root paths, applies size/entry bounds, validates manifest/project/media consistency, assigns a new project ID and only then publishes it into managed storage.

This makes `Salvar cópia do projeto` a true round-trip persistence path, not merely an export archive.

Project duplication follows the same managed-media invariant: every referenced source and editing proxy is copied into an independent destination project before its JSON is published. Missing media aborts and removes the incomplete destination; duplication never publishes dangling managed paths.

## Master-render architecture
Final audio export is intentionally separated from editing proxies. `StudioMasterRenderer` consumes the current timeline/project mix state and produces a floating-point master WAV. Format adapters then either keep that Float32 WAV or encode a delivery copy such as FLAC/MP3. Source and proxy files are never used as destructive export targets.

## Studio output UX
The Studio top bar owns a dedicated Share action before Home. It opens `Salvar e exportar`, with two semantic groups:
- editable project: `.guitarlab`;
- master final: WAV 32-bit float, FLAC, MP3 320 kbps.

Options remains responsible for routes, monitoring, Studio preferences, import information and diagnostics. Output actions are deliberately not duplicated there.

## Timeline interaction architecture
Drag state belongs to the workspace/timeline layer rather than individual LazyColumn cells. Track and clip flows share stable identity, workspace pointer coordinates, overlay rendering, destination calculation and edge-autoscroll behavior. The ghost follows the pointer continuously while drop targeting may snap to lanes. One completed drop performs one project mutation; cancellation performs none.

## Track settings architecture
`Configurar pista` is responsive. Name/function, palette and source metadata remain readable across wide/narrow layouts. Content clearing and structural track deletion are separate. Tracks with content use an edit-pencil affordance.

## M5/M6 boundary
M5 now owns the complete reliable capture + Studio consolidation + requested media I/O/persistence/export gate. M6 starts only after M5 PASS/CLOSED and begins with measured round-trip latency, synchronization, take compensation, jitter and loopback work.
