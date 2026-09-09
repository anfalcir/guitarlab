# Studio Options Center and Mixer Dock

This document is a normative UX/architecture contract for GuitarLab Studio options, routing and mixer presentation.

## Options Center principle
The Studio editing canvas stays focused on music. Low-frequency setup, routing, export and engineering commands are centralized in one `Options` center rather than scattered across the timeline.

Options groups:
- Audio I/O: one global recording input, one main output route, project/sample-rate context and monitoring setup when implemented;
- Export: mix export, track export, range/full-project selection and portable project package when implemented;
- Project & Studio: low-frequency workflow preferences;
- Import & codecs: capability/status and codec diagnostics;
- Advanced / diagnostics: M2/M3 engineering tools.

## Audio routing scope
Recording input is global to the Studio. Per-track input routing is intentionally not exposed in current scope.

Main output selection also belongs in Options. The Master strip may summarize route state but must not duplicate the route selector.

Android audio device IDs are session-ephemeral. Persisted route preferences use device signature information and are revalidated against currently enumerated devices. Current playback resolves the selected output at runtime and attempts `AudioTrack.setPreferredDevice`; unavailable/rejected routes fall back to Android Auto with an explicit status rather than crashing.

## Export placement
Export is a command workflow, not a permanent timeline control. Planned targets remain mix/stems/ranges, WAV 16/24/32-float, FLAC 16/24, MP3, AAC/M4A, Opus and applicable sample-rate/encoding parameters. No export action is enabled before the engine exists and passes its gate.

## Mixer Dock
The Mixer is a bottom dock with horizontal channel strips and a distinct Master strip.

Independent user-visible states:
- hidden/visible;
- temporary/pinned.

Temporary mode may close with `X`. Pinned mode remains anchored while track selection changes. Timeline and Mixer share one selected-track concept.

## Track strip scope — current state
Implemented and real:
- track name/identity;
- gain -60..+12 dB, persisted;
- pan L100..R100, persisted;
- Mute and Solo, persisted and honored by playback;
- live post-track-bus peak/RMS metering with peak hold/decay presentation.

Still gated:
- record arm behavior and monitoring until M5;
- live-safe automation/parameter changes during playback;
- EQ/processing until a real DSP path exists.

## Master strip scope — current state
Implemented and real:
- project-persisted master gain -60..+12 dB;
- live Master peak/RMS measured after track sum + master gain and before output clamp;
- visible clipping when signal exceeds 0 dBFS;
- peak hold/decay presentation;
- route summary text.

Output-device selection itself remains in Options.

## Meter semantics
Track meters measure the audible track bus after clip+track gain/pan and before Master. Master measures the final mix after Master gain and before clamp. Meter display state is transient; it is never serialized into a project. `MeterBallisticsPolicy` supplies immediate attack, 750 ms peak hold and elapsed-time decay.

## Master persistence compatibility
`GuitarProject.masterGainDb` is additive metadata with a default of 0 dB. Existing schema-v1 JSON without this property decodes at unity, and round-trip tests protect the compatibility contract. Project validation enforces the supported gain range.

## Visual direction
Graphite Studio:
- near-black background;
- layered graphite surfaces;
- restrained teal product identity;
- semantic timeline colors reserved for playhead, loop, trim and record;
- compact track colors/accents;
- no floating explanatory prose in the creative canvas.

## Fullscreen
The Android shell uses immersive fullscreen. System bars are hidden during normal use and may be revealed transiently using standard edge gestures.
