# M8 Global Digital Regression

Updated: 2026-09-14

## Active state
Active candidate: `0.5.0-rc3`, versionCode `23`.

Latest fully green signed baseline: CI #615 at source `74bf86efbec94d249c4968c3284bf1985cd66b44`, which passed software, full API 36 instrumentation, isolated 1920×1200 target geometry and signed homologation. Signed APK SHA-256: `d443cb33a010d2d21dad43e9ff12554d7ada4f2bb783be0c80e3ac11bfe2d6c9`.

Current `main` advanced after #615 with H0–H6 editing/recording hardening. Therefore H0–H6 rows below are **IMPLEMENTED / PRE-GATE** until one new exact-source canonical workflow passes. Historical PASS claims continue to apply to unchanged covered behavior but never substitute for the next candidate's exact-source gate.

## Objective coverage matrix

| Area | Automatic/digital evidence | Residual physical need | Status |
|---|---|---|---|
| Project factory/template/roles/names | JVM model tests | none | PASS baseline |
| Selected USB input | fail-closed route policy tests | confirm MK-300 effective route | PASS policy / physical route residual |
| Takes/audition/punch/sections/level | model/editor policy and invariant tests | workflow ergonomics/listening | PASS baseline |
| Transport natural completion/live seek | deterministic policy + engine/instrumentation regression | tactile/listening smoke | PASS baseline |
| Section preview/end-boundary/fixed slot | JVM + Compose geometry/semantics | final visual confirmation | PASS in #615 |
| Countdown 3-second overlay | JVM + Compose instrumentation | final visual confirmation | PASS in #615 |
| Shared Home/Studio guide | shared implementation + Compose regression | none beyond UI smoke | PASS in #615 |
| Save/load/list/delete | filesystem repository tests | none | PASS baseline |
| Duplicate project + managed media | source/proxy comparison + rollback | none | PASS baseline |
| `.guitarlab` writer/reader | ZIP round trip/traversal/version/staging/rollback | none | PASS baseline |
| Legacy JSON compatibility | historical/current fixtures | none for covered fixtures | PASS baseline |
| Undo/Redo | mixed sequence/snapshot recovery/branch invalidation | none | PASS baseline |
| Mixer policy/accessibility | audio truth tables + Compose semantics/callbacks | listening optional | PASS baseline |
| SRC | multi-rate duration/pitch/RMS/channels/source immutability | listening sanity only | PASS baseline |
| WAV codec/waveform | decode/seek/metadata/envelope tests | target listening smoke only | PASS baseline |
| Master render/realtime parity | deterministic PCM/shared mix kernel/chunk parity | real route/device only | PASS baseline |
| Recording transaction/recovery | state machine, managed take, cancellation, Float32 recovery | real USB capture | PASS baseline |
| SAF publication | staging/truncating publish/error/cancel rollback | provider-specific smoke | PASS policy |
| FLAC Android export | native FLAC extraction/decoding API 36 | listening smoke only | PASS emulator baseline |
| MP3 Android export | conditional capability contract | Samsung encoder availability | PASS behavior / target capability residual |
| Lifecycle recreation | route codec + `ActivityScenario.recreate()` | none for covered path | PASS emulator baseline |
| Large/fuzz project performance | save/load/bundle/reopen/render scenarios | Samsung realtime stress | PASS structural baseline |
| **H1 trim handles** | pure pointer→frame policy + new instrumented handle semantics/interaction | tactile acquisition/feel | IMPLEMENTED / next CI pending |
| **H2 split take lineage/delete** | new editor/persistence regressions for sibling promotion/delete/move/save-reopen | one real workflow smoke | IMPLEMENTED / next CI pending |
| **H3 drag transaction/trash** | `Move`/`Delete`/`NoOp` policy + confirmation instrumentation | real touch drag ergonomics | IMPLEMENTED / next CI pending |
| **H4 recording startup synchronization** | pure timing compensation tests with independent startup/route components | MK-300 guitar-vs-backing confirmation | IMPLEMENTED / next CI pending |
| **H5 live REC waveform** | frame-span accumulator, compaction/continuity/long-run tests | multi-minute real take visual confirmation | IMPLEMENTED / next CI pending |
| **H6 integrated hardening regression** | save-reopen lineage, editing instrumentation, timing/waveform tests, guide sync/materialization | exact signed candidate only | IMPLEMENTED / next CI pending |

## Defects found and corrected during global regression history

