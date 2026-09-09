# Studio Options Center and Mixer Dock

This document is a normative UX/architecture contract for GuitarLab Studio options, routing and mixer presentation.

## Options Center principle
The Studio editing canvas must stay focused on music. Low-frequency setup, routing, export and engineering commands are centralized in one `Options` center rather than scattered across the timeline.

The Options center groups:
- Audio I/O: one global recording input, one main output route, project/sample-rate context, monitoring-related setup when implemented;
- Export: mix export, track export and portable project package commands and their parameters;
- Project & Studio: autosave, new-project defaults, mixer presentation preferences and other low-frequency workflow settings;
- Import & codecs: capability/status and codec diagnostics;
- Advanced / diagnostics: M2/M3 engineering tools.

## Audio routing scope
For the current product scope, recording input is global to the Studio session. Per-track input routing is intentionally not exposed. This matches the current single-guitar-interface workflow and avoids premature routing complexity.

Main output routing is also configured in Options. The mixer master strip may show the selected route, but route selection itself belongs to Options.

Android audio device IDs are session-ephemeral. Persisted preferences must never depend on a raw numeric Android device ID being stable across USB disconnect/reconnect. Route preferences must be revalidated against currently enumerated devices.

## Export placement
Export is a command workflow, not a permanent timeline control. It belongs in Options so the main Studio remains clean.

The final export workflow is expected to cover:
- mix export;
- individual track export;
- range/full-project selection;
- WAV 16/24/32-float;
- FLAC 16/24;
- MP3;
- AAC/M4A;
- Opus;
- target sample rate and applicable encoding parameters.

No export button may be enabled before the export engine is implemented and software-gated.

## Mixer Dock
The mixer is a bottom dock with horizontal channel strips and a fixed semantic Master strip.

User-visible states are independent:
- hidden/visible;
- temporary/pinned.

Temporary mode may be closed with `X`. Pinned mode remains anchored at the bottom while the user changes timeline selection. Unpinning restores the ability to close it.

Selecting a track in the mixer changes mixer focus. Timeline-to-mixer selection synchronization is the target interaction model and must not create a second independent project selection concept.

## Track strip scope
The model already carries gain, pan, mute, solo and record-arm fields. The mixer visual model is therefore:
- track name/identity;
- level/gain;
- pan;
- mute;
- solo;
- record arm when the recording engine is active;
- meter when engine metering is available.

Controls must not be made editable merely because the UI exists. A control becomes interactive only when its underlying engine path and persistence semantics are wired and tested.

## Master strip scope
The Master strip is always visually distinct and includes:
- master level/meter;
- main output route summary;
- future master processing only when backed by a real engine.

Output-device selection itself stays in Options to avoid duplicate routing configuration surfaces.

## Visual direction
The product uses a Graphite Studio visual language:
- near-black background;
- layered graphite surfaces;
- restrained teal product identity;
- semantic timeline colors reserved for playhead, loop, trim and record;
- track colors as compact accents, not large decorative fields;
- modern cards and clear hierarchy on Home;
- no floating explanatory prose in the Studio editing canvas.

## Fullscreen
The Android shell uses immersive fullscreen. System bars remain hidden during normal use and may be revealed transiently by the standard edge swipe gesture.
