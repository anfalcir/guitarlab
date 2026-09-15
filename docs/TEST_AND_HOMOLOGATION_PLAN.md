# Test and Homologation Plan

Updated: 2026-09-14

## Active candidate
The active source candidate is `0.5.0-rc3` / versionCode `23` on `main`. Its exact validated source SHA is not predeclared: it is the `github.sha` of the manually dispatched canonical workflow run that passes all mandatory gates.

The latest fully green hosted baseline is CI #615 at `74bf86efbec94d249c4968c3284bf1985cd66b44`. It passed software, full API 36 regression, isolated 1920×1200 target geometry and signed homologation. Signed APK SHA-256: `d443cb33a010d2d21dad43e9ff12554d7ada4f2bb783be0c80e3ac11bfe2d6c9`.

Current source advanced after #615 through the H0–H6 physical editing/recording hardening program. Therefore #615 is a baseline, not proof for the current HEAD; the current source requires one new exact-source manual workflow before promotion.

## RC3 automated coverage
### Transport/practice/UI
Deterministic and instrumented coverage includes:
- loop playback start before/inside/at-end/after active loop;
- natural non-loop completion → STOP + 00:00;
- natural looped user-Play completion at `L▶` → STOP + `L◀`;
- live playhead seek during Play and active-loop clamp;
- transient REC intent and punch pre/post-roll policy;
- Auto-section preview/application equivalence, edge normalization and project-end clipping;
- fixed `Auto seções` → `Aplicar` + red `X` slot geometry;
- shared Home/Studio guide entry;
- 3-second centered countdown overlay semantics/geometry;
- track-function assignment/conflict policy.

### H0–H3 editing/clip transaction hardening
Required deterministic/instrumented coverage includes:
- trim pointer/frame mapping and source/timeline clamps;
- independent start/end trim-handle acquisition/semantics on target geometry;
- split siblings sharing recording-take lineage;
- moving one split child without invalidating siblings;
- deleting non-canonical and canonical split children;
- deterministic canonical-sibling promotion;
- save/reopen after split → move/delete;
- Undo/Redo around trim, move and delete;
- drag intent resolution: `Move`, `Delete`, `NoOp`;
- drag-to-trash confirmation and cancel path;
- stale/invalid drag must not partially mutate project/history/media state.

### H4 recording synchronization hardening
Coverage must distinguish and independently control:
- capture startup time;
- backing presentation startup time;
- accepted route latency/calibration;
- punch/pre-roll offsets.

Assertions:
- per-session startup skew is measured in frame/time domain;
- route latency and startup skew are combined exactly once;
- no hard-coded ~0.5 s constant is used;
- no-calibration fallback remains deterministic;
- zero-time and non-zero playhead placement stay in valid bounds;
- loop punch and normal recording do not double-compensate.

### H5 live-waveform hardening
Coverage must prove:
- envelope timebase follows captured frames, not callback count;
- variable callback chunk sizes preserve exact covered-frame continuity;
- bounded compaction keeps memory bounded while preserving maximum transient;
- duplicate/non-advancing frame updates do not redefine time;
- clear/reset restarts frame coverage from zero;
- long-duration inputs remain monotonic and bounded.

## Source materialization integrity
Large RC3/H0–H6 refinements are represented by versioned patches under `.source-parts` and materialized before every build/test gate.

The materializer must be:
- **serial**: H1 trim → H2 lineage/delete → H3 drag → H4 timing → H5 waveform → H6 integrated regression/guide/final test fix;
- **idempotence-oriented**: a second run recognizes already materialized changes instead of duplicating them;
- **fail-fast**: a patch that is neither cleanly applicable nor already applied fails the gate;
- **reproducible**: output is deterministic for the same repository SHA.

`H6WaveformTestFix.patch` is part of the canonical chain and corrects the final test-review assertion without changing production waveform behavior.

## In-app help synchronization gate
`StudioUserGuideDialog.kt` is product documentation. Any user-visible change to controls, labels, trim, clip deletion, drag behavior, transport/loop, sections, recording, comparison/mixer, import/export or options must review/update the guide in the same logical change set.

Before RC promotion, source review must confirm labels match current UI wording, behavior matches implementation, novice-relevant workflow is represented concisely and obsolete behavior is removed.

## Latest hosted evidence — CI #615
CI #615 at `74bf86efbec94d249c4968c3284bf1985cd66b44` passed:
- unit/JVM regression;
- performance evidence;
- Android Lint;
- debug/release assembly;
- full API 36 instrumentation;
- isolated 1920×1200 target geometry;
- signed homologation and source/package/version identity;
- APK Signature Scheme v2 and locked certificate verification.

