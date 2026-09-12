# Architectural and Product Decisions

Updated: 2026-09-12

This log records decisions that must survive chat/context loss. Historical decisions remain binding unless a later numbered decision explicitly supersedes them.

## D-001 — Android-first
GuitarLab targets Android/tablet as the primary environment.

## D-002 — Blank and Guitar-template projects coexist
Users may start blank or from the Guitar template.

## D-003 — Roles are explicit metadata
Built-in/custom roles organize tracks without making structure rigid.

## D-004 — Non-destructive clip model
Move, trim, gain and mute change metadata, never immutable source audio.

## D-005 — Multi-format interoperability is product scope
WAV-first is staged validation only. Broader import/export remains governed by `CODEC_SUPPORT_MATRIX.md`.

## D-006 — Sample-rate mismatches are explicit
Never change speed/pitch accidentally. Mismatches require validated resampling.

## D-007 — Capability claims follow gates
Planned/partial features are not advertised as supported.

## D-008 — M2 hardware gate is closed
Pocket Amp + Samsung SM-X230 / Android 16(API 36) is PASS/CLOSED for the tested USB audio path. Other hardware combinations require their own evidence.

## D-009 — Production audio is distinct from diagnostics
Diagnostic probes never substitute for Studio transport/recording architecture.

## D-010 — Imported/recorded media is project-managed and immutable
External documents are read-only origins; accepted source media is promoted to managed immutable storage. Waveforms/proxies/renders are derivatives.

## D-011 — Graphite Studio visual language
Use near-black layered graphite surfaces, restrained product identity and semantic functional colors.

## D-012 — Build/signing continuity
Historical rule superseded by D-050. Signing secrets remain environment-only and signer identity must be cryptographically verified.

## D-013 — Documentation is canonical project context
Repository documentation defines scope, roadmap, decisions, active gates and current state; historical candidate files are evidence, not current truth.

## D-014 — Timeline controls use explicit marker heads
Playhead and Loop use clear top marker heads with ergonomic targets. Trim handles stay local to the active waveform. Playhead is blue, loop green, trim mustard and recording red.

## D-015 — Structural timeline edits require compatible stopped state
Marker movement, clip structural edits and import are blocked during incompatible transport/recording states. Live mix controls follow their own safe policy.

## D-016 — Playhead follows the audio hardware clock
Playback UI follows hardware-presented frames, not an arbitrary timer.

## D-017 — Trim is staged and non-destructive
Trim opens around 35%/65%, shows precise local boundary bubbles, clamps to immutable source bounds and persists metadata only when applied.

## D-018 — Studio is timeline-first and single-workspace
Normal editing does not page-scroll the whole Studio. Track overflow scrolls only inside the timeline body; duplicate permanent representations and development prose are prohibited.

## D-019 — Loop markers are contextual
Loop-specific markers disappear when Loop is disabled.

## D-020 — Options Center owns low-frequency setup/commands
Global audio I/O, monitoring, project/Studio preferences and diagnostics live in Options rather than cluttering the creative timeline. The former assignment of final save/export actions to Options is superseded by D-037.

## D-021 — Input/output routing is globally resolved and revalidated
Current scope exposes one global recording input and one main output. Durable preferences use stable signatures, never ephemeral Android device IDs.

## D-022 — Mixer Dock has independent visibility/pinning
Timeline and Mixer share selected-track identity; Master remains fixed while track strips can scroll.

## D-023 — Mixer controls operate real engine state
Gain/pan/mute/solo and Arm are functional state, not decoration. Exactly one armed track targets each recording take.

## D-024 — M5 recording is transactional and single-target
Capture begins only after visible countdown and zero-time revalidation. Metadata references a take only after valid WAV finalization and atomic promotion; zero-frame failures create no clip.

## D-025 — Master gain is durable project metadata
Master gain is validated, backward-compatible project state applied after track summing.

## D-026 — Metering is post-bus and non-persistent
Track/Master meter display is transient; overload remains observable before final output clamp.

## D-027 — Android shell uses immersive fullscreen
System bars stay hidden during normal use and may be revealed transiently with standard system gestures.

## D-028 — Drag/drop belongs to a shared workspace coordinator
Track reorder and clip migration share one workspace-level drag state. Drag ghosts must stay in the Compose tree above the LazyColumn; `Popup` is prohibited. Ghost position follows the pointer continuously, independently from snapped destination indicators.

## D-029 — Drag autoscroll is continuous and geometry-driven
Edge autoscroll runs in a coroutine/frame loop with bounded proportional speed. Targets are recalculated from current `LazyListState.layoutInfo` bounds after scrolling; preview motion never records project history.

## D-030 — Completed drop is one atomic metadata mutation
Track drop uses `ProjectTrackEditor.reorderTrack`; clip migration uses `ProjectClipEditor.moveClipToTrack`. Cancel and same-origin drop are exact no-ops. Stable IDs, source references and clip metadata are preserved.

## D-031 — Clear and delete are different operations
`Limpar pista` removes only track content and preserves track identity/function/color/order/mix. `Excluir pista` is structural, exists in `Configurar pista`, and is enabled only when the track is empty.

## D-032 — Track Settings is responsive and metadata-aware
Wide layouts use balanced identity/color and source-metadata columns; narrow layouts stack them. Source filename/format/sample rate/channels/bit depth/encoding/duration are shown when available, with a compact empty state otherwise.

## D-033 — M5 physical approval is mandatory
Software-green and signed identity are necessary but insufficient. Historical M5 candidate wording is superseded by the explicit M5 PASS/CLOSED state and later milestone decisions.

