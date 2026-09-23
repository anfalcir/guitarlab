# U12 — Final Physical Homologation — GuitarLab

Updated: 2026-09-23
Status: **CORRECTIVE ACTIVE — RC7 WITHDRAWN; RC8 DIGITAL VALIDATION PENDING**
Target candidate: pending rc8 corrective validation (`0.5.0-rc8` / versionCode `28`). Rc7 is withdrawn from final approval.
Package: `studio.guitarlab.app`  
Target device: Samsung SM-X230 / Android 16 / API 36  
Audio hardware: M-VAVE MK-300 over USB  
Hardware loopback baseline: **OFF**

Exact rc7 candidate binding:
- producer SHA: `74274dd51ad75a7d4b9e15a82fe4b64ba448c498`;
- Android CI: #792 / run `35810108338`;
- signed artifact: `GuitarLabStudio-0.5.0-rc7-homologacao` / artifact id `10729009613`;

Rc7 physical finding (2026-09-23): a separation requested before the tablet's scheduled reboot never crossed the cloud boundary and remained visually active; cancellation could not resolve it because the client persisted the job identity only inside the deferred worker. Cloud correlation found no corresponding Storage input, Firestore job, Functions invocation or Cloud Run execution. Rc8 must close persistence-before-scheduling, reboot resume, cancellation routing/terminalization and cloud identity binding. Repeat only Prepare separation across lock/reboot, cancel/retry and adjacent activity/backup smoke; unrelated physical evidence remains reusable.
- signed APK SHA-256: `142b892b375d495df90030806843e66a2f884a0e1023aefad183d9fe46304e44`;
- signed APK size: `79,937,168` bytes;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- Android digital gate: exact-source materialization/unit/performance/Lint/build/API36/signing PASS.

Exact rc6 candidate binding:
- producer SHA: `d0926e9dbd231b6d91c19448279fe8749d182ba1`;
- Android CI: #789 / run `35807008698`;
- signed artifact: `GuitarLabStudio-0.5.0-rc6-homologacao` / artifact id `10728840806`;
- signed APK SHA-256: `4bc76565c74366d89df23db2e6410e77976a1cc827a79486ceec629fd16112b6`;
- signed APK size: `82,894,480` bytes;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- Android digital gate: unit/performance/Lint/build/API36/signing PASS.

Cloud-source continuity:
- the rc5→rc6 corrective delta changes no `cloud/` source, U4/U7 workflow or cloud-smoke script;
- U4 Cloud Integration Smoke #104 / run `35793456955` remains applicable for the unchanged real six-stem Cloud Run contract;
- U7 Cloud Backend #62 / run `35793456941` remains applicable for the unchanged backend tests/security/container contract.

Historical U11 rc5 binding:
- producer SHA: `4218e4343746932a4de61c5abaa29ba5769a30ed`;
- Android CI: #783 / run `35793456972`;
- signed APK SHA-256: `795839766f2b7546af53b2c56a0638b11c25b79622fe73cce5860b5602f050e0`.

A later docs-only commit never changes the APK producer identity. Rc5 remains historical evidence only and is no longer eligible for final U12 approval.

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
- [ ] install only the exact signed rc6 replacement APK after its digital gate closes;
- [ ] producer SHA matches the rc6 replacement release evidence;
- [ ] package is `studio.guitarlab.app`;
- [ ] version is `0.5.0-rc6` / `26`;
- [ ] APK SHA-256 matches the rc6 replacement checksum;
- [ ] signer SHA-256 is `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- [ ] device is Samsung SM-X230 / Android 16 / API 36;
- [ ] MK-300 is connected through the intended normal-use USB/hub/power topology;
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

- [ ] source search/import interaction is usable on the target tablet;
- [ ] real cloud separation can be initiated and its progress/status understood;
- [ ] leaving/reopening the app/project does not confuse ownership/status;
- [ ] completed six-stem preparation reaches Ready for Studio;
- [ ] prepared backing/reference is usable in Studio without manual reimport;
- [ ] if a new reference revision is available, Update/Keep-current choice is understandable and non-destructive.

The server protocol itself is already digitally gated; U12 judges target-device/account/network interaction and resulting audio usability.

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
