# Current State — GuitarLab Studio

Updated: 2026-09-14

## Active candidate
- Canonical repository/branch: `anfalcir/guitarlab` / `main`.
- Candidate prepared for the next manual gate: `0.5.0-rc3`, versionCode `23`.
- Signed APK name after a successful canonical workflow: `GuitarLabStudio-0.5.0-rc3-homologacao.apk`.
- Locked homologation signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Exact validated source SHA and APK SHA-256 are recorded only after the manually dispatched workflow succeeds for the final `main` HEAD.

## Milestone state
- M2 through M6: PASS/CLOSED.
- M7/M8 digital baseline: previously PASS in signed `0.4.0-rc2` / CI #593.
- `0.5.0-rc1`: recording/practice hardening implemented and signed; residual physical gate remained open.
- `0.5.0-rc2`: timeline/practice visual integration refinement.
- `0.5.0-rc3`: final loop/section/punch/transport and Studio-clarity refinement prepared; canonical manual dual-gate + signing run is PENDING.

## RC3 scope
### Loop playback and natural completion
- with Loop active, explicit Play starts only inside `[loopStart, loopEnd)`;
- playhead before the loop, at loop end or after the loop normalizes to loop start;
- explicit user Play is one bounded pass when Loop is active: `L▶` is its natural end;
- natural looped completion stops and returns the playhead to `L◀`;
- natural non-loop completion stops and returns the playhead to project start;
- recording/backing playback retains repeating-loop behavior where required by punch semantics.

### Live playhead seek
- playhead drag is allowed while ordinary Play is active;
- the audio engine performs an in-session seek instead of requiring Stop → reposition → Play;
- rapid drag updates coalesce to the most recent requested frame;
- active Loop clamps live seek inside `[L◀, L▶)`;
- countdown, recording and finalization reject playhead movement;
- loop markers and structural edits remain stopped-only.

### Studio organization and feedback
- Comparação and Timeline controls are positioned immediately below the workspace/timeline and before the Mixer;
- comparison badges use vivid neon `ATIVA`/`OCULTA` semantics;
- `Auto seções` replaces the older `Detectar seções` label;
- `Criar seção do loop` is disabled while Loop is off and enabled only with Loop active;
- armed tracks receive a red visual state in both the track list and timeline lane, in addition to the Mixer REC control.

### Track functions
- creating a new track can offer meaningful workflow functions that are still unassigned;
- the user may keep the track generic or choose one of the available suggestions;
- `Configurar pista` can add, change or remove the track function in the existing function area;
- one central assignment policy prevents structural function duplicates/incompatible combinations while preserving valid L/R pairs and repeatable generic instrument roles;
- edit-time policy prevents new conflicts without turning old project files into invalid/unsavable data.

### In-app help
- the top bar exposes `Ajuda`, opening a concise novice-facing guide for Studio operation;
- `USER_GUIDE_POLICY.md` makes guide synchronization mandatory whenever visible controls, wording or workflows change.

### Sections
- automatic detection produces a visible, non-persistent timeline preview before acceptance;
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

## RC3 regression coverage
Deterministic transport/practice tests cover:
- loop playback start before/inside/at-end/after the loop;
- natural playback end/reset for normal and looped Play;
- live seek below/inside/at/above an active loop;
- playback cursor confinement without altering recording-position semantics;
- transient recording modes and invalid-loop rejection;
- section preview/application boundary equivalence;
- clear-sections behavior;
- track-function availability, assignment and conflict handling.

Compose instrumentation covers the transient Loop + REC choice, `Auto seções`, the Loop-dependent enabled state of `Criar seção do loop`, and Cancel preserving loop intent. The test setup was hardened after CI #596 so the loop precondition is established through the same activity-scoped Studio ViewModel command rather than depending on toolbar timing in an empty project.

## Latest CI evidence
Manual CI #596 ran against source `1bc6652dcdbc580badb3e7aea0c416ba4d06ecce`.

Established by #596:
- unit/JVM tests: PASS;
- reproducible performance evidence: PASS;
- Android Lint: PASS;
- debug APK assembly: PASS;
- Android instrumentation compilation: PASS;
- API 36 runtime regression: 8/9 tests PASS.

The sole API 36 failure was the newly added `PracticeWorkflowInstrumentedTest.loopRecShowsTransientChoiceAndCancelPreservesLoop`, which timed out waiting for the `Ativar loop` toolbar node in a zero-length blank-project scenario. The failure was test-fixture/timing-specific, not a broad API 36 or application crash: the other eight instrumented tests passed. That test has since been rewritten deterministically.

The active source has advanced after #596 with transport and Studio UX changes. Therefore #596 is useful partial evidence but **does not validate the current RC3 HEAD**. No signed homologation artifact from #596 is authoritative because the mandatory API 36 gate did not pass.

## Next authoritative digital gate
Ordinary commits remain `[skip ci]` and the hosted workflow remains manual-only. The next authoritative evidence must come from one explicit `.github/workflows/android-ci.yml` dispatch with `signed_homologation=true` against the final current `main` HEAD.

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
Ordinary commits do not trigger GitHub Actions. `.github/workflows/android-ci.yml` is manual-only. Source materialization is idempotent and fail-fast: RC3 large-source deltas are applied from versioned patches before tests/build, and any patch drift blocks the run rather than silently compiling a partial state. The signed job depends on both software and Android integration gates, so signing cannot bypass a failed mandatory gate. `CANDIDATE_IDENTITY_POLICY.md` defines the authoritative RC3 identity contract.

## Remaining physical gate after automated PASS
Only facts requiring the real Samsung SM-X230 + M-VAVE MK-300 remain:
- real MK-300 USB input/output route and fail-closed isolation;
- live waveform during a real take;
- verify recorded capture contains guitar only while backing plays;
- natural end → reset behavior and live playhead seek ergonomics on the real tablet;
- loop-bound live seeking and stop-at-`L▶`/return-to-`L◀` behavior;
- section/track-control readability and armed-lane visual clarity on the real tablet;
- transient REC choice and retained punch alignment;
- recording stop/restart, return-to-start, reopen and route reconnect;
- monitoring latency/feel, pops/dropouts and subjective audio quality;
- target MP3 availability/playability and one representative stress/export smoke.

The active residual checklist is `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`. After automated PASS, exact artifact verification and explicit physical approval, record final PASS/CLOSED directly on `main`.
