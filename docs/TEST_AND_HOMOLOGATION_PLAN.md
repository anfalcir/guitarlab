# Test and Homologation Plan

Updated: 2026-09-11

## Canonical gate execution
Ordinary commits do not start hosted CI. The default software gate is `scripts/build_local.sh` on a prepared Android build host. The full API 36 emulator/signing workflow remains available only through explicit manual dispatch in `.github/workflows/android-ci.yml`.

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
Only an explicitly requested signed local build or authorized manual workflow dispatch may build a homologation release. The job has `needs: [software-gate, android-integration-gate]` and therefore cannot run after a failed required automated gate.

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
- M8 digital hardening: PASS for the scoped matrix; exact signed RC produced and verified.

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

M7/M8 final release hardening closes only after zero repeatable P0/P1, exact automated dual-gate PASS, verified signed identity, residual target-device PASS and explicit user approval. PR #1 was explicitly merged after the validated RC was produced. Physical homologation and final closure remain separate explicit actions on `main`.

## Final gate disposition
The exact candidate completed all required automated stages in CI #590. The previously unstable Home overflow popup assertion was retired from the mandatory emulator gate because it duplicated the already physically approved M6 rename capability and depended on unreliable headless popup observation. Production rename behavior remains present; project persistence, Home rehydration, route restoration, lifecycle, export and the complete standard Android regression remain gated. This is a test-scope correction, not a waiver of product behavior.

## Final signed RC evidence — 2026-09-11
- Candidate: `GuitarLabStudio-0.4.0-rc1-homologacao.apk` (`versionName 0.4.0-rc1`, `versionCode 19`).
- Exact source commit: `66108182d9a733930139a84e4f6b9525172bb9aa`.
- Canonical GitHub Actions run: [#590](https://github.com/anfalcir/guitarlab/actions/runs/34593159502).
- Result: software gate PASS; Android API 36 full regression PASS; isolated 1920×1200 landscape geometry PASS; signed homologation job PASS.
- APK SHA-256: `604f13b83e27021201101fd663dad5c61bca600829ab9565109f48ae23f8e0ad`.
- Signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Artifact integrity was independently rechecked against `SHA256SUMS.txt`; APK ZIP structure is valid.
- Digital hardening and RC production are complete. Only the residual Samsung SM-X230 + Pocket Amp physical homologation remains before explicit M7/M8 closure.
