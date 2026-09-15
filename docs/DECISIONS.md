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
The default gate is `scripts/build_local.sh` on a prepared Android build host. GitHub Actions remains a manually dispatched fallback for the full API 36 emulator/signing matrix and has no `push` or `pull_request` trigger. No candidate may be called validated solely because automatic CI was disabled; build, test, signature and checksum evidence remain mandatory.

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

## D-057 — Hosted CI is optional, evidence is mandatory
GitHub Actions stays manual-only. A local pinned toolchain may establish software/build/signature evidence; emulator-only and physical-only claims must remain explicitly distinguished.

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