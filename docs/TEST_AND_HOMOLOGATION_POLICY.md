# Test and Homologation Policy

Updated: 2026-10-02

## Purpose

This is the steady-state qualification policy for the frozen GuitarLab product. It replaces the completed RC20 campaign plan, which is archived under `history/TEST_AND_HOMOLOGATION_PLAN_RC20_FINAL_2026-09-25.md`.

The policy protects credible owner harm without turning historical gate volume into a permanent requirement for every future change.

## Protected outcomes

When applicable, qualification must fail closed for:

- project, recording or managed-media loss/corruption;
- structurally invalid or musically unusable prepared audio;
- broken primary acquisition → Prepare → Studio flow;
- persistence/recovery/backup/restore integrity failure;
- duplicate jobs, quota consumption or uncontrolled cost;
- credential/authorization failure;
- wrong APK package/version/provenance/signer;
- wrong worker digest/model/runtime identity;
- unsafe production cutover without rollback;
- audio routing/timing regressions that materially affect recording or playback.

Unrelated cosmetic/flaky evidence may be scoped only with explicit rationale. Protected failures above are never quarantined.

## Evidence selection

Qualification is proportional to the change.

### Documentation-only

No runtime CI is required when a commit changes only truthful documentation/history and does not mutate workflows, materializers, source payloads, configuration or shipped artifacts.

### Android UI/domain/persistence

Run the software gate and affected API 36 instrumented regression. Add target-device validation only when the change can invalidate a physical-only behavior.

### Playback/recording/audio timing

Run deterministic core/audio tests, Android integration and relevant API 36 coverage. Target SM-X230/MK-300 validation is required when route, timestamp, buffering, capture, monitoring, latency or audible/visual synchronization can materially change.

For dual-output/CUE changes, digital gates must cover backward-compatible project persistence, per-track routing policy, same-endpoint rejection, missing-route behavior, stable clock-anchor policy, the 12 ms initial presentation-offset guard, continuous drift bounds, non-blocking secondary backpressure behavior, Mixer accessibility and both Play/REC integration. Physical acceptance must then verify the actual intended MAIN + CUE hardware combination because Android/HAL routing and acoustic/device latency of two simultaneous physical outputs cannot be proven by emulator CI.

Never introduce a hidden fixed timing fudge merely to satisfy a visual observation; prefer device/sink timing evidence and bounded fallback.

### Remote separation/backend

Run U7 verification and the smallest representative shadow/production path required by the mutation. A new worker image is qualified by immutable digest. Production promotion must deploy the exact qualified digest without rebuild. Run U4 transactional evidence when lifecycle, recovery, quota, output publication, ACK/purge or cleanup can change.

### Drive backup

Repeat focused provider-real evidence when OAuth, Drive transport, resumable upload, catalog/commit identity, retention or restore behavior changes. Unrelated Android/backend changes do not invalidate retained Drive evidence.

A catalog/cache-only maintenance change does not automatically require replaying the entire destructive U8m campaign when OAuth, resumable transport, commit publication and restore store semantics are unchanged. In that case, retain U8m as transport/store authority and add focused real-provider evidence for the changed catalog/background orchestration, preferably as a representative target-device backup → catalog refresh → restore smoke.

### Signing/release candidate

A promoted Android candidate must satisfy the build-once/sign-exactly contract in `CANDIDATE_IDENTITY_POLICY.md`. Physical acceptance, when required, occurs on the exact signed APK intended for promotion.

## Android baseline gates

The canonical Android pipeline may include broad coverage, but the required outcome is:

1. deterministic source materialization;
2. relevant JVM/unit/domain tests;
3. Android Lint;
4. release assembly and exact unsigned provenance;
5. API 36 representative regression;
6. exact-artifact signing only when a signed candidate is intentionally requested.

## Cloud baseline gates

The frozen RC20 backend evidence uses the official Demucs/PyTorch CPU8 route. Future backend changes must preserve exact engine/model/config identity, structural/finiteness/quality rejection and transactional safety. Stochastic Demucs runs are not required to be sample-identical; hard invariants remain blocking and numeric/hash differences may remain diagnostic under D-091.

## Physical evidence

Physical testing is residual evidence, not a duplicate of CI. Use it for facts CI cannot establish reliably, such as:

- actual USB input/output routing;
- hardware loopback/isolation;
- audible latency/alignment;
- real recording placement;
- reconnect/interruption behavior;
- target-only codec availability;
- target-device ergonomics when changed;
- representative musical usefulness when a technical gate cannot prove it.

The reference hardware is Samsung SM-X230 / Android 16 / API 36 and M-VAVE MK-300 over USB where the affected capability uses it.

## Evidence reuse

Accepted evidence remains valid until a later change can materially invalidate it. Do not replay an entire historical campaign for an unrelated maintenance change.

Historical plans may contain “pending/open/current” language from their own date. They are evidence only and never reopen a live gate by themselves.

## Candidate invalidation

A candidate is invalidated when source, materialized runtime bytes, package/version, unsigned APK, signer, relevant backend digest or another protected identity changes after qualification. A documentation-only commit does not retroactively change an already-built candidate.

## Current feature application

RC27 is an owner-requested Mixer visual-hierarchy refinement after review of the signed RC26 Studio. It reopens only affected Mixer presentation and adjacent Studio geometry evidence: centered track/Master headers, soft section boundaries, selected/unselected card treatment, existing pointer/slider interaction, large-font containment, horizontal overflow and target-tablet density. RC26 remains signed digital evidence for its code tree, but RC27 is a new runtime candidate and must qualify its exact source. Existing audio routing/recording, frozen Demucs and unrelated Drive evidence remain applicable because RC27 does not change those paths. RC20 remains the accepted physical baseline until an exact signed successor is owner-accepted.

Automated geometry must establish useful hierarchy and workspace density, not only control existence: inline non-overlapping 48 dp touch regions, centered header identity groups, full-width/non-overlapping internal sections, complete/minimum height budgets, a third full waveform lane with complete Mixer on target-tablet geometry and stable navbar slots. Capture and inspect RC27 screenshots of five managed-audio clips in complete/minimum states before declaring digital UI qualification complete. Owner physical acceptance remains a distinct step.

## Frozen baseline

The accepted RC20 identity and physical result are recorded in `RELEASE_BASELINE.md` and `history/RC20_PHYSICAL_HOMOLOGATION_FINAL_2026-09-25.md`.

No recurring test schedule is required for the frozen personal-use appliance. Qualification resumes only after a maintenance trigger or explicit new feature.
