# U12 — Final Physical Homologation — GuitarLab

Updated: 2026-09-24
Status: **RC20 SUCCESSOR ACTIVE — FINAL ACCEPTANCE MUST USE THE EXACT SIGNED CANDIDATE**
Package: `studio.guitarlab.app`
Target device: Samsung SM-X230 / Android 16 / API 36
Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`

## RC20 authoritative physical acceptance

This section is the current U12 authority. The rc5–rc19 material retained later in this file is historical corrective evidence and does not create new RC20 gates unless this section or the active RC20 plan explicitly reactivates one.

### Prerequisite

Do not declare final physical PASS on an unsigned/pre-sign build. Before starting, `CURRENT_STATE.md` and signed-artifact evidence must identify the exact RC20 producer SHA, `0.5.0-rc20` / versionCode `40`, signed APK SHA-256, locked certificate and finalist production worker digest/model/recipe.

The signed APK must be the exact tested unsigned release artifact after the no-recompile signing boundary.

### Minimum owner workflow

On that exact signed APK:

- [ ] install/upgrade successfully without clearing data unless a clean install is explicitly being tested;
- [ ] launch/open the intended current project successfully;
- [ ] perform one representative normal source search/acquisition and observe an explicit terminal search state;
- [ ] run Prepare on a representative real source and confirm backing/guitar references are musically usable;
- [ ] verify interrupted/retried Prepare does not consume duplicate accepted-job quota or lose project state;
- [ ] open Studio through the normal handoff and confirm the intended prepared references are usable;
- [ ] save/close/reopen and confirm the project remains correct;
- [ ] run a representative export if export is part of that session;
- [ ] explicitly approve the exact signed APK for freeze.

### Capability-specific physical checks

Required only when the capability is part of the frozen baseline being accepted or the RC20 delta can materially affect it:

- **MK-300 / USB recording:** correct selected/effective route, no silent tablet-mic fallback, no backing printed into the take with loopback OFF, usable monitoring/timing, safe disconnect/reconnect and a representative continuous recording/playback session.
- **Drive backup/restore:** one representative target-device connect/backup/restore smoke. Existing U8m provider-real semantics remain reusable; do not repeat the entire destructive campaign without a real reason.

### Explicitly non-blocking by default for RC20

Unless shipped in RC20 or required to diagnose a release-critical defect: DID_YOU_MEAN beyond non-silent search, reference-reinsertion convenience, diagnostics reorganization, consolidated diagnostics, persistent audit journal, diagnostic ZIP, extended observability and exhaustive matrices do not block the signed personal appliance.

### Acceptance and freeze

U12 PASS requires no repeatable release-critical defect in the representative supported flow, explicit owner approval of the exact signed APK, and recorded APK/worker identities. After PASS, freeze the APK and worker under `PROJECT_IDENTITY.md` / D-090.

## Historical corrective record — non-authoritative for RC20

The sections below preserve rc5–rc19 findings and evidence. Their old “current”, “must” and candidate-binding language applies only to the dated candidate unless the RC20 authority above explicitly reuses it.

## Exact rc11 candidate binding
- Android CI: #806 / run `35885021871` — PASS, including full API 36 regression and signed homologation;
- U4 Cloud Integration Smoke: #123 / run `35885021902` — PASS, real six-stem Cloud Run contract;
- U7 Cloud Backend: #81 / run `35885021969` — PASS, Firebase Email/Password configuration + allowlisted password-user verification + backend source/container/security verification;
- signed artifact: `GuitarLabStudio-0.5.0-rc11-homologacao` / artifact id `10762204176`;
- signed APK SHA-256: `3cd9bfe06c9aa1af7f452f0dd8d9e9bead50774bde4a3b22c72f09f51e5ea769`;
- signed APK size: `79,965,840` bytes;
- artifact ZIP SHA-256: `9f9ad8e16222c4eac1d8dcc27d7946e75e54be6a9a9bfcd243beb0284e14e7b2`;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- exact-source materialization through U12w: PASS.

## Rc11 authentication/lifecycle corrective

Target-device diagnostics after rc9 proved that the worker itself was alive: WorkManager repeatedly started `RemoteSeparationWorker`, but each execution failed before any Storage object, Firestore job, callable invocation or Cloud Run execution existed. The first cloud gate reproduced `ADMIN_ONLY_OPERATION` for anonymous signup. Enabling anonymous auth then proved a second incompatibility: the backend still correctly enforced `GBW_ALLOWED_UIDS`, while anonymous login creates a new unstable UID.

Historical GBW source confirmed the intended security model: one personal Firebase Email/Password account with a stable UID allowlisted in `GBW_ALLOWED_UIDS`. Rc11 restores that contract rather than weakening authorization.

Rc11 therefore:
- removes anonymous login from the Android remote-separation path;
- adds an explicit Opções → Conta e nuvem Email/Password login;
- keeps the password only for the immediate authentication call; GuitarLab does not persist it;
- relies on Firebase session persistence after successful login;
- validates the authenticated account against `remoteBackendStatus` before accepting the session;
- refuses to start separation without a stable authenticated session;
- types Firebase/Auth/Firestore/Storage/Functions failures by pipeline stage and retries only transient failures;
- limits worker/cancel retries and exposes explicit retry/terminal messaging;
- terminalizes stale Activity entries when projects disappear;
- adds `Limpar resolvidos`, which removes only succeeded/cancelled history and preserves failed/unresolved operations.

## Focused rc11 physical retest

Before the remaining USB/audio campaign:
- [ ] install/upgrade the exact rc11 APK;
- [ ] open Opções → Conta e nuvem;
- [ ] sign in with the existing personal Firebase Email/Password account whose UID is already allowlisted;
- [ ] verify the UI reports the account as authenticated/authorized;
- [ ] reopen the affected/new Prepare project and start a fresh separation;
- [ ] confirm the source crosses Storage → Function → Firestore/Cloud Run rather than remaining in local retry;
- [ ] confirm six stems return and import successfully;
- [ ] background/foreground and one reboot while pending preserve/reconcile the active job;
- [ ] cancel-before-dispatch or cancel-in-flight reaches a terminal state and permits another attempt;
- [ ] delete a project only after resolving/cancelling its active work and verify Activity does not retain a phantom active card;
- [ ] use `Limpar resolvidos` and verify only completed/cancelled items are removed while failed/open items remain;
- [ ] adjacent navigation and backup smoke remain healthy.

Rc10, rc9, rc8, rc7, rc6 and rc5 are historical evidence only and are not eligible for final approval.

## Target-device rc8 finding and rc9 corrective

Rc8 improved persistence-before-scheduling and reboot/cancellation routing, but physical retest exposed a second orphan class: the local durable job itself can exist while no corresponding Firestore/Cloud Run job exists. The observed project remained in `CANCEL_REQUESTED`; Cloud correlation found no remote execution for that job identity.

Root cause in rc8:
- orphan recovery treated any local nonterminal durable job as evidence that the separation was still valid;
- `CANCEL_REQUESTED` therefore blocked the project-level `SOURCE_READY` recovery;
- cancellation retries could exhaust while leaving the local durable state nonterminal;
- reconciliation did not terminalize `RUNNING/COMPLETED/IMPORTING/CANCEL_REQUESTED` when backend status returned no job.

Rc9 closes the lifecycle end to end:
- backend existence is authoritative for remote-required durable states;
- `UPLOADING/READY/QUEUED` with no remote job is replayed idempotently;
- `CANCEL_REQUESTED` with no remote job becomes local `CANCELLED`;
- `RUNNING/COMPLETED/IMPORTING` with no remote job becomes `EXPIRED`;
- cancellation retry is bounded and terminalizes safely on exhaustion;
- general worker retry exhaustion also terminalizes rather than leaving a phantom active operation;
- accepted source recovery is guarded by source generation and by absence of a newer active job;
- project snapshot selection prefers a current active generation over a late terminal record from an older job;
- Activity copy distinguishes “cancelamento solicitado” from terminal cancellation.

Automated coverage includes the exact `CANCEL_REQUESTED + backend.status()==null` scenario, other missing-remote states, bounded failures, source-generation protection, stale-terminal race and an API36 recovered-orphan retry surface.

## Focused rc9 corrective physical retest

Before broader U12 continuation, use the actual project/device state that reproduced the rc8 defect:

- [ ] upgrade/install the exact rc9 APK without deleting the affected project;
- [ ] open the project/Prepare screen that was stuck at `CANCEL_REQUESTED`;
- [ ] the old orphan converges to a terminal state and does **not** remain indefinitely in “Cancelamento solicitado…”;
- [ ] the accepted source remains present and unchanged;
- [ ] the UI exposes “Tentar separação novamente” / equivalent retry path;
- [ ] start a new separation and confirm the new job has a distinct identity;
- [ ] confirm that this new job actually crosses the cloud boundary (Firestore/Functions/Cloud Run correlation);
- [ ] background/foreground and one reboot while pending preserve/reconcile the correct new job;
- [ ] cancel-before-dispatch or cancel-in-flight reaches a terminal state and permits another attempt;
- [ ] an old terminal job never masks the newer active generation;
- [ ] Activity reflects requested vs terminal cancellation coherently;
- [ ] adjacent project navigation and backup smoke remain healthy.

Rc8, rc7, rc6 and rc5 are historical evidence only and are not eligible for final approval.

## Target-device rc6 finding

The rc6 target-device retry confirmed that the Prepare search button starts the operation, but the online discovery path can terminate immediately. Activity records the terminal state as “Pesquisa concluída sem fontes compatíveis” even when the yt-dlp YouTube/SoundCloud provider actually failed. Rc6 is therefore withdrawn from final approval.

The U12g corrective must:
- require extracted native libraries for the embedded yt-dlp runtime;
- perform one controlled runtime refresh/retry when all yt-dlp discovery calls fail;
- preserve cancellation semantics;
- surface persistent provider failure as failure/diagnostics, never as successful empty search;
- keep genuine zero-result searches distinct from provider failure;
- pass deterministic unit/API36/signing gates before U12 resumes.

## Target-device findings that superseded rc5

The first rc5 pass found the following reproducible product defects before physical acceptance completed:

- Prepare source search could appear to do nothing and produced no visible Activity record;
- one manual backup interaction could overlap a pending automatic backup, yielding two same-time history records;
- backup exposed only an indeterminate spinner and did not refresh available versions after automatic completion;
- the development-only U8m acceptance surface remained visible in Backup;
- shell navigation was not geometrically centered, Home used a text “Projetos” action, and Prepare/Studio project-menu actions lacked icons;
- Home branding/action copy still read “GuitarLab” and “Abrir projeto” instead of “GuitarLab Studio” and “Importar projeto”.

These findings require a new signed candidate. Existing rc5 physical evidence remains reusable only where the rc6 delta cannot materially affect it; sections 1, 7, 8 and 9 plus adjacent navigation/background-operation smoke must be repeated on rc6.

## Purpose

U12 validates only claims that the digital campaign cannot establish. U10/C8 already closes product architecture, navigation, responsive/accessibility semantics, backup protocol behavior, cloud contracts, failure handling, persistence and screenshot cohesion. Do not repeat those tests manually unless physical behavior depends on them.

Legacy standalone GBW/H37/pre-unification migration is out of scope. H33 real external-controller tactile acceptance remains a separate 1.1 boundary unless explicitly enabled as a release claim.

## Candidate binding

Before starting:
- [ ] install/upgrade only the exact signed rc11 APK produced by `34cb60624b2fabf21cfe2c60003b04eac1597418`;
- [ ] package is `studio.guitarlab.app`;
- [ ] version is `0.5.0-rc11` / versionCode `31`;
- [ ] APK SHA-256 is `3cd9bfe06c9aa1af7f452f0dd8d9e9bead50774bde4a3b22c72f09f51e5ea769`;
- [ ] signer SHA-256 is `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- [ ] device is Samsung SM-X230 / Android 16 / API 36;
- [ ] MK-300 is connected through the intended normal-use USB/hub/power topology for audio-specific residual checks;
- [ ] MK-300 hardware loopback is OFF.

