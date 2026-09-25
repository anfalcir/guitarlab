# GuitarLab Studio 0.2.0-alpha05 — historical signed M4 playback/trim candidate

Version: `0.2.0-alpha05`
Version code: `6`
Status: **historical signed candidate; no longer represents current branch head**.

This package was prepared for the M4 playback + loop + non-destructive trim consolidation gate on Samsung SM-X230 / Android 16. Its package identity/signature/hash were internally verified.

Included at that time:
- M2 Pocket Amp hardware gate already homologated;
- M3 managed WAV import/codec foundation;
- immutable project-managed media copies;
- waveform cache/rendering;
- real Android managed-WAV playback;
- hardware-clock-driven playhead;
- Stop, start-from-playhead and loop wrapping;
- STOPPED-only editing;
- marker heads and mustard trim draft/apply/cancel.

Subsequent branch work is **not** contained in this APK:
- single-screen Studio redesign;
- loop-marker contextual visibility fix;
- stronger trim confirmation UX;
- Graphite Studio refinement and immersive fullscreen;
- Options Center;
- global input/output selection and output fallback routing;
- Mixer Dock and real track gain/pan/mute/solo;
- Master gain/meters and per-track metering work.

Therefore alpha05 must not be used as final physical evidence for the current M4 branch state. A newer signed consolidation candidate is required after the current software checkpoint is green.

Recording (M5), production resampling, compressed-format verification and final release readiness remain outside this historical candidate.
