# Architectural and Product Decisions

Updated: 2026-10-03

This log records durable decisions that must survive chat/context loss. Later numbered decisions supersede earlier ones. Milestone-specific wording is historical unless a current live document retains the invariant; D-090 governs the proportional release and maintenance interpretation of every earlier decision.

## D-001 — Android-first
GuitarLab targets Android/tablet as the primary environment.

## D-002 — Blank and Guitar-template projects coexist
Users may start blank or from the Guitar template.

## D-003 — Roles are explicit metadata
Built-in/custom roles organize tracks without making structure rigid.

## D-004 — Non-destructive clip model
Move, trim, gain and mute change metadata, never immutable source audio.

## D-005 — Implemented media formats are explicit, not universal
WAV-first is not the only implemented path, but GuitarLab makes claims only for formats and owner use cases currently validated under `CODEC_SUPPORT_MATRIX.md`. This does not create a universal interoperability or matrix-expansion obligation.

## D-006 — Sample-rate mismatches are explicit
Never change speed/pitch accidentally. Mismatches require validated resampling.

## D-007 — Capability claims follow gates
Planned/partial features are not advertised as supported.

## D-008 — Historical M2 Pocket Amp hardware gate is closed
Pocket Amp + Samsung SM-X230 / Android 16(API 36) is PASS/CLOSED only for the hardware combination tested at that milestone. D-052 supersedes it for the active physical target: Samsung SM-X230 + M-VAVE MK-300.

## D-009 — Production audio is distinct from diagnostics
Diagnostic probes never substitute for Studio transport/recording architecture.

## D-010 — Imported/recorded media is project-managed and immutable
External documents are read-only origins; accepted source media is promoted to managed immutable storage. Waveforms/proxies/renders are derivatives.

## D-011 — Graphite Studio visual language
Use near-black layered graphite surfaces, restrained product identity and semantic functional colors.

## D-012 — Build/signing continuity
Historical rule superseded by D-050. Signing secrets remain environment-only and signer identity must be cryptographically verified.

## D-013 — Documentation is canonical project context
Repository documentation defines scope, roadmap, decisions, active gates and current state; historical candidate files are evidence, not current truth.

## D-014 — Timeline controls use explicit marker heads
Playhead and Loop use clear top marker heads with ergonomic targets. Trim handles stay local to the active waveform. Playhead is blue, loop green, trim mustard and recording red.

## D-015 — Structural timeline edits require compatible stopped state
Marker movement, clip structural edits and import are blocked during incompatible transport/recording states. Live mix controls follow their own safe policy. D-061 later creates a deliberate exception for playhead-only seeking during Play.

## D-016 — Playhead follows the audio hardware clock
Playback UI follows hardware-presented frames, not an arbitrary timer.

## D-017 — Trim is staged and non-destructive
Trim opens around 35%/65%, shows precise local boundary bubbles, clamps to immutable source bounds and persists metadata only when applied.

## D-018 — Studio is timeline-first and single-workspace
Normal editing does not page-scroll the whole Studio. Track overflow scrolls only inside the timeline body; duplicate permanent representations and development prose are prohibited.

## D-019 — Loop markers are contextual
Loop-specific markers disappear when Loop is disabled.

## D-020 — Options Center owns low-frequency setup/commands
Global audio I/O, monitoring, project/Studio preferences and diagnostics live in Options rather than cluttering the creative timeline. The former assignment of final save/export actions to Options is superseded by D-037.

## D-021 — Input/output routing is globally resolved and revalidated
Current scope exposes one global recording input and one main output. Durable preferences use stable signatures, never ephemeral Android device IDs.

## D-022 — Mixer Dock has independent visibility/pinning
Timeline and Mixer share selected-track identity; Master remains fixed while track strips can scroll.

## D-023 — Mixer controls operate real engine state
Gain/pan/mute/solo and Arm are functional state, not decoration. Exactly one armed track targets each recording take.

## D-024 — M5 recording is transactional and single-target
Capture begins only after visible countdown and zero-time revalidation. Metadata references a take only after valid WAV finalization and atomic promotion; zero-frame failures create no clip.

## D-025 — Master gain is durable project metadata
Master gain is validated, backward-compatible project state applied after track summing.

## D-026 — Metering is post-bus and non-persistent
Track/Master meter display is transient; overload remains observable before final output clamp.

## D-027 — Android shell uses immersive fullscreen
System bars stay hidden during normal use and may be revealed transiently with standard system gestures.

## D-028 — Drag/drop belongs to a shared workspace coordinator
Track reorder and clip migration share one workspace-level drag state. Drag ghosts must stay in the Compose tree above the LazyColumn; `Popup` is prohibited. Ghost position follows the pointer continuously, independently from snapped destination indicators.

## D-029 — Drag autoscroll is continuous and geometry-driven
Edge autoscroll runs in a coroutine/frame loop with bounded proportional speed. Targets are recalculated from current `LazyListState.layoutInfo` bounds after scrolling; preview motion never records project history.

## D-030 — Completed drop is one atomic metadata mutation
Track drop uses `ProjectTrackEditor.reorderTrack`; clip migration uses `ProjectClipEditor.moveClipToTrack`. Cancel and same-origin drop are exact no-ops. Stable IDs, source references and clip metadata are preserved.

## D-031 — Clear and delete are different operations
`Limpar pista` removes only track content and preserves track identity/function/color/order/mix. `Excluir pista` is structural, exists in `Configurar pista`, and is enabled only when the track is empty.

## D-032 — Track Settings is responsive and metadata-aware
Wide layouts use balanced identity/color and source-metadata columns; narrow layouts stack them. Source filename/format/sample rate/channels/bit depth/encoding/duration are shown when available, with a compact empty state otherwise.

## D-033 — Physical behavior is accepted on the relevant signed candidate
Software-green and signed identity are insufficient for behavior that depends on the owner's actual device, route or hearing. Historical M5 is closed; D-090 limits future physical repetition to capabilities affected by the candidate or deliberately included in its frozen baseline.

## D-034 — Native original and edit proxy are distinct
Every successful import preserves the project-managed native original as authoritative immutable source. A format that cannot be consumed directly by the Studio WAV path receives a separate managed PCM WAV proxy. The proxy is derivative/regenerable and never replaces or redefines the original source.

