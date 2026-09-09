# M4 Track Mix Checkpoint

This checkpoint wires the Studio mixer foundation to persisted project state and real M4 playback.

## Implemented
- timeline track selection and Mixer selection share one selected track id;
- selected timeline track exposes a compact control affordance to open the Mixer;
- Mixer gain is editable from -60 dB to +12 dB and is persisted on `AudioTrack.gainDb`;
- Mixer pan is editable from L100 to R100 and is persisted on `AudioTrack.pan`;
- track Mute and Solo are persisted and affect playback audibility;
- clip gain and track gain are combined only in playback math; source media remains immutable;
- pan is applied in the stereo mix stage for mono and stereo sources;
- any active Solo restricts playback to soloed, non-muted tracks;
- Mute always wins over Solo;
- mixer edits remain locked while transport is active, during import, or while trim/edit transactions are open;
- slider drag uses a local draft and persists on gesture completion rather than saving on every pixel.

## Deliberately still gated
- track Arm/record remains an M5 function;
- track metering is not fake-enabled;
- master gain is not yet implemented;
- audio input/output routing remains centralized in Options;
- no live mixer automation while playback is running yet.

## Validation
Pure `TrackMixPolicy` tests cover mute/solo audibility, gain conversion and pan balance. Android CI must still pass unit tests, lint and debug assembly before this checkpoint is considered software-green.
