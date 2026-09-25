# Product Requirements

Updated: 2026-09-24

Stable product identity, support boundaries, proportional quality, freeze policy and maintenance triggers are governed by `PROJECT_IDENTITY.md` and D-090.

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

## Unified Prepare workflow
The approved successor product direction is governed by `history/UNIFIED_GUITARLAB_GBW_IMPLEMENTATION_ROADMAP.md`.

- One immutable projectId spans source acquisition, separation, prepared assets, Studio, exports and backup.
- A project may remain Studio-only; Prepare is optional and must not block Blank projects.
- Supported source acquisition publishes a project-managed immutable source only after validation.
- Demucs separation publishes exactly the validated expected stem set with source/model/job provenance.
- Remote separation uses Firebase/Cloud Run/temporary Storage as a transient processing plane; temporary cloud objects are not durable backup.
- Durable project backup remains a separate direct Google Drive concern.
- Prepared backing/reference media is project-managed and available to Studio without export/reimport.
- A newer prepared reference never silently replaces a reference already used by Studio; rebind is explicit and non-destructive.
- Study exports and Studio masters remain semantically distinct.
- Long-running preparation, separation, import, backup and restore operations are lifecycle-safe, idempotent and observable.
- Standalone GBW/H37/pre-unification migration is historical and outside the supported release scope unless a later explicit decision reopens it.
- No tuner, pitch/tuning conversion, BS-RoFormer or pitched-export scope is introduced by the unification.

### Unified managed assets
- Authoritative/derived media uses explicit asset identity, role, integrity metadata and provenance.
- Filename/display name is never asset or project identity.
- Cleanup uses reachability and may not delete media referenced by project state, retained history, pending migration or pending backup.
- Re-separation/backing regeneration creates a new asset/version rather than mutating immutable bytes.

### Unified cloud backup destination
- The successor cloud backup must avoid requiring full large-media reupload for every metadata-only project edit.
- Immutable content-addressed media plus transactional project revision metadata is the target architecture.
- Upload/restore is resumable where applicable, server-confirmed, checksum-validated and conflict-aware.
- Restore stages and validates all required project/media state before atomic publication.
- Legacy H37/GBW backup discovery/import is outside current support scope; current unified backup/restore integrity remains mandatory.

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
- Never correct device/session timing with a hidden hard-coded offset.
- Distinguish per-session capture/backing mapping, accepted route+rate physical calibration, global future-recording residual adjustment, take-specific post-recording synchronization and punch/pre-roll logic.
- Global fine adjustment is default-zero, route+rate scoped, bounded at ±500 ms and captured when REC starts; changing it never moves an existing take.
- Take-specific synchronization is stored on the take, applies only the delta from its prior value to the complete take lineage, preserves source bytes/trim offsets/durations and fails closed rather than crossing timeline frame zero.
- Silent digital verification may validate routes/clocks with PCM zero but must never be persisted as physical round-trip calibration.
- Physical calibration must confirm the exact live selected input/output before emitting a non-zero stimulus.
- Trustworthy Android audio timestamps/monotonic clocks are preferred with bounded fallback.
- Stale/backwards timestamp evidence fails closed.
- Compensation components combine exactly once; double compensation is blocking.
- Placement/trim remain in valid timeline/source bounds, including zero-time starts and pathological arithmetic bounds.

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
- Deployment profile is private single-owner/personal use, not public distribution or commercial service. Release gates are risk-based: data/media integrity, musical validity, primary acquisition, quota/cost, credentials, recovery and exact signing remain strict; non-applicable scanner severity, alternative performance cells, unsupported percentile claims and convenience diagnostics do not block by default.
- After owner acceptance, the signed APK and digest-pinned backend form a frozen appliance baseline. Do not perform maintenance-only rebuilds or upgrades without an observed regression, provider/platform deprecation, applicable known-exploited vulnerability, credential exposure, cost/integrity risk or owner-requested feature.
- Mandatory promoted-RC gates: relevant JVM/unit tests, Android Lint, release assembly, representative API36 regression, exact artifact provenance and locked signing verification. Debug assembly and isolated geometry run when affected by the candidate or when prior evidence is invalidated; unrelated flaky/cosmetic coverage may be quarantined with an explicit reason.
- Current regression scope retains prior editing/persistence/audio/routing/timing/waveform/Home/backup behavior through H35a, including global-vs-take synchronization and quiet/silent calibration contracts.
- Source materialization must be deterministic, hash-verified, idempotent and fail closed on drift.
- Signed homologation uses CI-only signing material and locked certificate verification.
- Portable package extraction defends against traversal/out-of-root writes and bounded-resource abuse.
- Keystores, credentials and local SDK configuration are never committed.
- Physical homologation remains distinct from software CI; current RC3/H37 evidence keeps its historical acceptance rules.
- For the approved unified successor program, digitally provable claims are removed from manual QA and the target is one consolidated final signed-candidate physical campaign, with repeat only when a source fix invalidates relevant evidence.