## D-035 — `.guitarlab` is a versioned round-trip project package
Portable project save contains a versioned manifest, `project.json` and referenced managed media. Restore validates format/version/project/media, blocks ZIP path traversal, stages into temporary storage and assigns a new project identity before publication. A portable save must be reopenable by GuitarLab; it is not a one-way backup archive.

## D-036 — Master export is an offline render pipeline
Final output renders current timeline/trim/placement/clip gain/track mix/Mute/Solo/Pan/Master state into a floating-point master representation, then encodes the selected delivery format. Export never mutates source/proxy media. Requested outputs are WAV 32-bit float, FLAC and MP3 320 kbps.

## D-037 — Share owns project save and final export UX
A Share icon lives immediately before Home in the Studio top bar. It opens `Salvar e exportar`, semantically separating editable `.guitarlab` persistence from final-audio masters. Options continues to own setup/preferences/diagnostics and must not duplicate these output actions as primary commands.

D-083 supersedes the modal-specific surface: the semantic separation remains, while the canonical Export workspace now owns external format/output selection.

## D-038 — Media I/O/persistence/export were intentionally pulled forward into M5
Historical alpha13 wording is evidence only. M5 later closed by explicit approval of alpha14; M6 later closed by explicit approval of `0.3.0-alpha1`.

## D-039 — Encoder/support claims remain device-gated where Android capability is optional
A code path does not establish universal Android support. MP3 export requests a device-exposed Android MP3 encoder and remains target-capability-gated. FLAC can advance independently because API 36 instrumented native extraction/decoding now objectively verifies its current export structure.

## D-040 — Import processing is always visible
After Android document selection returns to GuitarLab, ingest/transcode/waveform preparation must expose a blocking progress surface with an explicit current operation. Silent background import is prohibited because it is indistinguishable from a missed command or freeze.

## D-041 — Stereo waveforms preserve channel identity
A two-channel source is visualized as two real envelopes, L above R. The previous aggregate-only waveform remains a compatibility/drag/overview representation but must not be the sole visual representation for stereo clips.

## D-042 — Stereo import is role-aware and non-destructive
A successful stereo import triggers an explicit decision. Backing/stereo-oriented tracks default conceptually to keeping both channels in one stereo clip. Guitar-role tracks with an existing L/R role pair offer direct synchronized distribution of source L/R into those two mono tracks. The immutable managed original remains shared; channel separation creates derived mono PCM proxies only.

## D-043 — Timeline split and stereo separation are distinct commands
`Dividir no cursor` remains a temporal clip edit. `Separar estéreo em 2 pistas mono` is a separate channel-routing command, available only for two-channel clips. They must never share ambiguous wording or behavior.

## D-044 — Abandonment and cleanup are lossless by default
Cleanup must never infer that a payload-bearing recording or authoritative source is disposable. Header-only recording temporaries may be removed; payload-bearing partial takes are retained/reported. Safe cleanup is limited to provable temporaries and derived caches.

## D-045 — Process-death recovery is conservative and format-specific
Interrupted project-import staging is internal and must never surface as a project. A Float32 recording header may be repaired only when the file exactly matches GuitarLab's canonical writer contract; otherwise it is retained untouched for diagnosis/recovery.

## D-046 — External publication stages first and fails closed
Project packages and masters are fully created/validated before opening a SAF destination. Final publication uses truncating semantics, cooperative cancellation and rollback attempts. A failed publish must not be reported as success.

## D-047 — Codec integration claims use real Android semantics
JVM/build success is insufficient for Android codec claims. FLAC export requires valid native stream metadata plus API-level extraction/decoding evidence. Tests must distinguish container identity from the decoded track MIME returned by Android extractors.

## D-048 — Signed candidates require both software and Android integration gates
The canonical CI contains a software gate and API 36 emulator gate. The signed homologation job has explicit dependencies on both; a signing request cannot bypass a failed required gate. Emulator/cache third-party actions are pinned to immutable commit SHAs.

## D-049 — Physical homologation is residual, not duplicated QA
Anything objectively established by automated model/file/JVM/emulator regression is removed from the manual checklist. The final physical pass is limited to target-hardware routing/capture, optional target codec capability, subjective latency/listening, real-device stress and ergonomics.

## D-050 — Commits do not automatically consume hosted CI
`scripts/build_local.sh` may be used as an optional prepared-host preflight. Canonical candidate promotion uses the manually dispatched GitHub workflow for the full software/API36/geometry/signing matrix and has no `push` or `pull_request` trigger. No candidate may be called validated solely because automatic CI is disabled; build, test, signature and checksum evidence remain mandatory.

## D-051 — Studio navigation is centered as one unit
The complete transport/navigation control group is geometrically centered on the full Studio top bar; no individual control (including Play) is privileged as the center anchor. The current position remains conveyed by the playhead and remaining time is not duplicated. General project summary belongs to the Pistas header, while Adicionar pista is placed below the final track as an explicit workspace action.

## D-052 — Current physical target is M-VAVE MK-300
Historical Pocket Amp results remain valid evidence for the hardware combination tested at the time, but the active residual homologation target is Samsung SM-X230 plus M-VAVE MK-300 over USB. Final routing, capture, monitoring, reconnect, latency/listening and ergonomics evidence must use that current setup.

## D-053 — Explicit recording input fails closed
When a user selects an input, Android must confirm the same effective `AudioRecord` route before capture is accepted and throughout the session. Missing/mismatched routing stops safely; silent fallback to the tablet microphone is forbidden.

## D-054 — Monitoring never defines recorded content
Off/Auto/On controls only software return to output. Backing playback and monitor output have no software path into the recording writer. Hardware loopback remains an external condition and must be disabled for isolation homologation.

## D-055 — Takes are non-destructive with one active take
Multiple takes may remain attached to one track, but validation requires exactly one active take whenever takes exist. Timeline playback and master export use active-take clips; editing operations must reconcile references transactionally.

## D-056 — Analysis produces reviewable suggestions
Automatic section detection and level analysis never mutate creative state silently. Suggestions are bounded and become persistent only after explicit acceptance/application.

## D-057 — Hosted CI is optional; relevant evidence is required
GitHub Actions stays controlled. A local pinned toolchain may establish software/build/signature evidence; emulator-only and physical-only claims remain explicitly distinguished. Under D-090, relevant evidence covers the changed path and credible owner risk rather than replaying every historical gate automatically.

## D-058 — Loop constrains explicit Play, not recording pre-roll
When Loop is active, an explicit Play action may begin only inside `[loopStart, loopEnd)`. A playhead outside that interval normalizes to loop start, and visible playback callbacks are kept inside the interval. This rule belongs to playback transport policy and must not be pushed into shared recording semantics because punch recording may legitimately begin before loop start for pre-roll.

