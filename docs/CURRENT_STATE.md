# Current State — GuitarLab Studio

Updated: 2026-09-14

## Active candidate
- Canonical repository/branch: `anfalcir/guitarlab` / `main`.
- Candidate prepared for the next manual gate: `0.5.0-rc3`, versionCode `23`.
- Signed APK name after a successful canonical workflow: `GuitarLabStudio-0.5.0-rc3-homologacao.apk`.
- Locked homologation signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Exact source SHA and APK SHA-256 are intentionally recorded only after the manually dispatched workflow succeeds for the final `main` HEAD.

## Milestone state
- M2 through M6: PASS/CLOSED.
- M7/M8 digital baseline: previously PASS in signed `0.4.0-rc2` / CI #593.
- `0.5.0-rc1`: recording/practice hardening implemented and signed; residual physical gate remained open.
- `0.5.0-rc2`: timeline/practice visual integration refinement.
- `0.5.0-rc3`: final loop/section/punch workflow refinement prepared; canonical manual dual-gate + signing run is PENDING.

## RC3 scope
### Loop playback
- with Loop active, explicit Play starts only inside `[loopStart, loopEnd)`;
- playhead before the loop, at loop end or after the loop normalizes to loop start;
- playback position callbacks are kept inside the loop interval;
- this is deliberately Play-only so punch recording may still use pre-roll before loop start.

### Sections
- automatic detection now produces a visible, non-persistent timeline preview before acceptance;
- preview and persisted application use the same normalized boundaries;
- preview can be cancelled without changing project sections;
- `Limpar seções` removes persisted sections and pending preview without touching clips, markers or loop bounds;
- section labels remain in the shared timeline header while loop markers preserve visual priority.

### Recording with Loop active
- the dedicated persistent punch-control group is removed;
- REC with Loop disabled uses ordinary current-playhead recording;
- REC with Loop enabled asks for `Somente o loop`, `Desde o início` or `Cancelar`;
- `Somente o loop` creates transient punch intent for that recording only, preserving pre-roll/post-roll and latency-aware retained-region behavior;
- `Desde o início` starts from 00:00 with loop disabled for that recording;
- legacy persisted `punchRegion` remains readable for file compatibility but no longer silently arms a future recording.

## Regression coverage added for RC3
- loop playback start before/inside/at-end/after the loop;
- playback cursor confinement without altering recording-position semantics;
- non-loop project-end restart behavior;
- transient recording modes and invalid-loop rejection;
- section preview/application boundary equivalence;
- clear-sections behavior.

## Validation status
The RC3 source has been reviewed for cross-layer consistency across transport policy, practice workflow policy, Studio shell, Studio ViewModel, timeline/section UI and the new regression tests. During the refactor review, an incorrect pair of track-mix draft references was detected and corrected before candidate promotion.

No claim is made yet that the exact RC3 HEAD has passed Gradle/JVM/Lint/Android emulator/signing gates. Ordinary commits are `[skip ci]` and the hosted workflow is manual-only by design. The next authoritative digital evidence must come from one explicit manual `.github/workflows/android-ci.yml` run with `signed_homologation=true` against the final RC3 HEAD.

That run must pass:
1. JVM/unit regression;
2. Android Lint;
3. debug APK assembly;
4. Android test compilation;
5. API 36 full instrumented regression;
6. isolated 1920×1200 target-geometry pass;
7. signed release assembly;
8. signer verification and APK checksum generation.

## CI and recovery policy
Ordinary commits do not trigger GitHub Actions. `.github/workflows/android-ci.yml` is manual-only. The signed job depends on both software and Android integration gates, so signing cannot bypass a failed mandatory gate. `CANDIDATE_IDENTITY_POLICY.md` defines the authoritative RC3 identity contract.

## Remaining physical gate after automated PASS
Only facts requiring the real Samsung SM-X230 + M-VAVE MK-300 remain:
- real MK-300 USB input/output route and fail-closed isolation;
- live waveform during a real take;
- verify recorded capture contains guitar only while backing plays;
- loop playback ergonomics on the real tablet;
- section preview/apply/cancel/clear ergonomics and readability;
- transient REC choice and retained punch alignment;
- recording stop/restart, return-to-start, reopen and route reconnect;
- monitoring latency/feel, pops/dropouts and subjective audio quality;
- target MP3 availability/playability and one representative stress/export smoke.

The active residual checklist is `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`. After automated PASS, exact artifact verification and explicit physical approval, record final PASS/CLOSED directly on `main`.
