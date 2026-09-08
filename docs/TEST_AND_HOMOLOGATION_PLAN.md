# Test and Homologation Plan

## Software gate for every development checkpoint
Mandatory: source materialization, unit tests, Android Lint, debug APK assembly, diagnostics/artifact upload. Green CI is software evidence only.

## Signed homologation
Only explicit candidates restore CI signing material, build signed APK, verify locked signer fingerprint, publish hashes/identity, then destroy temporary signing assets.

## M2 physical audio gate — HOMOLOGATED / PASS
Target evidence: Samsung SM-X230, Android 16/API 36, GuitarLab Studio alpha04 diagnostic path, Pocket Amp USB audio.
Validated checklist:
- correct USB input/output enumeration and requested routing;
- isolated Play PASS;
- isolated Record PASS for silence/frame progression and real-guitar non-zero signal;
- Duplex PASS, including a reported 44.1 kHz stereo FLOAT32 run with 220500 captured frames, 220500 output frames, peak 76%, RMS 32% and `outputUnderruns=0`;
- hot-unplug during Play PASS as controlled failure/no crash;
- hot-unplug during Record PASS as controlled failure/no crash;
- hot-unplug during Duplex PASS as controlled failure/no crash, with route fallback detected rather than false success;
- idle disconnect/reconnect PASS; Android may assign new USB device IDs after re-enumeration, so IDs must never be treated as stable identity;
- RECORD_AUDIO permission-denied path PASS/no crash;
- repeated stability sequence PASS as reported by the tester.

This closes M2 for this target tablet/interface combination. Additional hardware models require their own compatibility evidence, not reopening this result.

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
- marker/import/clip edits are disabled while transport is active;
- trim is draft/apply/cancel; playback and unrelated edits are blocked while a draft is open;
- T◀ and T▶ never cross and never expose frames outside immutable source bounds.

Playback checkpoint:
- supported managed WAV starts from current stopped playhead;
- Play changes to Stop and Stop halts safely;
- playhead progression follows `AudioTrack.playbackHeadPosition`, not UI wall-clock timing;
- mono output is heard in both stereo channels; stereo keeps L/R;
- overlapping audible clips mix without source mutation;
- loop wraps exactly at loop out to loop in and repeats without marker crossing;
- automatic end stops at project end;
- a sample-rate mismatch is rejected clearly until resampling is validated;
- stopping/replaying repeatedly does not leak/crash or corrupt project/media;
- playback remains functional after the original external file is unavailable.

Trim checkpoint:
- entering Trim shows mustard T◀/T▶ heads for the selected clip;
- dragging is smooth and tablet-friendly while STOPPED;
- left-edge trim changes timeline start and source start by the same delta;
- right-edge trim changes only visible end/length;
- extending outward is allowed only where immutable source frames exist;
- Cancel leaves project metadata unchanged;
- Apply persists one metadata edit and source file hash remains unchanged;
- Play/loop/import/mute/remove are not allowed to race an open trim draft;
- while PLAYING, all trim markers and edit actions remain locked.

Physical tablet evidence required before closing the M4 consolidation gate:
- play from frame 0;
- seek playhead while stopped, then play from that position;
- stop mid-song and verify final playhead position;
- loop a short region repeatedly;
- confirm markers and clip edits are locked while playing;
- test trim in/out, Apply and Cancel on a managed WAV;
- verify source file remains unchanged after trim;
- repeat play/stop/loop sequence at least 10 times;
- test representative mono/stereo managed WAV at supported matching sample rates.

Recording remains M5 and must not be considered implemented merely because playback is enabled.

## Regression severity
P0: data loss/security/project corruption. P1: crash/wrong route/destructive source mutation/core workflow unusable. No milestone closes with known P0/P1 in scope.
