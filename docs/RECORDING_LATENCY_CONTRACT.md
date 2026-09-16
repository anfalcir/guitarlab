# Recording Timing and Latency Contract

Updated: 2026-09-16

## Product goal
A take played in time with the audible backing must land in time on the GuitarLab timeline without requiring manual movement. Manual fine adjustment is a fallback for route-specific residual hardware behavior, not the primary synchronization mechanism.

## Three independent timing domains
1. **Session clock offset** — signed difference between capture stream origin and backing presentation stream origin.
2. **Measured route latency** — accepted round-trip calibration for the exact physical input + output + sample rate.
3. **Fine residual adjustment** — optional user correction for a repeatable residual not removed by automatic timing/calibration.

These domains must never be folded together early or applied more than once.

## Plan A — automatic session synchronization
- Capture and playback obtain monotonic audio clock observations.
- Multiple observations are converted to stream-origin estimates.
- Unstable/stale observations fail closed.
- Capture and playback must use the same evidence class: hardware timestamp vs hardware timestamp, or command fallback vs command fallback.
- The clock offset is signed: capture-before-backing is negative; capture-after-backing is positive.
- Mapping operates in frames at the actual session sample rate.
- Timeline cannot become negative; any required pre-zero portion is trimmed from the recorded source instead.

## Plan B — analyzer/calibration
- Calibration is optional and requires explicit input/output selection plus a loopback path.
- It runs at the same editing/recording rate used by the project.
- Several passes are required; median, jitter, confidence and drift decide acceptance.
- Rejected/unstable measurements are stored for diagnostics if useful but are never applied automatically.
- Calibration keys include semantic route signature + sample rate.

## Fine adjustment
- Default: 0 frames.
- Scope: exact input + output + sample-rate tuple.
- Safety bound: ±120 ms.
- Positive value advances the take; negative value delays it.
- It is applied after the automatic clock mapping and measured route latency, exactly once.
- Fine adjustment must never be silently copied between different sample rates or physical routes.

## Punch recording
Punch pre-roll/post-roll controls capture extent only. Final kept source/timeline window is derived from the fully compensated take placement so startup offset and route latency are not double-counted.

## Failure rules
- No trustworthy common clock basis → startup offset = 0 rather than inventing a value.
- Selected input disappears → recording fails closed according to the explicit-route policy; no silent microphone fallback.
- Selected output falls back → route-specific calibration/fine adjustment is invalidated for that take.
- Project has conflicting editing sample rates → recording/calibration must require normalization instead of guessing.

## Validation matrix
Required deterministic coverage:
- capture starts before backing;
- capture starts after backing;
- 44.1, 48 and 96 kHz frame/time conversion;
- hardware/hardware vs command/command basis;
- mixed basis fails closed;
- timeline-zero trimming;
- positive/negative/zero fine adjustment;
- calibration absent/rejected/accepted;
- route fallback invalidates route-specific compensation;
- punch crop after compensation;
- repeated takes cannot inherit stale timing state.

Required physical coverage on target hardware:
- MK-300 input/output, loopback disabled for normal REC;
- record against a transient-rich backing at the real project rate;
- repeat at least three takes without changing setup;
- verify the residual is not systematic before applying fine adjustment;
- if fine adjustment is needed, verify one saved setting corrects repeated takes on that same route/rate and does not affect another route/rate.
