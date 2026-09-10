# Current State — GuitarLab Studio

Updated: 2026-09-10

## Active branch and gates
- Repository: `anfalcir/guitarlab`
- Branch: `dev/parallel-m3-m5` (historical branch name retained while PR #1 stays active)
- Draft PR: #1; `main` remains untouched by the current hardening work.
- M2: PASS/CLOSED
- M3/M4: absorbed
- M5: **PASS/CLOSED by explicit user physical approval of `0.2.0-alpha14`**
- M6: **PASS/CLOSED by explicit user physical approval of `0.3.0-alpha1`**
- M7: implementation candidate `0.4.0-alpha2`, versionCode 18; **OPEN for one final residual physical homologation only**
- M8.A compatibility/edge hardening: **DIGITALLY COVERED for the current scoped matrix**
- M8.B accessibility/lifecycle/performance hardening: **DIGITALLY COVERED for the current scoped matrix; physical ergonomics/realtime hardware performance remain residual**
- M8.C RC: **BLOCKED only on final exact-candidate signing + residual physical gate**
- Signed expanded-regression checkpoint: `0.4.0-alpha2`, commit `ca5b9d57ed07bb9cbd27a5da61386cc764209fd0`, push CI #465.
- Latest functional/CI hardening baseline before this documentation synchronization: `922c1c499248800ecce2ddf447c5201d95bbe9cb`, canonical CI #545. Do not physically homologate the old alpha2 checkpoint; wait for the intentionally cut final signed RC.

## Canonical automated gate
`.github/workflows/android-ci.yml` now contains both required automated jobs:
1. **software-gate** — unit tests, performance evidence, Android Lint and debug APK;
2. **android-integration-gate** — Android API 36 emulator instrumentation.

The signed homologation job depends on both gates. The old duplicate `pull_request` execution path and standalone emulator workflow were removed; the API 36 AVD is snapshot-cached. Signing remains opt-in only.

## M7 production audio polish
M7 includes validated sample-rate conversion, non-destructive fades/crossfades, realtime/offline render parity, larger-session memory discipline and shared Home/Studio save-export behavior.

### Sample-rate contract
The imported native source remains immutable. When source rate differs from the project/editing rate, GuitarLab creates a project-managed 32-bit-float WAV proxy using bounded-memory conversion. Timeline/sourceStart/length operate in the editing-proxy frame domain while native rate/format remain provenance.

### Fade/crossfade contract
Fade in/out are clip metadata and never rewrite source audio. Realtime playback and offline master rendering use the same deterministic envelope. Crossfade requires actual same-track overlap and maps the overlap to left fade-out + right fade-in.

### Managed-media and interruption contract
Sources are authoritative and never auto-deleted as cleanup. Proxies and waveform caches are derived. Interrupted project imports remain hidden staging and are safely removable. Recording abandonment is lossless: header-only temporary takes may be removed, but payload-bearing partial recordings are retained as recoverable media. A process-killed Float32 recording with a zero-length WAV header is conservatively repaired only when it exactly matches GuitarLab's canonical recording format.

### Export/publication contract
Project bundles and audio masters are fully staged before opening the user destination. SAF publication uses truncating write mode and attempts rollback on write error/cancellation so a partial destination is not presented as a valid completed export. Source/proxy media are never destructive export targets.

### Codec contract
Android master-encoder presentation timestamps start at the source chunk start and remain monotonic through EOS. FLAC export writes the required native `fLaC`/STREAMINFO codec data and is instrumented on API 36 by native extraction/decoding at 48 kHz stereo. MP3 remains target-capability-gated because Android does not guarantee an MP3 encoder; the emulator suite verifies both successful encoding when available and controlled failure when absent.

### Accessibility/lifecycle contract
Mixer Mute/Solo/Arm and CLIP clearing expose button semantics and state/context descriptions. Instrumented Compose tests cover callbacks and non-colliding M/S/Arm centers. `ActivityScenario.recreate()` verifies saveable navigation-route restoration. Persistence/media tests separately cover interrupted staging and recovery invariants.

### Performance evidence
Deterministic JVM/CI scenarios cover Small 5 tracks/10 clips, Medium 12/50 and Large 24/120 for save/load, bundle write/reopen and offline render. These numbers are regression baselines, not claims about Samsung realtime performance; final tablet stress remains physical.

## Residual physical gate
The final user homologation is deliberately limited to facts that cannot be established objectively in CI:
- actual Samsung SM-X230 + Pocket Amp USB input/output routing and guitar capture;
- subjective/real-route latency and monitoring feel;
- MP3 export availability/playability on that exact Samsung device;
- listening for hardware-route pops/dropouts/pitch or transition artifacts;
- representative large-session responsiveness and tactile/visual ergonomics on the tablet;
- one concise final smoke after restart/reopen.

No mathematical SRC checks, malformed package tests, persistence invariants, FLAC container validation or generic lifecycle recreation should be re-delegated to physical homologation.

Canonical roadmap: `docs/IMPLEMENTATION_ROADMAP.md`.
Active residual checklist: `docs/M7_ALPHA1_HOMOLOGATION_CHECKLIST.md`.
Global matrix: `docs/M8_GLOBAL_DIGITAL_REGRESSION.md`.
