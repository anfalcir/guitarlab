# RC3 physical-review hardening — editing, drag/drop and recording sync

Updated: 2026-09-14

## Objective
Treat the latest physical findings as an end-to-end workflow review, not isolated UI fixes. The target is one hardened editing/recording flow whose domain invariants, Android audio timing, persistence, Undo/Redo, gestures, accessibility and failure handling agree before the next physical candidate is promoted.

## Evidence baseline and current status
- CI #614 failed only because a new Compose test used `assertDoesNotExist`, unavailable in the project's current test API. That compatibility defect was corrected independently.
- CI #615 then passed the complete canonical matrix at `74bf86efbec94d249c4968c3284bf1985cd66b44`, including software, API 36, 1920×1200 geometry and signed homologation.
- The current source advanced after #615 with the H0–H6 hardening below.
- H0–H6 are **IMPLEMENTED / PRE-GATE**. This means source/materialization/tests/documentation are implemented and reviewed, but the current HEAD is not digitally homologated until a new exact-source manual workflow passes.

## Root-cause findings from physical use
1. **Trim acquisition:** the old full-card `RangeSlider` did not expose independent, deterministic handle targets suitable for target-tablet gesture testing.
2. **Clip deletion:** domain/ViewModel removal capability existed, but clip-level deletion was not exposed coherently in lane UX.
3. **Split → move failure:** split siblings shared one `takeId`; moving a child could remove the referenced `RecordingTake`, leaving a sibling with a dangling reference and causing repository validation/save failure.
4. **Recording delay:** capture and backing startup were not represented by one session synchronization record. Route calibration alone could not distinguish per-session startup skew.
5. **Live waveform drift:** callback-count-based peaks plus pairwise compaction and one UI coroutine per audio read destroyed a stable timebase and could create queued catch-up animation.

## Serial implementation record
### H0 — reproduction contracts and invariants — IMPLEMENTED / PRE-GATE
Added/expanded deterministic fixtures and regression contracts for:
- recorded takes with split siblings sharing lineage;
- split → move/delete → save/reopen;
- trim geometry/handle acquisition;
- independent capture/backing timing inputs;
- long live-waveform runs with variable captured-frame cadence.

Gate rule remains: each physical defect should have a deterministic regression or measurable invariant whenever hardware is not genuinely required.

### H1 — trim interaction hardening — IMPLEMENTED / PRE-GATE
- Replaced reliance on opaque full-card slider acquisition with independent start/end handles.
- Handles have separate semantics/test identity and ergonomic touch targets.
- Pointer X is converted to timeline frames by one pure policy and clamped against opposite handle/source bounds.
- Time bubbles are passive visuals, not gesture owners.
- Trim remains non-destructive, metadata-only and Undo/Redo compatible.

Acceptance after CI/physical gate: both handles repeatedly acquire on target geometry; close handles do not swap ownership unexpectedly; save/reopen preserves the result.

### H2 — clip lifecycle, split lineage and deletion — IMPLEMENTED / PRE-GATE
- Split children may share one recording-take lineage safely.
- Moving/deleting one child detaches only that segment.
- If the canonical `RecordingTake.clipId` leaves, a surviving sibling is promoted deterministically.
- The take is removed only when no child still references it; active-take fallback is reconciled deterministically.
- Shared immutable source/proxy media is not deleted while still referenced.
- Added explicit confirmed `Excluir clipe` as a non-drag/accessibility path.

Acceptance after CI/physical gate: split → move, split → delete left/right, canonical-child deletion, Undo/Redo and save/reopen produce no dangling take/media references.

### H3 — drag/drop transaction hardening — IMPLEMENTED / PRE-GATE
- Drop resolution is explicit: `Move`, `Delete` or `NoOp`.
- Drag start snapshots clip/track identity; state is revalidated before commit.
- Drag-to-trash appears only while a clip is dragged and enters the same confirmation/domain deletion path as `Excluir clipe`.
- Same-origin/cancel/invalid drop is a no-op.
- Failed/stale drop is designed to avoid partial project/history/media/cache mutation.