Historical corrected P0/P1/P2 items remain covered by regression, including duplicate-project media publication, persistence-boundary cancellation, interrupted recording retention/repair, interrupted-import ghost projects, encoder timestamps, SAF partial publication, FLAC native header construction and Mixer accessibility.

### RC3 physical-review defects addressed by H0–H6
| Severity | Reproduction/finding | Structural correction | Required next evidence |
|---|---|---|---|
| P1 | Trim start/end markers were not reliably touch-acquirable on tablet. | Independent explicit trim handles with deterministic frame mapping and passive bubbles. | API36/1920×1200 instrumentation + tactile physical confirmation. |
| P1 | Split segment moved to another track could produce repository-validation error because a shared take was removed while sibling still referenced it. | Transactional take-lineage reconciliation; sibling promotion; remove take only when no child remains. | JVM persistence save/reopen + physical split/move smoke. |
| P1 workflow gap | One cut segment had no direct clip-level deletion workflow. | Confirmed `Excluir clipe` plus drag-to-trash sharing one domain delete command. | Compose instrumentation + physical touch smoke. |
| P1 | Recorded guitar could replay roughly 0.5 s late against backing. | Model per-session startup skew separately from route calibration/punch offsets; combine once; prohibit magic fixed offset. | Pure timing tests + real MK-300 alignment confirmation. |
| P1 visual/workflow | Live REC waveform progressively accelerated/piled material backward. | Frame-span envelope, bounded time-preserving compaction and conflated/rate-bounded UI publication. | JVM long-run tests + multi-minute physical take. |
| Test defect | H6 waveform regression initially called `.single()` on `append()` returning `Unit`. | Separate `append()` call then assert `snapshot().single()`. Versioned final test-fix patch added to materializer. | Next software gate compilation/tests. |

No current H0–H6 P0/P1 may be declared closed until the exact post-hardening workflow passes and the target-only checks in `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` are completed.

## Performance evidence policy
CI metrics are regression evidence, not device benchmarks. Existing scenarios measure save/load, bundle write/reopen/import, JSON/bundle sizes, rough heap delta and offline render. Exact per-run metrics remain stored in CI diagnostics. Physical realtime stress remains target-only.

## CI architecture
Canonical workflow contains parallel:
- `software-gate`;
- `android-integration-gate`;
followed by `homologation-apk`, which needs both.

GitHub Actions is manual-only. API 36 AVD uses the retained snapshot cache; integration Gradle cache is read-only; final signing reuses the exact unsigned release produced by software-gate and does not recompile.

## Source materialization gate
H1–H6 deltas are versioned under `.source-parts` and materialized serially:

`H1 trim → H2 lineage/delete → H3 drag → H4 sync → H5 waveform → H6 integrated regression → H6 waveform-test fix → H6 guide sync`.

A patch that is neither cleanly applicable nor already applied blocks the build. Partial source materialization is not acceptable evidence.

## Remaining physical checklist after exact-source PASS
1. Install only the newly signed post-H0–H6 RC3 artifact on Samsung SM-X230.
2. Confirm both trim handles acquire reliably.
3. Exercise split → move child → delete child via explicit action/trash → save/reopen/Undo/Redo.
4. Connect MK-300 and confirm real fail-closed input/output + guitar-only capture.
5. Record guitar against backing and confirm no repeatable systematic late placement.
6. Record a 2–3 minute take and confirm live waveform remains temporally stable.
7. Judge subjective monitoring/latency/listening and one representative export/stress smoke.

Everything else objectively established by the automated matrix should not be manually repeated.

## Candidate/evidence history
- CI #593 / `0.4.0-rc2`: historical software + API36 + geometry + signed baseline.
- CI #613 / RC3: earlier optimized full PASS at `db5a4208848e4b6ca2163ce715d0c5bb464cfe37`.
- CI #614: Compose test-API compatibility failure; signing correctly blocked.
- CI #615 / `74bf86efbec94d249c4968c3284bf1985cd66b44`: latest fully green signed pre-H0–H6 baseline; signed APK SHA-256 `d443cb33a010d2d21dad43e9ff12554d7ada4f2bb783be0c80e3ac11bfe2d6c9`; official signer verified.

## Closure rule
M7/M8 release closure requires zero repeatable P0/P1, a canonical exact-source automated PASS for the final current HEAD, verified package/version/checksum/signer, residual Samsung SM-X230 + M-VAVE MK-300 PASS and explicit user approval of that exact signed APK.