## 1. Installation and current-project smoke

- [ ] clean install or supported upgrade succeeds;
- [ ] app launches without crash;
- [ ] current unified projects remain listed and openable;
- [ ] one representative project can traverse Home → Prepare → Studio → Export;
- [ ] process/background/foreground round-trip does not lose the current project/workspace.

Automatic migration of legacy GBW/H37/pre-unification projects is intentionally not tested.

## 2. Real USB routing and isolation

With MK-300 connected:

- [ ] GuitarLab identifies/selects the intended MK-300 input;
- [ ] GuitarLab identifies/selects the intended MK-300 output;
- [ ] effective capture route is MK-300, not tablet microphone;
- [ ] effective playback/monitor route is the intended MK-300 path;
- [ ] no silent fallback to tablet microphone occurs if the intended input becomes unavailable;
- [ ] with hardware loopback OFF, backing/reference playback is **not printed into the guitar recording**;
- [ ] guitar input is present in the take at expected level;
- [ ] monitoring behavior is usable and does not create feedback/duplicate monitoring;
- [ ] recorded file/channel behavior matches the intended mono guitar capture contract.

Failure predicate: any backing bleed caused by GuitarLab routing, silent mic fallback, wrong effective device, or corrupted capture is P0/P1 and blocks acceptance.

