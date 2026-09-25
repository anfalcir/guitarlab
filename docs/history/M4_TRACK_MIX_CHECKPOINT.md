# M4 Track Mix Checkpoint

This checkpoint wires the Studio mixer to persisted project state and real playback. It has evolved beyond the initial gain/pan/mute/solo slice and now also defines the core Master/meter behavior.

## Implemented
- timeline track selection and Mixer selection share one selected track id;
- selected timeline track can open its controls directly in the Mixer;
- Mixer visibility and pinning are independent;
- track gain is editable from -60 dB to +12 dB, persisted and applied during playback;
- pan is persisted and applied in the stereo mix stage;
- Mute and Solo are persisted and affect audibility; Mute wins over Solo;
- clip gain and track gain combine only in mix math; immutable media is untouched;
- audio output selection remains centralized in Options and is applied at playback with controlled Auto fallback;
- Master gain is applied after track summing;
- Master peak/RMS is measured before final clamp, preserving clipping evidence;
- current checkpoint adds per-track post-track-bus meters, time-based peak hold/decay and project-persisted Master gain;
- mixer edits remain locked while transport is active, during import or while trim/edit transactions are open;
- slider drags use local draft state and persist on gesture completion.

## Persistence compatibility
`masterGainDb` is an additive `GuitarProject` field with default 0 dB. Existing project JSON without the field remains readable at unity. Round-trip tests cover non-zero Master values. Project validation enforces track/master gain bounds.

## Meter semantics
- track meter: after clip + track gain/pan and before Master;
- Master meter: after audible-track summing + master gain and before output clamp;
- meter state is transient and never written to project JSON;
- display ballistics: immediate attack, 750 ms peak hold and elapsed-time decay.

## Still gated
- record Arm/record/monitoring remains M5;
- no live mixer automation while playback is running;
- no EQ/insert processing until backed by a real DSP engine;
- current track/master meter checkpoint still requires its own CI gate and later physical tablet validation.

## Validation
Pure `TrackMixPolicy` tests cover audibility, gain conversion and pan. `MeterBallisticsPolicy` tests cover attack, peak hold, decay and reset. Project codec compatibility tests cover legacy Master default and Master round-trip. Android CI must pass unit tests, lint and debug assembly before this checkpoint is software-green.
