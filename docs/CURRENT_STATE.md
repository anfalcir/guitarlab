# Current State

Last updated: 2026-09-09

## Stable baseline
- `main`: signed baseline `0.2.0-alpha03`, versionCode 4.
- `main` remains intentionally unchanged until the current M4 consolidation checkpoint is physically validated.

## Active development
- branch: `dev/parallel-m3-m5`
- draft PR: #1
- branch app version: `0.2.0-alpha07`, versionCode 8.
- alpha06 remains the last signed M4 artifact; alpha07 is the active commercial-polish/Mixer V3 software checkpoint and must pass its own CI before a new signed candidate is produced.

## Gates
- M2 software diagnostics: CI-green.
- M2 Pocket Amp physical homologation on Samsung SM-X230 / Android 16 API 36: **PASS / HOMOLOGATED**.
- M3 WAV codec core/Android path: software-green; tested tablet evidence exists for PCM24/44.1 kHz/stereo direct seek.
- M4: **IN PROGRESS**. Physical consolidation remains pending.

## M4 foundations already established
- single-screen, timeline-first Studio with immersive fullscreen;
- immutable project-managed WAV import, waveform cache and non-destructive trim;
- real managed-WAV Android playback with hardware-clock playhead, seek/start, Stop and loop;
- global audio input/output preferences with stable signatures and safe output fallback;
- persisted track gain, pan, mute and solo;
- persisted project Master gain;
- real per-track and Master peak/RMS metering with deterministic peak hold/decay;
- bottom Mixer Dock and centralized Options Center.

## Alpha07 commercial-polish checkpoint
The active checkpoint turns the M4 Studio from an engineering-oriented UI into a product-facing workspace while preserving the established audio/media invariants:
- pt-BR user-facing UI across Home, project creation, Studio, Options and diagnostic screens;
- icon-first navigation/actions where the meaning is unambiguous, with accessibility descriptions;
- transport moved to the center of the Studio top bar;
- persistent mixer pin preference; pinned mixer shows only Close, which also unpins;
- Mixer V3 with fixed Master strip at the right and horizontally scrolling track strips at the left;
- professional M/S/R state buttons: Mute red, Solo amber/yellow, Arm red, no checkmarks;
- Arm metadata is user-operable preparation only; real recording/monitoring remains M5 and the Record transport remains disabled;
- live track volume/pan and live Master gain during playback, with persistence on gesture completion;
- gentle snap around 0 dB / center pan plus an explicit neutral reference mark;
- latched per-track and Master CLIP indicators that survive Stop, clear by tapping the indicator, and reset at the start of a new playback/record attempt;
- 20-color track palette synchronized across sidebar, clips/waveforms and Mixer;
- track settings for rename, color, reorder and safe delete; new-track creation;
- clip actions reduced to scissors (trim) and trash (delete); clip mute action removed from the clip card;
- timeline marker rail and ruler share the exact same horizontal origin as track waveforms, eliminating the previous sidebar-width desynchronization;
- richer Graphite palette using teal identity plus blue/amber semantic accents and track colors instead of a mostly monochrome teal presentation;
- normal creative UI no longer exposes M2/M3/homologation/development wording; technical diagnostics remain available under advanced tools.

See `M4_COMMERCIAL_POLISH_CHECKPOINT.md` and `STUDIO_OPTIONS_AND_MIXER.md`.

## Compatibility and safety
- `AudioTrack.colorIndex` is additive metadata with default `-1`; schema-v1 projects without the field remain loadable.
- project-owned media stays immutable during ordinary editing.
- track deletion is blocked while the track still owns clips.
- live mix changes update the running playback engine without rewriting source media.
- structural edits remain STOPPED-only; live volume/pan/Master are the explicit transport-time exception.

## Signed candidate status
- signed alpha06 is historical evidence for the immediately preceding M4 state.
- alpha07 is **not yet a signed/physical candidate** until its branch-head software CI is green.
- after software green, generate a signed alpha07 artifact through the controlled CI path, inspect identity/hash/signer evidence, then run the updated physical M4 checklist on the Samsung SM-X230.

## Intentional limitations
- user-facing import currently begins with WAV;
- compressed formats remain planned/unverified;
- production resampling is not enabled;
- playback requires managed mono/stereo WAV clips at the project/playback sample rate;
- real recording, monitoring and record transport belong to M5;
- Arm state does not claim capture capability;
- export engine remains unimplemented/gated.

## Merge policy
PR #1 remains draft. Do not merge to `main` until the latest signed M4 candidate passes the physical tablet gate with no P0/P1 regression and the evidence is persisted in the repository/PR.
