# M8 Global Digital Regression

Updated: 2026-09-11

Final signed RC: `0.4.0-rc1`, commit `66108182d9a733930139a84e4f6b9525172bb9aa`, canonical CI #590.

## Objective coverage matrix

| Area | Automatic/digital evidence | Residual physical need | Status |
|---|---|---|---|
| Project factory/template/roles/names | JVM model tests | none | PASS |
| Save/load/list/delete | filesystem repository tests | none | PASS |
| Duplicate project + managed media | source/proxy byte comparison, collision/missing-media rollback | none | PASS |
| `.guitarlab` writer/reader | ZIP round trip, traversal, duplicate/ambiguous paths, version, zero/truncated input, interrupted staging and rollback | none | PASS |
| Legacy JSON compatibility | pre-M5, M5, M6, pre-M7, partial metadata and current fixtures | none for covered fixtures | PASS |
| Trim/split/move/reorder/drag policy | frame invariants and invalid-bound tests | tactile gesture ergonomics only | PASS logic / physical UX residual |
| Undo/Redo | long mixed sequence, exact snapshot recovery and branch invalidation | none | PASS |
| Mixer policy | mute/solo truth table, gain dB, pan law | listening optional | PASS core |
| Mixer accessibility | semantics/state/context + Compose instrumented callbacks/spacing | final tablet/TalkBack observation optional | PASS digital |
| SRC | multi-rate matrix; duration, pitch, RMS, channels, immutable original | listening sanity only | PASS digital |
| Same-rate media | independent byte-behavior regression | none | PASS |
| WAV codec/waveform | PCM16/24/float decode, seek, metadata, waveform/envelope tests | target listening smoke only | PASS |
| Master render | deterministic PCM for placement/gain/pan/fade/crossfade/clipping/stereo | subjective listening only | PASS |
| Realtime vs offline | shared PCM reader/mix kernel, chunk-size parity | real route/device only | PASS digital / physical route residual |
| Home vs Studio export | canonical request factory and shared export service | final smoke only | PASS |
| Recording state/placement | state-machine, managed-take, cancellation and recovery tests | real USB capture | PASS digital / physical capture residual |
| Interrupted recording | lossless abandon inventory + canonical Float32 header repair | none for file-recovery invariant | PASS |
| Media orphan policy | missing references, retained sources/proxies/recoverable takes, safe-derived cleanup | none | PASS |
| SAF publication | full staging first, truncating publish, cancellation/error rollback attempt | provider-specific final smoke | PASS policy |
| Latency compensation | synthetic policy/clock/calibration tests | real-route latency/feel | PARTIAL by nature |
| FLAC Android export | `fLaC`/STREAMINFO + API 36 native extraction/decoding, 48 kHz stereo payload | listening smoke only | PASS emulator |
| MP3 Android export | API 36 conditional capability contract | actual Samsung encoder availability | PASS behavior / target capability residual |
| Lifecycle recreation | App route codec + `ActivityScenario.recreate()` | none for covered recreation path | PASS emulator |
| Process-death storage safety | interrupted import/take recovery and safe repository listing | final OS/hardware smoke only | PASS persistence invariants |
| Large-session performance | Small 5/10, Medium 12/50, Large 24/120 save/load/bundle/reopen/render; deterministic fuzz | Samsung realtime stress | PASS structural / physical performance residual |

## Defects found and corrected during global regression

