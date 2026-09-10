# M8 global digital regression

Updated: 2026-09-10

Baseline: M7 alpha1 `5159de1f95ca0cfb483d0b244d8f956d49300c23`.
Signed expanded-regression checkpoint: M7 alpha2 `ca5b9d57ed07bb9cbd27a5da61386cc764209fd0`, CI run #465.
Latest completed software gate before cancellation/file-lifecycle hardening: commit `3bb3c8f37a826230d4cf619299054b79779176fd`, CI run #493; unit tests, Android Lint and assembleDebug PASS.

## Objective coverage matrix

| Area | Automatic/digital evidence | Physical need | Status |
|---|---|---|---|
| Project factory/template/roles/names | JVM model tests | none | PASS |
| Save/load/list/delete | filesystem repository tests | none | PASS |
| Duplicate project + managed media | source/proxy byte comparison and rollback | none | PASS |
| `.guitarlab` writer/reader | ZIP round trip, traversal, duplicate/ambiguous paths, version, zero/truncated input and rollback | none | PASS |
| Legacy JSON compatibility | pre-M5, M5, M6, pre-M7, partial metadata and current fixtures | none for covered fixtures | PASS |
| Trim/split/move/reorder/drag policy | frame invariants and invalid-bound tests | gesture ergonomics only | PASS logic / physical UX pending |
| Undo/Redo | long mixed sequence, exact snapshot recovery and branch invalidation | none | PASS |
| Mixer policy | mute/solo truth table, gain dB and pan law | listening optional | PASS core |
| SRC | five-rate matrix; duration, pitch, RMS, channels, immutable original | listening optional | PASS |
| Same-rate media | byte-exact independent output | none | PASS |
| WAV codec/waveform | PCM16/24/float decode, seek, metadata and envelope tests | none for supported WAV core | PASS |
| Master render | deterministic PCM for placement/gain/pan/fade/crossfade/clipping/stereo | listening optional | PASS |
| Realtime playback vs offline export | shared PCM reader/kernel; realtime-sized and offline-sized chunks are bit-exact | route/device still physical | PASS digital / physical route pending |
| Home vs Studio export | one request factory and one export service; identical rate/mute/solo/media rules | none | PASS |
| Recording state/placement | complete state-machine regression, managed-take tests and cancellation-safe publication | real USB capture required | PASS digital / physical capture pending |
| Latency compensation | synthetic policy/clock/calibration tests | real route latency required | PARTIAL |
| FLAC/MP3 Android codecs | build/lint only; device codec integration not instrumented | device validation remains | PENDING |
| Lifecycle/process recreation | no instrumented suite yet | some device confirmation | PENDING |
| Accessibility/responsive Compose UI | lint plus static semantics/touch-target audit; track colors are 48 dp radio controls with selected state | TalkBack/layout observation remains | PARTIAL |
| Large-session performance | 24-track/120-clip save, bundle round trip and reopen; 2,000 deterministic fuzz projects | tablet realtime stress remains | PASS structural / physical performance pending |

## Defects found and corrected

| Severity | Reproduction | Correction | Regression evidence |
|---|---|---|---|
| P1 | Duplicate project persisted JSON whose managed source/proxy files were absent in the new project directory. | Transactional copy of every distinct referenced source/proxy; rollback on any failure; reject ID collision. | Byte comparison and missing-media rollback test PASS. |
| P2 | M7 editing proxy bounds, fades and frame additions could bypass validation or overflow. | Canonical editing-domain bounds and explicit rate/total/fade/overflow validation. | Three focused model regression tests PASS. |
| P0 | Cancellation at the repository-save boundary could invoke rollback after imported/proxy/take media had already become referenced by the persisted project. | Non-cancellable publication boundary, commit-aware rollback and failed-import waveform cleanup. | Source review plus global unit/lint/build gate; physical capture is not needed to prove the persistence invariant. |
| P2 | Home and Studio built master requests independently; Studio omitted Home's editing-rate rejection. | Canonical request factory and shared export service for both entry points. | Solo/mute/proxy/rate/timeline factory tests PASS. |
| P2 | Project export opened the user destination before package construction had succeeded. | Stage and validate the complete package in cache before opening the destination; unconditional temp cleanup. | Bundle failure/rollback tests and global gate. |
| P3 | Track color controls exposed 30 dp touch targets without selected-state semantics. | 48 dp adaptive controls with radio role, description and selected state. | Android Lint PASS; final device observation remains. |
| P3 | Initial new test harness missed imports/Android JUnit annotation. | Correct imports and JUnit variant annotation. | CI #452 PASS. |

No reproducible P0/P1 remains after the cancellation fix. No RC is justified while the current hardening HEAD lacks a final signed green identity and the physical M7 gate remains open.

## Quantitative audio tolerances used

- SRC target frames: rounded exact ratio; matrix observed within exact asserted mapping.
- 440 Hz channel: accepted 430–450 Hz by deterministic positive zero crossings.
- 880 Hz channel: accepted 860–900 Hz.
- RMS: 0.5-amplitude sine accepted 0.33–0.38; 0.25-amplitude sine accepted 0.16–0.20.
- same-rate output: byte-for-byte equality.
- rendered silence/channel isolation: absolute sample error below `1e-5`.
- crossfade overlap: sample-domain envelope checked per overlap frame within `1e-4`.

## Remaining physical checklist

1. Install the final signed RC candidate on Samsung SM-X230.
2. Connect Pocket Amp and confirm real USB input/output routing.
3. Record and monitor guitar; judge tactile/subjective latency.
4. Listen for pops, dropouts, pitch artifacts and crossfade quality.
5. Stress a representative large session on the tablet and confirm physical ergonomics.
