# Test and Homologation Plan

## Software gate for every development checkpoint
Mandatory: source materialization, unit tests, Android Lint, debug APK assembly, diagnostics/artifact upload. Green CI is software evidence only.

## Signed homologation
Only explicit candidates restore CI signing material, build signed APK, verify locked signer fingerprint, publish hashes/identity, then destroy temporary signing assets.

## M2 physical audio gate — OPEN
Pocket Amp USB routing, play/record/duplex, disconnect/reconnect, permission handling and repeated stability remain physically required.

## M3 codec gate
Per format/variant: metadata, complete frames, random seek, malformed rejection, decoded sample safety, Android provider access and real-device representative files.

## M4 Studio gate
Managed media integrity:
- external original read-only;
- complete internal source copy before project dependence;
- immutable managed source survives loss of external origin;
- move/trim/mute/gain/waveform never alter source bytes;
- waveform cache regenerates safely.

Timeline/UX:
- marker heads remain >=48dp and usable in light/dark themes;
- playhead blue, loop green, trim restrained mustard, record red;
- marker/import/clip edits are disabled while transport is active.

Playback checkpoint:
- supported managed WAV starts from current stopped playhead;
- Play changes to Stop and Stop halts safely;
- playhead progression follows `AudioTrack.playbackHeadPosition`, not UI wall-clock timing;
- mono output is heard in both stereo channels; stereo keeps L/R;
- overlapping audible clips mix without source mutation;
- loop wraps exactly at loop out to loop in and repeats without marker crossing;
- automatic end stops at project end;
- a sample-rate mismatch is rejected clearly until resampling is validated;
- stopping and replaying repeatedly does not leak/crash or corrupt project/media;
- playback remains functional after the original external file is unavailable.

Physical tablet evidence required before closing this M4 playback gate:
- play from frame 0;
- seek marker while stopped then play from that position;
- stop mid-song and verify final playhead position;
- loop a short region repeatedly;
- confirm markers and clip edits are locked while playing;
- repeat play/stop/loop sequence at least 10 times;
- test representative mono/stereo managed WAV at supported matching sample rates.

Recording remains M5 and must not be considered implemented merely because playback is enabled.

## Regression severity
P0: data loss/security/project corruption. P1: crash/wrong route/destructive source mutation/core workflow unusable. No milestone closes with known P0/P1 in scope.
