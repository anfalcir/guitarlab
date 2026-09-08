# GuitarLab Studio 0.2.0-alpha05 — signed M4 consolidation candidate

Version: `0.2.0-alpha05`
Version code: `6`
Purpose: physical consolidation gate for M4 Studio playback + loop + non-destructive trim on the Samsung SM-X230 / Android 16 baseline.

Included scope:
- M2 Pocket Amp hardware gate already homologated and retained as regression baseline;
- M3 managed WAV import/codec foundation;
- immutable project-managed media copies;
- waveform cache/rendering;
- real Android managed-WAV playback;
- hardware-clock-driven playhead;
- Stop, start-from-playhead and real loop wrapping;
- STOPPED-only editing;
- explicit top marker heads;
- mustard T◀/T▶ trim markers with draft/apply/cancel;
- metadata-only trim preserving immutable managed source media.

Not included/claimed:
- Studio recording (M5);
- production resampling;
- verified compressed import formats beyond the existing WAV slice;
- final release readiness.

Physical procedure is defined in `M4_ALPHA05_HOMOLOGATION_CHECKLIST.md`. A green CI/signed APK only establishes the software/signing gate; PR #1 remains draft until the physical M4 checklist passes.
