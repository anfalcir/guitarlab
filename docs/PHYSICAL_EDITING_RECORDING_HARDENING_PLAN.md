# RC3 physical-review hardening plan — editing, drag/drop and recording sync

Updated: 2026-09-14

## Objective

Treat the latest physical findings as an end-to-end workflow review, not isolated UI fixes. The target is a single hardened editing/recording flow whose domain invariants, Android audio timing, persistence, Undo/Redo, gestures, accessibility and failure handling agree with each other before the next physical candidate is produced.

The latest failed CI run is a separate release-engineering defect: run #614 reached the API 36 gate but failed while compiling `AutoSectionsSlotInstrumentedTest` because the current Compose test API does not expose `assertDoesNotExist`. That compatibility problem is fixed independently and must remain separate from the functional hardening below.

## Source-level findings from the physical reports

1. **Trim handles need a gesture-level redesign, not only a larger visual.** The current trim UI is a `RangeSlider` stretched over the waveform, with time bubbles layered in the same box. There is no dedicated per-handle semantic/test target and no instrumented drag test proving that each edge can be acquired reliably on the target tablet geometry.
2. **Single-clip deletion already exists in the domain/ViewModel but is not exposed in the lane UI.** `removeClip()` is wired into `StudioTrackLane`, but the current clip menu never invokes that callback. The only destructive command exposed there is `Limpar pista`.
3. **The reported split → drag-to-empty-track error has a concrete take-lineage hazard.** A split copy inherits the same `takeId`. `moveClipToTrack()` currently detaches the moved clip and unconditionally removes the referenced `RecordingTake`; any sibling split segment still carrying that `takeId` then becomes invalid and repository validation can reject the save. This path must be redesigned as a take/clip lineage transaction rather than patched at the UI layer.
4. **Recording start is not driven by one shared audio clock.** `AudioRecord` is started first; only after `onStarted` reaches the ViewModel is backing playback started. Placement compensation then uses only a previously accepted route calibration, if one exists. There is no per-session measurement of capture-start versus backing presentation-start. A route/startup skew can therefore become silence at the beginning of the take and present as a late guitar on playback.
5. **The live waveform has two independent timebase risks.** One peak is appended per `AudioRecord` read callback, while old points are pairwise compacted after the bounded list fills; the renderer later assumes all points are uniformly spaced in time. In addition, every audio callback launches a main-thread coroutine, so UI updates can queue and later catch up in bursts. Both behaviors can produce the observed acceleration/compression effect during longer REC sessions.

## Serial implementation plan

### H0 — Reproduction contracts and invariants

Before changing behavior, add deterministic regressions for each reported path.

- Create recorded-take fixtures with one clip, split clips sharing a take lineage, inactive/active alternate takes, and shared managed media.
- Reproduce and lock down `split → move one segment → save/reopen` and `split → delete one segment → save/reopen`.
- Add trim gesture instrumentation at the exact tablet geometry, including start handle, end handle, close handles, minimum-width clip and long clip.
- Add a fake recording-session timing model with independently controllable capture-start, playback-start, output latency and input latency.
- Add long live-waveform simulations with variable input chunk sizes and delayed/conflated UI consumers.

Gate: every issue must first have a failing automated test or a measurable invariant, except behavior that genuinely requires MK300 hardware.

### H1 — Trim interaction hardening

Replace reliance on an opaque full-card `RangeSlider` gesture surface with explicit trim-handle interaction.

- Two independent handles with minimum 48 dp touch targets, deterministic z-order and separate test tags/semantics.
- Convert pointer X directly into timeline frames through one pure policy; clamp start/end against each other and immutable source bounds.
- Keep time bubbles visual/passive so they cannot steal handle gestures.
- Preserve non-destructive trim semantics, Undo/Redo and source provenance.
- Support fine dragging when the two handles are close without swapping ownership unexpectedly.

Acceptance: both handles can be grabbed repeatedly on the target geometry; no gesture conflict with clip drag, playhead or scrolling; trim remains valid after save/reopen/undo/redo.

### H2 — Clip lifecycle, split lineage and deletion

Make a clip segment a first-class editable object.

- Define take lineage explicitly: multiple split segments may belong to the same take and must remain active/inactive together.
- Moving one split segment to another track detaches only that segment from the original take; it must not invalidate siblings.
- Deleting a split child preserves the take while siblings exist. If the canonical `RecordingTake.clipId` is deleted, promote a surviving sibling deterministically; remove the take only when no clips reference it.
- Keep shared source/proxy media immutable. A clip deletion must never delete a media file still referenced by another clip; orphan cleanup, if performed, happens only after a successfully persisted project state.
- Add an explicit `Excluir clipe` action as an accessibility/non-drag fallback.
- Add drag-to-trash as the primary direct-manipulation flow: trash target appears only while a clip is dragged, changes state when hovered, and dropping there opens confirmation naming the clip/segment. Confirm uses the same domain command as `Excluir clipe`; cancel is a no-op.
- All delete/move/split operations participate in Undo/Redo and waveform-cache pruning without corrupting media ownership.

