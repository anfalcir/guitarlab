# M4 alpha07 commercial-polish homologation checklist

Target line: GuitarLab Studio `0.2.0-alpha07`, versionCode 8.
Target physical baseline: Samsung SM-X230, Android 16 / API 36.
Pocket Amp M2 hardware gate remains closed separately. This checklist is the M4 Studio consolidation gate for the commercial-polish/Mixer V3 checkpoint.

## Pre-conditions
- use only the signed alpha07 artifact produced by the controlled CI signing job after the alpha07 software gate is green;
- install over the previous signed GuitarLab build without uninstalling;
- confirm existing project compatibility and preservation of managed media;
- create a new project and import at least one supported WAV;
- where practical, use at least two tracks with audible material to validate solo/meter/mix behavior.

## Commercial UI / pt-BR
- Home, Novo projeto, Studio and Opções present normal user-facing copy in pt-BR;
- no milestone/homologation/development wording appears in the normal creative workflow;
- Home/Studio navigation and frequent actions use clear icons without ambiguous unlabeled glyphs;
- TalkBack/accessibility descriptions exist for icon-only actions where applicable;
- Graphite theme has clear visual hierarchy and is no longer overwhelmingly monochrome teal.

## Studio top bar
- project name and compact track/clip count appear at left;
- Return to start, Play/Stop, Record affordance and Loop are centered;
- Mixer, Opções and Início are icon-only at right;
- former bottom transport bar is absent;
- Record remains disabled in M4 and is not a failure; real recording is M5.

## Timeline alignment
- timeline marker rail starts exactly at the waveform/timeline origin, not at the left edge of the whole screen;
- ruler timestamps align with the same waveform width used by marker dragging;
- blue Playhead maps to the audible waveform position;
- green Loop markers appear only when Loop is enabled;
- mustard Trim markers appear only during trim;
- trim playback alignment remains correct after Apply;
- scissors opens trim; trash removes a clip; no clip mute shortcut is shown beside trim.

## Mixer pin/navigation persistence
- open Mixer temporarily: PushPin and Close are visible;
- tap PushPin: Mixer becomes pinned and only Close remains;
- enter Opções and return to Studio: pinned Mixer is still pinned/visible;
- tap Close on a pinned Mixer: Mixer closes and becomes unpinned;
- return through Opções again: it remains unpinned.

## Mixer V3 layout
- Master remains fixed at right while track strips scroll horizontally at left;
- with enough tracks to overflow, horizontal scrolling never moves Master off-screen;
- selected track and its Mixer strip use coherent identity/highlight;
- each track color is consistent across sidebar stripe, clip/waveform and Mixer strip.

## M / S / R visual states
- inactive M/S/R are subdued rounded-square buttons;
- active M is red without a checkmark;
- active S is amber/yellow without a checkmark;
- active R is record-red without a checkmark;
- R persists as arm metadata after project reopen;
- Record transport remains disabled; Arm does not imply capture support.

## Live gain / pan / Master
- start playback and drag track volume: audible level changes without Stop/restart;
- drag track pan during playback: stereo position changes live;
- drag Master gain during playback: final output level changes live;
- finish each gesture, stop/reopen project, and confirm persisted values;
- gain has a visible 0 dB reference and a narrow useful snap to 0;
- pan has a visible center reference and a narrow useful snap to center;
- nearby fine values remain selectable; snapping is helpful rather than obstructive.

## Metering / clipping latch
- track Peak/RMS meters respond to the post-track-mix signal;
- Master Peak/RMS responds to the post-Master pre-clamp signal;
- create/observe an overload if safely possible: corresponding CLIP badge appears;
- Stop: moving meter resets but CLIP badge remains;
- tap the CLIP badge: it clears without a separate X;
- trigger CLIP again, then start a new Play: old latch clears before the new run;
- no stale clip warning remains from the previous run unless clipping occurs again.

## Track management
- add a new track from the Pistas header;
- open track Settings gear;
- rename track and verify persistence;
- choose several colors and verify timeline/Mixer synchronization;
- move track up/down and verify stable ordering after reopen;
- delete an empty track successfully;
- attempt to delete a track containing clips: deletion is blocked with a clear message;
- import WAV into a newly created track.

## Managed-media / playback regression
- existing alpha06/legacy projects still open; legacy tracks without explicit color render with a stable fallback color;
- imported WAV remains playable if the original external URI becomes unavailable;
- Play from frame 0 and from a moved playhead;
- Stop mid-song and verify stable playhead behavior;
- enable/disable loop and validate repeated loop wrapping;
- apply/cancel trim and verify immutable source behavior;
- repeat Play/Stop at least 10 times with Mixer visible and hidden;
- no P0/P1 crash, hang, project corruption or managed-media loss.

## Result policy
PASS requires:
- signed artifact identity/signature/hash validated before installation;
- no P0/P1 issue in this scope;
- live mixer, persistent pin, track management, marker alignment and pt-BR commercial UI behaving as specified;
- evidence persisted in repository/PR before PR #1 is marked ready for merge.
