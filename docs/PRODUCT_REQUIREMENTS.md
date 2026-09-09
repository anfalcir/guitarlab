# Product Requirements

## Projects
- Create Blank or Guitar-template projects.
- Persist project identity, template, sample-rate policy, groups, tracks, roles and clips.
- Reopen without data loss; writes must be atomic or safely replaceable.
- Validate references and reject structurally invalid project state.
- Support future schema evolution without silently corrupting older projects.

## Tracks and roles
- Built-in guitar-oriented roles plus user-defined roles.
- Grouping, ordering and clear track identity.
- Track state planned for arm, mute, solo, gain and pan.
- Mono/stereo channel layout where appropriate.

## Timeline and clips
- Non-destructive clips reference project-managed immutable source assets rather than embedding raw audio in project JSON.
- Persist track target, timeline start, source start, duration, gain and mute.
- Planned editing: move, trim, split, duplicate/remove and later fades as validated.
- Move/trim/split/gain/mute operations must never rewrite the managed source asset; they change project metadata only.
- Waveform/proxy/render data are derived artifacts and must be replaceable/regenerable independently from source assets.
- Waveform must represent the underlying audio source efficiently and must not require decoding the entire file on every render.

## Managed media and source immutability
- The user's external file is always treated as read-only input.
- Import MUST copy the complete selected file into app-controlled project storage before the project depends on it.
- After a successful import, normal project operation must depend on the internal managed copy, not continued availability of the original external file or SAF permission.
- The internal managed source copy is immutable for the lifetime of references to it. Editing never overwrites, truncates, normalizes, resamples, renames-in-place, or otherwise mutates it.
- Conversion, resampling, waveform, proxy, freeze and export outputs must be separate derived files/assets.
- If ingestion or validation fails before the project references the copy, the uncommitted copy may be discarded as transaction rollback.
- Removing a clip must not silently rewrite or repurpose its source file; orphan cleanup, when implemented, must be an explicit safe garbage-collection concern.
- Original URI/path may be retained only as provenance metadata and must not be a runtime dependency after successful ingestion.

## Import
Target V1 interoperability includes WAV, FLAC, AIFF, MP3, AAC/M4A, OGG Vorbis and Opus where Android/runtime licensing and decoder availability permit a robust implementation.
- Accept common mono/stereo sources.
- Support project-relevant rates 44.1/48/88.2/96 kHz; other valid source rates may be imported when resampling is available.
- WAV PCM support includes 8/16/24/32-bit integer and 32-bit float where the codec matrix says verified.
- Validate the managed copy and reject malformed/truncated sources safely.
- Never advertise a format before its software/device gate is passed.

## Sample-rate handling
- Projects may use Auto or a fixed project rate.
- Equal-rate media may pass through.
- Mismatched media must use an explicit resampling strategy rather than implicit speed/pitch changes.
- Resampling quality and performance must be tested before general release.
- Resampling must produce a derivative; it must never overwrite the immutable managed source.

## Recording and audio I/O
- Android input/output device discovery and route visibility.
- USB host support, especially Pocket Amp-class interfaces.
- Safe microphone permission/privacy handling.
- Low-latency playback/capture path for production use; diagnostics may use simpler probe paths.
- Duplex must avoid startup-starvation artifacts and report meaningful underruns.
- Hotplug/disconnect must fail in a controlled manner without app crashes.
- Record always uses a visible and cancelable five-second countdown, then revalidates permission, route, project and exactly one armed track at zero.
- Finalized takes enter immutable managed storage atomically. Zero-frame attempts create no clip; valid partial captures may be preserved after route loss.
- A successful take creates its AudioClip and waveform automatically at the REC start position.

## Transport and monitoring
- Canonical transport states are STOPPED, PLAYING and RECORDING; there is no separate pause button.
- Primary visible controls are return-to-start, play/stop, record and loop, represented by familiar symbols/icons rather than text labels.
- When PLAYING or RECORDING, user timeline editing is locked: playhead drag, loop/trim markers, clip move/trim, import and clip-edit actions cannot be changed until transport returns to STOPPED.
- The audio engine owns automatic playhead/record-head progression while transport is active.
- Seek and loop playback must be sample/time consistent with the project timeline and explicit resampling strategy.
- Monitoring must be explicit and latency-aware.
- Play/record controls must not be enabled before the production transport/record engine is implemented and its applicable gate passes.

## Mixing
Planned: track gain/pan/mute/solo, project summing, clip gain, meters, and export rendering. More advanced processing is additive and must not block the core guitar practice/recording workflow.

## Export
Planned targets include high-quality WAV (16/24-bit and 32-bit float where practical), FLAC (16/24-bit), and selected compressed delivery formats such as MP3, AAC/M4A and Opus when robustly supported. Export scope includes full mix and, where implemented, stems/tracks and selected timeline ranges. Export always writes a new destination/derivative and never modifies project source media.

## UX
- Tablet-first readability, large touch targets and scalable hierarchy.
- Clean modern visual language; light/dark adaptive theme.
- Technical diagnostics remain separate from everyday creative screens.
- No fake enabled controls for features that are not implemented.
- Directly manipulated timeline positions use explicit top marker heads with at least 48 dp touch targets; the thin guide line is not the primary drag target.
- Semantic timeline colors are restrained: playhead blue, loop green, trim muted mustard, recording red; color is never the only cue.
- The upper marker rail contains only Playhead and Loop. Trim is local to the active waveform, initialized at 35%/65%, with both precise times and selected-region-only fill.

## Reliability and security
- Unit tests, Android Lint and debug assembly are mandatory software gates.
- Signed homologation builds use private CI-only signing material and locked signer verification.
- Keystores, credentials and local SDK configuration are never committed.
- Physical device homologation gates are tracked separately from software CI.
