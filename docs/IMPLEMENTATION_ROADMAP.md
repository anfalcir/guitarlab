# Implementation Roadmap

Updated: 2026-09-10

## M1 — Project/model foundation — CLOSED
Project model, templates, persistence baseline and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Samsung SM-X230 Android 16/API36 + Pocket Amp USB physical gate established.

## M3 — Codec/import foundation — ABSORBED
Codec/import work was consolidated into later milestones.

## M4 — Studio playback/edit/mix foundation — ABSORBED
Timeline, playback, editing, Mixer/Master and project interaction foundations are integrated.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after explicit user physical approval of `0.2.0-alpha14`.

## M6 — Measured latency and synchronization — PASS/CLOSED
Closed after explicit user physical approval of `0.3.0-alpha1`. Loopback calibration, route-scoped compensation, clocks, live Mute/Solo and project rename are no longer pending milestone work.

## M7 — Production audio polish — ACTIVE FINAL PHYSICAL GATE
### Implemented
- validated bounded-memory sample-rate conversion for mismatched imported source/project rates;
- immutable native original retained while converted audio is a managed editing proxy;
- additive editing-rate metadata and canonical editing-frame-domain validation;
- non-destructive clip fade-in/fade-out and same-track overlap crossfade;
- shared deterministic fade/mix kernel for realtime playback and offline master render;
- reusable render scratch buffers and structural large-session regression;
- Home project Rename plus the same shared `Salvar e exportar` flow as Studio;
- `.guitarlab`, WAV Float32, FLAC and MP3 master paths;
- lossless interrupted-recording preservation plus conservative Float32 WAV header repair;
- cancellation-safe managed-media publication and interrupted-import staging recovery;
- safer SAF destination publication/rollback;
- corrected Android encoder timestamp monotonicity and FLAC native header emission.

### M7 closure gates
Digital/model/file/codec/lifecycle/accessibility checks are no longer delegated to the user. Closure now requires:
1. final exact-candidate software gate PASS;
2. final exact-candidate API 36 Android integration gate PASS;
3. intentional signed homologation build with locked certificate identity;
4. one residual Samsung/Pocket Amp physical pass covering real USB routing/capture, target MP3 capability, subjective latency/listening quality, real-tablet stress and ergonomics;
5. zero repeatable P0/P1 plus explicit user M7 PASS/CLOSED.

## M8 — Release hardening
### M8.A — Compatibility + edge cases — DIGITAL SCOPE COVERED
Objective coverage now includes:
- exact-source CI snapshots;
- legacy/current project migration fixtures;
- malformed/truncated/ambiguous package rejection and rollback;
- project duplication with transactional managed-media copying;
- editing-domain/fade/frame-overflow validation;
- broad deterministic SRC and master-render regression;
- cancellation-safe import/split/take publication;
- interrupted import and interrupted recording recovery;
- source/proxy/waveform orphan classification and safe-derived cleanup;
- safer user-destination publication.

### M8.B — UX + accessibility + performance — DIGITAL SCOPE COVERED / PHYSICAL RESIDUAL
Objective coverage now includes:
- Mixer accessibility semantics/callbacks through Android Compose instrumentation;
- `ActivityScenario.recreate()` route restoration;
- Small/Medium/Large structural and offline-render performance evidence;
- deterministic large fuzz/project stress.

Still physical by nature:
- real tablet tactile/visual ergonomics;
- real USB realtime route performance;
- subjective monitoring/latency/listening.

### M8.C — Release Candidate — NEXT
After the current automated gates remain green:
1. cut one final exact RC identity/version;
2. run the canonical dual automated gate;
3. explicitly trigger signing only after both gates pass;
4. deliver that single signed RC for the residual physical checklist;
5. on explicit PASS, close M7/M8 release hardening and prepare merge/release actions separately. Do not merge PR #1 automatically.

## Gate discipline
`.github/workflows/android-ci.yml` is canonical. A signed homologation APK is forbidden unless both `software-gate` and `android-integration-gate` succeed for that exact workflow run. Documentation must distinguish JVM/CI evidence, emulator evidence and target-hardware evidence.
