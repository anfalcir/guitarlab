# GuitarLab Studio 0.4.0-alpha2

M7 regression/hardening candidate; not a Release Candidate.

## Corrected
- Project duplication now carries every referenced managed original and editing proxy into the independent copy and rolls back incomplete copies.
- Project validation now rejects M7 editing-domain errors, invalid fades and frame-range overflow.

## Verified digitally
- 127 tests across model, project, codec, audio and Android offline render modules.
- Five-direction sample-rate conversion matrix plus same-rate byte-exact copy.
- Deterministic PCM master behavior for placement, gain, pan, clipping, fades, overlaps and stereo routing.
- Android Lint and debug assembly.

## Known limitations
- M7 remains open until Pocket Amp/SM-X230 physical homologation.
- Full realtime-vs-offline PCM capture parity, Android FLAC/MP3 encoder integration, lifecycle UI automation, accessibility configurations and large-session benchmarks remain M8 work.

## Candidate packaging
- Signed alpha2 homologation artifact is generated only after the exact-commit software gate passes.
