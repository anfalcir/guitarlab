# Product Requirements

Updated: 2026-10-01

Stable product identity, support boundaries, proportional quality, freeze policy and maintenance triggers are governed by `PROJECT_IDENTITY.md` and D-090.

## Projects and Home library
- Create Blank or Guitar-template projects.
- Persist project identity, template, sample-rate policy, groups, tracks, roles, clips, takes and Master state.
- Reopen internal projects without data loss; writes must be atomic or safely replaceable.
- Save a portable versioned `.guitarlab` package and reopen it from Home as an independent project without silently overwriting the original.
- Validate package/project/media identity and reject structurally invalid, incompatible or unsafe packages before publication.
- Read supported current-line schema/package versions safely and reject unknown incompatible versions without corruption; no indefinite historical migration promise is implied.
- Cryptographic/revision integrity of stored project state is verified before compatibility repair is applied to the decoded in-memory representation.

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

## Prepare workflow

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
- Cleanup uses reachability and may not delete media referenced by project state, retained history, an in-flight package/import transaction or pending backup.
- Re-separation/backing regeneration creates a new asset/version rather than mutating immutable bytes.

### Cloud backup destination
- Cloud backup avoids requiring full large-media reupload for every metadata-only project edit.
- Immutable content-addressed media plus transactional project revision metadata is the target architecture.
- Upload/restore is resumable where applicable, server-confirmed, checksum-validated and conflict-aware.
- Restore stages and validates all required project/media state before atomic publication.
- Remote Drive heads/manifests are authoritative for version history; a local metadata cache may accelerate display but may never replace remote verification.
- Backup screen entry and explicit refresh verify current remote heads; cached versions may remain visible while that verification runs.
- Automatic backup must remain correct with no Backup UI alive. Off-screen completion must not require a catalog read; a later screen entry must reconcile from Drive.
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
- Separating a stereo clip into mono tracks must use the actual derived media frame/rate bounds and preserve unambiguous take metadata/active-state semantics.
- If an existing take lineage is shared across multiple temporal clips, stereo separation must fail closed rather than silently inventing lineage.
- Failed/stale/invalid drag must not partially commit project/history/media state.

## Managed media
- External files are read-only origins.
- Import copies selected media into project-controlled authoritative storage before dependency is committed.
- Native managed source is immutable.
- Optional PCM edit proxy is derived and never replaces source identity.
- Conversion/resampling/waveform/freeze/export outputs are derivatives.
- Media still referenced by any clip/take must not be deleted.

## Import and codecs
The currently implemented format set includes WAV PCM, FLAC, AIFF/AIFC PCM, MP3, AAC/M4A, OGG Vorbis and Opus, subject to the exact claims and device qualifications in `CODEC_SUPPORT_MATRIX.md`. This is not a promise to accept arbitrary files or expand the matrix.
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
- Studio supports a primary MAIN output and an optional explicit secondary CUE output for per-track monitoring/playback routing.
- Each track persists a backward-compatible output route; legacy projects default to MAIN.
- The mixer headphone/CUE control may route any track to the secondary bus; the first UI contract is exclusive MAIN ↔ CUE while the engine retains MAIN_AND_CUE capability for future controlled use.
- Per-track output-route changes are allowed only with transport stopped; Play/REC uses a stable routing snapshot.
- CUE requires an explicit MAIN route, a resolvable low-latency secondary device and live proof that MAIN/CUE are distinct endpoints. Bluetooth is excluded from synchronized CUE because its presentation latency is not suitable for the alignment contract. CUE never falls back silently to MAIN.
- The secondary CUE sink is opened only when playable content is assigned to a CUE-routed track; empty CUE tracks do not create an idle secondary stream.
- CUE output writes may not block MAIN. Secondary backpressure/partial writes suppress CUE fail-closed instead of stalling the primary render loop.
- Before audible use, MAIN and CUE must each provide stable presentation-clock evidence and their estimated stream origins must differ by no more than 12 ms. During playback, repeated presented-frame divergence beyond the continuous guard (~15.6 ms at 48 kHz) suppresses CUE. If the CUE route is lost, rejected, converges with MAIN, lacks stable clock evidence, exceeds either synchronization guard or cannot accept complete non-blocking chunks, CUE is silenced and the user receives explicit feedback; safe MAIN playback continues.
- Playback/backing/monitoring return must never feed the recording writer.
- Record uses visible 3-second countdown plus zero-time revalidation of permission, route, project and exactly one armed track.
- Finalized takes enter immutable managed storage transactionally; zero-frame attempts create no clip.
- A track may retain multiple takes with one active take for playback/export.
- Current-line recordings always persist take lineage; narrowly recognizable legacy managed recordings may recover missing take metadata deterministically and idempotently.
- Compatibility recovery must never classify a generic imported WAV as a recording merely from its track role or filename coincidence.

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
- Visible playhead progress follows sink presentation timing when reliable Android audio timestamps exist, re-anchors after seek/flush, and must never advance beyond audio frames already accepted by the output sink. A hidden fixed startup delay is not a valid synchronization strategy.
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
- Regression scope protects current editing, persistence, audio, routing, timing, waveform, Home and backup behavior when a candidate can materially affect it.
- Source materialization must be deterministic, hash-verified, idempotent and fail closed on drift.
- Signed homologation uses CI-only signing material and locked certificate verification.
- Portable package extraction defends against traversal/out-of-root writes and bounded-resource abuse.
- Keystores, credentials and local SDK configuration are never committed.
- Physical homologation remains distinct from software CI and evaluates only target-device behavior relevant to the exact signed candidate and frozen baseline.
- Digitally provable claims are removed from manual QA. Repeat physical testing only when a source/backend change invalidates relevant evidence or the owner deliberately includes the capability in the frozen baseline.
