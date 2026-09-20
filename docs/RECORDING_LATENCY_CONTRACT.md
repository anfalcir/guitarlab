# Recording Timing and Latency Contract

Updated: 2026-09-20

## Product goal
A take played in time with the audible backing must land in time on the GuitarLab timeline without manual movement. Manual fine adjustment is a fallback for route-specific residual hardware behavior, not the primary synchronization mechanism.

The detailed implementation model is documented in `docs/RECORDING_LATENCY_ARCHITECTURE.md`.

## Three independent timing domains
1. **Session clock alignment** — signed relationship between capture stream origin and backing presentation stream origin.
2. **Route latency compensation** — accepted physical round-trip calibration for the exact input + output + sample rate tuple.
3. **Residual fine adjustment** — optional, small user correction for repeatable residual error after automatic alignment/calibration.

These values must not be folded together early, silently substituted for one another, or applied twice.

## Sign convention
Let:
- `requested` = requested timeline start frame;
- `sessionDelta` = capture stream origin minus backing presentation origin, in frames;
- `routeLatency` = accepted non-negative route latency, in frames;
- `fine` = residual adjustment, where **positive advances the take** and negative delays it.

Before the timeline-zero bound is applied:

`mappedStart = requested + sessionDelta - routeLatency - fine`

If `mappedStart < 0`, timeline start is clamped to zero and the corresponding amount is trimmed from the recorded source. The source length must remain positive.

## Plan A — automatic synchronization
- Prefer hardware audio timestamp vs hardware audio timestamp when both sides produce a trustworthy anchor.
- Otherwise use command monotonic clock vs command monotonic clock.
- Never compare one hardware-timestamp basis with the other side's command clock.
- Hardware anchors require multiple progressing observations; repeated/stale samples do not count and backwards frame/time progression fails closed.
- Session delta is signed; capture-before-backing and capture-after-backing are both represented.
- Conversion uses the actual session rate, including **44.1 / 48 / 88.2 / 96 kHz**.
- Pathological arithmetic must saturate rather than wrap into invalid frames.
- No trustworthy common basis means startup correction is zero; do not invent an offset.

## Plan B — route analyzer/calibration
- Calibration requires explicit input and output plus a valid test loopback path.
- It runs at the same editing/recording rate as the project.
- **Silent digital verification** is a separate operation: PCM zero only, exact current routed-device confirmation, stable capture/playback timestamp anchors, clock delta/jitter reporting, and no physical-latency persistence.
- Physical round-trip calibration activates the route with PCM zero first and emits no non-zero signal until both effective devices match the selected live `AudioDeviceInfo.id` values.
- Its stimulus is a deterministic 32 ms windowed chirp, band-limited from 700 Hz to a sample-rate-safe upper bound of at most 6.5 kHz.
- Peak gain is adaptive: 0.03 → 0.06 → maximum 0.12. The old harsh pseudo-random ±0.62 burst is not used.
- Several passes are required.
- Median latency, jitter, confidence, drift and attempt count are retained.
- Unstable/rejected calibration is diagnostic-only and never auto-applied.
- Persistence scope is exact `input route + output route + sample rate`.
- H23b uses an unambiguous v2 key; the prior 32-bit hashed key is not automatically applied because it cannot prove exact route identity under collision.

## Global fine residual adjustment — future recordings
- Default: `0` frames / `0.0 ms`.
- Scope: exact input + output + sample-rate tuple.
- Range: ±500 ms maximum. This is a product guardrail for manual residual correction, not a DSP/Android limit.
- Positive advances a **new** take; negative delays it.
- UI provides practical ±1 ms / ±5 ms / ±25 ms steps plus `Zerar`.
- The value is captured at recording-session start and applied once after automatic clock mapping and route-latency terms are defined.
- Changing this Settings value never repositions a take that already exists.
- It must not be copied across routes or sample rates.

## Take-specific synchronization — post-recording edit
- Each `RecordingTake` owns a persistent `fineAdjustmentFrames`, default 0.
- This value is independent from the current global Settings value.
- A change applies only `newFine - oldFine` to every clip carrying that `takeId`.
- Source offsets, clip lengths and audio bytes are unchanged; split/moved lineage retains its internal spacing.
- Repeated edits therefore do not accumulate drift, and returning to 0 applies the exact inverse delta.
- If advancing would cross timeline frame 0, the entire operation fails closed; H35 never hides a source trim to force the requested take edit.
- The edit is persisted through normal project save/reopen and participates in the existing Undo/Redo history.

## Punch recording
Punch pre-roll/post-roll controls capture extent. The final punch keep-window is derived **after** the take has one final compensated placement. Punch cropping must not repeat session-offset, route-latency or residual-adjustment math.

## Lifecycle and route failure
- Stop/error/finalization clears active timing state before another take can begin.
- Selected input disappearance fails closed; do not silently fall back to the tablet microphone.
- If playback output falls back during REC, route-specific calibration/fine adjustment for that take is invalidated.
- Mixed editing sample rates fail closed rather than guessing a recording/calibration rate.

## Required deterministic regression matrix
- capture starts before backing;
- capture starts after backing;
- simultaneous start;
- positive/negative/zero session delta;
- 44.1/48/88.2/96 kHz ns→frame conversion;
- deterministic rounding;
- timeline-zero bounds and non-negative placement;
- positive/zero route latency;
- positive/negative/zero global residual adjustment;
- per-take delta adjustment, split-lineage preservation, exact return-to-zero and timeline-zero fail-closed;
- absent/rejected/accepted calibration;
- other route/rate cannot match the current calibration key;
- output fallback invalidates route-specific terms;
- punch crop after final compensation;
- consecutive takes do not share policy state;
- stale/repeated/backwards timestamps fail closed;
- fallback without trustworthy `AudioTimestamp` uses command/command basis only;
- property/fuzz coverage with no overflow, invalid frame, nondeterminism or duplicate compensation.

## Physical acceptance
On SM-X230 + MK-300:
1. begin with fine adjustment at `0.0 ms`;
2. record at least three repeated takes against a transient-rich backing at the actual project rate;
3. verify there is no repeatable systematic late/early placement;
4. only if a stable residual remains, run the route/rate analyzer;
5. only after accepted calibration, use the smallest residual fine adjustment if still necessary.

`WATG - Enemy-master.wav` is historical physical evidence of the residual issue. Only its **left channel** is the relevant recorded-guitar evidence channel. It is not an isolated reference and must not be used to derive a global magic offset.
