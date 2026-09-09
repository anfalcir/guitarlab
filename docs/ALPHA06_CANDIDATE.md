# GuitarLab Studio 0.2.0-alpha06 — signed M4 consolidation candidate

Version: `0.2.0-alpha06`
Version code: `7`
Purpose: current physical consolidation gate for the modernized M4 Studio on Samsung SM-X230 / Android 16.

Included scope:
- M2 Pocket Amp hardware gate already homologated and retained as regression baseline;
- M3 managed-WAV import/codec foundation;
- immutable project-managed media copies and waveform cache/rendering;
- single-screen Studio workspace and immersive fullscreen shell;
- contextual loop and trim markers with obvious Apply/Cancel trim transaction;
- real Android managed-WAV playback driven by hardware-presented frames;
- Play/Stop, play-from-playhead and loop wrapping;
- synchronized timeline/Mixer track selection;
- persisted track gain, pan, mute and solo with real playback behavior;
- Options Center with global audio input/output configuration and export command placement;
- stable-signature Android audio route preferences with runtime resolution and controlled Auto fallback;
- bottom Mixer Dock with temporary/pinned states;
- persistent project Master gain;
- real per-track and Master Peak/RMS metering with peak hold/decay;
- STOPPED-only editing and immutable-source guarantees.

Not included/claimed:
- Studio recording/arm/monitoring (M5);
- production resampling;
- compressed import formats beyond the currently gated WAV slice;
- implemented export engine;
- final release readiness.

The physical procedure is defined in `M4_ALPHA06_HOMOLOGATION_CHECKLIST.md`. CI-green plus a correctly signed APK establishes software/signing evidence only. PR #1 remains draft until the physical M4 consolidation checklist passes without P0/P1 issues.
