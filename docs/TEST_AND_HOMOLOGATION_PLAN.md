# Test and Homologation Plan

Updated: 2026-09-10

## Canonical automated gate
Every candidate commit on the active integration branch is evaluated by `.github/workflows/android-ci.yml`.

### 1. Software gate
Required checks:
1. `git diff --check` on the candidate delta;
2. source materialization;
3. unit/JVM tests;
4. reproducible performance evidence extraction;
5. Android Lint;
6. debug APK assembly;
7. report/diagnostic/source-snapshot artifacts.

### 2. Android integration gate
Required checks on Android API 36 emulator:
1. instrumented app/activity recreation behavior;
2. Compose Mixer accessibility/callback behavior;
3. Android FLAC export container + native extraction/decoding;
4. MP3 capability contract — success when encoder exists or controlled no-file failure when absent.

The API 36 AVD uses a pinned snapshot cache for repeatable, lower-cost execution.

A failure in either gate blocks signed-candidate promotion.

## Signed homologation gate
Only explicit `[sign-homologation]` intent or authorized manual dispatch may build a homologation release. The job has `needs: [software-gate, android-integration-gate]` and therefore cannot run after a failed required automated gate.

The signing job must:
- restore signing material only inside the runner;
- build release;
- verify APK signature with `apksigner`;
- compare signer SHA-256 against the locked certificate;
- generate `SHA256SUMS.txt` and `BUILD_IDENTITY.txt`;
- upload the homologation artifact;
- destroy restored signing material even after failure.

Expected certificate SHA-256 is maintained by the workflow and must match the established GuitarLab homologation identity.

## Milestone state
- M5: PASS/CLOSED (`0.2.0-alpha14`, explicit physical approval)
- M6: PASS/CLOSED (`0.3.0-alpha1`, explicit physical approval)
- M7: OPEN only for final residual physical homologation
- M8 digital hardening: automated scoped matrix covered; RC preparation follows final exact-gate pass.

Historical alpha checklists remain evidence only. The active residual physical checklist is `M7_ALPHA1_HOMOLOGATION_CHECKLIST.md` despite its historical filename.

## What belongs in automation
Do not ask the user to manually re-prove:
- deterministic model/editor/history behavior;
- file/package validation or path traversal;
- source/proxy/media rollback invariants;
- SRC ratios/pitch/RMS/channel math;
- sample-domain master/fade/crossfade behavior;
- lifecycle recreation already covered by instrumentation;
- generic accessibility semantics already covered by Compose instrumentation;
- FLAC container/header/extraction correctness;
- structural large-project save/bundle/render behavior.

## What remains physical
The final target-device pass is intentionally residual:
- Samsung SM-X230 + Pocket Amp real USB input/output route;
- real guitar recording/monitoring and reconnect behavior;
- subjective latency/feel;
- target-specific MP3 encoder availability/playability;
- listening for pops/dropouts/route/pitch/transition artifacts;
- real tablet large-session responsiveness and touch/layout ergonomics;
- concise restart/reopen/project/export smoke.

## Severity and closure
- **P0** — data loss/corruption, invalid overwrite, broken committed media/project: blocks.
- **P1** — repeatable major workflow/output/recording failure: blocks.
- **P2** — relevant edge degradation: fix before RC when objectively actionable.
- **P3** — cosmetic/maintainability/ergonomic issue: fix when low-risk; physical subjective items may remain in final checklist.

M7/M8 final release hardening closes only after zero repeatable P0/P1, exact automated dual-gate PASS, verified signed identity, residual target-device PASS and explicit user approval. PR merge is a separate action and is never implied by homologation.
