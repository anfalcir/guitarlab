# U12 — Final Physical Homologation — GuitarLab

Updated: 2026-09-23
Status: **READY — RC9 DIGITAL PASS; FOCUSED PHYSICAL RETEST + REMAINING PHYSICAL HOMOLOGATION PENDING**
Target candidate: `0.5.0-rc9` / versionCode `29` / producer `635124acfbf133553a96c8b2013f2245f58a6877`
Package: `studio.guitarlab.app`
Target device: Samsung SM-X230 / Android 16 / API 36
Audio hardware: M-VAVE MK-300 over USB
Hardware loopback baseline: **OFF**

## Exact rc9 candidate binding
- Android CI: #794 / run `35856278980` — PASS;
- U4 Cloud Integration Smoke: #111 / run `35856278976` — PASS, real six-stem Cloud Run contract;
- U7 Cloud Backend: #69 / run `35856279117` — PASS, source/container/security verification; deploy skipped;
- signed artifact: `GuitarLabStudio-0.5.0-rc9-homologacao` / artifact id `10748475073`;
- signed APK SHA-256: `4fcf529b935a584217b3b882ca8dfa05e3c360f99cfe9d70071080175439dd6c`;
- signed APK size: `79,945,360` bytes;
- artifact ZIP SHA-256: `86077306c01afc80a07ba10b14080aec9fdb1a6a2e6416ef193bad190d274f53`;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- Android digital gate: exact-source materialization/unit/performance/Lint/build/API36/signing PASS.

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
- [ ] install/upgrade only the exact signed rc9 APK produced by `635124acfbf133553a96c8b2013f2245f58a6877`;
- [ ] package is `studio.guitarlab.app`;
- [ ] version is `0.5.0-rc9` / versionCode `29`;
- [ ] APK SHA-256 is `4fcf529b935a584217b3b882ca8dfa05e3c360f99cfe9d70071080175439dd6c`;
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
