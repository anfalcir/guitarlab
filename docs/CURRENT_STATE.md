# Current State — GuitarLab Studio

Updated: 2026-10-07

## Status

**RC32 COMMUNICATION SPLIT STABILIZATION — IMPLEMENTED; DIGITAL QUALIFICATION PENDING; PHYSICAL REQUALIFICATION PENDING**

RC31 owner-device evidence on SM-X230 / Android 16 is now positive for physical routing: MAIN MK-300 and wired CUE were repeatedly observed as distinct effective routes under `COMMUNICATION_SPLIT`, and real project audio was heard simultaneously on the intended destinations. The remaining RC31 instability is caused by validation/runtime policy, not by inability to expose the two routes.

The diagnostic bundle `GuitarLab-Diagnostics-1791369836868.zip` records repeated `SUPPORTED` Communication Split results with MAIN `usb|usb-audio - mk300|card=1`, CUE `wired-jack|`, MAIN 44.1 kHz, CUE 48 kHz and `modeRequired=false`. It also records intermittent `OFFSET_EXCEEDED` outcomes; the final failures use `MODE_IN_COMMUNICATION`, while all captured successful qualifications use the non-mode communication path. The companion screen recording shows two runtime suppressions: transient non-blocking CUE backpressure and runtime route requalification failure. One run reproduced correct real audio separation for several seconds before the previous drift/backpressure guard disabled CUE.

RC32 / versionCode `52` therefore separates route safety from synchronization quality. Physical route identity remains exact and fail-closed. Communication-capable CUE is attempted before MULTI_DEVICE to avoid first perturbing AudioPolicy with a known-convergent dual-MEDIA probe; MULTI_DEVICE remains a compatibility fallback. `MODE_IN_COMMUNICATION` is no longer part of the automatic path.

The default 12 ms startup offset limit remains for conventional MULTI_DEVICE. COMMUNICATION_SPLIT uses a bounded 60 ms startup limit and exports the actual measured offset. Runtime communication drift uses a 60 ms bound that must persist for 750 ms before suppression. These wider communication-specific limits are quality/stability policy only; they never weaken physical-route proof.

CUE remains strictly non-blocking relative to MAIN, but partial/zero `AudioTrack.WRITE_NON_BLOCKING` results are no longer treated as immediate failure. A bounded FIFO keeps sample order and absorbs transient backpressure; overflow/fatal write still disables CUE. Runtime Communication Split no longer repeats the full selection-time silent clock preflight on the same tracks before real playback; the actual playback start performs fresh physical route qualification and runtime drift supervision instead.

User-facing text is product-facing (`Saída CUE pronta` / concise failure messages). Internal strategy/homologation/ducking language stays in diagnostics. Route diagnostics advance to schema v4 with `initialOffsetNs` and `lastCueRuntime`, including backlog, drift and effective-route evidence for any runtime suppression.

Zero ducking remains mandatory. Mixer/project gains remain authoritative. RC31 CI #969 remains the last signed digital PASS; RC32 must pass software, API36 and signing before physical requalification.

The professional USB multichannel MAIN 1/2 + CUE 3/4 plan remains active at `history/CUE_USB_MULTICHANNEL_IMPLEMENTATION_PLAN_2026-10-06.md`. RC32 stabilization evidence and rationale are recorded at `history/CUE_COMMUNICATION_SPLIT_STABILIZATION_2026-10-07.md`.

## Exact signed RC31 identity — physical validation pending

- producer/signing SHA: `00fe4d621849bf5f8f1211e25ecba9d6a6c28598`;
- Android CI authority: **#969 / run `37549317925`**;
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc31`; versionCode: `51`;
- unsigned APK SHA-256: `d7d930ca782a6ad326cfb7295e2c2e09b96575cf8b1635c0baed05f60278a46a`;
- signed APK SHA-256: `7d9a0892ac221449ce72d3297e402cd1c605504f82bc808a6c00eaea2e4b3544`;
- signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact: `GuitarLabStudio-0.5.0-rc31-homologacao`;
- artifact ZIP digest: `sha256:323f971a7987fba017bb8adff5525cafb536d156fced025e8a9cf30f95fbc4e8`;
- artifact gate: `software+android-integration-passed;physical-validation-pending`;
- physical target: SM-X230 / Android 16, MAIN MK-300 media + CUE wired communication;
- Communication Split fidelity remains `PENDING_PHYSICAL`; digital PASS is not a claim of hi-fi or independent physical routing on the owner device.

## Exact signed RC28 identity and physical rejection

- qualification source: `132b8ffc4025645b1b061058c0fcdc9c03b414b5` / Git tree `2971085fcd2e386bc65055d1d7339c6d4e519ec7`;
- software/API36 authority: Android CI **#958 / run `37124218772`**;
- producer/signing SHA: `a837dd9aa533951448bbbd3607fe60b529894520`, preserving the same Git tree;
- signed authority: Android CI **#959 / run `37126984321`**;
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc28`; versionCode: `48`;
- signed APK SHA-256: `a916a5908c8b2160d390f10b9019fa3f55833e53297d091dfeaedac3a5438edb`;
- signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact: `GuitarLabStudio-0.5.0-rc28-homologacao` from #959;
- physical result: **REJECTED for synchronized CUE** by `142418.mp4`; the app still reports `ROUTE_UNCONFIRMED` for MK-300 MAIN + wired CUE.