## D-034 — Native original and edit proxy are distinct
Every successful import preserves the project-managed native original as authoritative immutable source. A format that cannot be consumed directly by the Studio WAV path receives a separate managed PCM WAV proxy. The proxy is derivative/regenerable and never replaces or redefines the original source.

## D-035 — `.guitarlab` is a versioned round-trip project package
Portable project save contains a versioned manifest, `project.json` and referenced managed media. Restore validates format/version/project/media, blocks ZIP path traversal, stages into temporary storage and assigns a new project identity before publication. A portable save must be reopenable by GuitarLab; it is not a one-way backup archive.

## D-036 — Master export is an offline render pipeline
Final output renders current timeline/trim/placement/clip gain/track mix/Mute/Solo/Pan/Master state into a floating-point master representation, then encodes the selected delivery format. Export never mutates source/proxy media. Requested outputs are WAV 32-bit float, FLAC and MP3 320 kbps.

## D-037 — Share owns project save and final export UX
A Share icon lives immediately before Home in the Studio top bar. It opens `Salvar e exportar`, semantically separating editable `.guitarlab` persistence from final-audio masters. Options continues to own setup/preferences/diagnostics and must not duplicate these output actions as primary commands.

## D-038 — Media I/O/persistence/export were intentionally pulled forward into M5
Historical alpha13 wording is evidence only. M5 later closed by explicit approval of alpha14; M6 later closed by explicit approval of `0.3.0-alpha1`.

## D-039 — Encoder/support claims remain device-gated where Android capability is optional
A code path does not establish universal Android support. MP3 export requests a device-exposed Android MP3 encoder and remains target-capability-gated. FLAC can advance independently because API 36 instrumented native extraction/decoding now objectively verifies its current export structure.

## D-040 — Import processing is always visible
After Android document selection returns to GuitarLab, ingest/transcode/waveform preparation must expose a blocking progress surface with an explicit current operation. Silent background import is prohibited because it is indistinguishable from a missed command or freeze.

## D-041 — Stereo waveforms preserve channel identity
A two-channel source is visualized as two real envelopes, L above R. The previous aggregate-only waveform remains a compatibility/drag/overview representation but must not be the sole visual representation for stereo clips.

## D-042 — Stereo import is role-aware and non-destructive
A successful stereo import triggers an explicit decision. Backing/stereo-oriented tracks default conceptually to keeping both channels in one stereo clip. Guitar-role tracks with an existing L/R role pair offer direct synchronized distribution of source L/R into those two mono tracks. The immutable managed original remains shared; channel separation creates derived mono PCM proxies only.

## D-043 — Timeline split and stereo separation are distinct commands
`Dividir no cursor` remains a temporal clip edit. `Separar estéreo em 2 pistas mono` is a separate channel-routing command, available only for two-channel clips. They must never share ambiguous wording or behavior.

## D-044 — Abandonment and cleanup are lossless by default
Cleanup must never infer that a payload-bearing recording or authoritative source is disposable. Header-only recording temporaries may be removed; payload-bearing partial takes are retained/reported. Safe cleanup is limited to provable temporaries and derived caches.

## D-045 — Process-death recovery is conservative and format-specific
Interrupted project-import staging is internal and must never surface as a project. A Float32 recording header may be repaired only when the file exactly matches GuitarLab's canonical writer contract; otherwise it is retained untouched for diagnosis/recovery.

## D-046 — External publication stages first and fails closed
Project packages and masters are fully created/validated before opening a SAF destination. Final publication uses truncating semantics, cooperative cancellation and rollback attempts. A failed publish must not be reported as success.

## D-047 — Codec integration claims use real Android semantics
JVM/build success is insufficient for Android codec claims. FLAC export requires valid native stream metadata plus API-level extraction/decoding evidence. Tests must distinguish container identity from the decoded track MIME returned by Android extractors.

## D-048 — Signed candidates require both software and Android integration gates
The canonical CI contains a software gate and API 36 emulator gate. The signed homologation job has explicit dependencies on both; a signing request cannot bypass a failed required gate. Emulator/cache third-party actions are pinned to immutable commit SHAs.

## D-049 — Physical homologation is residual, not duplicated QA
Anything objectively established by automated model/file/JVM/emulator regression is removed from the manual checklist. The final physical pass is limited to target-hardware routing/capture, optional target codec capability, subjective latency/listening, real-device stress and ergonomics.

## D-050 — Commits do not automatically consume hosted CI
The default gate is `scripts/build_local.sh` on a prepared Android build host. GitHub Actions remains a manually dispatched fallback for the full API 36 emulator/signing matrix and has no `push` or `pull_request` trigger. No candidate may be called validated solely because automatic CI was disabled; build, test, signature and checksum evidence remain mandatory.

## D-051 — Studio navigation is centered as one unit
The complete transport/navigation control group is geometrically centered on the full Studio top bar; no individual control (including Play) is privileged as the center anchor. The current position remains conveyed by the playhead and remaining time is not duplicated. General project summary belongs to the Pistas header, while Adicionar pista is placed below the final track as an explicit workspace action.

## D-052 — Current physical target is M-VAVE MK-300
Historical Pocket Amp results remain valid evidence for the hardware combination tested at the time, but the active residual homologation target is Samsung SM-X230 plus M-VAVE MK-300 over USB. Final routing, capture, monitoring, reconnect, latency/listening and ergonomics evidence must use that current setup.
