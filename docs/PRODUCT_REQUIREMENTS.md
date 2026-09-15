# Product Requirements

Updated: 2026-09-14

## Projects
- Create Blank or Guitar-template projects.
- Persist project identity, template, sample-rate policy, groups, tracks, roles, clips, takes and Master state.
- Reopen internal projects without data loss; writes must be atomic or safely replaceable.
- Save a portable versioned `.guitarlab` package and reopen it from Home as an independent project without silently overwriting the original.
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
- Move, trim, split, duplicate and remove are metadata operations; source bytes remain immutable.
- Waveform/proxy/render data are derived artifacts and independently replaceable/regenerable.
- Track and clip drag use the shared workspace coordinator and remain stable through edge autoscroll.
- Trim MUST expose independently acquireable start/end handles with large touch targets and deterministic pointer→frame mapping; passive labels/bubbles must not steal the handle gesture.
- Clip-level removal MUST be available as explicit confirmed `Excluir clipe`, separate from `Limpar pista` and `Excluir pista`.
- Dragging a clip MAY expose drag-to-trash, but trash drop MUST use the same confirmed domain deletion as `Excluir clipe`; cancel is a no-op.
- Split segments may share one recording-take lineage. Moving/deleting one segment MUST NOT leave siblings with dangling `takeId`/`RecordingTake.clipId` references.
- If the canonical take clip is removed/moved and siblings survive, one surviving sibling MUST be promoted deterministically. The take is removed only when no segment remains.
- Failed/stale/invalid drag MUST NOT partially commit project/history/media state.

## Managed media and source immutability
- External files are read-only origins.
- Import MUST copy the selected file into project-controlled source storage before the project depends on it.
- The managed native source is authoritative and immutable.
- Non-WAV/native formats that require conversion for Studio editing use a separate managed PCM WAV edit proxy; the proxy never replaces the source identity.
- Conversion, resampling, waveform, proxy, freeze and export outputs are derivatives.
- Original URI/path is provenance only after successful ingestion.
- Deleting one clip segment MUST NOT delete a source/proxy still referenced by another clip/take.

## Import
Target V1 interoperability includes WAV PCM, FLAC, AIFF/AIFC PCM, MP3, AAC/M4A, OGG Vorbis and Opus, subject to `CODEC_SUPPORT_MATRIX.md`.
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
- Resampling creates a derivative and never overwrites source.

## Recording and audio I/O
- Android input/output discovery and route visibility.
- USB host support, especially M-VAVE MK-300-class interfaces.
- Safe microphone permission/privacy handling.
- Production playback/capture path distinct from diagnostics.
- Explicitly selected input must fail closed unless Android confirms that effective route; microphone fallback is forbidden.
- Playback/backing and monitoring return must never be mixed into the app's recording writer.
- Record uses a visible **3-second** centered translucent countdown overlay and zero-time revalidation of permission, route, project and exactly one armed track.
- Finalized takes enter immutable managed storage transactionally; zero-frame attempts create no clip.
- A track may retain multiple takes, with exactly one active take used by playback/export.

### Recording synchronization
- GuitarLab MUST NOT correct device/session timing with a hard-coded fixed offset.
- Recording session timing MUST distinguish per-session capture/backing startup skew from accepted route calibration/round-trip latency and punch/pre-roll offsets.
- Trustworthy Android audio timestamps/monotonic clocks SHOULD be used where available with bounded fallback.
- Compensation components MUST be combined exactly once; double compensation is a blocking defect.
- Final take placement/trim MUST remain in valid timeline/source bounds, including zero-time starts.
- Route calibration remains scoped to effective route/sample-rate identity; volatile Android device IDs are not durable identity.

### Live recording waveform
- Live waveform MUST use captured frame/time coverage as its timebase, not callback count.
- Envelope points MUST carry explicit frame coverage and normalized peak.
- Bounded compaction MUST preserve total covered time and important transients.
- UI publication MUST be bounded/conflated so callback bursts do not produce queued catch-up animation.
- `recordingFrames` remains authoritative for live clip width.
- Finalized waveform is regenerated/derived from committed media and must align to the same clip start/end.

## Transport and monitoring
- Canonical transport states: STOPPED, PLAYING, RECORDING.
- Primary controls: return-to-start, play/stop, record, loop.
- Structural timeline editing is locked during incompatible transport/recording states.
- Hardware-presented audio clock drives playback UI where implemented.
- Monitoring is explicit and route-aware.
- Return-to-start remains available during playback.
- Play/Stop is visibly disabled during recording; REC is the recording stop control.

## Practice workflow
- Support Reference, My Guitar and Both audition modes.
- Persist markers, named sections and loop selection.
- Automatic section analysis creates reviewable suggestions and never commits silently. Suggested/previewed boundaries stay inside real project duration, reject edge-adjacent micro-sections and never render beyond project end.
- Punch recording derives its region from loop and accounts for pre-roll, post-roll and latency without redefining ordinary Play behavior.
- Track level analysis reports RMS/peak and offers a bounded recommendation requiring explicit application.

## Mixing
Track gain/pan/mute/solo, clip gain, summing, meters and Master gain are part of the current Studio contract. More advanced processing is additive and must not block the core guitar practice/recording workflow.

## Master export
Requested final-output scope:
- WAV IEEE 32-bit float master;
- FLAC lossless delivery path;
- MP3 320 kbps delivery path.

Export renders current project/timeline/mix offline into a floating-point master before format-specific delivery encoding. It respects clip placement/trim/gain, track gain/pan/mute/solo and Master gain. It creates a new destination and never modifies source/proxy media.

FLAC stream structure/extraction is covered by API 36 instrumentation. MP3 depends on a device-exposed Android encoder and must fail clearly when unavailable.

## Output UX
A dedicated Share icon in the Studio top bar opens `Salvar e exportar`, separating editable `.guitarlab` persistence from final-audio masters. Options remains for audio routes, monitoring, preferences, import information and diagnostics.

## UX
- Tablet-first readability and large touch targets.
- `Comparação` and `Timeline` use available width without accidental dead space; narrow layouts may stack rather than clip controls.
- `Auto seções` owns a fixed slot: preview replaces it with `Aplicar` + red `X` without shifting neighbors.
- The same in-app GuitarLab guide is reachable from Home and Studio.
- Clean modern Graphite Studio language.
- Technical diagnostics remain separate from creative flow.
- No fake enabled controls for unimplemented features.
- Playhead/Loop/Trim/recording semantics and colors follow timeline guidelines.
- Populated tracks use a clear edit affordance.

## Reliability and security
- Unit tests, Android Lint, debug/release assembly, API 36 integration and target geometry are mandatory automated gates for a promoted signed RC.
- H0–H6 regressions for clip lineage, trim interaction, recording timing and live waveform are part of the active RC gate.
- Signed homologation builds use CI-only signing material and locked certificate verification.
- Project-package extraction defends against traversal/out-of-root writes and bounds resource usage.
- Keystores, credentials and local SDK configuration are never committed.
- Physical device homologation remains distinct from software CI; the active RC cannot close until its residual target-only checklist receives explicit approval.
