# Current State — GuitarLab Studio

Updated: 2026-09-11

## Active branch and gates
- Repository: `anfalcir/guitarlab`
- Canonical branch: `main`
- PR #1 merged on 2026-09-11 as commit `12aaa1c7d5762c1d4dd99e273418ed5b0c25c352`.
- Historical integration branch: `dev/parallel-m3-m5`.
- M2: PASS/CLOSED
- M3/M4: absorbed
- M5: **PASS/CLOSED by explicit user physical approval of `0.2.0-alpha14`**
- M6: **PASS/CLOSED by explicit user physical approval of `0.3.0-alpha1`**
- M7: exact signed candidate `0.4.0-rc2`, versionCode 20; all digital gates PASS; OPEN only for the final residual physical homologation.
- M8.A/B digital hardening: PASS for the scoped matrix.
- M8.C RC production: PASS. Final signed artifact produced and verified; residual physical gate pending.

## Build and gate policy
Ordinary commits do not trigger GitHub Actions. `scripts/build_local.sh` is the default local software gate. `.github/workflows/android-ci.yml` remains available only through explicit `workflow_dispatch` for a full API 36 emulator/signing run. This preserves the validated pipeline while preventing unintentional consumption of hosted-runner minutes.

The fully validated signed baseline is `0.4.0-rc2` / CI #593.

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

## Final signed RC evidence — 2026-09-12
- Candidate: `GuitarLabStudio-0.4.0-rc2-homologacao.apk` (`versionName 0.4.0-rc2`, `versionCode 20`).
- Exact source commit: `573015d9bb98c704074ab7c4e731eb9a9dec6a2e`.
- Canonical GitHub Actions run: [#593](https://github.com/anfalcir/guitarlab/actions/runs/34656567233).
- Result: software gate PASS; Android API 36 full regression PASS; isolated 1920×1200 landscape geometry PASS; signed homologation job PASS.
- APK SHA-256: `b067558e4ef6793df206d3e966faa029aa952b446ee63c8e47070a481297df85`.
- Signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Artifact integrity was independently rechecked against `SHA256SUMS.txt`; APK ZIP structure is valid.
- Digital hardening and RC production are complete. Only the residual Samsung SM-X230 + Pocket Amp physical homologation remains before explicit M7/M8 closure.

## Repository promotion
The validated RC source and synchronized documentation are now on `main`. Final physical homologation results and explicit closure will be recorded directly on `main`.

## RC2 interface refinement — validated
- Candidate: `0.4.0-rc2`, versionCode 20.
- Scope: geometric centering of the complete Studio navigation group; removal of redundant remaining-time readout; project summary (`pistas · clipes · duração total`) in the Pistas header; explicit Adicionar pista footer below the last track.
- Audio, persistence, codec and routing behavior are unchanged.
- Promotion status: PASS in CI #593. RC2 supersedes RC1 as the active physical-homologation candidate.