Acceptance: split → move, split → delete left/right, delete original canonical segment, undo/redo, save/reopen and project validation all pass without dangling take references.

### H3 — Drag/drop transaction hardening

Review the complete path from gesture acquisition to repository commit.

- Separate drag geometry result from mutation intent (`MoveClip`, `DeleteClip`, `NoOp`) so an invalid drop never reaches persistence.
- Snapshot source clip/track identity at drag start and revalidate at commit.
- Keep destination-role restrictions explicit and return typed domain failures rather than raw `require()` text.
- Surface user-safe messages while retaining diagnostic cause in logs/test output.
- Verify autoscroll, lane retargeting, empty target tracks, scroll during drag, cancel, lifecycle recreation and rapid repeated drags.
- Ensure a failed move leaves project, history, media and waveform caches byte-for-byte unchanged.

Acceptance: no generic repository-validation toast is reachable from a valid user drag; failure is transactional and non-destructive.

### H4 — Recording synchronization and latency architecture

Do **not** compensate the observed ~0.5 s by hard-coding an offset. Replace the start/placement model with one synchronized session clock.

- Introduce a recording synchronization record containing monotonic timestamps/frame positions for capture readiness/start and backing presentation start.
- Extend playback/recording engines to expose the earliest trustworthy Android audio timestamps (`AudioRecord`/`AudioTrack` timestamp when available), with a bounded monotonic-clock fallback.
- Measure per-session capture↔backing startup skew and combine it exactly once with accepted route-specific round-trip calibration.
- Distinguish three quantities in code/tests: session startup skew, route round-trip latency, and punch/pre-roll offset. Prevent double compensation.
- Keep calibration keyed by effective input route + effective output route + sample rate, not merely requested routes; invalidate/fallback when routing changes.
- At finalization, compensate through source trim and/or timeline placement with explicit bounds, preserving zero as a hard timeline floor.
- Record diagnostic evidence for each take: effective routes, sample rate, compensation components and total applied frames. Do not persist volatile Android device IDs as long-term identity.
- Exercise 44.1/48/88.2/96 kHz, start-at-zero, non-zero playhead, loop punch, route loss and no-calibration fallback.

Acceptance: fake-engine tests with known offsets land within a small frame tolerance; no offset is applied twice; a route without calibration remains deterministic; final playback/export use the same corrected clip placement.

Physical residual gate: one MK300 loopback/calibration check and one guitar-against-backing performance check. The human check should only confirm the already-measured result, not discover the algorithm.

### H5 — Live REC waveform timebase rewrite

Make rendering frame-based rather than callback-count-based.

- Replace `List<Float>` callback samples with an envelope whose points carry frame/time coverage (for example start/end frame + peak).
- Compaction must merge adjacent time spans while preserving their covered frame range and maximum transient; the renderer maps each point using frame coordinates, not list index.
- Decouple the audio thread/callback cadence from Compose state updates with a conflated/latest-value pipeline and a bounded UI refresh rate (target roughly display-frame/30 Hz class, not one recomposition per audio read).
- `recordingFrames` remains authoritative for the live clip width; waveform points cannot run ahead of or lag behind that frame count.
- On stop, replace the live envelope with the canonical file-derived waveform, preserving the same timeline start/end.
- Stress variable buffer sizes, long takes, silent takes, transients, UI stalls and background/foreground transitions.

Acceptance: monotonic width and time mapping for long recordings; no burst catch-up or backward pile-up; memory remains bounded and transients remain visible.

### H6 — Integrated regression and release gate

After H1–H5 are green independently, run the complete suite as one integrated candidate.

Required automated evidence:
- core model/project/audio unit regressions;
- persistence validator + save/reopen + migration coverage;
- Undo/Redo across split/move/delete/trim;
- instrumented trim-handle and drag-to-trash gestures;
- API 36 full instrumentation plus target tablet geometry;
- recording timing fake-engine tests and latency policy tests;
- live waveform long-duration/conflation tests;
- lint/debug/release/signing/source-provenance gates;
- malformed/cancel/lifecycle regression unchanged.

Only after the complete digital gate passes should a signed physical candidate be produced. The residual MK300/tablet checklist should be short: trim handle ergonomics, drag-to-trash confirmation, split/move/delete workflow, one synchronized recording, and visual confirmation of the live waveform over a multi-minute take.

## Implementation order

The implementation order is fixed to avoid masking causes:

`H0 tests/reproduction → H1 trim → H2 clip lineage/delete → H3 drag transaction → H4 sync/latency → H5 live waveform → H6 full regression/documentation/release`.

Do not mix H4 latency changes with H5 waveform changes in the same logical commit: both observe recording frames, so keeping them isolated preserves diagnosability and makes regressions bisectable.