## 3. Live recording feedback

During a representative recording:

- [ ] countdown/REC transition is coherent;
- [ ] live waveform appears while recording, not only after stop;
- [ ] input level/peak feedback moves plausibly with the guitar signal;
- [ ] waveform temporal position follows the actual captured take;
- [ ] stopping/finalization replaces transient waveform with finalized-file waveform without visible placement jump;
- [ ] cancellation/error does not leave a false completed take.

## 4. Timing, synchronization and listening

Use repeated sharp transients/picked notes against a known backing transient.

- [ ] repeated takes do not show a repeatable systematic late/early placement;
- [ ] route/rate global residual adjustment affects only future recordings;
- [ ] take-specific synchronization moves only the intended take/lineage;
- [ ] take-specific synchronization survives save/reopen;
- [ ] changing global adjustment does not retroactively move an existing take;
- [ ] subjective monitoring latency is acceptable for the intended guitar workflow;
- [ ] playback has no wrong speed, pitch, channel order or obvious corruption.

If physical calibration is exercised:
- [ ] exact selected live input/output devices are confirmed before stimulus;
- [ ] stimulus is short/controlled rather than a harsh sustained burst;
- [ ] invalid route state aborts safely rather than storing a false calibration.

## 5. USB interruption and recovery

While recording a representative take:

