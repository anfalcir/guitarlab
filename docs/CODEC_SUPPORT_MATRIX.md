# Codec Support Matrix

Updated: 2026-09-10

Legend:
- **IMPLEMENTED** — code path exists;
- **JVM VERIFIED** — deterministic core/software tests pass;
- **EMULATOR VERIFIED** — Android API 36 instrumented integration passes;
- **TARGET VERIFIED** — representative physical Samsung/Pocket Amp/device gate passes where target hardware matters;
- **DEVICE-GATED** — availability depends on codecs exposed by the actual Android device.

A capability is never promoted merely because code compiles.

| Format | Import | Export | Current status | Notes |
|---|---:|---:|---|---|
| WAV PCM / Float | Yes | Yes | JVM VERIFIED; prior target workflow verified representative WAV use | Core decode supports PCM U8/S16/S24/S32 and IEEE Float32. Master WAV output is IEEE Float32. |
| FLAC | Yes | Yes | Export **EMULATOR VERIFIED** on API 36 | Export writes native `fLaC` + STREAMINFO codec-specific data, then encoded frames. Instrumentation verifies marker, native extraction/decoding, 48 kHz stereo metadata and payload. Current encoder input is PCM16, so no 24-bit FLAC export claim is made. |
| AIFF / AIFF-C PCM | Yes | No | IMPLEMENTED import | Dedicated PCM path supports common 8/16/24/32-bit AIFF and AIFC `NONE`, `twos`, `sowt`. Unsupported compressed AIFF-C is rejected. |
| MP3 | Yes | Yes | Export behavior **EMULATOR VERIFIED / DEVICE-GATED** | Export requests 320 kbps from a device-exposed Android MP3 encoder. Android does not guarantee such an encoder. Instrumentation verifies successful extractable output when an encoder exists and controlled `AudioCodecException` with no leftover destination when it does not. Final availability remains Samsung-target-specific. |
| AAC / M4A | Yes | Later | IMPLEMENTED import | Common mobile interchange import through Android media decoding. Final AAC/M4A export is outside the current scope. |
| OGG Vorbis | Yes | Optional later | IMPLEMENTED import | Import through Android media decoding; no current export claim. |
| Opus | Yes | Later | IMPLEMENTED import | Import through Android media decoding; no current export claim. |

## Import architecture
Successful import preserves a byte-identical project-managed native original as the authoritative source. Directly compatible WAV may be edited from that managed source. Other accepted formats decode to a separate managed PCM WAV editing proxy. The proxy is derived/regenerable and never replaces the original.

## Sample rates
Project-relevant rates include 44.1, 48, 88.2 and 96 kHz. Mismatched source/project rates use validated bounded-memory conversion to a managed Float32 editing proxy; the native source remains immutable. Deterministic software regression covers 44.1→48, 48→44.1, 88.2→48, 96→48 and 44.1→96 with asserted duration/pitch/RMS/channel behavior. Equal-rate conversion has independent byte-behavior coverage. Silent speed/pitch changes are prohibited.

## Channel layouts
Mono and stereo are the primary V1 target. Unsupported broader layouts are rejected rather than silently remapped. Stereo editing/display keeps L/R identity and role-aware separation remains non-destructive.

## WAV encoding/decoding
Core parsing/decoding supports:
- PCM unsigned 8-bit;
- PCM signed 16-bit little-endian;
- PCM signed 24-bit little-endian;
- PCM signed 32-bit little-endian;
- IEEE Float32 little-endian.

Master WAV export uses IEEE 32-bit float.

## Android master-encoder rules
- presentation timestamps are based on each input chunk's start frame and EOS is monotonic;
- failed encoding removes the staged destination;
- FLAC must contain a valid native stream marker/STREAMINFO before audio frames;
- Android integration uses native extraction/decoding rather than extension/size-only checks;
- MP3 absence is a capability result, never a reason to fabricate support.

## Residual target-device codec gate
Only target-specific facts remain physical:
- whether the Samsung image exposes a compatible MP3 encoder;
- final playability/listening smoke through the intended target workflow.
FLAC structural validity is no longer delegated to physical homologation because it is API 36 instrumented.
