# Architectural and Product Decisions

This log records decisions that must survive chat/context loss.

## D-001 — Android-first
GuitarLab targets Android/tablet as the primary environment. Desktop may be useful for development/export workflows but cannot be required for the core user experience.

## D-002 — Guitar template and blank project coexist
New Project must allow either a Blank project or a Guitar template. Users must not be forced to create a template and then delete tracks.

## D-003 — Roles are explicit metadata
Track roles are first-class and may be built-in or custom. Roles help organization/defaults but must not make the project structure rigid.

## D-004 — Non-destructive clip model
Project JSON stores references and edits; it does not rewrite source audio for ordinary move/trim/gain operations.

## D-005 — Multi-format interoperability is product scope
WAV-first implementation is a staged validation decision only. Planned import scope includes WAV, FLAC, AIFF, MP3, AAC/M4A, OGG Vorbis and Opus. Export targets include high-quality WAV/FLAC plus validated compressed delivery formats.

## D-006 — Sample-rate mismatches are explicit
Never change playback speed/pitch as an accidental consequence of differing source/project sample rates. Equal rates may pass through; mismatches require a defined resampler/conversion path.

## D-007 — Capability claims follow gates
A planned/partially implemented format or feature is not advertised as supported until its software and required device gates pass.

## D-008 — M2 hardware gate remains authoritative
Parallel M3/M4 development was explicitly allowed while Pocket Amp is unavailable, but this does not close M2. The draft PR stays isolated from stable `main` until required gates are resolved.

## D-009 — Production audio engine is distinct from diagnostics
M2 probes validate Android routes and capture/playback behavior. Final Studio transport/recording requires a sustained low-latency architecture and later latency compensation.

## D-010 — Imported media is project-managed and immutable
External documents are read-only import origins. New imports are copied into project-controlled `media/source/` storage and Studio operates on that managed copy. The managed source is itself immutable after ingest. Move/trim/gain/mute are metadata operations; waveform, proxy, resampling and render outputs are separate derived files. The original external document and the managed source must never be rewritten by normal editing.

## D-011 — Clean modern UI
The old graphite/amber-heavy appearance was replaced by neutral surfaces, restrained teal accent, adaptive light/dark themes and clearer hierarchy. Record/warning/error colors remain functional, not decorative.

## D-012 — Build/signing continuity
GitHub Actions is the canonical remote build path. Signing secrets/keystore are never committed; homologation APK signer fingerprint is verified. Build reproducibility and handoff documentation are part of project quality, not optional maintenance.

## D-013 — Documentation is canonical project context
The target product, roadmap, support matrices, state and homologation rules must live in the repository. Chat history may explain decisions but must not be the only source of truth.

## D-014 — Timeline controls use explicit marker heads
Directly movable timeline positions must expose a clear draggable marker head at the top of the vertical guide. The thin line is visual guidance, not the primary drag target. Playhead/navigation uses restrained blue, loop boundaries green, trim boundaries coral/red and recording red; color is always reinforced by label/glyph/context. Primary interaction targets must be tablet-friendly (minimum 48 dp) and must not require mouse-like precision. This contract applies to future trim, loop, recording, punch and related timeline controls. See `TIMELINE_INTERACTION_GUIDELINES.md`.
