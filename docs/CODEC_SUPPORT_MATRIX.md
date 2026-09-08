# Codec Support Matrix

Legend: PLANNED = product scope retained but not implemented; IMPLEMENTED = code exists; SOFTWARE VERIFIED = unit/CI gate passed; ANDROID VERIFIED = representative real-device test passed. A format is not advertised to users until the required verified state is reached.

| Format | Import target | Export target | Current status | Notes |
|---|---:|---:|---|---|
| WAV PCM | Yes | Yes | ANDROID VERIFIED (partial matrix) | Core reader/decoder supports PCM U8/S16/S24/S32 and Float32; real tablet evidence exists for PCM24 44.1k stereo. More variants still require device matrix coverage. |
| FLAC | Yes | Yes | PLANNED | Target 16/24-bit export where robust encoder path is validated. |
| AIFF | Yes | Optional | PLANNED | Import required by product interoperability plan. |
| MP3 | Yes | Yes | PLANNED | Compressed delivery/import; implementation must use robust Android/runtime path. |
| AAC/M4A | Yes | Yes | PLANNED | Common mobile interchange format. |
| OGG Vorbis | Yes | Optional | PLANNED | Import target; export depends on validated encoder choice. |
| Opus | Yes | Yes | PLANNED | Efficient compressed delivery/import target. |

## Sample rates
Project-relevant rates: 44.1, 48, 88.2 and 96 kHz. Equal-rate media may pass through. Mismatched source/project rates require explicit resampling. Current code has sample-rate strategy planning, but production resampling quality/performance must be implemented and gated before claiming transparent arbitrary-rate mixing.

## Channel layouts
Mono and stereo are primary V1 targets. WAV parser can identify broader channel counts, but the Studio must not imply arbitrary multichannel workflow support before channel mapping is explicitly implemented.

## Bit depth / encoding
For WAV, implemented core parsing/decoding includes:
- PCM unsigned 8-bit;
- PCM signed 16-bit little-endian;
- PCM signed 24-bit little-endian;
- PCM signed 32-bit little-endian;
- IEEE Float32 little-endian.

## Validation rules
Each new format progresses through:
1. parser/decoder or platform adapter implementation;
2. malformed/truncated-input tests;
3. metadata and random-seek tests where applicable;
4. Android SAF/direct-seek/cache-fallback tests;
5. representative real files on target tablet;
6. mixed sample-rate/channel tests;
7. only then user-facing advertisement.

## Export quality targets
Planned product outputs include WAV 16/24-bit and 32-bit float where practical, FLAC 16/24-bit, and selected compressed formats (MP3, AAC/M4A, Opus) after encoder/legal/runtime constraints are validated. Dither/noise-shaping policy for integer down-quantization must be defined before final export homologation.

This matrix is a scope guardrail: the fact that M3/M4 begin with WAV must never be interpreted as a decision to make GuitarLab WAV-only.