## D-059 — Punch is transient REC intent, not durable armed state
Loop punch is chosen at REC time for the current recording only. With Loop active, REC asks whether to record only the loop, record from project start, or cancel. Only the loop choice creates a transient punch plan. Persisted `GuitarProject.punchRegion` remains readable solely for backward file-format compatibility and must never silently arm a later recording.

## D-060 — Section detection previews before mutation
Automatic section detection must render a non-persistent preview before the project is changed. Preview and acceptance use the same normalized boundaries. Cancelling preview is a no-op on persisted sections. `Limpar seções` is an explicit non-destructive command that removes saved sections and pending preview while preserving clips, markers and loop bounds.

## D-061 — Playhead seek is live during Play and forbidden during recording
The playhead is the sole timeline marker allowed to move while ordinary Play is active. Dragging it performs a low-latency in-session audio seek and playback continues from the selected frame. With Loop active, live seek is clamped to `[loopStart, loopEnd)`; loop markers and structural edits remain stopped-only. Countdown, active recording and finalization reject playhead seeking.

## D-062 — User Play completes and returns to its logical start
Natural non-loop playback completion stops and returns the playhead to project start. With Loop active, explicit user Play is a single bounded pass whose logical end is `L▶`; natural completion stops and returns the playhead to `L◀`. This one-pass rule applies only to explicit user Play. Recording/backing loop playback retains repeating-loop semantics so punch capture and its pre/post-roll behavior are not redefined.

## D-063 — Auto-section preview preserves control geometry and timeline bounds
`Auto seções` owns one fixed UI slot. While suggestions are previewed, that same slot becomes `Aplicar` plus a red `X`; unrelated Timeline controls do not shift. Section preview/accepted boundaries exclude edge-adjacent micro-sections and visual section surfaces are clipped to the real project end, so the UI cannot imply content after the song.

## D-064 — REC countdown is a three-second overlay, not layout content
The recording countdown is exactly `3 → 2 → 1`, rendered as a large centered translucent overlay above the workspace. It must not consume Column height, move `Comparação`/`Timeline`, or alter track/Mixer geometry. Capture still begins only after countdown completion and zero-time route/permission/target revalidation.

## D-065 — Home and Studio share one user-guide implementation
Home and Studio may expose separate `Ajuda` entry buttons, but both must open the same `StudioUserGuideDialog`. Duplicated help screens/copy are prohibited because they can drift from one another.

## D-066 — Trim handles are independent interaction objects
D-017 remains the non-destructive trim contract, but interaction is now explicit: start and end are independent handles with their own ergonomic touch targets and semantic/test identities. Pointer X maps deterministically to frames and each gesture retains the handle acquired at gesture start. Passive time bubbles must never steal handle input.

## D-067 — Split clip segments preserve recording-take lineage transactionally
A temporal split may leave multiple clips referencing one `RecordingTake`. Moving or deleting one child detaches only that child. If the canonical `RecordingTake.clipId` leaves, a surviving sibling is promoted deterministically; the take is removed only when no sibling remains. A committed edit must never leave dangling `takeId`/`clipId` references or one take lineage spanning incompatible tracks.

## D-068 — Clip deletion and drag-to-trash share one confirmed domain command
`Excluir clipe` is the explicit accessibility/context-menu path for deleting one clip segment. Drag-to-trash is a direct-manipulation shortcut, not a second deletion implementation: dropping on trash opens the same confirmation and invokes the same domain mutation. Cancelling is a strict no-op, and shared managed media remains while referenced.

## D-069 — Recording synchronization uses measured session skew plus route latency, never a magic offset
Per-session capture/backing startup skew, accepted route latency/calibration and punch/pre-roll offsets are distinct quantities. Trustworthy Android audio timestamps or bounded monotonic fallbacks may establish session timing. Compensation components are converted to frames and combined exactly once. A fixed correction such as `-0.5 s` is prohibited because it would encode one hardware/session observation as a global rule.

## D-070 — Live REC waveform timebase is captured frames, not callback cadence
Live recording visualization uses bounded frame-span envelope points with explicit start/end frame coverage and peak. Compaction preserves complete covered time and maximum transient. UI publication is conflated/rate-bounded rather than one update per `AudioRecord` read. `recordingFrames` remains authoritative for live clip width, and finalized media-derived waveform replacement must not move the clip timeline.

## D-071 — Project mutation commits must resynchronize derived Studio state
A persisted project edit is not complete until history availability, playback/mixer projections and editing readiness are synchronized from the committed snapshot. Features must not maintain independent stale copies of `canUndo`, `canRedo` or related readiness. Asynchronous work must verify that its originating project/session is still active before publishing results.

## D-072 — Level suggestions are based on effective audible level
Level analysis may inspect source media, but recommendation math must account for current clip/track gain so the suggestion refers to the effective signal the user will hear. After applying a recommendation, re-analysis should converge; repeatedly suggesting the same already-applied correction is treated as a defect.

## D-073 — Stop and REC share one successful recording-finalization command
During active capture, transport Stop and tapping REC again are two UI affordances for the same idempotent finalize operation. Stop during countdown cancels safely. Once finalization starts, duplicate requests are ignored/rejected so one session cannot produce duplicate takes or multiple media commits.

## D-074 — Practice controls have one stateful implementation and may change host location
Comparison/Timeline controls are a single reusable component/state surface. On wide layouts they may live in the Mixer header to reclaim vertical space; when Mixer closes the same logical component returns to workspace flow. Duplicating independent control implementations is prohibited.

## D-075 — Long live waveform uses uniform temporal resolution
Bounded live waveform storage must not leave historical data at a coarser visual density than newly captured data. When resolution changes, represented history and future points use one uniform temporal bucket size. Each bucket preserves its time interval and maximum transient; rendering spans that interval so older material cannot collapse into sparse midpoint strokes while recent material piles densely at the right edge.

## D-076 — GuitarLab is the surviving unified Android product
The approved successor program absorbs GBW Android into GuitarLab rather than maintaining two cooperating end-user Android apps. The final package/signing/upgradable product identity remains `studio.guitarlab.app`. GBW Android becomes the Prepare/source/separation capability; the frozen GBW Linux baseline remains historical/functional reference.

