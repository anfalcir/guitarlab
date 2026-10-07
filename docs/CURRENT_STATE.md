# Current State — GuitarLab Studio

Updated: 2026-10-07

## Status

**RC36 LEGACY SEQUENCE PROBE — IMPLEMENTED / SIGNED CI PENDING / PHYSICAL DIAGNOSTIC PENDING**

RC35 / Android CI #980 is the latest signed digital PASS.

## RC35 physical result

Owner evidence `GuitarLab-Diagnostics-1791414127697.zip` exercised both MAIN/CUE orientations. The recorded acoustic result is the same in both directions: `MAIN_LOST_IN_C`, followed by D/E/F = `CUE_ONLY`. MAIN-only preference reassertion is accepted but does not recover acoustic MAIN. Both AudioTracks remain logically routed, advancing and unmuted.

## Why RC36 exists

The remaining historical clue is the one owner-observed physically correct split in the rc31 line. RC31 differed from rc32+ in two relevant ordering details:

1. admission attempted a dual-`USAGE_MEDIA` configuration before Communication Split;
2. Communication Split selected the communication device before opening the new MAIN/CUE AudioTracks, then silently primed both, called `play()`, and reasserted MAIN + communication.

RC36 reconstructs those differences without changing Studio playback.

### G — communication before opening tracks

Select communication/CUE first; then open MAIN MEDIA + CUE VOICE_COMMUNICATION, prime both silently at per-track volume zero, play, reassert routes, stabilize muted, then expose low-level 440 Hz MAIN and 880 Hz CUE tones.

### H — media-first precondition + G

First reconstruct the old dual-MEDIA silent attempt: temporary MEDIA tracks, MAIN/CUE preferred devices, silent prime/play, post-play preference reassertion and bounded route/head activity. Release both, then execute the exact G sequence.

The media attempt is preserved as evidence even if it converges, because the hypothesis concerns its possible AudioPolicy side effect.

## Interpretation

- G=BOTH: communication-before-open ordering is sufficient.
- G=CUE_ONLY and H=BOTH: rc31 media-first preconditioning is the leading explanation.
- G=CUE_ONLY and H=CUE_ONLY: neither identified rc31 ordering difference is sufficient.
- Any BOTH result must be repeated and stability-qualified before production code changes.

The ZIP adds `audio-legacy-sequence-probe.json` with G/H owner outcomes, precondition routes/heads/writes, post-play reassertions, effective routes, playback heads, backpressure, underruns and stream volume/mute state.

## Safety

RC36 is diagnostic-only. Normal Studio routing, mixer behavior, zero-duck policy, fail-closed route checks and relative-drift supervision are unchanged.

`RELEASE_BASELINE.md` remains RC20. USB multichannel MAIN 1/2 + CUE 3/4 remains the deterministic professional fallback roadmap.