## Active CUE implementation plan

`history/CUE_DUAL_OUTPUT_CAPABILITY_IMPLEMENTATION_PLAN_2026-10-06.md` defines the implementation sequence and gates. It prohibits blind timeout expansion or unverified fallback, requires typed distinction between convergence/missing/wrong/mirrored routes, and treats a supported physical transport topology as a release prerequisite.

## Exact signed RC27 identity

- producer/signing SHA: `222e66616e2eceba3e2781785c69c72020c40d04`;
- qualification source with identical Git tree: `a506f3f81744f5de19e6a4fedb232a64503e2f50`;
- software/API36 authority: Android CI **#955 / run `37045089530`**;
- signed authority: Android CI **#956 / run `37047225250`** — https://github.com/anfalcir/guitarlab/actions/runs/37047225250;
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc27`; versionCode: `47`;
- delivered filename: `GuitarLabStudio-0.5.0-rc27-homologacao.apk`;
- unsigned APK SHA-256: `905dbe47151ca0e8a537cfd0679427eb7193ac2bbf1f4178649404c951b7c99d`;
- signed APK SHA-256: `d5186972bda0efb48652656d310c3e45f70702df3e0dff070828e11890fb827f`;
- signed artifact ZIP SHA-256: `376650bdc6c6f87aa40480c9dd96db592aa3edf970004ffd0809ef4213c21d52`;
- signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- gate recorded by the artifact: `software+android-integration-passed;physical-validation-pending`;
- the producer requalifies/signs the same tree as #955; later documentation-only commits do not redefine this APK identity.

## Exact signed RC26 identity

- producer/signing SHA: `df5791c2e4984a7fbdf4141df460be20e151bcf8`;
- qualification source with identical Git tree: `148d7aacfe2793b2d4d10305940d838654ec83d9`;
- software/API36 authority: Android CI **#950 / run `36944380748`**;
- signed authority: Android CI **#951 / run `36946189701`** — https://github.com/anfalcir/guitarlab/actions/runs/36946189701;
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc26`; versionCode: `46`;
- delivered filename: `GuitarLabStudio-0.5.0-rc26-homologacao.apk`;
- unsigned APK SHA-256: `3abc796c6a52e8f66b1bc283600fa7bce4d92dc0b4a783c6ca313fafcc2ec69b`;
- signed APK SHA-256: `a096cc3f0aa31c07bb6c937fed7fa3da086a0a9568b1e51f8e357f4172650e97`;
- signed artifact ZIP SHA-256: `75e151eac0b1562428df6781a7e7dd6d78d8d2b32e39a574c8dd3f00bc3edd4d`;
- signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- gate recorded by the artifact: `software+android-integration-passed;physical-validation-pending`;
- this signed identity is frozen to the producer above; later documentation-only commits do not redefine the APK producer.

## Exact signed RC25 identity

