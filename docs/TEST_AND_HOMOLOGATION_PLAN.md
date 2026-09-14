# Test and Homologation Plan

Updated: 2026-09-14

## Active candidate
The active source candidate is `0.5.0-rc3` / versionCode `23` on `main`. Its exact validated source SHA is not predeclared: it is the `github.sha` of the manually dispatched canonical workflow run that passes all mandatory gates.

The latest fully green hosted baseline is `0.5.0-rc3` / CI #613 at `db5a4208848e4b6ca2163ce715d0c5bb464cfe37`, which passed software, full API 36 regression, isolated 1920×1200 target geometry and signed homologation. Current source advanced after that run with physical-review UX corrections, so those new deltas require one new exact-source manual workflow before promotion.

## RC3-specific automated coverage
Deterministic coverage includes:
- loop playback start before, inside, exactly at the end of and after an active loop;
- confinement of visible playback position to the active loop while recording-position semantics remain untouched;
- natural non-loop completion → STOP + playhead 00:00;
- natural looped user-Play completion at `L▶` → STOP + playhead `L◀`;
- live-playhead seek during Play, including lower/upper clamp to `[L◀, L▶)` when Loop is active;
- recording intent resolved at REC time rather than from persisted punch state;
- current-playhead, from-project-start and loop-punch plans;
- rejection of punch without a valid active loop;
- punch pre-roll/post-roll/latency calculations;
- section detection preview using the same boundaries as persistence;
- discard-preview as non-persistent state;
- clear-sections semantics;
- track-function availability, assignment/removal and structural conflict prevention;
- valid L/R function coexistence plus repeatable generic instrument functions;
- fixed-slot regression proving `Auto seções` and `Aplicar` + red `X` occupy the same geometry;
- Home `Ajuda` regression proving the same shared guide content opens outside Studio;
- 3-second countdown policy regression plus overlay semantics/large-geometry instrumentation;
- section edge-guard and project-end clipping invariants.

Existing global suites continue to cover persistence, history, managed media, codec, SRC, waveform, recording transactionality, active takes, mixer/audio math, export, recovery, lifecycle, accessibility, geometry and performance/stress invariants.

## Source materialization integrity
Large RC3 Studio-source refinements are represented by versioned patches under `.source-parts` and materialized before every build/test gate. The materializer must be:
- idempotent: running it twice must not duplicate changes;
- fail-fast: a patch that is neither cleanly applicable nor already applied must fail the gate;
- reproducible: materialized output must match the intended ViewModel/workspace/timeline source exactly.

This is part of the software gate, not a manual homologation task.

## In-app help synchronization gate
`StudioUserGuideDialog.kt` is product documentation. Any user-visible change to controls, labels, track functions, transport/loop, sections, recording, comparison/mixer, import/export or options must review/update the guide in the same change set when relevant.

Before RC promotion, source review must confirm:
- guide labels match current UI wording;
- described behavior matches current implementation;
- new novice-relevant behavior is represented concisely;
- obsolete behavior is removed.

See `USER_GUIDE_POLICY.md`.

## Latest hosted evidence — CI #613
CI #613 ran against `db5a4208848e4b6ca2163ce715d0c5bb464cfe37` and passed the complete canonical matrix: unit/JVM, performance evidence, Lint, debug/release assembly, API 36 full instrumentation, isolated 1920×1200 geometry, signing/certificate and artifact identity. Signed APK SHA-256: `4b62d38c1caf3f449c94b4f9111263dacd87d8cf5e9116caa26245ecb716f341`.

That run is a valid baseline but does not automatically validate source added afterward. The section/layout/help/countdown delta introduced during the following physical review must receive its own manual run.

## Canonical gate execution
Ordinary commits do not start hosted CI. GitHub Actions is intentionally `workflow_dispatch` only. The user manually starts `.github/workflows/android-ci.yml` after source/documentation consolidation.

For the active RC3 homologation run, dispatch with `signed_homologation=true`.

### 1. Software gate
Required checks:
1. checkout of the exact candidate SHA;
2. `git diff --check HEAD^ --`;
3. source materialization, including fail-fast RC3 source patches;
4. unit/JVM tests;
5. reproducible performance evidence extraction;
6. Android Lint;
7. debug APK assembly;
8. when homologation is requested, compile the unsigned release once from the same warm source tree and publish its checksum/identity;
9. diagnostics/reports/exact-source snapshot upload.

Any failure blocks candidate promotion.

