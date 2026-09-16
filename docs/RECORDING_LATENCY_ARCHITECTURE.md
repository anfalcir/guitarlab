# Recording Latency Architecture

Updated: 2026-09-16

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

### 3. Residual fine adjustment
Optional signed correction, default zero, bounded ±120 ms, scoped to the same exact tuple. Positive advances the take; negative delays it.

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
Calibration and fine adjustment use an exact v2 key built from length-prefixed input signature, output signature and sample rate. A calibration for MK-300@44.1k is therefore a different entry from MK-300@48k or tablet input + MK-300 output.

The old H23 32-bit hashed key is not auto-applied in H23b because a collision cannot prove route identity. Recalibration after H23b is intentionally safer than applying an unverifiable legacy tuple.

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
- recording backing/output falls back → clear route-specific latency/fine terms for that take;
- conflicting project editing rates → require a single editing domain rather than guessing;
- stop/error → clear temporal state before next take.

## Validation boundary
Local source validation can prove deterministic policy behavior, source-part integrity and pure Kotlin invariants. It cannot prove Android driver behavior or a physical MK-300 round-trip. The official user-dispatched Android CI is required before a source becomes a signed candidate, and final physical timing acceptance still requires SM-X230 + MK-300.