## D-077 — Prepare and Studio share one project/asset domain
The unified product uses one immutable projectId across source acquisition, Demucs separation, stems, prepared references, Studio recordings/takes, exports and cloud backup. Authoritative/derived media is represented by explicit immutable managed assets with content integrity and provenance. There is no final "export from GBW then import into GuitarLab" boundary.

## D-078 — Firebase separation and Drive backup remain separate data/security planes
Sharing one Google Cloud/Firebase project does not merge authentication or storage semantics. Firebase/Cloud Run/temporary Storage serve remote Demucs orchestration and transient transport. Durable project backup remains direct Drive API client-side with narrow authorization. Temporary separation objects are purged after validated local import/ACK and are never the durable project backup.

## D-079 — Unified cloud backup evolves to asset-deduplicated transactional revisions
Because prepared projects can contain a source, six large stems, recordings and exports, the successor backup architecture may not require monolithic full-media reupload for every metadata edit. The unified cloud schema uses immutable content-addressed assets plus transactional project revision manifests/current state, server-confirmed integrity, explicit conflict semantics and reachability-safe garbage collection. Portable `.guitarlab` remains a separate self-contained package contract.

## D-080 — Physical homologation is consolidated after maximal digital closure
The unified program should carry digitally unprovable hardware residuals forward and execute one consolidated final signed-candidate physical campaign whenever technically possible. Automated unit/integration/property/fuzz/emulator/cloud/Drive/migration/stress evidence must remove objectively provable checks from the manual campaign. A second physical pass is required only when source changes invalidate relevant physical evidence.


## D-081 — Internal Prepare → Studio media never uses delivery-codec export
The unified GBW + GuitarLab pipeline treats the prepared backing and guitar reference as canonical project-managed Studio inputs, not as files that must be exported and re-imported. U5 produces Studio-native lossless PCM WAV references once, preserving project/stem sample rate and the shared-gain/alignment contract; optional Guitar L/R references are derived only for the Studio template. Studio binds those managed assets directly.

U6 Study Export is therefore an **external delivery** concern only. Explicit backing/guitar WAV delivery uses a direct byte publication fast path whenever the requested WAV contract matches the canonical managed asset. FLAC and MP3 are encoded once from that canonical asset only when the user explicitly requests the format and the Android codec capability gate permits it. Delivery files never replace the canonical managed reference and conversions may not be chained through another delivery format.

Studio Master remains a separate semantic output: the current timeline/mix is rendered once to the canonical float-WAV render domain, after which WAV is published directly or FLAC/MP3 is encoded exactly once for external delivery.


## D-082 — Clean cutover; no legacy project migration obligation

The unified GuitarLab product line does **not** invest further engineering time in automatic migration/import compatibility for standalone GBW Android projects, H37/pre-unification GuitarLab backups, old `.gbwbackup` archives or historical Drive project corpora.

Reason: the owner has only two legacy projects and explicitly accepts recreating them manually in the unified application. The expected implementation/test/maintenance cost and risk of preserving historical migration paths is therefore disproportionate to the practical value.

Consequences:
- U9 is retired and will not be implemented;
- U8 must implement only the new unified transactional Drive backup/restore model;
- no H37 legacy adapter, GBW Share/Open bridge or historical importer is a release requirement;
- U10/U11 omit legacy migration corpus gates;
- historical migration code/tests already produced may remain if harmless, but they do not establish a future support obligation;
- no automatic deletion of old GBW/GuitarLab backup files is introduced; historical files are simply outside the supported migration path;
- current-product compatibility remains mandatory for projects created by the unified line, including save/reopen, duplicate, backup/restore, schema evolution, identity/integrity checks and fail-closed malformed-data handling.

This is a scope reduction, not a weakening of persistence, backup correctness, transactional restore, project identity or asset integrity.


## D-083 — One product shell; duplicate subsystem UX is release-blocking

The final application must present Prepare, Studio, Export, cloud processing, backup and long-running work as capabilities of one GuitarLab product.

Consequences:
- Prepare / Studio / Export share one adaptive project-level shell and navigation language;
- Home opens the last/relevant project workspace instead of hard-coding Studio for every project;
- exactly one canonical export workflow owns external format/output selection;
- Home/Studio export actions navigate to that workflow rather than maintaining independent chooser logic;
- long-running source/separation/preparation/export/backup/restore work converges on one semantic Activity/operation model;
- user-facing state is mapped to product language rather than rendering raw backend/domain enum names;
- common visual/status/error/empty/destructive components become shared product primitives;
- the global U10/U11 gate includes screenshot/geometry/accessibility/terminology evidence.

A technically functional feature is not release-complete if it leaves a second interaction language that makes the product look like two applications stitched together.

## D-084 — Default post-stem reference generation is automatic

After all six authoritative stems are validated and published, the default backing/reference recipe is deterministic and requires no additional user choice. The unified application therefore generates the canonical backing and guitar reference automatically as the normal continuation of Prepare.

The normal user path must not stop at a redundant “Criar base e referência” confirmation.

A manual rebuild/retry action remains valid after failure or for an explicitly requested future alternate recipe. Automatic generation does not change the non-destructive Studio binding rule: a newly prepared reference never silently replaces an older version already bound to an edited Studio session.

## D-085 — Remote job existence is authoritative during separation recovery

A durable local separation record is recovery intent and provenance; it is **not** proof that a corresponding Firebase/Cloud Run job exists.

When reconciling a nonterminal local job:
- `UPLOADING`, `READY` and `QUEUED` may replay upload/enqueue idempotently when no remote job exists;
- `CANCEL_REQUESTED` with no remote counterpart terminalizes locally as `CANCELLED`;
- `RUNNING`, `COMPLETED` or `IMPORTING` with no remote counterpart terminalizes as `EXPIRED`;
- bounded cancellation/worker retries must never leave a project indefinitely in a nonterminal state after exhaustion.

Restoring a project from `SEPARATING` to `SOURCE_READY` is generation-safe: the job must belong to the current source asset and no newer active separation may exist. Likewise, per-project job selection prefers an active generation over a late terminal completion from an older job.

This decision is fail-safe for media: terminalizing an orphan does not delete or replace the accepted source. It restores the ability to retry preparation while preserving source/project identity. It supersedes the rc8 assumption that any local nonterminal durable job should block orphan recovery.

## D-086 — Cloud separation uses the qualified official Demucs baseline and balances quality with latency

