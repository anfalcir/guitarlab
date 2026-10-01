# Current State — GuitarLab Studio

Updated: 2026-10-01

## Status

**RC22 DUAL-OUTPUT/CUE FEATURE CANDIDATE — MIXER 48DP CLEARANCE HARDENED / REQUALIFICATION REQUESTED**

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
- final protected payload blob: `7839c931d6a3ca894a1c58fc58fecb3db3391d9b`;
- final RC22 materializer blob: `14e79d48507640b0e1015e6556b7eca6ee252fbb`;
- the 22 terminal source/test blobs were cross-checked against the branch source graph with zero mismatches before qualification;
- low-latency/Bluetooth eligibility hardening is sealed in the RC22 materializer;
- initial qualification CI #913 / run `36865931256`: **FAIL** because `StudioViewModel.kt` referenced `cueOutputSignature` without resolving it inside backing playback during recording;
- corrective source and payload were applied; final hardening makes CUE transport-stable, non-blocking relative to MAIN and restricted to low-latency secondary routes. The exact hash-reconciled source is now under final branch qualification before integration. CI #923 isolated one API36 accessibility defect: expanded CUE/Arm touch targets overlapped in the Mixer; the row spacing was corrected and the RC22 payload/materializer were re-sealed to that fix.

- API36 #923 isolated one Mixer accessibility regression: expanded CUE/Arm touch targets overlapped; spacing was corrected and the RC22 payload/materializer re-locked before the next qualification.

- API36 #925 proved the 12 dp visual-gap fix was still insufficient because Compose minimum-touch expansion overlapped CUE/Arm. Mixer state controls now use explicit 48×48 dp clickable containers with compact visuals inside, eliminating framework-dependent hitbox overlap.

- API36 #925 confirmed the remaining failure was still Mixer CUE/Arm target geometry. RC22 now uses explicit 48x48dp semantic/click targets around the 38x30dp visuals, eliminating dependence on implicit Compose touch-target expansion; payload and materializer are re-locked.

- API36 #927 proved the 48x48dp control targets themselves are correct, but exposed a secondary layout regression: the taller control row clipped the per-track CLIP action vertically, so its callback did not fire. The Mixer dock height was increased to preserve the full meter/slider action surface while retaining explicit 48dp targets; payload and materializer were re-locked.

- API36 #928 confirmed the 48dp Mixer targets and vertical-space corrections, but the per-track CLIP callback still did not fire through the previous small Surface semantics path. Mixer CLIP now exposes an explicit deterministic accessibility OnClick action backed by the same real callback; the RC22 payload and materializer hashes were re-locked.

- API36 #929 still failed at the per-track CLIP callback (line 106), proving that an explicit semantics OnClick alone was insufficient. The temporary semantic-action duplication was removed and CLIP now uses a real 48x48dp clickable target with a compact 38x24dp visual, matching the robust target strategy used for Mixer state controls; payload and materializer hashes were re-locked.

- API36 #930 proved the per-track CLIP callback is now fixed and isolated the only remaining failure to CUE/Arm center spacing. Mixer track strips were widened from 232dp to 252dp and the four 48dp state controls now use 12dp spacing, so their centers have deliberate clearance instead of relying on near-threshold packing. The instrumented test now reports measured/minimum distances on failure; RC22 payload and materializer hashes were re-locked.

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
- synchronized CUE exposes only low-latency secondary routes; Bluetooth is intentionally excluded;
- MAIN and CUE must resolve to different physical Android endpoints;
- no ephemeral Android device ID is persisted; durable selection continues to use stable semantic signatures;
- if MAIN becomes automatic or CUE becomes unavailable/same-route, CUE is disabled/fails closed.

### Playback and recording

- normal Play renders independent MAIN and CUE buses from the same project timeline, opening the CUE sink only when currently playable content is routed there;
- backing playback during REC uses the same dual-bus engine;
- when software monitoring is enabled, the armed track's exclusive MAIN/CUE state selects the required monitor output;
- the recording writer remains input-only: playback/CUE audio never enters the recorded file;
- before audible CUE use, the engine silently primes both sinks, verifies distinct effective routes and requires stable AudioTimestamp-derived presentation clocks; CUE is admitted only when the initial MAIN↔CUE presentation-origin offset is at most 12 ms;
- during playback it rechecks routing and monitors presented-frame drift with a musically tighter guard (750 frames at 48 kHz, about 15.6 ms; 689 frames at 44.1 kHz); three consecutive violations suppress CUE;
- CUE writes are non-blocking relative to MAIN; a partial/zero/error secondary write is treated as backpressure and suppresses CUE instead of stalling the primary render loop;
- if CUE is rejected, disconnected, converges with MAIN, lacks stable clock evidence, begins more than 12 ms offset, cannot keep up or crosses the continuous drift guard, only CUE is suppressed and the user receives explicit feedback. MAIN is never delayed to rescue an unsafe CUE route.

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

## Qualification chronology

- CI #913 / run `36865931256`: FAIL — recording backing path referenced an unresolved CUE signature; source corrected.
- CI #914–#917: superseded/cancelled by newer hardened source/materializer revisions.
- CI #918 / run `36873594654`: FAIL-CLOSED in RC22 materializer guard because the new content-aware routing test file was referenced before being loaded by `verify()`; runtime source was not the cause. Guard corrected without changing runtime bytes.
- CI #919 / run `36873872006`: FAIL-CLOSED in a copy guard that incorrectly required a quote immediately before the Bluetooth sentence; routing behavior/runtime bytes were unchanged. Guard made copy-stable.
- CI #920 / run `36874120837`: FAIL at Kotlin compile because content-aware CUE admission had been inserted into two auxiliary playback paths before their local `tracks/clips` context was defined; the current HEAD moves those calculations into the correct take/playback scopes and removes the unresolved symbols.
- After the #920 corrective, the RC22 payload was rebuilt from canonical `main` to the exact current 22-file source set; materializer terminal hashes and `PATCH_BLOB` were cross-checked with zero mismatches before this rerun.
- CI #922 / run `36876106290`: the pre-clock-offset hardening graph passed Unit tests, Android Lint and APK assembly; API36 was still running when the final clock-sync gap was identified. Because runtime bytes changed afterward, #922 is supporting evidence only and cannot qualify the final RC22 candidate.
- Final hardening adds stable dual-AudioTimestamp preflight, a 12 ms initial-offset admission limit, tighter continuous drift bounds and a longer silent probe; these bytes require a fresh exact-source qualification.

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
