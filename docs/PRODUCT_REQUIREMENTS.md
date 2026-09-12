# Product Requirements

Updated: 2026-09-09

## Projects
- Create Blank or Guitar-template projects.
- Persist project identity, template, sample-rate policy, groups, tracks, roles, clips and Master state.
- Reopen internal projects without data loss; writes must be atomic or safely replaceable.
- Save a portable versioned `.guitarlab` project package and reopen it from Home as an independent project without silently overwriting the original.
- Portable packages must include project metadata and all referenced authoritative managed sources; edit proxies may be included as derived caches.
- Validate manifest/project/media identity and reject structurally invalid, incompatible or unsafe packages before publishing a restored project.
- Support future schema/package evolution without silently corrupting older projects.

## Tracks and roles
- Built-in guitar-oriented roles plus user-defined roles.
- Grouping, ordering and clear track identity.
- Arm, mute, solo, gain and bipolar pan are functional persisted state where applicable.
- Mono/stereo channel layout is the primary V1 scope.

## Timeline and clips
- Non-destructive clips reference project-managed immutable source assets rather than embedding audio in project JSON.
- Persist track target, timeline start, source start, duration, gain and mute.
- Move, trim, split, duplicate/remove are metadata operations; later fades follow the same invariant.
- Waveform/proxy/render data are derived artifacts and independently replaceable/regenerable.
- Track and clip drag use the shared workspace coordinator and must remain stable through edge autoscroll.

## Managed media and source immutability
- External files are read-only origins.
- Import MUST copy the selected file into project-controlled source storage before the project depends on it.
- The managed native source is authoritative and immutable.
- Non-WAV/native formats that require conversion for Studio editing use a separate managed PCM WAV edit proxy; the proxy never replaces the source identity.
- Conversion, resampling, waveform, proxy, freeze and export outputs are derivatives.
- Original URI/path is provenance only after successful ingestion.

## Import
Target V1 interoperability includes WAV PCM, FLAC, AIFF/AIFC PCM, MP3, AAC/M4A, OGG Vorbis and Opus, subject to the explicit state in `CODEC_SUPPORT_MATRIX.md`.
- Accept common mono/stereo sources.
- WAV PCM core supports U8/S16/S24/S32/Float32 where verified.
- Dedicated AIFF path supports common uncompressed PCM AIFF/AIFC variants; compressed AIFF-C is not implied.
- Android media formats may decode into the managed PCM proxy.
- Validate and reject malformed/truncated/unsupported sources safely without committing a broken clip.
- Never advertise a format merely because a code path exists.

## Sample-rate handling
- Projects may use Auto or fixed project rate.
- Equal-rate media may pass through.
- Mismatched media requires an explicit validated resampling strategy; no silent speed/pitch change is allowed.
- Until production resampling is validated, mismatch may be blocked rather than handled incorrectly.
- Resampling, when implemented, creates a derivative and never overwrites source.

## Recording and audio I/O
- Android input/output discovery and route visibility.
- USB host support, especially M-VAVE MK-300-class interfaces.
- Safe microphone permission/privacy handling.
- Production playback/capture path distinct from diagnostics.
- Duplex startup must avoid starvation artifacts and route loss must fail predictably.
- Record uses visible countdown and zero-time revalidation of permission, route, project and exactly one armed track.
- Finalized takes enter immutable managed storage transactionally; zero-frame attempts create no clip.
- Valid takes create clip + waveform at the recording start position.

## Transport and monitoring
- Canonical transport states: STOPPED, PLAYING, RECORDING.
- Primary controls: return-to-start, play/stop, record, loop.
- Structural timeline editing is locked during incompatible active transport/recording states.
- Hardware-presented audio clock drives playback UI where implemented.
- Monitoring is explicit and route-aware.

## Mixing
Track gain/pan/mute/solo, clip gain, summing, meters and Master gain are part of the current Studio contract. More advanced processing is additive and must not block the core guitar practice/recording workflow.

## Master export
The alpha13 requested final-output scope is:
- WAV IEEE 32-bit float master;
- FLAC lossless delivery path;
- MP3 320 kbps delivery path.

Export renders the current project/timeline/mix offline into a floating-point master representation before format-specific delivery encoding. It must respect clip placement/trim/gain, track gain/pan/mute/solo and Master gain. It always creates a new destination and never modifies project source/proxy media.

FLAC/MP3 are not considered Android-verified until physical alpha13 validation. The current MP3 path depends on a device-exposed Android encoder and must fail clearly when unavailable.

Future AAC/M4A/Opus export, stems, ranges, additional WAV/FLAC bit-depth controls and dither policy remain later scope unless reprioritized.

## Output UX
A dedicated Share icon in the Studio top bar, immediately before Home, opens `Salvar e exportar`. The modal clearly separates:
- `.guitarlab` editable project persistence;
- final master formats.

Options remains for audio routes, monitoring, preferences, import information and diagnostics; it does not duplicate project-save/master-export as primary commands.

## UX
- Tablet-first readability and large touch targets.
- Clean modern Graphite Studio language.
- Technical diagnostics separate from everyday creative flow.
- No fake enabled controls for unimplemented features.
- Playhead/Loop/Trim/recording semantics and colors remain governed by timeline guidelines.
- Populated tracks use a clear pencil/edit affordance.

## Reliability and security
- Unit tests, Android Lint and debug assembly are mandatory software gates.
- Signed homologation builds use CI-only signing material and locked certificate verification.
- Project-package extraction must defend against traversal/out-of-root writes and bound resource usage.
- Keystores, credentials and local SDK configuration are never committed.
- Physical device homologation remains distinct from software CI; M5 cannot close without alpha13 physical approval.