RC19 physical forensics proved that the substituted `demucs.cpp`/GGML engine produces structurally valid but musically unusable six-stem output. Every stem acquired systematic negative DC, most musical content collapsed incorrectly into `other`, and a DC blocker removed only the offset rather than repairing separation. The canonical input, Firebase transport, Android publication and prepared-reference renderer were exonerated.

The RC20 worker is therefore reconstructed from the consolidated GBW Linux Demucs path: official PyTorch `htdemucs_6s`, float32 output, clip mode none, baseline shifts 1 and overlap 0.5. This does not authorize BS-RoFormer. Neither rejected `demucs.cpp` strategy (`mt4_omp2` non-finite or `single8` musically invalid) is eligible for production.

Worker selection is multi-objective and fail-closed:
- structural/audio quality gates are mandatory; owner listening was an RC20 qualification aid until explicitly waived by the owner under D-092;
- <=5 minutes for a representative three-to-four-minute song is optimal;
- >5 and <=15 minutes is acceptable;
- >15 minutes is an alert requiring resource/architecture review;
- a faster bad result always loses to a slower result inside the acceptable tier;
- cold start, model load, inference, render, publication and estimated cost are measured separately;
- production cutover follows digest-pinned shadow qualification with rollback evidence.

The existing Firebase/Firestore/Storage lifecycle, quota, ownership, ACK/purge, cancellation and Android validation contracts remain unchanged across the engine migration.

## D-087 — RC20 image publication bypasses the failing Cloud Build builder path

U7 #146–#149 demonstrated that both the implicit Cloud Build `gcb-internal` builder and an explicit `gcr.io/cloud-builders/docker` step failed before the RC20 Dockerfile could execute, while the same digest-pinned Dockerfile built successfully on the GitHub-hosted verification runner. This was classified as a Cloud Build publication-path failure, not a worker/audio failure.

RC20 shadow publication therefore uses the existing keyless GitHub WIF identity, which already has Artifact Registry administration rights: `docker build --pull --no-cache` executes on the verified runner, `docker push` publishes to the existing Artifact Registry, the resulting registry digest is resolved, and Cloud Run is deployed by digest. No new static credential, secret, project or production permission is introduced. U7 #150/#151 proved this path can publish and deploy the shadow image while production remains unchanged.

## D-088 — Cross-host Demucs parity is bounded metric equivalence, not bit identity (superseded by D-091)

The same official PyTorch Demucs digest can produce small floating-point differences on different CPU hosts. U7 #152 observed a 0.37% drum-peak delta between the GitHub-hosted local/container run and Cloud Run; the original 0.2% tolerance was therefore too strict and did not represent a musical/structural failure.

W3 now requires exact engine/model/config/source contracts and hard quality-gate PASS, while numeric audio metrics use a 1% relative / 5e-6 absolute envelope. Prepared-reference metrics, shared gain and stem energy shares are also compared. Stem/prepared SHA equality is retained as evidence where available but is not a veto when the runtime does not promise cross-host bit determinism.

U7 #156 later proved that even this widened numeric envelope is not a valid blocking invariant for independent `--shifts 1` executions. D-091 replaces only that veto rule; the exact contracts, hard quality checks and retained comparison evidence remain.

## D-089 — RC20 CRITICAL gate permits only evidence-backed, non-applicable findings

U7 #153 generated the complete RC20 SBOM and vulnerability report, then Trivy classified `CVE-2023-45853` as CRITICAL for Debian `zlib1g` by inheriting the NVD score. The vulnerable code is unsupported `contrib/minizip`, which Debian does not ship in `zlib1g`; Debian marks the finding `will_not_fix` and provides no fixed version.

The gate retains this finding and rationale in its evidence but does not block on it only when CVE, package, `will_not_fix` status and absence of a fixed version all match. Any different CRITICAL finding, changed package/status, or newly available fixed version remains fail-closed. This is a scoped applicability decision, not a severity-wide waiver.

U7 #154 proved the scoped waiver behaved correctly by still blocking ten other CRITICAL package findings. Rather than broadening waivers, RC20 moves its otherwise unchanged Python 3.12.14 runtime from digest-pinned Debian 12 to digest-pinned Debian 13; the replacement base reported zero CRITICAL findings under the same Trivy database. Full-image scanning remains authoritative because FFmpeg expands the runtime package closure.

The full Debian 13 image in U7 #155 retained one finding: `CVE-2026-6653` in `libxml2`, rated CRITICAL only by the NVD source while Red Hat and Ubuntu rate the Linux issue MEDIUM; no fixed version exists. It is accepted only when CVE, package, affected status, absence of fix, NVD severity source and both vendor ratings match. The report remains retained and any metadata change reopens the block automatically.

## D-090 — RC20 is a frozen personal-use appliance, not a continuously serviced distribution

GuitarLab targets one owner, private projects and provider-mediated source acquisition. Release rigor protects the owner's actual risks: media/project loss, unusable audio, broken primary flow, quota/cost duplication, credentials, recovery and signed artifact identity. Evidence without a credible path to those harms does not automatically veto homologation.

After physical acceptance, the exact signed APK and digest-pinned worker are frozen. Scanner-database churn, new optional matrix cells, percentile collection, convenience diagnostics and dependency freshness do not themselves authorize a rebuild. Maintenance reopens only for an observed regression, provider/platform deprecation, applicable known-exploited vulnerability, credential exposure, unacceptable cost/integrity risk or an owner-requested feature. Any rebuilt digest receives proportional requalification of the affected path.

## D-091 — Stochastic Demucs parity validates invariants, not sample closeness

U7 #156 proved that independent executions of the exact official Demucs contract can exceed a fixed numeric closeness envelope while both outputs remain structurally valid and pass the worker's quality gates. This is expected because `--shifts 1` applies a random shift; forcing a seed merely to satisfy CI would change the qualified runtime recipe.

W3 therefore blocks on exact engine/model/config/source identity, frame/channel/finite-sample structure and independent quality rejection. It records stem/prepared hashes and all peak/RMS/DC/energy/shared-gain/energy-share deltas, including misses against the former 1% envelope, as evidence rather than vetoes. D-092 supersedes the RC20-specific W5 owner-listening veto; technical quality/integrity gates remain mandatory.


## D-092 — RC20 owner listening is waived; technical PyTorch qualification governs cutover

On 2026-09-24 local time, after reviewing the already-qualified official PyTorch Demucs route and its W4 CPU8 result, the owner explicitly chose to keep the selected configuration and skip the additional W5 listening run against the private real-song fixture.