- [ ] unplugging/disconnecting MK-300 does not silently switch to tablet mic;
- [ ] already captured valid audio is preserved according to the recovery contract;
- [ ] recording transitions to a safe stopped/error/recovery state;
- [ ] reconnecting MK-300 restores a selectable valid route;
- [ ] REC does **not** auto-resume after reconnect;
- [ ] a subsequent new recording uses the revalidated MK-300 route;
- [ ] no duplicate take/publication is created by reconnect/retry.

## 6. Continuous 10-minute quality session

Perform one representative continuous session of at least 10 minutes with playback and guitar recording.

- [ ] no growing audio offset/drift;
- [ ] no repeatable dropouts attributable to GuitarLab;
- [ ] no wrong speed/pitch;
- [ ] live waveform/meters remain responsive;
- [ ] UI remains responsive enough for transport actions;
- [ ] stop/finalization succeeds;
- [ ] project save succeeds;
- [ ] close/reopen restores the project correctly;
- [ ] recorded take remains correctly placed;
- [ ] representative master/export succeeds and sounds coherent.

## 7. Real Prepare flow

Using normal network/account conditions:

- [ ] the focused rc9 orphan-recovery retest above passes first;
- [ ] source search/import interaction is usable on the target tablet;
- [ ] a new real cloud separation can be initiated and its progress/status understood;
- [ ] local job identity correlates to the intended Firebase/Cloud Run processing identity;
- [ ] leaving/reopening the app/project and rebooting once do not confuse ownership/status;
- [ ] a local durable job with no remote counterpart converges safely instead of remaining active forever;
- [ ] cancellation reaches a terminal state even if the remote job is absent or cancellation cannot be confirmed after bounded retries;
- [ ] completed six-stem preparation reaches Ready for Studio;
- [ ] prepared backing/reference is usable in Studio without manual reimport;
- [ ] if a new reference revision is available, Update/Keep-current choice is understandable and non-destructive.

The six-stem server protocol and rc9 recovery contracts are digitally gated; U12 judges target-device/network interaction and resulting audio usability.

## 8. Real Drive account and backup/restore

The U8m campaign already proves Drive v3 transactional/integrity semantics. U12 validates the real target-device consent/account and user workflow:

- [ ] Drive connection/consent succeeds with the intended account;
- [ ] requested consent is consistent with GuitarLab backup, without unexpected broad-drive behavior;
- [ ] manual project backup completes and is understandable in UI/Activity;
- [ ] unchanged repeat backup does not present confusing duplicate history;
- [ ] a changed project can be backed up;
- [ ] restore produces an independent local project copy;
- [ ] restored project opens and its representative media is usable;
- [ ] disconnect/reconnect is understandable and does not delete remote backups.

## 9. Tablet ergonomics and final listening review

- [ ] primary Home/Prepare/Studio/Export/Settings/Backup controls are comfortably reachable;
- [ ] transport controls are unambiguous during real playing;
- [ ] project/workspace navigation does not cause accidental destructive actions;
- [ ] recording state is obvious at playing distance;
- [ ] destructive confirmations are readable and deliberate;
- [ ] no clipping/overlap appears under the user's normal system font/display settings;
- [ ] dark/light behavior used in normal device settings remains readable;
- [ ] final representative playback/export has no audible artifact that was absent from the source/take.

## 10. Acceptance

