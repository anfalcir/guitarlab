# Recording Latency Architecture

Updated: 2026-09-20

## Goal
`played in sync → saved take in sync`, automatically. No song-specific offset, global magic constant or hidden manual correction is acceptable.

## Time model
GuitarLab keeps three domains separate until final take placement:

### 1. Session clock alignment
Maps capture frame zero against backing presentation frame zero.

Preferred evidence:
- `AudioRecord` stable `AudioTimestamp` anchor;
- `AudioTrack` stable `AudioTimestamp` anchor;
- both on monotonic time.

Fallback evidence:
- capture command monotonic start;
- playback command monotonic start.

A hardware anchor is never paired with a command anchor. Mixed evidence fails closed to zero startup correction.

### 2. Route latency
Optional measured physical round-trip latency for one exact input/output/rate tuple. It is accepted only after multi-pass stability/confidence policy succeeds.

### 3. Global residual fine adjustment
Optional signed correction, default zero, bounded ±500 ms, scoped to the same exact route/rate tuple. It is captured when REC starts and affects only that new take's initial placement. Changing the Settings value later never moves existing takes.

### 4. Take-specific synchronization
A post-recording creative/technical edit stored on `RecordingTake.fineAdjustmentFrames`. Positive advances that take; negative delays it. The policy shifts every clip in the same `takeId` lineage by only the delta from the prior stored value, preserving source offsets, lengths and media bytes. Timeline-zero crossing fails closed instead of introducing a hidden trim.

## Stable audio anchor acquisition
A single timestamp immediately after `startRecording()`/`play()` is not authoritative.

For each stream:
1. poll several observations;
2. discard invalid observations (`frame < 0`, non-positive monotonic time);
3. repeated frame or repeated time is stale and does not add evidence;
4. backwards frame or monotonic time invalidates the anchor;
5. require at least two progressing observations;
6. transform each observation to a stream-frame-zero origin estimate;
7. use median origin and reject excessive jitter/spread.

This prevents a startup `framePosition=0` repeated by a driver from looking like several independent samples.

## Signed startup delta
`sessionDeltaNs = captureOriginNs - backingOriginNs`

Convert at the actual recording/session sample rate with deterministic nearest-frame rounding.

- negative: capture began before backing;
- zero: aligned origins;
- positive: capture began after backing.

H23b tests 44,100; 48,000; 88,200; and 96,000 Hz explicitly.

## Final placement
Conceptually:

`mappedStart = requestedStart + sessionDelta - acceptedRouteLatency - residualFineAdjustment`

Sign meaning:
- positive `sessionDelta` moves the take later;
- positive route latency advances the take;
- positive residual fine adjustment advances the take.

After that single computation:
- if `mappedStart >= 0`, source starts at frame 0;
- if `mappedStart < 0`, timeline start becomes 0 and the equivalent prefix is trimmed from the source;
- arithmetic saturates on pathological `Long` values rather than wrapping.

## Punch ordering
1. capture with enough pre/post-roll/tail;
2. compute one final compensated take placement;
3. derive punch keep-window from that placement;
4. crop once.

No timing term is re-applied after crop.

## Route/rate key
Calibration and the **global future-recording adjustment** use an exact v2 key built from length-prefixed input signature, output signature and sample rate. The take-specific adjustment is project metadata owned by the take and is not route-store state. A calibration for MK-300@44.1k is therefore a different entry from MK-300@48k or tablet input + MK-300 output.

The old H23 32-bit hashed key is not auto-applied in H23b because a collision cannot prove route identity. Recalibration after H23b is intentionally safer than applying an unverifiable legacy tuple.

## Silent verification and physical stimulus
H35 separates two diagnostics:
- **Digital silent verification:** PCM zero only; exact live routed-device IDs must match selection; stable AudioRecord/AudioTrack anchors report clock delta/jitter. It never becomes a physical round-trip latency calibration.
- **Physical round-trip calibration:** starts with silence, confirms exact live input/output IDs, then emits a deterministic 32 ms windowed chirp with adaptive 3%/6%/12% peak gain. If exact routing cannot be confirmed, it aborts before any chirp.

This preserves the distinction between software clock synchronization and actual hardware/DSP path delay.

## Analyzer result
The normal options UI exposes:
- selected input;
- selected output;
- sample rate;
- valid / unstable / not calibrated status;
- median latency;
- attempt count;
- jitter;
- drift;
- confidence;
- accepted automatic route compensation;
- residual fine adjustment.

Unstable measurement may be displayed diagnostically but must not become active compensation.

## Failure behavior
- no common trustworthy clock basis → startup delta 0;
- selected recording input unavailable → fail closed;
- recording backing/output falls back → clear route-specific automatic/global fine terms for that recording session;
- physical calibration effective route differs from selected current device ID → abort while still silent;
- conflicting project editing rates → require a single editing domain rather than guessing;
- stop/error → clear temporal state before next take.

## Validation boundary
Local source validation can prove deterministic policy behavior, source-part integrity and pure Kotlin invariants. It cannot prove Android driver behavior or a physical MK-300 round-trip. The official user-dispatched Android CI is required before a source becomes a signed candidate, and final physical timing acceptance still requires SM-X230 + MK-300.