### 2. Android integration gate
Required checks on Android API 36 emulator:
1. complete standard connected Android regression;
2. lifecycle/activity recreation behavior;
3. Compose accessibility/callback behavior already in the suite;
4. `Auto seções` fixed-slot preview actions and Loop-dependent create-section state;
5. shared Home/Studio guide entry behavior;
6. 3-second REC countdown overlay semantics/geometry;
7. transient Loop + REC modal regression;
8. Android codec/export integration checks;
9. isolated 1920×1200 landscape target-geometry execution.

Any failure blocks signing.

### 3. Signed homologation gate
The signed job has `needs: [software-gate, android-integration-gate]` and runs only when `signed_homologation=true`.

It must:
- download the exact unsigned release artifact produced by `software-gate` rather than recompiling source;
- verify unsigned artifact checksum/source/package/version identity against the same `github.sha`;
- restore signing material only inside the runner;
- align/sign that exact staged APK;
- verify the APK with `apksigner`;
- compare signer SHA-256 against `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- package `GuitarLabStudio-0.5.0-rc3-homologacao.apk`;
- generate `SHA256SUMS.txt`;
- generate `BUILD_IDENTITY.txt` containing the same `github.sha`, versionName and versionCode;
- upload the signed artifact;
- destroy restored signing material even after failure.

A successful APK from another SHA/version/signer is not the active candidate.

## Post-run artifact verification
Before sending RC3 to physical homologation:
1. confirm all three workflow jobs are green;
2. confirm workflow head SHA equals the intended final `main` HEAD;
3. download the signed homologation artifact;
4. verify artifact contains APK, `SHA256SUMS.txt` and `BUILD_IDENTITY.txt`;
5. recompute APK SHA-256 and compare with `SHA256SUMS.txt`;
6. confirm `BUILD_IDENTITY.txt` reports `0.5.0-rc3`, versionCode `23` and the exact workflow SHA;
7. independently inspect APK signer and package/version identity when tooling is available.

Only after this verification does the residual physical checklist become active.

## What belongs in automation
Do not ask the user to manually re-prove deterministic model/editor/history behavior, loop/seek/punch math, section preview/application normalization, fixed Auto-sections slot geometry, shared guide routing, countdown timing/overlay geometry, track-function conflict rules, project/package/file validation, source/proxy rollback, SRC/audio math, lifecycle recreation, generic accessibility semantics or structural stress behavior when those are objectively covered by the automated matrix.

## What remains physical
After exact RC3 automated PASS, only target-device facts remain:
- Samsung SM-X230 + M-VAVE MK-300 real USB input/output route;
- real guitar recording isolation and reconnect behavior;
- live waveform visibility during a real take;
- real-tablet natural-end/reset and live-seek ergonomics, including loop-bounded seek behavior;
- real-tablet readability/ergonomics of comparison, sections, armed-lane feedback and countdown overlay;
- transient REC-choice/punch alignment;
- subjective latency/feel and listening for pops/dropouts/artifacts during normal play and live seek;
- target-specific MP3 encoder availability/playability;
- representative real-device stress/export smoke.

Use `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`; older alpha/RC checklists are historical evidence only.

## Severity and closure
- **P0** — data loss/corruption, invalid overwrite, broken committed media/project: blocks.
- **P1** — repeatable major workflow/output/recording failure: blocks.
- **P2** — relevant edge degradation: fix before release when objectively actionable.
- **P3** — cosmetic/maintainability/subjective ergonomic issue: fix when low-risk or explicitly accept with rationale.

M7/M8 release hardening closes only after zero repeatable P0/P1, exact-source automated dual-gate PASS, verified signed identity, residual target-device PASS and explicit user approval.

## Historical evidence
- `0.4.0-rc2`, versionCode 20 — CI #593: software + API 36 + isolated target geometry + signed homologation PASS; APK SHA-256 `b067558e4ef6793df206d3e966faa029aa952b446ee63c8e47070a481297df85`; signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- CI #596 — historical partial RC3 evidence (8/9 API 36; deterministic fixture timeout; signing correctly blocked).
- CI #613 — full RC3 baseline PASS at `db5a4208848e4b6ca2163ce715d0c5bb464cfe37`, including API 36, 1920×1200 geometry and signed homologation; signed APK SHA-256 `4b62d38c1caf3f449c94b4f9111263dacd87d8cf5e9116caa26245ecb716f341`.
- RC1/RC2 evidence remains useful for regression history but does not substitute for an exact RC3 run because RC3 changes transport/practice/recording-source/Studio-UX behavior.
