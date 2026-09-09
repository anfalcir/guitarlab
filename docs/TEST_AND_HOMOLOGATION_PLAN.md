# Test and Homologation Plan

## Software gate for every development checkpoint
Mandatory: source materialization, unit tests, Android Lint, debug APK assembly and artifact upload. Green CI is software evidence only.

## Signed homologation
Only explicit candidates restore CI signing material, build signed APK, verify the locked signer fingerprint, publish hashes/identity, then destroy temporary signing assets.

## M2 physical audio gate — HOMOLOGATED / PASS
Target: Samsung SM-X230, Android 16/API 36, Pocket Amp USB audio.
Validated: correct USB routing, Play, Record, Duplex, hot-unplug handling, idle reconnect/re-enumeration, RECORD_AUDIO denial safety and repeated stability. See `M2_HOMOLOGATION_EVIDENCE.md`.

## M3 codec gate
Per format/variant: metadata, complete frames, random seek, malformed rejection, decoded sample safety, Android provider access and representative real-device files.

## M4 Studio gate
Managed media integrity:
- external original read-only;
- complete internal source copy before project dependence;
- immutable managed source survives loss of external origin;
- move/trim/mute/gain/waveform/mix operations never alter source bytes;
- waveform cache regenerates safely.

Timeline/UX:
- whole Studio workflow fits the single-screen workspace; only track-area overflow may scroll internally;
- marker heads remain >=48dp and semantic colors remain stable;
- loop heads appear only while loop is enabled;
- marker/import/clip/mix edits are disabled while transport is active;
- trim is draft/apply/cancel with a clearly visible Apply action;
- T◀/T▶ never cross or expose frames outside immutable source bounds;
- immersive fullscreen hides system bars during normal use and system edge gestures can reveal them transiently.

Playback:
- managed matching-rate WAV starts from stopped playhead;
- Play→Stop halts safely and final playhead is credible;
- playhead progression follows `AudioTrack.playbackHeadPosition`;
- mono reaches both output channels; stereo retains L/R;
- overlapping audible clips mix without source mutation;
- loop wraps exactly at loop out to loop in;
- automatic end stops at project end;
- sample-rate mismatch is rejected clearly until resampling is validated;
- repeated play/stop/loop does not leak, crash or corrupt project/media.

Routing / Options:
- input/output device lists refresh from Android enumeration;
- input is global, not per-track;
- selecting Main Output persists a stable signature, not a raw numeric device ID;
- selected output is re-resolved after reconnect/re-enumeration;
- available preferred output is applied to playback;
- missing/rejected preferred output falls back to Android Auto without crash or false success;
- Export remains disabled until a real export engine exists.

Mixer / track mix:
- timeline and Mixer share selected track;
- hidden/visible and temporary/pinned states behave independently;
- track gain persists and audibly affects playback;
- pan persists and affects stereo balance;
- Mute always removes a track from mix;
- when any Solo exists, only non-muted solo tracks are audible;
- mix controls remain locked during active transport/trim/import transactions.

Master / metering:
- Master gain persists in project JSON and reopens at the saved value;
- legacy project JSON without master gain opens at 0 dB;
- track/master gain validation rejects values outside -60..+12 dB;
- per-track meter represents the post-track-bus signal before Master;
- Master meter represents post-master-gain signal before output clamp;
- overload above 0 dBFS is observable as clipping rather than hidden by clamp;
- peak attack is immediate, peak hold is deterministic and decay follows elapsed time;
- meters clear safely on Stop/error and are never persisted as project state.

Physical tablet evidence required before closing M4 consolidation:
- new signed candidate built from the current branch head (historical alpha05 is insufficient for newer UI/Mixer work);
- Home/Studio visual/fullscreen review;
- managed WAV import and reopen after external origin is unavailable;
- Play from 0 and moved playhead; Stop; short repeated loop;
- trim in/out, Apply/Cancel and source-hash immutability;
- track selection + Mixer open/close/pin;
- gain/pan/mute/solo behavior;
- Options output selection and disconnect/reconnect fallback;
- Master gain + track/master meters, including visible response to real audio;
- at least 10 repeated play/stop/loop cycles;
- representative mono/stereo managed WAV at supported matching sample rates;
- no P0/P1 regression.

Recording remains M5 and must not be considered implemented because arm metadata or a disabled record affordance exists.

## Regression severity
P0: data loss/security/project corruption. P1: crash/wrong route/destructive source mutation/core workflow unusable. No milestone closes with known in-scope P0/P1.
