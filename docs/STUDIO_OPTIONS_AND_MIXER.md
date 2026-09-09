# Studio Options Center and Mixer Dock

This document is the normative UX/architecture contract for GuitarLab Studio options, routing and mixer presentation.

## Product-facing principle
The creative canvas stays focused on music. Normal user-facing UI is pt-BR and uses clear conventional icons for frequent navigation/actions where that reduces visual noise without reducing communication or accessibility. Engineering milestone/homologation labels are reserved for documentation and advanced diagnostics, not normal Studio workflows.

## Options Center
Low-frequency setup, routing, export and technical tools are centralized in `Opções` rather than scattered across the timeline.

Groups:
- Áudio: one global recording input, one main output route and project/sample-rate context;
- Exportação: mix/track/package workflows when implemented;
- Projeto e Studio: low-frequency workflow preferences;
- Importação: user-visible file capability;
- Ferramentas avançadas: audio/codec diagnostics.

## Audio routing scope
Recording input is global to the Studio. Per-track input routing is intentionally not exposed in current scope. Because there is one physical input, M5 records into exactly one armed track per take and rejects ambiguous multi-arm starts.

Main output selection belongs in Options. The Master strip summarizes the main output concept but does not duplicate the route selector.

Android audio device IDs are session-ephemeral. Persisted route preferences use device signature information and are revalidated against currently enumerated devices. Playback resolves the selected output at runtime and attempts `AudioTrack.setPreferredDevice`; unavailable/rejected routes fall back to Android Auto rather than crashing.

## Export placement
Export is a command workflow, not a permanent timeline control. Planned targets remain mix/stems/ranges and the formats tracked in `CODEC_SUPPORT_MATRIX.md`. No export action is enabled before the export engine exists and passes its gate.

## Mixer Dock states
Independent states:
- hidden/visible;
- temporary/pinned.

Pinning is a persistent Studio UI preference, not project-audio metadata.
- temporary Mixer shows PushPin + Close;
- pinned Mixer shows only Close;
- Close on a pinned Mixer closes and unpins it;
- pin state survives navigation to Options and back.

Timeline and Mixer share one selected-track concept.

## Mixer V3 layout
The Mixer is a bottom dock with two regions:
- left: horizontally scrolling track strips;
- right: fixed Master strip outside the track scroll container.

This guarantees that Master remains reachable regardless of track count.

## Track strip scope
Implemented and real:
- track identity/name/color;
- gain -60..+12 dB;
- pan L100..R100;
- Mute and Solo state/persistence and playback behavior;
- Arm metadata state (preparation for M5 only);
- post-track-bus Peak/RMS metering with peak hold/decay;
- latched clipping indicator.

M/S/R presentation:
- Mute active = red;
- Solo active = amber/yellow;
- Arm active = record red;
- inactive = subdued neutral outline/foreground;
- no appended checkmark glyphs.

## Live mix contract
Track gain/pan and Master gain are explicit live-safe controls during playback.

Track processing order:
1. immutable clip decode;
2. clip gain;
3. live track gain + pan;
4. track Peak/RMS meter;
5. track sum;
6. live Master gain;
7. Master Peak/RMS meter;
8. final clamp/output.

Drag preview updates the running playback engine immediately. Persistence occurs at gesture completion to avoid a write per slider pixel. Project saves are serialized against the latest stored project snapshot to avoid stale live-mix writes overwriting unrelated project edits.

Faders show a neutral reference and use narrow soft snapping:
- track/Master gain: 0 dB;
- pan: center.

The snap region is intentionally small so it assists neutral positioning without blocking fine nearby adjustments.

## Structural edit contract
Structural edits remain STOPPED-only in M4:
- trim/remove clip;
- add/rename/reorder/recolor/delete track;
- Mute/Solo/Arm changes;
- timeline marker edits.

Live gain/pan/Master are the deliberate exception. Record, capture and monitoring are implemented by M5.C.

## Meter and clip-latch semantics
`MeterBallisticsPolicy` remains transient and provides immediate attack, peak hold and elapsed-time decay.

Clipping uses a separate latch:
- track latch triggers above 0 dBFS after track mix;
- Master latch triggers above 0 dBFS after Master gain and before clamp;
- latch survives Stop even though moving meters reset;
- tapping the `CLIP` badge clears it directly;
- a new playback/record attempt clears all latches before the new run.

No separate dismiss X is used for clip warnings.

## Track colors and management
`AudioTrack.colorIndex` selects one of 20 Graphite-compatible track colors shared across:
- sidebar identity stripe/border;
- timeline clip/card;
- waveform;
- corresponding Mixer strip.

The field is additive with default `-1`, preserving legacy schema-v1 projects. Tracks without an explicit color derive a stable visual color from current track order until the user chooses one.

Track settings support rename, color, move up/down and safe delete. Track deletion is blocked while clips still belong to that track. New generic mono tracks can be added from the timeline sidebar header.

## Visual direction
Graphite remains the foundation, but the UI is intentionally richer than the alpha06 teal-monochrome pass:
- near-black layered graphite surfaces;
- teal GuitarLab identity;
- blue secondary/navigation accents;
- amber attention/Solo accents;
- red/coral record/mute/error semantics;
- 20 track identity colors;
- semantic timeline colors reserved for playhead, loop and trim.

## Fullscreen
The Android shell uses immersive fullscreen. System bars are hidden during normal use and may be revealed transiently using standard edge gestures.
