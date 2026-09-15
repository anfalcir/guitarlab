# GuitarLab Architecture

Updated: 2026-09-14

## Repository/branch policy
- `main` is canonical after the explicit merge of PR #1.
- Final physical homologation evidence and closure are recorded directly on `main`.
- A new APK identity is not promoted merely because source was committed; compilation, validation and signer evidence remain required.

## Core boundaries
- `core:model`: immutable project/track/clip metadata contracts, including managed source/proxy references.
- `core:project`: deterministic editors/history, repository, managed media, portable bundle reader/writer, recording/session/timeline policies, trim/drag policies, live-waveform frame envelope, interruption recovery and media integrity audit.
- `core:codec`: WAV metadata/decoder/waveform primitives, import-format policy, Float32 WAV writer and pure encoding-timeline rules.
- `core:audio`: pure audio/timing policies, including recording timing compensation.
- `platform:codec-android`: Android compressed-format decode-to-proxy and master encoder adapters.
- `platform:audio-android`: playback/capture engines, routing, timing evidence and offline master renderer.
- `app`: Compose presentation, ViewModels, SAF launchers, orchestration and Android instrumentation surface.

## Managed-media architecture
An imported document has two identities:
1. **authoritative source** — byte-preserved project-managed native original, immutable after commit;
2. **editing representation** — optional managed PCM WAV proxy, derived/regenerable and never authoritative.

`AudioClip.managedSourcePath` refers to the source; `managedEditProxyPath` optionally refers to the editing representation. Playback/waveform/render resolve proxy first when present, otherwise source. Non-destructive edits stay in project metadata.

### Interruption and orphan policy
Sources are never auto-deleted by cleanup. Unreferenced sources and payload-bearing interrupted takes are reported/retained rather than guessed disposable. Proxies are regenerable but are not silently deleted merely because unreferenced; waveform caches and proven temporaries are safe-derived cleanup candidates.

Recording abandonment is lossless. Header-only `.recording.part.wav` may be discarded. A payload-bearing partial take is retained and inventoried. If a process dies before `FloatWavFileWriter.finish()` patches the header, recovery may repair only a canonical GuitarLab IEEE-Float32 WAV whose layout validates exactly; partial trailing bytes are reduced only to the last complete frame.

Clip deletion never implies media deletion while any clip still references the same managed source/proxy.

## Portable project architecture
`.guitarlab` is a versioned ZIP containing manifest, `project.json` and referenced managed media. Reader extraction occurs in hidden `.import-*` staging, blocks traversal/out-of-root paths, applies bounds, validates consistency, assigns a new project ID and only then publishes into managed storage.

Repository listing ignores internal hidden staging. Startup cleanup deletes only GuitarLab interrupted-import staging, not arbitrary hidden directories.

Project duplication follows the same invariant: all referenced source/edit proxies are copied before JSON publication; missing media aborts and rolls back the incomplete destination.

## Master-render and codec architecture
`StudioMasterRenderer` consumes current project/timeline/mix state and produces a Float32 WAV staging master. Format adapters keep that WAV or encode a delivery copy. Source/proxy files are never destructive output targets.

Android encoder input converts the staged float stream to PCM16 for current FLAC/MP3 encoder adapters. Presentation timestamps are calculated from each chunk's start frame; EOS uses the final decoder frame position.

FLAC output is a native FLAC stream with `fLaC` + STREAMINFO codec-specific data written exactly once before frames. Android API 36 instrumentation validates native extraction/decoding. MP3 remains capability-gated because Android does not mandate an MP3 encoder.

## User-destination publication
Project packages and masters are completely staged/validated before the selected SAF destination is opened. Final publication uses truncating write mode, cooperative cancellation and rollback-to-empty attempts after final-write error/cancellation.

## Studio output UX
The Studio top bar owns Share > `Salvar e exportar`, separated into editable project (`.guitarlab`) and final master (WAV Float32, FLAC, MP3). Options owns routes, monitoring, preferences and diagnostics and does not duplicate output actions as primary commands. Home reuses the same rename/export contracts.

## Timeline/edit interaction architecture
Workspace-level drag state coordinates track reorder and clip drag. The ghost remains above the LazyColumn and follows pointer continuously; edge autoscroll is coroutine/geometry driven.

Clip drop is resolved before mutation as one explicit intent:
- `Move(targetTrackId)`;
- `Delete` when the pointer is inside the active trash target;
- `NoOp` for same-origin/cancel/invalid drop.

