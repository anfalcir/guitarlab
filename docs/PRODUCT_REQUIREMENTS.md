# Product Requirements

Updated: 2026-09-16

## Projects and Home library
- Create Blank or Guitar-template projects.
- Persist project identity, template, sample-rate policy, groups, tracks, roles, clips, takes and Master state.
- Reopen internal projects without data loss; writes must be atomic or safely replaceable.
- Save a portable versioned `.guitarlab` package and reopen it from Home as an independent project without silently overwriting the original.
- Validate package/project/media identity and reject structurally invalid, incompatible or unsafe packages before publication.
- Support future schema/package evolution without silently corrupting older projects.

Home project-library requirements:
- Search project names in real time without case or diacritic sensitivity.
- Search MUST operate in memory over the current repository snapshot; typing MUST NOT reread project files.
- Template, content and sample-rate filters are independently combinable.
- Content filters distinguish projects with recordings, projects with any audio/clips and projects with no clips.
- Sample-rate filters support Auto, 44.1, 48, 88.2 and 96 kHz.
- Ordering supports modified newest/oldest, created newest/oldest and name A–Z/Z–A.
- Ordering MUST be deterministic under equal primary keys.
- Default ordering is modified-descending.
- A narrowed result displays visible/total count.
- True-empty and no-query-result states are distinct.
- Clearing filters does not silently reset the selected sort order.
- Search/filter/sort is non-destructive and MUST NOT mutate project content or package schema.

## Tracks, roles and mixing
- Built-in guitar-oriented roles plus user-defined roles.
- Grouping, ordering and clear track identity.
- Arm, mute, solo, gain and bipolar pan are functional persisted state where applicable.
- Mono/stereo is the primary V1 channel scope.
- Playback/export respect track gain/pan/mute/solo, clip gain and Master gain.

## Timeline and clips
- Non-destructive clips reference project-managed immutable source assets.
- Persist track target, timeline start, source start, duration, gain and mute.
- Move, trim, split, duplicate and remove are metadata operations; source bytes remain immutable.
- Waveform/proxy/render data are derived and independently replaceable/regenerable.
- Track/clip drag remains stable through edge autoscroll.
- Trim exposes independently acquireable start/end handles with deterministic pointer→frame mapping.
- `Excluir clipe`, `Limpar pista` and `Excluir pista` remain distinct scopes.
- Drag-to-trash uses the same confirmed domain deletion as `Excluir clipe`; cancel is a no-op.
- Split recording segments may share take lineage, but moves/deletions must never create dangling take references.
- Failed/stale/invalid drag must not partially commit project/history/media state.

## Managed media
- External files are read-only origins.
- Import copies selected media into project-controlled authoritative storage before dependency is committed.
- Native managed source is immutable.
- Optional PCM edit proxy is derived and never replaces source identity.
- Conversion/resampling/waveform/freeze/export outputs are derivatives.
- Media still referenced by any clip/take must not be deleted.

## Import and codecs
Target V1 interoperability includes WAV PCM, FLAC, AIFF/AIFC PCM, MP3, AAC/M4A, OGG Vorbis and Opus subject to `CODEC_SUPPORT_MATRIX.md`.
- Accept common mono/stereo sources.
- WAV PCM core covers verified U8/S16/S24/S32/Float32 variants.
- Android media formats may decode into managed PCM proxies.
- Reject malformed/truncated/unsupported sources safely without committing a broken clip.
- Never advertise a format merely because a code path exists.

## Sample-rate handling
- Projects may use Auto or fixed rate.
- Equal-rate media may pass through.
- Mismatched media requires explicit validated resampling; no silent speed/pitch change.
- Resampling creates a derivative and never overwrites source.

## Recording and audio I/O
- Android input/output discovery and semantic route visibility.
- USB-host support, especially M-VAVE MK-300-class interfaces.
- Safe microphone permission/privacy handling.
- Production playback/capture distinct from diagnostics.
- Explicit selected input fails closed unless Android confirms the effective route; microphone fallback is forbidden.
- Playback/backing/monitoring return must never feed the recording writer.
- Record uses visible 3-second countdown plus zero-time revalidation of permission, route, project and exactly one armed track.
- Finalized takes enter immutable managed storage transactionally; zero-frame attempts create no clip.
- A track may retain multiple takes with one active take for playback/export.

### Recording synchronization
- Never correct device/session timing with a hard-coded global offset.
- Distinguish per-session capture/backing mapping, accepted route+rate calibration, residual fine adjustment and punch/pre-roll logic.
- Trustworthy Android audio timestamps/monotonic clocks are preferred with bounded fallback.
- Stale/backwards timestamp evidence fails closed.
- Compensation components combine exactly once; double compensation is blocking.
- Placement/trim remain in valid timeline/source bounds, including zero-time starts and pathological arithmetic bounds.
- Calibration/fine adjustment is scoped to exact effective input+output+sample-rate identity.

### Live recording waveform
- Captured frames/time coverage is authoritative, not callback count.
- Envelope points carry explicit frame coverage and normalized peak.
- Bounded compaction preserves total covered time and important transients.
- UI publication is bounded/conflated.
- Finalized waveform derives from committed media and remains aligned to clip placement.

## Transport and practice workflow
- Canonical states: STOPPED, PLAYING, RECORDING.
- Primary controls: return-to-start, play/stop, record, loop.
- Return-to-start remains available during playback.
- Structural timeline editing is locked during incompatible states.
- Support Reference, My Guitar and Both audition modes.
- Persist markers, named sections and loop selection.
- Automatic sections are previewable suggestions and never commit silently.
- Punch derives from loop and handles pre/post-roll/timing without redefining normal Play.
- Level analysis reports RMS/peak and requires explicit application of recommendation.

## Master export
- WAV IEEE Float32;
- FLAC lossless;
- MP3 320 kbps where an encoder is actually available.

Offline export respects current timeline/mix and never modifies authoritative source/proxy media. Publication to SAF destination is staged/validated first and handles cancellation/failure safely.

## UX and accessibility
- Tablet-first readability, responsive layouts and large touch targets.
- Clean modern Graphite Studio language.
- Technical diagnostics remain separate from creative flow.
- No fake enabled controls for unimplemented features.
- Home project search, filter and sort controls expose meaningful accessibility semantics and remain usable at larger font scale.
- The same in-app GuitarLab guide is reachable from Home and Studio and stays synchronized with visible workflows.

## Reliability and security
- Mandatory promoted-RC gates: JVM/unit, Android Lint, debug/release assembly, API36 connected regression, isolated target geometry, exact artifact provenance and locked signing verification.
- Current regression scope retains prior editing/persistence/audio/routing/timing/waveform behavior plus H23b and H24 Home-library policy/semantics.
- Source materialization must be deterministic, hash-verified, idempotent and fail closed on drift.
- Signed homologation uses CI-only signing material and locked certificate verification.
- Portable package extraction defends against traversal/out-of-root writes and bounded-resource abuse.
- Keystores, credentials and local SDK configuration are never committed.
- Physical homologation remains distinct from software CI; RC3 closes only after residual target-only checks receive explicit approval.
