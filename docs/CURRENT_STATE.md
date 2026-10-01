# Current State — GuitarLab Studio

Updated: 2026-10-01

## Status

**RC22 DUAL-OUTPUT/CUE FEATURE CANDIDATE — IMPLEMENTED / DIGITAL REQUALIFICATION PENDING**

RC20 remains the currently accepted, physically homologated baseline recorded in `RELEASE_BASELINE.md`. RC22 is an explicit owner-requested successor candidate built on the RC21-integrated Android source. It adds per-track MAIN/CUE monitoring and therefore reopens only the affected Android playback/routing/monitoring paths plus adjacent persistence/UI coverage. The frozen Demucs backend and unrelated Drive transport evidence are unchanged.

RC21 reached signed digital-candidate state, but RC22 now supersedes it as the active Android candidate before RC21 promotion. RC20 remains the accepted baseline until an exact signed RC22 artifact passes the residual affected-path physical acceptance.

## RC22 candidate identity

- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc22`;
- versionCode: `42`;
- development branch: `feature/dual-output-cue-routing`;
- base canonical `main`: `ba2227bfb28afcc3d95602bcb622a05f27457141`;
- source materialization tail: `scripts/materialize_ci_sources_rc22.py`;
- protected payload: `.source-parts/RC22DualOutputCueRouting.patch`;
- initial qualification CI #913 / run `36865931256`: **FAIL** because `StudioViewModel.kt` referenced `cueOutputSignature` without resolving it inside backing playback during recording;
- corrective source and payload have been applied and hash-reconciled; the full corrected qualification rerun is requested before integration.

## RC22 feature scope

### Per-track routing

- every track persists `TrackOutputRoute` with backward-compatible default `MAIN`;
- engine contract supports `MAIN`, `CUE` and `MAIN_AND_CUE`;
- the first mixer headphone button intentionally toggles exclusive MAIN ↔ CUE;
- route changes are stopped-state operations; the CUE control is disabled during Play/REC so an active session cannot acquire inconsistent monitor routing;
- projects created before RC22 reopen with every pre-existing track routed to MAIN;
- CUE is monitoring/playback metadata and does not destructively modify media.

### Output configuration

- Options exposes recording input, MAIN output and optional secondary/CUE output;
- CUE is explicit-only and requires an explicit MAIN selection;
- MAIN and CUE must resolve to different physical Android endpoints;
- no ephemeral Android device ID is persisted; durable selection continues to use stable semantic signatures;
- if MAIN becomes automatic or CUE becomes unavailable/same-route, CUE is disabled/fails closed.

### Playback and recording

- normal Play renders independent MAIN and CUE buses from the same project timeline;
- backing playback during REC uses the same dual-bus engine;
- when software monitoring is enabled, the armed track's exclusive MAIN/CUE state selects the required monitor output;
- the recording writer remains input-only: playback/CUE audio never enters the recorded file;
- before audible CUE use, the engine silently primes and verifies distinct effective routes;
- during playback it rechecks routing and monitors presented-frame drift;
- CUE writes are non-blocking relative to MAIN; a partial/zero/error secondary write is treated as backpressure and suppresses CUE instead of stalling the primary render loop;
- if CUE is rejected, disconnected, converges with MAIN, cannot keep up or crosses the bounded drift guard, only CUE is suppressed and the user receives explicit feedback.

### Mixer, diagnostics and help

- each track strip has an accessible headphone/CUE state control next to Mute/Solo/Arm;
- touch-target separation is covered by instrumentation;
- Settings and Diagnostics expose CUE selection/health using sanitized semantic identities;
- diagnostic bundles include selected/effective CUE state without raw transient route identifiers;
- the shared Home/Studio in-app guide explains MAIN/CUE behavior and fail-closed semantics.

### Export and persistence

- MAIN/CUE routing is stored in project state and survives save/reopen/backup;
- project compatibility tests prove legacy default MAIN and CUE round-trip;
- live CUE routing is not an export-exclusion flag: Studio Master continues to render the project mix/timeline independently of physical monitoring assignment.

## Qualification required before merge/promotion

Digital qualification must pass on the exact corrected RC22 source:

1. deterministic RC22 materialization and reverse-apply/idempotence;
2. JVM/unit tests including routing and project compatibility;
3. Android Lint;
4. debug/release assembly and unsigned candidate staging;
5. API 36 representative instrumented regression, including Mixer accessibility;
6. canonical-main integration only after the branch qualification is green;
7. signed exact-artifact qualification from canonical `main`.

Because this feature changes simultaneous physical audio routing, final support for a concrete MAIN+CUE device combination additionally requires residual physical validation on the exact signed RC22 APK. CI cannot prove Samsung/Android HAL behavior for two physical outputs.

## Accepted baseline until RC22 promotion

RC20 remains immutable and accepted. Its signed APK hash, signer, producer SHA, physical acceptance and frozen production worker digest remain exactly as recorded in `RELEASE_BASELINE.md`.