- producer/merge SHA: `a17b5f63dbb394bc257c3b6d33fb079205014559`;
- signed authority: Android CI **#947 / run `36936451831`** — https://github.com/anfalcir/guitarlab/actions/runs/36936451831;
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc25`; versionCode: `45`;
- delivered filename: `GuitarLabStudio-0.5.0-rc25-homologacao.apk`;
- unsigned APK SHA-256: `756cc4c2c34614fd142a8c7f25a2538125c27c93f7c9682d39c3a75e2a0bd307`;
- signed APK SHA-256: `edbc376ab0ff600e0296894efdde8050704517a39eff655f8f225e4a8fdfb808`;
- signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signing verifies the exact tested release artifact after software/API36 qualification; physical successor acceptance remains separate.

## Inherited RC24 implementation qualified through RC25

- versionName `0.5.0-rc24`, versionCode `44`; no producer SHA or signed artifact declared before qualification;
- Comparison/Timeline vertical panels; balanced fixed navbar slots; Níveis in Master;
- fixed Mixer only; complete/minimum 252/172 dp, 200 dp channels, contiguous four-button row with 46 dp faces inside 48 dp targets;
- complete controls preserved; minimum retains all four buttons and gain, with shared complete details for pan/meter/clip reset;
- legacy visibility retained; new minimum preference defaults complete and persists independently;
- D-100 and subsystem/guide docs updated; immutable RC24 stage follows RC23;
- focused tests cover visual/target bounds, menu fit, minimum/details pan commits, preference upgrade and filled five-track tablet geometry/captures;
- local materialization/diff checks are recorded in `history/RC24_STUDIO_DENSITY_2026-10-01.md`; Android compile/Lint/instrumentation and screenshot review are qualified through RC25 CI #946.

## RC25 correction

- candidate `0.5.0-rc25` / `45`, based on RC24 qualification source `3279b8ba7217d27ee7c22aea6c56477044bb3054`;
- hide SCO from media outputs, preserve A2DP stereo, migrate old selection only with unambiguous identity; recording inputs and mono USB preserved;
- CUE options require both low-latency destinations; missing configuration blocks new activation without changing track/history;
- bounded continuously fed interleaved startup replaces drained/sequential clock sampling; all actual-route/clock/offset/runtime safety guards retained;
- pure fake-clock tests, Bluetooth/migration unit tests and unavailable-CUE instrumented regression added;
- qualification report: `history/RC25_OUTPUT_CUE_CORRECTION_2026-10-01.md`; Android/software PASS at CI #946; physical pair acceptance pending.

## RC26 Studio Mixer / Níveis correction

- candidate `0.5.0-rc26` / `46`, based on canonical signed RC25 source;
- exactly three left navbar actions: Comparação, Timeline, Níveis; Níveis opens level analysis directly with distinct gauge iconography;
- balanced 152 dp left/right navbar reservations preserve transport centering;
- Master no longer owns a Níveis text button;
- complete track/Master PK/RMS use full inner width; CLIP reset remains in headers;
- Master volume is wide and uses one-line `VOL +dB` readout below;
- dock heights (252/172 dp), channel/Master widths and all audio/domain behavior remain unchanged;
- deterministic materialization tail: `scripts/materialize_ci_sources_rc26a.py`; RC26/RC25 payloads remain immutable.

## RC26 qualification attempts

- CI #948 / run `36942666882` stopped at Diff sanity because the new RC26 history Markdown contained trailing whitespace; no source qualification ran.
- CI #949 / run `36943179605` passed Diff sanity, materialization, identity validation and unit tests. Lint then found one RC26-local error: the Master slider used `BoxWithConstraints` without consuming its scope. API36 independently reached `MixerDockInstrumentedTest`; its new full-width-meter assertion queried a merged semantics tree even though the tagged meter node exists in the unmerged tree.
- RC26a removes the unnecessary constrained scope and makes the geometry assertions explicitly query the unmerged semantics nodes. Product geometry and behavior are unchanged.

## RC27 Mixer card hierarchy

- candidate `0.5.0-rc27` / versionCode `47`, based on signed RC26 behavior;
- track headers center the accent dot + title as one identity group; Master centers `MASTER`;
- track cards softly segment header, state actions, metering and mix; Volume/Pan remain one mix region with a quiet internal divider;
- Master uses the same header/meter/volume hierarchy without an empty or fake action region;
- unselected track cards retain restrained track-accent identity; selection strengthens border/tonal emphasis without making color the sole state cue;
- 200 dp track width, 144 dp Master width, 252/172 dp dock heights, 48 dp targets, 46 dp faces, full-width PK/RMS, wide Master volume and large-font growth remain unchanged;
- audio, routing, persistence, level-analysis, export and MAIN/CUE behavior are unchanged;
- focused tests assert centered header groups, full-width/non-overlapping section geometry, existing control/touch behavior and target-tablet five-channel fit;
- deterministic tail advances to `scripts/materialize_ci_sources_rc27c.py`; RC27b/RC27a/RC27/RC26 payloads remain immutable.

## RC27 qualification attempt

- CI #952 / run `37034736534`: **software/API36 PASS** on `720ace0c76de20205ca6adde53d145fe4d867c8d`;
- minimum-mode full-tablet screenshot and focused Mixer screenshots show the approved centered headers and soft card segmentation;
- the complete-mode full-tablet screenshot is unusable as visual evidence because the loading spinner was still painted when the frame was captured, despite semantics already exposing the loaded nodes;
- no product/runtime defect is claimed from that artifact; the visual gate remains open because both complete and minimum evidence were explicitly required.

## RC27a qualification result

- CI #953 / run `37037749921`: **software/API36 PASS** on `b0e439d25a64ad8563228efd69515c779f41f25b`;
- refreshed complete and minimum target-tablet screenshots are loaded and reviewable; the approved centered-header/soft-segmentation layout is present in both;
- focused clipping/large-font screenshots revealed a separate transient-title collision: the textual `CLIP` face can cover the centered title, most visibly on Master.

## RC27b qualification result

- CI #954 / run `37040291049`: Unit Tests, Lint and APK build **PASS**;
- API36 reached `MixerDockInstrumentedTest` and **failed only** the new assertion `Master CLIP indicator must not cover MASTER`;
- the failure confirms the compact face size is correct but its centered placement within the 48 dp action target still intrudes into `MASTER`.

## Final RC27 digital qualification

- CI #955 / run `37045089530`: **PASS** — deterministic materialization, Unit Tests, Android Lint, APK build and API36 regression all passed on `a506f3f81744f5de19e6a4fedb232a64503e2f50`;
- complete/minimum target-tablet captures and focused clipping/large-font captures were reviewed; centered titles, soft segmentation and edge-anchored clipping badge are visually correct and non-overlapping;
- signing trigger `222e66616e2eceba3e2781785c69c72020c40d04` preserves the exact Git tree qualified in #955;
- CI #956 / run `37047225250`: **PASS** — software, API36 and signed homologation all passed; unsigned provenance, package/version, zipalign/signature and locked certificate checks passed;
- canonical signed RC27 artifact is available for physical validation.

## Next action

Install `GuitarLabStudio-0.5.0-rc27-homologacao.apk` on the target SM-X230 / Android 16 tablet and perform physical validation of the changed Mixer surface plus the intended MAIN/CUE output behavior. RC20 remains the promoted physical baseline until owner acceptance is explicitly recorded.

## Final RC26 digital qualification

- CI #950 / run `36944380748`: **PASS** — Diff sanity, deterministic materialization, identity, Unit Tests, Android Lint, APK build and API36 instrumented regression all passed;
- owner-requested Mixer/navbar screenshots from #950 were reviewed and matched the approved RC26 layout contract;
- signing trigger `df5791c2e4984a7fbdf4141df460be20e151bcf8` is an empty-tree-equivalent release commit over the exact #950 code tree;
- CI #951 / run `36946189701`: **PASS** — software, API36 and signed homologation all passed; unsigned provenance, package/version, zipalign/signature and locked certificate checks passed;
- canonical signed RC26 artifact is now available for owner physical validation.

## RC26 residual physical status

RC26 remains a valid signed digital artifact but was not promoted to the physical baseline before the owner requested RC27. Its digital evidence remains reusable for unchanged audio/routing paths; RC27 must qualify its own changed Mixer presentation. RC20 therefore remains the accepted physical baseline.

No backend, Demucs, backup or MAIN/CUE algorithm was reopened by RC26 or RC27 presentation changes.

## Predecessors

RC22 signed digital evidence: `history/RC22_DUAL_OUTPUT_CUE_QUALIFICATION_2026-10-01.md`. Accepted RC20 artifact/backend identity remains exclusively in `RELEASE_BASELINE.md` until explicit successor physical acceptance.

RC24 CI #940 reviewed after owner reported failure: build/unit/Lint PASS; API36 Studio group timed out in large-font mixer regression. RC25 follow-up replaces its unbounded scroll helper, uses full geometry and retains target/visibility checks; preserves original CI timeout status. Source stage RC25b; new Android qualification pending. Precise blocked call in old run remains unproven without per-step logs/thread dump.

RC25 CI #942: build/unit/Lint PASS; instrumented mixer large-font test terminates, failing its volume-target-size assertion. RC25c fixes test helper inner-layout vs semantic-bounds confusion and checks effective touch bounds separately. New Android qualification pending; no product layout changes, no CI monitoring.

RC25 CI #943: build/unit/Lint PASS; bounded mixer test identifies Volume semantic overhang at horizontal scroll end. RC25d fixes shared compact Slider padding and thumb measurement to contain expanded semantics and retain 48dp interaction, with physical off-thumb tap preview/commit test. New Android qualification pending. Dock/channel dimensions unchanged.

RC25 CI #944: build/unit/Lint PASS; all five mixer component tests PASS, including large font/off-thumb interaction. Failure advances to fixed-navbar test applying tablet viewport centering to a scrolled phone row. RC25e separates content-relative fixed-slot checks from actual-tablet viewport centering and adds transport invariance to tablet coverage. Navbar geometry unchanged; new Android qualification pending.

RC25 CI #945: build/unit/Lint PASS; navbar regression still read viewport width from a shared scroll/content layout node. RC25f gives the scroll Box and fixed child Row separate layout/semantic identities, retains dimensions and verifies both geometry sizes. Tablet centering and fixed-slot tests remain. New Android qualification pending.

RC25 digital authority: https://github.com/anfalcir/guitarlab/actions/runs/36934260224 (CI #946), source 854aeee0da730d2011e1141fc50a69d59bb13d58. Terminal stage materialize_ci_sources_rc25f.py; previous payloads immutable. Canonical signing pending.
