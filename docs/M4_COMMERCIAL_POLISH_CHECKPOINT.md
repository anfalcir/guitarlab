# M4 Commercial Polish + Mixer V3 Checkpoint

This checkpoint is the product-facing refinement pass after the alpha06 M4 consolidation build. It does not change the immutable-managed-media contract and does not advance real recording into M4.

## User-facing language and navigation
- Normal application UI is pt-BR.
- Navigation and frequent commands prefer clear icons over repeated text labels when the icon is conventional and accessible.
- Icon-only controls carry semantic/content descriptions.
- Engineering milestone names and homologation language are removed from normal creative workflows; diagnostics remain intentionally available under advanced tools.

## Studio top bar
The Studio top bar is a three-zone layout:
- left: project identity and compact `N pistas · N clipes` summary;
- center: return-to-start, Play/Stop, Record affordance and Loop;
- right: Mixer, Options and Home icons.

The former bottom transport bar is removed so the timeline gains vertical space.

Record remains disabled until M5 capture/monitoring exists. The visible record/arm affordances must never be interpreted as implemented recording.

## Timeline alignment
The timeline ruler, marker rail and waveform lanes share one horizontal coordinate system. The marker rail begins after the exact same sidebar width/gap used by every track lane. Playhead/loop/trim markers therefore map directly onto the waveform time axis instead of spanning the sidebar area.

Loop heads appear only while Loop is enabled. Trim heads appear only while a trim draft is active.

Clip card actions are intentionally minimal:
- scissors: begin non-destructive trim;
- trash: remove clip metadata from the project.

The previous clip-level mute shortcut is removed from the card to reduce action clutter.

## Mixer pin persistence
Mixer pinning is a Studio UI preference stored outside project audio metadata.
- unpinned Mixer: PushPin + Close are shown;
- pinned Mixer: only Close is shown;
- closing a pinned Mixer also unpins it;
- pin state survives navigation through Options and back to Studio.

## Mixer V3 layout
- track strips occupy a horizontally scrollable region at the left;
- Master is outside that scroll container and remains fixed on the right;
- track identity color is shared with the timeline/sidebar/waveform;
- Master uses a distinct neutral/secondary treatment.

## Mute / Solo / Arm
Each track strip exposes M, S and R state buttons.
- inactive: subdued foreground/border, dark neutral surface;
- Mute active: red;
- Solo active: amber/yellow;
- Arm active: record red;
- no checkmarks are appended to the labels.

Mute/Solo/Arm are structural/session-state controls and remain disabled during active transport in the current deterministic M4 architecture. Arm is persisted project metadata only; capture remains M5.

## Live volume and pan
Track gain and pan are explicitly live-safe during playback.
- drag updates the running playback engine immediately;
- gesture completion persists the value to the project;
- track gain is applied at the track bus after clip gain and before track metering/summing;
- pan is applied at the same live track-bus stage;
- Master gain also supports live preview and persistence on gesture completion.

Controls include a visible neutral reference and a narrow soft-snap region around:
- gain: 0 dB;
- pan: center.

The snap is deliberately narrow enough to help returning to neutral without preventing precise nearby values.

## Metering and clip latches
Peak/RMS meter ballistics remain transient. A separate clip latch records overload events.
- track latch triggers when the raw post-track-mix peak exceeds 0 dBFS;
- Master latch triggers when the post-Master pre-clamp peak exceeds 0 dBFS;
- latch remains visible after Stop even though the moving meter resets;
- tapping the `CLIP` badge clears that latch directly;
- starting a new playback or recording attempt clears all latches before the new run.

No separate dismiss `X` is used.

## Track colors and management
`AudioTrack.colorIndex` is additive schema-v1 metadata with default `-1` for legacy projects. The UI maps it to a 20-color Graphite-compatible palette. Legacy `-1` tracks derive a stable visual color from their current order until the user explicitly chooses one.

Track settings support:
- rename;
- select one of 20 colors;
- move up/down;
- delete only when the track owns no clips.

New tracks can be added from the timeline sidebar header. New tracks are mono/generic by default and receive the next palette color.

## Visual direction
Graphite remains the base, but the product is no longer teal-monochrome:
- teal: GuitarLab identity/default accent;
- blue: secondary/navigation emphasis;
- amber: tertiary/solo/attention emphasis;
- red/coral: record/mute/error semantics;
- 20 track colors: track identity across timeline and Mixer.

## Gate
This checkpoint is software-pending until branch-head CI completes unit tests, Android Lint and debug APK assembly. After it is software-green, prepare a signed alpha07 candidate and run the full M4 physical tablet consolidation before PR #1 may leave draft state.
