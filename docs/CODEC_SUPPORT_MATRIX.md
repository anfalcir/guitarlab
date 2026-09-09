# Codec Support Matrix

Updated: 2026-09-09

Legend: **PLANNED** = product scope only; **IMPLEMENTED** = code path exists; **SOFTWARE VERIFIED** = automated candidate gate passed; **ANDROID VERIFIED** = representative real-device gate passed. A format is not advertised as fully supported merely because code exists.

| Format | Import | Export | Alpha13 status | Notes |
|---|---:|---:|---|---|
| WAV PCM | Yes | Yes | Import ANDROID VERIFIED (partial matrix); Float32 export IMPLEMENTED | Core import handles PCM U8/S16/S24/S32 and IEEE Float32. Alpha13 physically gates WAV 32-bit float master output. |
| FLAC | Yes | Yes | IMPLEMENTED | Import decodes to managed PCM proxy through Android media stack. Export uses Android FLAC encoder path; target-device validation required. Current render-to-encoder path quantizes to PCM16 before FLAC encoding; no 24-bit export claim is made. |
| AIFF / AIFF-C PCM | Yes | No | IMPLEMENTED import | Dedicated PCM path supports common 8/16/24/32-bit AIFF and AIFC `NONE`, `twos`, `sowt`. Compressed AIFF-C is rejected and not claimed. |
| MP3 | Yes | Yes | IMPLEMENTED, DEVICE-GATED | Import decodes to managed PCM proxy. Export requests MP3 320 kbps from the Android encoder exposed by the device. Android does not guarantee a platform MP3 encoder, so product support remains physically gated on the target device. |
| AAC / M4A | Yes | Later | IMPLEMENTED import | Common mobile interchange import via Android media decoding. Final AAC/M4A export is not part of alpha13. |
| OGG Vorbis | Yes | Optional later | IMPLEMENTED import | Import via Android media decoding; no alpha13 export claim. |
| Opus | Yes | Later | IMPLEMENTED import | Import via Android media decoding; no alpha13 export claim. |

## Import architecture
A successful import preserves the byte-identical managed original as the authoritative source. WAV files that are directly compatible can be edited from that managed source. Other implemented formats are decoded to a separate PCM WAV editing proxy. The proxy is derived/regenerable and never replaces the original source.

## Sample rates
Project-relevant rates remain 44.1, 48, 88.2 and 96 kHz. The app must never change speed/pitch silently. Equal-rate sources can proceed through the established path. Source/project mismatch remains explicitly unsupported where validated resampling is absent; transparent arbitrary-rate mixing is planned after M5, not implied by alpha13.

## Channel layouts
Mono and stereo are the primary V1 target. Alpha13 import paths explicitly gate unsupported broader channel counts instead of silently remapping them.

## WAV encoding
Core parsing/decoding supports:
- PCM unsigned 8-bit;
- PCM signed 16-bit little-endian;
- PCM signed 24-bit little-endian;
- PCM signed 32-bit little-endian;
- IEEE Float32 little-endian.

Master WAV export uses IEEE 32-bit float.

## Physical alpha13 codec gate
Representative files on Samsung SM-X230 must cover WAV, FLAC, AIFF, MP3, M4A/AAC, OGG and Opus import, followed by waveform/playback/Trim. Export must cover WAV Float32, FLAC and MP3. A target failure blocks the corresponding support claim and, when it affects a requested alpha13 function, blocks M5 closure.

## Validation rule
Every format progresses through implementation, malformed/error handling, project-managed source/proxy integrity, software gate and representative target-device validation. Documentation must distinguish those states explicitly.
