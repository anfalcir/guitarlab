# M5 Media I/O Consolidation — alpha13

Updated: 2026-09-09

This checkpoint records the M5 scope deliberately pulled forward before M6.

## Consolidated functionality
- multi-format audio import policy: WAV PCM, FLAC, AIFF/AIFC PCM, MP3, AAC/M4A, OGG/Vorbis and Opus;
- immutable project-managed native original plus optional managed PCM WAV edit proxy;
- playback/waveform/render resolution through proxy when present and source otherwise;
- portable `.guitarlab` writer/reader with versioned manifest, independent restore identity and safe ZIP extraction;
- Home `Abrir projeto` round-trip flow;
- offline floating-point master render;
- WAV IEEE 32-bit float output;
- FLAC and MP3 320 kbps Android encoder paths, explicitly subject to target-device verification;
- dedicated Studio Share modal for project persistence and final master outputs.

## Software evidence before candidate promotion
The consolidated implementation immediately preceding alpha13 promotion passed unit tests, Android Lint and debug APK assembly on CI. Alpha13 repeats the same gate on the exact documented/versioned/signing candidate commit.

## Boundary
This checkpoint does not close M5. Physical homologation remains mandatory and M6 remains blocked.
