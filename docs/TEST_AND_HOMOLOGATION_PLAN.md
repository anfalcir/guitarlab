# Test and Homologation Plan

Updated: 2026-09-14

## Active candidate
The active source candidate is `0.5.0-rc3` / versionCode `23` on `main`. Its exact validated source SHA is not predeclared: it is the `github.sha` of the manually dispatched canonical workflow run that passes all mandatory gates.

The latest previously established full hosted baseline is `0.4.0-rc2` / CI #593, which passed software, API 36 full instrumented regression, isolated 1920×1200 target geometry and signed homologation assembly. RC1/RC2 introduced later recording/practice/UI deltas; RC3 consolidates their final loop/section/punch refinements and therefore requires a new exact-source manual workflow before any digital PASS claim.

## RC3-specific automated coverage
Deterministic coverage includes:
- loop playback start before, inside, exactly at the end of and after an active loop;
- confinement of visible playback position to the active loop while recording-position semantics remain untouched;
- ordinary non-loop project-end restart behavior;
- recording intent resolved at REC time rather than from persisted punch state;
- current-playhead, from-project-start and loop-punch plans;
- rejection of punch without a valid active loop;
- punch pre-roll/post-roll/latency calculations;
- section detection preview using the same boundaries as persistence;
- cancel/discard preview as non-persistent state;
- clear-sections semantics.

Existing global suites continue to cover persistence, history, managed media, codec, SRC, waveform, recording transactionality, active takes, mixer/audio math, export, recovery, lifecycle, accessibility, geometry and performance/stress invariants.

## Canonical gate execution
Ordinary commits do not start hosted CI. GitHub Actions is intentionally `workflow_dispatch` only. The user manually starts `.github/workflows/android-ci.yml` after source/documentation consolidation.

For the active RC3 homologation run, dispatch with `signed_homologation=true`.

### 1. Software gate
Required checks:
1. checkout of the exact candidate SHA;
2. `git diff --check HEAD^ --`;
3. source materialization;
4. unit/JVM tests;
5. reproducible performance evidence extraction;
6. Android Lint;
7. debug APK assembly;
8. diagnostics/reports/exact-source snapshot upload.

Any failure blocks candidate promotion.

### 2. Android integration gate
Required checks on Android API 36 emulator:
1. Android instrumentation compilation;
2. complete standard connected Android regression;
3. lifecycle/activity recreation behavior;
4. Compose accessibility/callback behavior already in the suite;
5. Android codec/export integration checks;
6. isolated 1920×1200 landscape target-geometry execution.

Any failure blocks signing.

### 3. Signed homologation gate
The signed job has `needs: [software-gate, android-integration-gate]` and runs only when `signed_homologation=true`.

It must:
- restore signing material only inside the runner;
- build the release APK;
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
Do not ask the user to manually re-prove:
- deterministic model/editor/history behavior;
- loop boundary normalization math;
- punch planning math;
- section preview/application equivalence;
- project/package/file validation or path traversal;
- source/proxy/media rollback invariants;
- SRC ratios/pitch/RMS/channel math;
- sample-domain master/fade/crossfade behavior;
- lifecycle recreation already covered by instrumentation;
- generic accessibility semantics already covered by Compose instrumentation;
- structural large-project save/bundle/render behavior.

## What remains physical
After exact RC3 automated PASS, only target-device facts remain:
- Samsung SM-X230 + M-VAVE MK-300 real USB input/output route;
- real guitar recording isolation and reconnect behavior;
- live waveform visibility during a real take;
- real-tablet loop/section/REC-choice ergonomics;
- subjective latency/feel and listening for pops/dropouts/artifacts;
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
- RC1/RC2 evidence remains useful for regression history but does not substitute for an exact RC3 run because RC3 changes transport/practice/recording-source behavior.