Source clip/track identity is snapshotted at drag start and revalidated before commit. Trash drop enters the same confirmation/domain-delete path as `Excluir clipe`; preview/hover never mutates project state.

Trim uses independent start/end interaction handles backed by a pure pointer→frame policy. Handle ownership and source/timeline clamps are deterministic; labels/bubbles are presentation-only.

## Recording-take lineage architecture
Temporal split may leave multiple clip segments referencing one `RecordingTake`.

When one segment is moved or deleted:
- that segment is detached from the take lineage;
- surviving siblings retain the take;
- if the canonical `RecordingTake.clipId` leaves, a surviving sibling is promoted deterministically;
- the take is removed only when no sibling remains;
- active-take fallback is reconciled deterministically.

This prevents split → move/delete from creating dangling take references during project validation/save/reopen.

## Recording and synchronization architecture
Recording owns a dedicated `AudioRecord` input stream and managed Float32 writer. Playback/backing and software monitoring do not feed that writer. When an input is explicitly selected, capture begins only after `AudioRecord.routedDevice` confirms that effective device and stops if the route changes.

Recording synchronization separates three concepts that must never be collapsed into one magic offset:
1. **per-session startup skew** between capture and backing presentation;
2. **accepted route latency/calibration** for the effective input/output/sample-rate route;
3. **creative punch/pre-roll offset**.

Recording/playback engines expose trustworthy Android audio timestamp/monotonic timing evidence when available, with bounded fallback. Pure timing policy converts evidence to frames, combines compensation components exactly once and clamps final placement/trim to valid timeline/source bounds. A fixed hard-coded `-0.5 s` correction is prohibited.

## Live recording waveform architecture
Live waveform is a bounded **frame-span envelope**, not one point per callback.

Each `LiveWaveformPoint` owns:
- start frame;
- end frame exclusive;
- normalized peak.

`recordingFrames` is authoritative for live clip duration. When the envelope exceeds its bound, adjacent spans are compacted while retaining complete covered time and the maximum transient. UI publication is conflated/rate-bounded instead of launching one main-thread state update per `AudioRecord` read. On finalization, canonical file-derived waveform data replaces transient live-envelope state without changing clip placement.

## Mixer interaction
Mixer/Master controls operate engine/project state. Mute/Solo/Arm expose button role, contextual content and state descriptions. Compact visuals may rely on Compose minimum touch expansion, but neighboring control centers are spaced to avoid hit-target collision; API 36 Compose tests verify callbacks/semantics.

## Lifecycle boundary
Saveable navigation routes are encoded/decoded by a pure route codec and tested through real `ActivityScenario.recreate()`. Durable creative state lives in project persistence, not transient composable state. Interrupted import/recording artifacts are recovered/classified independently of Activity recreation.

## Build/release architecture
`scripts/build_local.sh` is the local software build gate. It validates Java/Gradle/SDK prerequisites, materializes split sources, and runs JVM tests, Lint and debug assembly. Signed release assembly is explicit and uses environment-only credentials.

`.github/workflows/android-ci.yml` is the canonical manually dispatched full software/API36/geometry/signing executor. It has no automatic commit trigger. Its software and API 36 gates are independent prerequisites of the signed homologation job; release compilation is produced once in the warm software job and the final job signs/verifies that exact unsigned binary without recompilation.

### Serial source materialization
Large RC3 deltas are versioned under `.source-parts` and materialized before tests/build. Post-615 hardening order is intentionally fixed:

`H1 trim → H2 lineage/delete → H3 drag transaction → H4 recording timing → H5 live waveform → H6 integrated regression → H6 final test fix → H6 guide sync`.

A patch must either apply cleanly or be recognized as already applied; otherwise materialization fails. This preserves diagnosability and blocks silent partial source.

## Current milestone boundary
M5 and M6 are closed. M7/M8 active RC hardening is implemented through H0–H6 but awaits an exact-source canonical workflow plus residual target-device validation. CI #615 is the last fully green signed baseline and predates the H0–H6 source.

## Studio information architecture
Home and Studio share the same `StudioUserGuideDialog`; separate entry buttons are navigation affordances, not separate help implementations. The guide documents trim handles, clip deletion/trash and current recording/practice behavior.

The Studio practice-control row is responsive and uses a stable fixed slot for Auto-sections preview actions. Recording countdown is a z-indexed overlay and never participates in workspace Column measurement.

The top bar uses independent start/center/end regions so the complete transport/navigation group is geometrically centered as a unit. Project summary belongs to the Pistas header; track creation is an explicit footer action below the final track.
