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
The M2 Pocket Amp gate is HOMOLOGATED/PASS for Samsung SM-X230 on Android 16/API 36 with the tested Pocket Amp USB audio path. M2 no longer blocks branch integration. Future hardware combinations remain separate compatibility evidence and do not rewrite this specific result.

## D-009 — Production audio is distinct from diagnostics
M2 probes do not substitute for Studio transport/recording architecture.

## D-010 — Imported media is project-managed and immutable
External documents are read-only origins; successful imports create immutable internal source copies. Edits are metadata; waveform/proxy/resampling/render outputs are separate derivatives.

## D-011 — Clean modern UI
Neutral surfaces, restrained teal identity, adaptive light/dark; semantic colors are functional.

## D-012 — Build/signing continuity
GitHub Actions is canonical; signing secrets are CI-only and signer identity is verified.

## D-013 — Documentation is canonical project context
Scope, roadmap, decisions, gates and state live in the repository.

## D-014 — Timeline controls use explicit marker heads
Manipulable positions use clear top marker heads with >=48dp targets; guide lines are visual only. Playhead blue, loop green, trim mustard, recording red.

## D-015 — Timeline edits are STOPPED-only
Marker movement, clip edits and import are locked while PLAYING/RECORDING. There is no separate Pause state in the current transport model; Play toggles to Stop.

## D-016 — Playhead follows the audio hardware clock
M4 playback uses Android `AudioTrack` over immutable managed WAV media. UI time follows hardware-presented frames (`playbackHeadPosition`) rather than an arbitrary timer. Loop wrapping is deterministic and tested. The engine rejects sample-rate mismatches until a validated derived-media resampler exists. Recording remains a separate M5 gate.

## D-017 — Trim is a staged, non-destructive marker edit
Trim uses mustard T◀/T▶ marker heads and a draft/apply/cancel transaction. Marker dragging changes only draft state while STOPPED; Apply persists clip metadata once. Moving the left edge changes timeline start and source offset by the same delta, preserving source/timeline alignment. The right edge changes visible length. Neither edge may expose frames outside the immutable managed source. Playback and unrelated edits are blocked while a trim draft is open.