Signed APK SHA-256: `d443cb33a010d2d21dad43e9ff12554d7ada4f2bb783be0c80e3ac11bfe2d6c9`.

The current H0–H6 source is newer and must receive its own run.

## Canonical gate execution
Ordinary commits do not start hosted CI. GitHub Actions remains `workflow_dispatch` only. The user manually starts `.github/workflows/android-ci.yml` after source/documentation consolidation.

For the active RC3 homologation run, dispatch with `signed_homologation=true`.

### 1. Software gate
Required checks:
1. checkout exact candidate SHA;
2. `git diff --check HEAD^ --`;
3. complete serial source materialization;
4. all unit/JVM tests, including H0/H2/H4/H5/H6 suites;
5. reproducible performance evidence;
6. Android Lint;
7. debug APK assembly;
8. unsigned release assembly from the same warm source tree;
9. checksum/identity/source snapshot and diagnostics upload.

Any failure blocks promotion.

### 2. Android integration gate
Required API 36 checks:
1. complete connected Android regression;
2. lifecycle/activity recreation;
3. accessibility/callback suite;
4. Auto-sections/guide/countdown/REC modal coverage;
5. new physical-editing instrumentation for trim handles and clip-delete interaction;
6. codec/export integration;
7. isolated 1920×1200 landscape target-geometry execution.

Any failure blocks signing.

### 3. Signed homologation gate
The signed job depends on both software and Android integration gates and runs only when signed homologation is requested.

It must download the exact unsigned artifact produced by software-gate, verify source/package/version/checksum, restore signing material only inside the runner, align/sign the same binary, verify with `apksigner`, compare certificate SHA-256 with `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`, generate `SHA256SUMS.txt` + `BUILD_IDENTITY.txt`, upload the signed artifact and destroy restored signing material.

A successful APK from another SHA/version/signer is not the active candidate.

## Post-run artifact verification
Before physical homologation:
1. all three workflow jobs green;
2. workflow head SHA equals intended final `main` HEAD;
3. signed artifact contains APK, `SHA256SUMS.txt`, `BUILD_IDENTITY.txt`;
4. recomputed APK SHA-256 equals manifest;
5. build identity reports `0.5.0-rc3`, versionCode `23` and exact workflow SHA;
6. signer/package/version verified independently when tooling is available.

## What belongs in automation
Do not delegate back to the user deterministic project/editor/history behavior, take-lineage reconciliation, drag-intent policy, recording-compensation math, live-waveform frame accounting/compaction, project/package validation, source/proxy ownership, SRC/audio math, lifecycle recreation, generic accessibility semantics or structural stress behavior when objectively covered by the automated matrix.

## What remains physical
After exact H0–H6 automated PASS, only target-device facts remain:
- real tablet acquisition/feel of both trim handles;
- split → move/delete/trash gesture workflow on real touch hardware;
- Samsung SM-X230 + M-VAVE MK-300 real USB input/output routing and fail-closed isolation;
- guitar-against-backing synchronization/subjective alignment;
- multi-minute live-waveform visual stability;
- listening for pops/dropouts/monitoring feel;
- target MP3 capability when available;
- one representative real-device export/stress smoke.

Use `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`; older alpha/RC checklists are historical evidence only.

## Severity and closure
- **P0** — data loss/corruption, invalid overwrite, broken committed media/project: blocks.
- **P1** — repeatable major workflow/output/recording failure: blocks.
- **P2** — relevant edge degradation: fix before release when objectively actionable.
- **P3** — cosmetic/maintainability/subjective ergonomic issue: fix when low-risk or explicitly accept with rationale.

M7/M8 release hardening closes only after zero repeatable P0/P1, exact-source automated dual-gate PASS, verified signed identity, residual target-device PASS and explicit user approval.

## Historical evidence
- `0.4.0-rc2` / CI #593: historical full software/API36/geometry/signing baseline.
- CI #613: earlier full RC3 baseline at `db5a4208848e4b6ca2163ce715d0c5bb464cfe37`.
- CI #614: test-API compatibility failure; signing correctly blocked.
- CI #615: latest fully green signed pre-H0–H6 baseline at `74bf86efbec94d249c4968c3284bf1985cd66b44`; signed APK SHA-256 `d443cb33a010d2d21dad43e9ff12554d7ada4f2bb783be0c80e3ac11bfe2d6c9`.
