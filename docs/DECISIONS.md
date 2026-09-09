# Architectural and Product Decisions

This log records decisions that must survive chat/context loss.

## D-001 — Android-first
GuitarLab targets Android/tablet as the primary environment.

## D-002 — Blank and Guitar-template projects coexist
Users may start blank or from the Guitar template.

## D-003 — Roles are explicit metadata
Built-in/custom roles organize tracks without making structure rigid.

## D-004 — Non-destructive clip model
Move/trim/gain/mute change metadata, never source audio.

## D-005 — Multi-format interoperability is product scope
WAV-first is staged validation only. Planned import includes WAV, FLAC, AIFF, MP3, AAC/M4A, OGG Vorbis and Opus; export includes high-quality WAV/FLAC plus validated compressed formats.

## D-006 — Sample-rate mismatches are explicit
Never change speed/pitch accidentally. Mismatches require validated resampling.

## D-007 — Capability claims follow gates
Planned/partial features are not advertised as supported.

## D-008 — M2 target hardware gate is closed
The M2 Pocket Amp gate is HOMOLOGATED/PASS for Samsung SM-X230 on Android 16/API 36 with the tested Pocket Amp USB audio path. Future hardware combinations require separate compatibility evidence.

## D-009 — Production audio is distinct from diagnostics
M2 probes do not substitute for Studio transport/recording architecture.

## D-010 — Imported media is project-managed and immutable
External documents are read-only origins; successful imports create immutable internal source copies. Edits are metadata; waveform/proxy/resampling/render outputs are separate derivatives.

## D-011 — Graphite Studio visual language
The creative UI uses near-black layered graphite surfaces, restrained teal product identity and semantic functional colors rather than generic Material-card prose.

## D-012 — Build/signing continuity
GitHub Actions is canonical; signing secrets are CI-only and signer identity is verified.

## D-013 — Documentation is canonical project context
Scope, roadmap, decisions, gates and state live in the repository.

## D-014 — Timeline controls use explicit marker heads
Playhead and Loop use clear top marker heads with >=48dp targets; guide lines are visual only. Trim uses equally ergonomic local handles inside the active waveform. Playhead is blue, loop green, trim mustard and recording red.

## D-015 — Timeline edits are STOPPED-only
Marker movement, clip edits, import and current mix edits are locked while PLAYING/RECORDING. There is no separate Pause state; Play toggles to Stop.

## D-016 — Playhead follows the audio hardware clock
M4 playback uses Android `AudioTrack` over immutable managed WAV media. UI time follows hardware-presented frames rather than an arbitrary timer. Sample-rate mismatch remains rejected until resampling is validated.

## D-017 — Trim is a staged, non-destructive waveform edit
Trim opens at 35%/65% with mustard local waveform handles, precise boundary times and a draft/apply/cancel transaction. Apply persists clip metadata once; neither edge may expose frames outside immutable source bounds.

## D-018 — Studio is timeline-first and single-screen
The Studio must not require whole-screen vertical scrolling for its normal workflow. Timeline, transport and contextual editing stay in one workspace; track overflow may scroll only inside the track region. Duplicate clip/track representations and floating explanatory prose are prohibited.

## D-019 — Loop markers exist only while loop is enabled
L◀/L▶ are contextual controls, not permanent timeline clutter.

## D-020 — Options Center owns low-frequency commands and setup
Global audio I/O, export workflow, project/Studio preferences, codec capability/status and advanced diagnostics belong in Options. The creative timeline must not duplicate these controls.

## D-021 — Input is global; output routing is global and revalidated
Current scope exposes one global recording input and one main output. Per-track input routing is intentionally deferred. Android numeric audio device IDs are ephemeral and must never be persisted as durable identity; saved route preferences use a stable device signature and are re-resolved at runtime.

## D-022 — Mixer Dock has independent visibility and pinning
The bottom Mixer may be hidden/visible and temporary/pinned independently. Timeline and Mixer share one selected-track concept. Output selection remains in Options; Master may summarize route state only.

## D-023 — Mixer controls must control real engine state
Track gain/pan/mute/solo are editable because they are persisted and honored by playback. Record Arm is functional in M5 and exactly one armed track is required per take. No decorative EQ, meter or monitor control is enabled before its engine path exists.

## D-021 — M5 recording is transactional and single-target
The current global input maps to exactly one armed track. Capture starts only after a visible five-second countdown and zero-time revalidation. A take is referenced by project metadata only after valid WAV finalization and atomic promotion to immutable managed storage; zero-frame failures leave no clip.

## D-024 — Master gain is durable project metadata
Master gain is part of `GuitarProject`, defaults to 0 dB for legacy JSON, is validated to -60..+12 dB and is applied after track summing. This additive field remains backward-compatible with existing schema-v1 JSON through serializer defaults.

## D-025 — Metering is post-bus and non-persistent
Track meters represent each audible post-track-mix bus before Master. Master meter is measured after summing and master gain, before final output clamp so overload remains observable. Meter ballistics use immediate attack, peak hold and time-based decay. Meter display state is transient and never stored in project JSON.

## D-026 — Android shell uses immersive fullscreen
System status/navigation bars stay hidden during normal use and may be revealed transiently with standard system edge gestures.