U12 PASS requires:
- no repeatable P0/P1 defect;
- no wrong physical input/output route;
- no silent tablet-mic fallback;
- no backing printed into guitar take with loopback OFF;
- no corrupt/losing take on ordinary stop;
- no unsafe USB reconnect behavior;
- no repeatable unacceptable timing/alignment defect;
- successful 10-minute session + save/reopen/export;
- successful real Prepare and Drive workflow smoke;
- explicit user approval of the exact signed candidate.

Record any accepted lower-severity issue with reproducible steps and rationale. A source fix after U11 freeze creates a new candidate and invalidates only the physical evidence materially affected by that change.


## RC19 corrective acceptance — install over RC18
RC19 must be installed **over the existing RC18 installation without clearing app data**.

Critical acceptance fixture:
- project: `18cf294d-7a76-4f03-8abd-bc55d3344beb`
- source asset: `f8c3e5d7-c759-41e1-945b-4968c3a54b0e`
- source SHA-256: `b0e24e05bf4adfe73a72b1cef961ad0f5322ddd4b995d8af7a84296f8feb324a`
- preserved completed remote job: `0860b0a7-6dda-439d-87af-b0f200c1a8b5`

The app must adopt that exact remote job without another Demucs execution or accepted-job increment, download only the prepared backing/guitar references, validate and publish them atomically, then ACK so Firestore reaches IMPORTED and remoteCleanupState reaches PURGED. Reopen/reboot must remain consistent and creative/recording state must remain intact.

Do not manually cancel, ACK, mutate or purge the fixture before this acceptance.

## RC19 physical audio-quality incident — acceptance blocked

RC19 is digitally qualified and signed, but it is **not physically accepted**.

Target-device evidence on 2026-09-24:
- project: `18cf294d-7a76-4f03-8abd-bc55d3344beb`;
- source asset: `f8c3e5d7-c759-41e1-945b-4968c3a54b0e`;
- source SHA-256: `b0e24e05bf4adfe73a72b1cef961ad0f5322ddd4b995d8af7a84296f8feb324a`;
- source operation: `5db9b4a8-b5ab-4a5e-a517-3c25e2678a7f`;
- processing/job: `c3ba40ec-6153-483a-801f-9e838b3e3b60`;
- active backing SHA-256: `d9f206f25f9eb28d02c7fb4a4a3e1d667077429e91520eb64763cd0251fe2a8e`;
- active guitar SHA-256: `8bbb4b5eb6b4ef8758669c1734b8d095893aadc9792d12a6330d9318eae725fa`.

The two supplied references are structurally valid 44.1 kHz stereo float32 WAVs with matching duration and no NaN/Inf, but listening remains unusable. Therefore container/hash validity is insufficient for final Prepare acceptance.

### Prepare search reliability corrective

A target-device recording also demonstrated a silent search terminal state:
- artist typed: `memphys may fire`;
- song: `misery`;
- busy state remained visible for roughly 15 seconds;
- the UI then returned to the idle search button with no persistent success/empty/error/suggestion state.

Acceptance now additionally requires:
- [ ] every source search ends visibly as results, no-exact-match, spelling suggestion, provider failure, timeout or explicit cancellation;
- [ ] provider failure is never shown as a successful empty search;
- [ ] a high-confidence typo such as `Memphys May Fire` can suggest `Memphis May Fire` using the existing local similarity/ranking primitives;
- [ ] accepting a suggestion is explicit and never auto-acquires media;
- [ ] rejecting a suggestion preserves the user's query;
- [ ] terminal search evidence is recorded in Activity and the diagnostics journal;
- [ ] no return from busy state to an indistinguishable idle form without a visible terminal outcome.

The incident also exposed:
- no explicit local action to reinsert already-active backing/guitar references after their Studio clips are deleted;
- ambiguous flat technical details that mix active, historical and derived L/R assets;
- no persistent/exportable application audit trail suitable for correlating Android actions to Firebase/Cloud Run evidence.

The authoritative corrective/investigation plan is:
`U12_RC19_AUDIO_FORENSICS_AND_DIAGNOSTICS_PLAN.md`.

Until that plan reaches its forensic decision point:
- do not alter the separation algorithm speculatively;
- do not apply a DC/high-pass corrective merely from the observed mean offset;
- do not declare RC19 physical PASS;
- do not sign a successor candidate.

Any source fix after RC19 freeze must use a new candidate identity. The planned successor is `0.5.0-rc20` / versionCode `40` after the forensic/root-cause and quality gates close.

Existing U12 evidence remains reusable only where the corrective cannot materially affect it. Prepare audio quality, reference reinsertion, diagnostics/observability and adjacent Studio handoff must be repeated on the successor candidate.