Acceptance after CI/physical gate: valid split-child migration to an empty compatible track succeeds; trash confirmation deletes only the selected segment; no raw repository-validation toast is reachable from a valid gesture.

### H4 — recording synchronization and latency architecture — IMPLEMENTED / PRE-GATE
No hard-coded `-0.5 s` correction is used.

- Introduced session timing evidence for capture/backing startup.
- Recording/playback engines expose trustworthy Android timestamp/monotonic evidence where available, with bounded fallback.
- Session startup skew, accepted route latency and punch/pre-roll offsets are modeled separately.
- Compensation components are combined exactly once to prevent double correction.
- Final placement/trim is frame-domain and clamped to timeline/source bounds.
- Route-scoped calibration remains distinct from volatile per-session startup timing.

Acceptance after CI: fake timing inputs with known skew/latency must land within bounded frame tolerance and no component may be applied twice.

Residual physical acceptance: one MK-300 guitar-against-backing synchronization check confirms the measured result.

### H5 — live REC waveform timebase rewrite — IMPLEMENTED / PRE-GATE
- Replaced callback-count timebase with frame-span envelope points.
- Each point records explicit start/end frame coverage plus normalized peak.
- Pairwise compaction merges adjacent frame spans and preserves the maximum transient and full covered time.
- `recordingFrames` is authoritative for the live clip width.
- UI publication is conflated/rate-bounded instead of dispatching one main-thread update per audio read.
- Finalized waveform remains canonical media-derived data.

Acceptance after CI: long/variable-cadence tests preserve monotonic frame coverage, bounded memory and transients.

Residual physical acceptance: a multi-minute take must not visually accelerate, pile backward or jump ahead of captured duration.

### H6 — integrated regression, materialization and documentation — IMPLEMENTED / PRE-GATE
- Added persistence regression for split/move/delete/save-reopen lineage.
- Added editing-policy/instrumented interaction coverage.
- Added timing-compensation and long-waveform regression.
- Synchronized the in-app user guide with trim/delete/trash behavior.
- H1→H6 are separate versioned patches under `.source-parts` and are applied in order by `scripts/materialize_ci_sources.sh`.
- A final test-review defect (`append()` returns `Unit`) was corrected in `H6WaveformTestFix.patch`; that patch is explicitly part of the materialization chain.
- Materialization remains fail-fast: a patch must apply cleanly or be recognized as already applied; ambiguous drift fails the build.

## Canonical next gate
Do not produce/promote another physical candidate from source review alone. The next authoritative step is one manual `.github/workflows/android-ci.yml` dispatch with signed homologation enabled against the final current `main` HEAD.

Required evidence for that exact SHA:
1. JVM/unit regression PASS;
2. Android Lint PASS;
3. debug/release assembly PASS;
4. Android test compilation PASS;
5. full API 36 instrumented regression PASS;
6. isolated 1920×1200 target-geometry PASS;
7. signed release PASS;
8. package/version/source-provenance PASS;
9. locked certificate PASS and APK SHA-256 publication.

## Residual physical gate after automated PASS
Manual work is intentionally short and target-only:
- trim start/end handle ergonomics;
- split → move one child → delete one child via explicit action and trash → save/reopen/Undo/Redo;
- MK-300 selected-input isolation/fail-closed routing;
- guitar-against-backing synchronization;
- multi-minute live waveform stability;
- subjective monitoring/listening for pops/dropouts and one representative export/stress smoke.

## Implementation order contract
The diagnostic order remains fixed:

`H0 tests/reproduction → H1 trim → H2 lineage/delete → H3 drag transaction → H4 sync/latency → H5 live waveform → H6 integrated regression/documentation/release`.

H4 and H5 remain logically separate because both observe recording frames; keeping them isolated preserves bisectability and prevents timing/waveform regressions from masking one another.