Consequences:
- W5 owner listening is **WAIVED BY OWNER**, not programmatically passed;
- no claim is made that the unexecuted W5 real-source listening test passed;
- W1/W2/W3/W4 hard technical gates remain mandatory, including engine/model/config identity, finiteness, stem structure, quality rejects, prepared-reference integrity, runtime contract, quota/recovery and lifecycle checks;
- because U12bj changes worker-image content only to widen the fail-closed diagnostic bundle namespace from W4 to explicit W4+W5, the rebuilt digest still requires proportional shadow requalification before production cutover;
- once that requalification passes, W6 may promote the exact qualified digest without a separate W5 musical artifact;
- the temporary private MMF - Misery source is no longer needed by the active release path and must be removed from the repository HEAD;
- final signed-APK physical homologation remains required for the application/hardware behaviors that are actually part of the freeze candidate.

This decision supersedes only the RC20-specific requirement that W5 owner listening veto W6. It does not authorize a return to demucs.cpp, weakening of hard audio-integrity gates, or bypass of exact-digest qualification.


## D-093 — RC20 shadow qualification consolidates W3/W4 into one representative Cloud Run execution

The RC20 shadow workflow is optimized for the private single-owner release profile without weakening blocking integrity gates.

Consequences:
- W3 and W4 remain distinct evidence categories, but the normal shadow path collects both from the same representative CPU8 benchmark execution;
- the benchmark's exact engine/model/config identity, source/stem contracts, finiteness and independent quality rejection satisfy the blocking W3 invariants under D-091, while timing/memory/cost evidence satisfies W4;
- the separate W3 Cloud Run parity job is retired from the normal workflow because cross-host numeric closeness is diagnostic rather than a veto under D-091;
- the standalone Cloud Run model-probe is disabled in the normal baseline because the full benchmark already validates assets/model loading and executes real inference;
- CPU4 remains opt-in only;
- the full transactional U4 smoke does not run in shadow; it executes after production cutover inside the controlled production deploy so failure still triggers automatic rollback;
- W5 workflow execution is removed under D-092;
- this workflow-only consolidation does not modify worker image contents or the qualified Demucs recipe.


## D-094 — Normal RC20 shadow qualification uses one Cloud Run execution and one Demucs inference

After D-093 consolidated W3/W4, review found that the retained warm probe still executed Demucs a second time inside the same Cloud Run job. For the selected CPU8 production shape this repeated inference is no longer required in the normal qualification path because cold/warm behavior was already characterized and the owner accepted the current performance.

Consequences:
- the normal CPU8 W3/W4 shadow cell uses exactly one Cloud Run execution and one Demucs inference;
- the optional CPU4 matrix cell, when explicitly requested, also uses one execution and one inference;
- warm-probe and standalone model-probe capability may remain in the benchmark harness for targeted diagnostics, but both default off and are not release prerequisites;
- the full U4 transactional smoke remains a separate post-cutover production execution because it validates Firebase/Firestore/Storage lifecycle, recovery, quota and ACK/purge rather than benchmark quality/performance;
- standalone U4 must use the official RC20 checkpoint SHA-256 `34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd`;
- no worker-image or Demucs-recipe content changes are introduced by this orchestration refinement.


## D-095 — W6 promotes the exact U7 #164 qualified digest without rebuilding

U7 #164 / run `36085821470` passed on producer `aab87776f314fddc20028432371a9a5d268079c1` and qualified immutable worker digest `sha256:14e240cb01b71131cb049dd34e0df078614238da3514325f80126d56f8d5e698`. Production before W6 remains `sha256:a70bd221ab50ef092508781c609cbfbecfe6b71ba4c8a722fc5f087b09dbd550`.

W6 therefore promotes that exact Artifact Registry digest. Production deploy must not rebuild or retag an equivalent worker as the promoted candidate. The deploy script accepts a production-only prequalified digest, validates the exact repository and sha256 shape plus registry existence, deploys it with the locked CPU8/16 GiB Demucs contract, then runs runtime verification and the full U4 transactional smoke. Failure restores the captured previous production image automatically. Shadow qualification continues to build/publish normally when a new worker image actually requires qualification.

## D-096 — RC20 is physically accepted and the exact APK/worker baseline is frozen

