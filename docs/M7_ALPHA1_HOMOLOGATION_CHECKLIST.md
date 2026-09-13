# Final Physical Homologation — GuitarLab 0.5.0-rc1

Updated: 2026-09-13

Use only `GuitarLabStudio-0.5.0-rc1-homologacao.apk`.

- versionCode: 21
- source: `5a14e4d6522ab9cc53eb8e1dd80a03306fc9d248`
- APK SHA-256: `5e343a9016cb5ea8fa9e381061ffb789667a6529a3b706fec7de9eb7bec9d1a5`
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`

Target: Samsung SM-X230 + M-VAVE MK-300 over USB, with MK-300 USB loopback disabled.

## 1. Install and persistence
- Install over the previous signed candidate.
- Open an existing project and confirm tracks/clips remain intact.
- Reopen after a full app restart.

## 2. Capture isolation and live waveform
- Select MK-300 explicitly as input and intended MK-300 output.
- Play a backing track, arm only one My Guitar track and start recording.
- Confirm the waveform grows visibly during capture.
- Confirm Play/Stop is visibly disabled while recording and REC stops the take.
- Play the take solo: it must contain the guitar input, not the backing track.
- Repeat with monitoring Off, Auto and On; these modes must not change recorded-file contents.
- Disconnect the selected input during a short take: capture must stop safely and must not fall back to the tablet microphone.

## 3. Transport recovery
- During playback press `|<`; playback must continue from the beginning.
- Start/stop playback repeatedly, then record and return to playback.
- Close/reopen the same project and verify Play remains responsive.

## 4. Practice workflow
- Create/remove a marker.
- Create a section from the loop and loop it.
- Run automatic section suggestions, review, accept or discard explicitly.
- Record at least two takes on one track and switch the active take.
- Compare Reference, My Guitar and Both.
- Create a punch region from the loop and verify the retained take aligns with that region.
- Run level analysis and audition before explicitly applying any suggested gain.

## 5. Real-device audio and stress
- Judge monitoring latency/feel and listen for repeatable pops, dropouts, wrong pitch/speed or unintended one-sided audio.
- Exercise a representative multi-track project, seeking, looping, Mixer and edits.
- Export a short WAV/FLAC and, if supported by this Samsung, MP3; confirm playability.

## PASS criteria
- no repeatable P0/P1;
- no backing leakage attributable to GuitarLab;
- no unintended input fallback;
- recording waveform, transport recovery and take/punch workflows behave as specified;
- explicit user approval of this exact APK.

Mathematical audio invariants, persistence/package validation, generic codec structure and previous full API 36 geometry regression are already automated and should not be manually repeated.
