# GuitarLab Architecture

Updated: 2026-09-12

## Repository/branch policy
- `main` is canonical after the explicit merge of PR #1.
- Final physical homologation evidence and closure are recorded directly on `main`.
- A new APK identity is not promoted merely because source was committed; compilation, validation and signer evidence remain required.

## Core boundaries
- `core:model`: immutable project/track/clip metadata contracts, including managed source/proxy references.
- `core:project`: deterministic editors/history, repository, managed media, portable bundle reader/writer, recording/session/timeline policies, interruption recovery and media integrity audit.
- `core:codec`: WAV metadata/decoder/waveform primitives, import-format policy, Float32 WAV writer and pure encoding-timeline rules.
- `platform:codec-android`: Android compressed-format decode-to-proxy and master encoder adapters.
- `platform:audio-android`: playback/capture engines, routing and offline master renderer.
- `app`: Compose presentation, ViewModels, SAF launchers, orchestration and Android instrumentation surface.

## Managed-media architecture
An imported document has two identities:
1. **authoritative source** — byte-preserved project-managed native original, immutable after commit;
2. **editing representation** — optional managed PCM WAV proxy, derived/regenerable and never authoritative.

`AudioClip.managedSourcePath` refers to the source; `managedEditProxyPath` optionally refers to the editing representation. Playback/waveform/render resolve proxy first when present, otherwise source. Non-destructive edits stay in project metadata.

### Interruption and orphan policy
Sources are never auto-deleted by cleanup. Unreferenced sources and payload-bearing interrupted takes are reported/retained rather than guessed disposable. Proxies are regenerable but are not silently deleted merely because unreferenced; waveform caches and proven temporaries are safe-derived cleanup candidates.

Recording abandonment is lossless. Header-only `.recording.part.wav` may be discarded. A payload-bearing partial take is retained and inventoried. If a process dies before `FloatWavFileWriter.finish()` patches the header, recovery may repair only a canonical GuitarLab IEEE-Float32 WAV whose layout validates exactly; partial trailing bytes are reduced only to the last complete frame.

## Portable project architecture
`.guitarlab` is a versioned ZIP containing manifest, `project.json` and referenced managed media. Reader extraction occurs in hidden `.import-*` staging, blocks traversal/out-of-root paths, applies bounds, validates consistency, assigns a new project ID and only then publishes into managed storage.

Repository listing ignores internal hidden staging. Startup cleanup deletes only GuitarLab interrupted-import staging, not arbitrary hidden directories.

Project duplication follows the same invariant: all referenced source/edit proxies are copied before JSON publication; missing media aborts and rolls back the incomplete destination.

## Master-render and codec architecture
`StudioMasterRenderer` consumes current project/timeline/mix state and produces a Float32 WAV staging master. Format adapters keep that WAV or encode a delivery copy. Source/proxy files are never destructive output targets.

Android encoder input converts the staged float stream to PCM16 for current FLAC/MP3 encoder adapters. Presentation timestamps are calculated from each chunk's start frame; EOS uses the final decoder frame position.

FLAC output is a native FLAC stream, not merely raw encoded frames: mandatory `fLaC` + STREAMINFO codec-specific data is written exactly once before frames, whether the platform exposes it through output-format CSD or a codec-config buffer. Android API 36 instrumentation validates marker plus native extraction/decoding.

MP3 remains capability-gated because the Android platform does not mandate an MP3 encoder.

## User-destination publication
Project packages and master files are completely staged/validated before the selected SAF destination is opened. The final publisher uses truncating write mode, copies cooperatively with cancellation and attempts rollback-to-empty after final-write error/cancellation. This minimizes the risk that a partial external file appears to be a completed export.

## Studio output UX
The Studio top bar owns Share > `Salvar e exportar`, separated into editable project (`.guitarlab`) and final master (WAV Float32, FLAC, MP3). Options owns routes, monitoring, preferences and diagnostics and does not duplicate those output actions as primary commands. Home reuses the same rename/export contracts rather than maintaining divergent render logic.

## Timeline and Mixer interaction
Workspace-level drag state coordinates track reorder and clip migration. The ghost remains above the LazyColumn and follows pointer motion continuously; edge autoscroll is coroutine/geometry driven. Completed drop is one atomic metadata mutation; cancellation is a no-op.

Mixer/Master controls operate engine/project state. Mute/Solo/Arm expose button role, contextual content and state descriptions. Compact visuals may rely on Compose minimum touch expansion, but neighboring control centers are spaced to avoid hit-target collision; API 36 Compose tests verify callbacks/semantics.

## Lifecycle boundary
Saveable navigation routes are encoded/decoded by a pure route codec and tested through real `ActivityScenario.recreate()`. Durable creative state lives in project persistence, not transient composable state. Interrupted import/recording artifacts are recovered/classified independently of Activity recreation.

## Build/release architecture
`scripts/build_local.sh` is the default software build gate. It validates Java/Gradle/SDK prerequisites, materializes split sources, and runs JVM tests, Lint and debug assembly. Signed release assembly is explicit and uses environment-only credentials.

`.github/workflows/android-ci.yml` is a manual fallback/full-emulator executor. It has no automatic commit trigger. When manually used, its software and API 36 gates remain independent prerequisites of `homologation-apk`; signer verification and cleanup rules remain unchanged.

## Current milestone boundary
M5 and M6 are closed. M7 is technically hardened and remains open for one residual target-device gate. M8.A/B automated release hardening is covered for the current scope; M8.C is final exact-candidate/signing/physical closure work.

## RC1 recording and practice architecture
Recording owns a dedicated AudioRecord input stream and managed Float32 writer. Playback/backing and software monitoring do not feed that writer. When an input was explicitly selected, capture begins only after `AudioRecord.routedDevice` confirms that exact device and stops if the route changes.

Live waveform data is a bounded peak accumulator published as transient UI state; the finalized waveform remains derived from committed media. Takes are persisted metadata with exactly one active take per track, and playback/export/timeline duration consume only active-take clips.

Markers, sections, punch regions and take identity are additive project metadata. Section detection and level analysis produce reviewable suggestions; only explicit user actions commit sections or gain changes.

## Studio information architecture
The top bar uses three independent overlays: project title at the start, the complete transport/navigation group at the geometric center of the available screen, and global actions at the end. Centering applies to the navigation group as a unit, not to the Play button. The playhead already communicates current position, so no duplicate current/remaining-time field is shown there.

Project-wide editing context belongs to the track workspace: the Pistas header reports track count, clip count and total project duration. Track creation is an explicit footer action below the final track, keeping the header informational and preserving alignment with the track sidebar.