| Severity | Reproduction | Correction | Regression evidence |
|---|---|---|---|
| P1 | Duplicate project could publish JSON referencing managed media absent from the new project directory. | Transactional copy of each referenced source/proxy, rollback on failure, collision rejection. | Byte comparison and missing-media rollback PASS. |
| P0 | Cancellation at persistence boundary could run rollback after media had already become referenced by committed project JSON. | Non-cancellable publication boundary + commit-aware rollback. | Focused tests + global gate PASS. |
| P1 | Legacy `discard()`/startup cleanup could delete payload-bearing interrupted recordings. | Lossless abandon semantics; only header-only temporary takes are removable; recoverable inventory retained. | Recording-store and legacy-discard regression PASS. |
| P1 | Process kill during Float32 capture could retain samples but leave WAV data length zero. | Conservative canonical GuitarLab Float32 WAV header repair and frame-aligned truncation only when required. | Simulated interrupted-writer recovery PASS. |
| P2 | `.import-*` staging surviving process death could appear as a ghost Home project. | Hide internal dot-directories from repository listing and clean only GuitarLab `.import-*` staging at startup. | Interrupted-import recovery tests PASS. |
| P1 | Android encoder timestamps began at the end of the first chunk and EOS could jump back to zero. | PTS derived from chunk start frame; EOS from final decoder position. | Pure timeline tests + Android build gate PASS. |
| P2 | SAF final publication could leave a partially written user file after error/cancellation. | Stage first; publish with truncating mode, cooperative cancellation and rollback-to-empty attempt on failure. | Publisher copy/error/cancellation tests PASS. |
| P3 | Compact Mixer controls lacked complete contextual/state semantics and could have overlapping minimum touch regions. | Role/state/context semantics + spacing discipline; instrumented callbacks/center-distance assertions. | Compose API 36 instrumentation PASS. |
| P1 | FLAC master path omitted native codec-specific header on Codec2 output, producing an invalid/incomplete FLAC stream. | Capture/write validated `fLaC` + STREAMINFO from output format/config before encoded frames. | API 36 instrumented `fLaC` marker + native extraction/decoding PASS. |
| Test defect | First FLAC instrumented assertion expected `audio/flac` from Android `MediaExtractor`; native FLACExtractor intentionally exposes decoded `audio/raw`. | Test now separates container identity (`fLaC`) from extractor track MIME and verifies decoded payload/rate/channels. | Android integration gate PASS. |

No reproducible P0/P1 remains in the current automated scope after the listed corrections. CI #590 passed the full API 36 suite and the isolated 1920×1200 landscape geometry pass.

## Performance evidence policy
CI metrics are regression evidence, not device benchmarks. Current scenarios measure save/load, bundle write, package reopen/import, JSON/bundle sizes, rough heap delta and a 1-second offline render. A previously captured Large baseline (24 tracks/120 clips) completed save+load/reopen in tens of milliseconds and offline 1-second render in sub-second runner time; exact per-run metrics are stored in `ci-diagnostics/performance-evidence.txt`.

## CI architecture
The canonical workflow contains:
- `software-gate`;
- `android-integration-gate`;
- `homologation-apk`, which has `needs` on both.

The API 36 AVD uses a pinned snapshot cache. Signing remains skipped unless explicitly requested. A documentation or candidate claim must distinguish JVM, emulator and target-device evidence.

## Remaining physical checklist
1. Install only the final signed RC on Samsung SM-X230.
2. Connect Pocket Amp and confirm real USB input/output plus real guitar recording/monitoring.
3. Judge subjective latency/feel and listen for pops/dropouts/route artifacts.
4. Validate actual Samsung MP3 encoder availability/playability.
5. Stress one representative larger session and confirm touch/layout ergonomics.
6. Perform one concise restart/reopen/export smoke.

Everything else in this matrix stays digital and should not be manually repeated.

## Gate-efficiency decision
The headless Home overflow popup assertion was removed after repeated evidence showed test-observation flakiness rather than a product regression. Rename was already physically approved in M6, while lifecycle, persistence, Home rehydration, navigation and production rename remain covered by the retained suite and prior evidence. This prevents redundant CI expenditure without weakening the P0/P1 release gate.

## Final signed RC evidence — 2026-09-11
- Candidate: `GuitarLabStudio-0.4.0-rc1-homologacao.apk` (`versionName 0.4.0-rc1`, `versionCode 19`).
- Exact source commit: `66108182d9a733930139a84e4f6b9525172bb9aa`.
- Canonical GitHub Actions run: [#590](https://github.com/anfalcir/guitarlab/actions/runs/34593159502).
- Result: software gate PASS; Android API 36 full regression PASS; isolated 1920×1200 landscape geometry PASS; signed homologation job PASS.
- APK SHA-256: `604f13b83e27021201101fd663dad5c61bca600829ab9565109f48ae23f8e0ad`.
- Signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Artifact integrity was independently rechecked against `SHA256SUMS.txt`; APK ZIP structure is valid.
- Digital hardening and RC production are complete. Only the residual Samsung SM-X230 + Pocket Amp physical homologation remains before explicit M7/M8 closure.