On 2026-09-25 the owner physically homologated the exact signed U12bx RC20 candidate on the reference Samsung SM-X230 / Android 16 line, with the M-VAVE MK-300 USB path used for the relevant hardware-audio validation. The accepted signed APK SHA-256 is `0d5832666a00484635ef37daecb9bead021ed771ddd053b88d88191a5f029fd2`; producer commit is `76a832afdd8045ac944046dae2c31d8f6ec00716`; signer certificate remains `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

The production separation worker is frozen at immutable digest `sha256:14e240cb01b71131cb049dd34e0df078614238da3514325f80126d56f8d5e698`, promoted without rebuild by U7 #165 after U7 #164 qualification. The official engine/model contract remains Demucs/PyTorch `htdemucs_6s` with checkpoint SHA-256 `34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd` and the qualified CPU8 recipe.

Consequences:
- U12 and the RC20 completion campaign are CLOSED;
- the U/C development roadmaps are historical evidence, not live work queues;
- the exact identities are recorded in `RELEASE_BASELINE.md`;
- completed development/homologation plans are archived under `docs/history/`;
- documentation-only commits after acceptance do not change the accepted producer identity;
- no recurring CI, rebuild, dependency refresh or re-homologation is required without a D-090 maintenance trigger or explicit owner-requested feature;
- any future runtime/source/backend mutation creates a new candidate and receives proportional qualification under `TEST_AND_HOMOLOGATION_POLICY.md`.

## D-097 — RC21 maintenance repairs legacy recording integrity and treats Drive catalog cache as non-authoritative

An owner-observed RC20 project failed persistence validation after stereo separation with `clip.trim.bounds`. Forensics showed a legacy managed recording without take metadata and a trimmed stereo clip whose derived mono editing bound had been incorrectly inherited from clip length rather than the splitter's actual output frame count. The same maintenance cycle also exposed duplicated/ad-hoc transient feedback and unnecessary Drive catalog reads.

RC21 reopens only the affected Android maintenance paths under D-090/D-096.

Consequences:
- legacy take recovery is deliberately narrow: only project-managed WAV recordings with a recorded-guitar role, exact managed URI/path convention, clip-id filename prefix and historical take timestamp pattern are eligible;
- generic imported media is never promoted to a recording take merely because it is placed on a guitar track;
- compatibility repair is applied only after cryptographic/revision validation of the persisted canonical project state, so recovery cannot redefine stored-state identity;
- stereo channel separation uses the actual splitter output sample rate and total frame count as the derived editing bound;
- modern take metadata is preserved by creating per-track lineage; an existing take shared across multiple temporal clips is ambiguous and separation fails closed rather than guessing;
- derived/proxy outputs are cleaned if the project transaction does not commit;
- the Drive catalog is derived from one remote head snapshot. A durable metadata-only cache is an optimization, not authority, and is scoped/validated against account, retention settings, local revision identities and manifest metadata;
- visible Backup-screen entry and explicit refresh verify Drive. Automatic WorkManager backup remains UI-independent; off-screen completion does not perform a redundant catalog read and the next entry performs the required remote refresh;
- normal transient Error/Warning/Async Completion presentation is centralized through the shared host and eligible events are consumed on handoff to prevent delayed replay after navigation;
- RC20 remains the accepted frozen baseline until the exact RC21 signed APK completes residual owner acceptance. Opening this maintenance candidate does not rewrite RC20 release identity or reopen the unchanged Demucs worker.

## D-098 — Per-track MAIN/CUE routing is explicit and fail-closed

The owner explicitly requested a secondary audio output so any Studio track can be monitored independently from the main speakers, for example Backing + My Guitar on MAIN and reference guitars on headphones/CUE. This is a supported RC22 feature, not a diagnostic-only experiment.

Consequences:
- each track persists `TrackOutputRoute` with backward-compatible default `MAIN`; the engine supports `MAIN`, `CUE` and `MAIN_AND_CUE`;
- the initial mixer headphone control toggles exclusive MAIN ↔ CUE for clear user intent, while `MAIN_AND_CUE` remains an internal capability for future explicit UX;
- output-route mutations are stopped-state operations; the button is disabled during Play/REC so recording-monitor selection and playback routing cannot diverge mid-session;
- Options exposes one recording input, one MAIN output and one optional CUE output;
- CUE requires MAIN to be explicitly selected and requires a different explicit low-latency live output endpoint; it never inherits Android automatic routing and synchronized CUE deliberately excludes Bluetooth;
- the secondary sink is opened only when the current playback content includes a CUE-routed track, avoiding idle dual-device streams from empty CUE tracks;
- route acceptance is not sufficient by itself: playback verifies effective MAIN/CUE routing and stable `AudioTimestamp` clocks before audible use, rejects an initial presentation-origin offset above 12 ms, and continues checking that the routes remain distinct and within the continuous drift envelope;
- if CUE is unavailable, rejected, lost, converges onto MAIN, lacks stable clock evidence, starts outside the 12 ms alignment limit, cannot accept a complete non-blocking render chunk or repeatedly crosses the continuous drift safety guard, CUE is silenced instead of leaking into MAIN or stalling it; MAIN continues when safe and is never delayed merely to rescue CUE;
- the dual-bus engine is used by ordinary Play and backing playback during recording; software monitoring of an armed CUE track also requires the selected CUE route and fails closed without affecting captured input;
- CUE state is monitoring/playback metadata and does not exclude a track from Studio Master export;
- the existing fail-closed selected-input recording contract, managed-media integrity, backup/restore and recording-writer isolation remain unchanged;
- digital qualification proves persistence, routing policy, materialization, build/Lint and API36 integration; physical support for a specific two-output hardware combination is claimed only after target-device validation on the exact signed candidate.

This decision supersedes D-021 only where D-021 said the current scope exposes one main output. Stable semantic signatures and the prohibition on persisting ephemeral Android device IDs remain in force.

## D-099 — Stable Studio navbar and complete narrow Mixer channels

The owner requested removal of the persistent Comparison/Adjustments/Timeline row, stable navbar geometry in every operational state, and narrower individual Mixer channels so more complete tracks fit simultaneously. Existing Mixer pinning must remain available and all controls/readouts must stay accessible.

- three fixed navbar triggers reuse the existing practice action commands through anchored overlays;
- state changes affect semantics/indicators and enabled appearance, never slot geometry;
- narrow channels are 168 dp instead of 232 dp; Mute/Solo and CUE/REC use two rows of 48 dp targets;
- all PK/RMS/held-peak/clipping/gain/pan information and preview/commit callbacks remain;
- Master remains separate; fixed/docked mode is default and durable; floating is explicit;
- 352 dp default height accommodates the complete narrower controls; height reduction is secondary;
- RC23 is a new exact-source Android candidate and preserves the RC22 audio/domain graph;
- normative older practice-bar layout provisions are superseded by this decision only where they conflict; prior evidence is retained, and affected geometry/interaction is requalified.

Reference rationale: Logic Pro for iPad documents complete per-channel volume/pan/mute/solo/record/level controls (https://support.apple.com/en-lamr/guide/logicpro-ipad/lpipfc8ea6fc/ipados); Cubase documents channel-width zoom independent of section height (https://www.steinberg.help/r/cubase-pro/15.0/en/cubase_nuendo/topics/mixconsole/mixconsole_functions_menu_r.html). The 168 dp / two-row touchscreen adaptation is a GuitarLab design decision; references do not certify its physical ergonomics.

## D-100 — Correct RC23 density using owner-device evidence

Date: 2026-10-01. Supersedes D-099 presentation/pinning/height choices and the floating presentation in D-022; visibility and all domain/audio contracts remain applicable.

Owner video `141016.mp4` rejects the delivered RC23 presentation: excessive dock height, off-center two-row buttons, bulky sliders, unused Master space, uniform-width clipped/empty menus and redundant Pin/Height controls. Software PASS did not establish UX acceptance.

Implement fixed Comparison/Timeline navbar slots with balanced side groups, Master-owned Níveis and one minimum/complete toggle. Mixer always docks. Complete/minimum heights are 252/172 dp; four 48 dp targets with 46 dp faces fit one row in 200 dp channels. All state buttons remain direct even in minimum; full pan/meters/clip reset remain available through the shared complete details strip. No engine changes or alternative mix state.

Proportional regression covers actual target/face geometry, slider commits, full/minimum details, transport locks, horizontal overflow, large fonts, old preference upgrade, menu fit and filled five-track tablet screenshots with explicit timeline height recovery. Existing Loop→REC and adjacent regression remain. RC24 is a new exact-source candidate; RC23 evidence/payload remains immutable and physical rejection is recorded separately from digital PASS. Do not monitor workflow execution; wait for owner-reported result before consulting completed evidence.

## D-101 — Media Bluetooth outputs and continuously fed CUE admission

2026-10-01. Owner evidence `1000079444.mp4` shows a silent mono Bluetooth choice alongside working stereo media output, and CUE startup clock rejection. SCO is a call endpoint and is excluded from media outputs, with unambiguous legacy migration and recording-input preservation. Both destinations in a synchronized pair exclude Bluetooth; settings explain this before activation. New CUE activation with unavailable configuration preserves MAIN/history.

RC22 startup supplied only 2048 frames (~43 ms at 48 kHz), then sampled six timestamps 3 ms apart for each sink sequentially without feeding either stream. Replace this with bounded interleaved continuous zero feed and independent advancing observations. Preserve 4 ms jitter, three observations, distinct actual endpoints, 12 ms offset, non-blocking runtime CUE and drift rejection. Slow/stale/error/converged/cancelled cases are executable fake-clock regressions. Preferred-device selection cannot certify hardware support; physical speaker+wired acceptance is pending. RC25 follows immutable RC24 and preserves its UI revision; no workflow monitoring.

A seleção secundária é validada imediatamente em Opções: mostra “Verificando”, abre duas saídas com buffers zerados e volume zero e somente persiste CUE depois de comprovar rotas distintas e clocks estáveis. Falhas mantêm CUE desativado e mostram a causa. Nova seleção, desativação, alteração de MAIN, atualização da lista e saída da tela cancelam/inutilizam resultados pendentes. Diagnósticos de latência não rodam em paralelo com esse teste. O teste usa a taxa do projeto disponível (48 kHz fora do projeto); mede evidência de apresentação relativa, não latência acústica/round-trip. Play/REC ainda verifica os streams reais e mudanças posteriores; uma aprovação anterior não garante suporte permanente do hardware.

## D-102 — Níveis is a third navbar action and Master prioritizes metering/gain

2026-10-01. Owner-approved Studio refinement supersedes D-100 only for Níveis placement and Master presentation.

- the left transport-navbar group contains exactly three actions in order: Comparação, Timeline, Níveis;
- Níveis opens the all-tracks level-analysis modal directly and uses a gauge/speedometer icon that is not reused by the visible Mixer toggle or Options/settings actions;
- left/right navbar reservations stay equal, preserving absolute transport centering and the existing narrow-screen horizontal-scroll contract;
- the Master strip no longer contains a Níveis text button;
- track and Master PK/RMS rows use the available inner strip width; CLIP reset remains directly accessible from the strip header instead of reserving meter width;
- Master uses a wide volume slider with a single-line `VOL +dB` readout below it;
- track width, Master width, complete/minimum dock heights, audio/mix state, level-analysis domain behavior and MAIN/CUE routing are unchanged;
- affected navbar/Mixer geometry and direct Níveis opening receive focused API36 regression and screenshot review before qualification.

## D-103 — Mixer cards use centered identity headers and soft functional segmentation

2026-10-02. Owner-approved RC27 visual refinement after physical review of the signed RC26 Studio.

- Track identity is presented as a centered accent-dot + title group; Master title is centered independently of its transient CLIP action.
- Track cards are visually partitioned into header, state-action, metering and mix regions using restrained 6 dp graphite surfaces and low-alpha accent outlines.
- Volume and Pan remain one mix region with a quiet internal divider; this visual grouping never introduces a second mix state or alternative callback path.
- Master reuses the header/meter/volume hierarchy but must not render an empty action placeholder merely to mirror track cards.
- Unselected track cards retain a subtle track-accent identity; selection strengthens border/tonal emphasis while existing semantic selected state remains authoritative.
- The refinement preserves 200 dp track width, 144 dp Master width, 252/172 dp dock heights, 48 dp touch targets, 46 dp button faces, RC26 full-width meters, Master volume geometry and large-font growth.
- Audio, routing, persistence, level-analysis, export and MAIN/CUE semantics are unchanged.
- Qualification adds centered-header and section-geometry assertions plus RC27 complete/minimum target-tablet screenshot review; existing interaction/accessibility regression remains blocking.

## D-104 — Clipping keeps a 48 dp header target with a compact non-overlapping warning face

2026-10-02. RC27 post-gate visual review found the previous textual `CLIP` face could visually collide with centered track/Master titles.

- clear-clipping semantics, callback and 48×48 dp interaction target are unchanged;
- the visible face becomes a compact 28×24 dp error-warning badge rather than a 38×24 dp `CLIP` wordmark;
- the visible badge must not overlap the centered track identity group or centered `MASTER` title;
- the action remains in the header so PK/RMS continue using full useful width;
- this is presentation-only and does not alter meter latch/reset logic, persistence, routing or audio.

## D-105 — Visible clipping warning is edge-anchored inside the 48 dp action target

2026-10-02. CI #954 showed a 28×24 dp warning face could still overlap `MASTER` when centered inside the 48×48 dp clipping action target.

- the clear-clipping target remains 48×48 dp with identical semantics/callback;
- the visible warning face remains 28×24 dp;
- the face is anchored to the outer/right edge of the target, maximizing separation from centered identity text;
- track/Master visible warning faces must not overlap their centered title bounds;
- no meter, layout-budget, routing, persistence or audio behavior changes.


## D-106 — CUE safety is based on canonical physical routes, not one logical endpoint id

2026-10-03. Owner evidence on SM-X230/Android 16 shows MK-300 MAIN + wired CUE rejected as `ROUTE_UNCONFIRMED`. The RC25/RC27 verifier compared `AudioTrack.routedDevice.id` to the initially requested `AudioDeviceInfo.id`, while the selector already merges logical USB/built-in endpoints into physical routes. This inconsistency can reject a valid route when the HAL reports a sibling logical endpoint.

RC28 defines one Android physical-route identity rule for admission and runtime safety. USB identity follows product + physical card after stripping logical device/endpoint components; built-in speaker aliases ignore logical address; other endpoints retain type/product/address identity. API36 consumes the full routed-device set. A stream is accepted only when route evidence is non-empty and every routed logical endpoint maps to its selected physical destination. MAIN/CUE physical convergence or any additional mirrored physical destination remains rejected. Stable clocks, the 12 ms initial offset bound, continuous drift guard, non-blocking CUE and no automatic fallback remain unchanged.
