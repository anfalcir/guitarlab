# M8 global digital regression

Updated: 2026-09-10

Baseline: M7 alpha1 `5159de1f95ca0cfb483d0b244d8f956d49300c23`.
First green expanded gate: commit `e686ccf62bdf79008e962a1a401ba69b46b3d5d9`, CI run #452.
Result: 127 tests, 0 failures, 0 skipped; Android Lint PASS; assembleDebug PASS.

## Objective coverage matrix

| Area | Automatic/digital evidence | Physical need | Status |
|---|---|---|---|
| Project factory/template/roles/names | JVM model tests | none | PASS |
| Save/load/list/delete | filesystem repository tests | none | PASS |
| Duplicate project + managed media | source/proxy byte comparison and rollback | none | PASS |
| `.guitarlab` writer/reader | ZIP round trip, traversal, version, zero/truncated input | none | PASS |
| Legacy JSON compatibility | absent master/proxy/color fields | none for covered fixtures | PASS partial: broader migration fixtures pending |
| Trim/split/move/reorder/drag policy | frame invariants and invalid-bound tests | gesture ergonomics only | PASS logic / physical UX pending |
| Undo/Redo | long mixed sequence, exact snapshot recovery and branch invalidation | none | PASS |
| Mixer policy | mute/solo truth table, gain dB and pan law | listening optional | PASS core |
| SRC | five-rate matrix; duration, pitch, RMS, channels, immutable original | listening optional | PASS |
| Same-rate media | byte-exact independent output | none | PASS |
| WAV codec/waveform | PCM16/24/float decode, seek, metadata and envelope tests | none for supported WAV core | PASS |
| Master render | deterministic PCM for placement/gain/pan/fade/crossfade/clipping/stereo | listening optional | PASS |
| Realtime playback vs offline export | shared policies proven; full captured PCM parity harness not yet isolated | route/device still physical | PARTIAL |
| Recording state/placement | policy and managed-take tests | real USB capture required | PARTIAL |
| Latency compensation | synthetic policy/clock/calibration tests | real route latency required | PARTIAL |
| FLAC/MP3 Android codecs | build/lint only; device codec integration not instrumented | device validation remains | PENDING |
| Lifecycle/process recreation | no instrumented suite yet | some device confirmation | PENDING |
| Accessibility/responsive Compose UI | lint only | instrumented/device inspection | PENDING |
| Large-session performance | bounded implementation, no reproducible benchmark artifact yet | tablet stress remains | PENDING |

## Defects found and corrected

| Severity | Reproduction | Correction | Regression evidence |
|---|---|---|---|
| P1 | Duplicate project persisted JSON whose managed source/proxy files were absent in the new project directory. | Transactional copy of every distinct referenced source/proxy; rollback on any failure; reject ID collision. | Byte comparison and missing-media rollback test PASS. |
| P2 | M7 editing proxy bounds, fades and frame additions could bypass validation or overflow. | Canonical editing-domain bounds and explicit rate/total/fade/overflow validation. | Three focused model regression tests PASS. |
| P3 | Initial new test harness missed imports/Android JUnit annotation. | Correct imports and JUnit variant annotation. | CI #452 PASS. |

No reproducible P0 remains. No RC is justified while rows marked PENDING and the physical M7 gate remain open.

## Quantitative audio tolerances used

- SRC target frames: rounded exact ratio; matrix observed within exact asserted mapping.
- 440 Hz channel: accepted 430–450 Hz by deterministic positive zero crossings.
- 880 Hz channel: accepted 860–900 Hz.
- RMS: 0.5-amplitude sine accepted 0.33–0.38; 0.25-amplitude sine accepted 0.16–0.20.
- same-rate output: byte-for-byte equality.
- rendered silence/channel isolation: absolute sample error below `1e-5`.
- crossfade overlap: sample-domain envelope checked per overlap frame within `1e-4`.

## Remaining physical checklist

1. Install the final signed alpha2 on Samsung SM-X230.
2. Connect Pocket Amp and confirm real USB input/output routing.
3. Record and monitor guitar; judge tactile/subjective latency.
4. Listen for pops, dropouts, pitch artifacts and crossfade quality.
5. Stress a representative large session on the tablet and confirm physical ergonomics.
