# Current State — GuitarLab Studio

Updated: 2026-10-01

## Status

**RC26 STUDIO MIXER / LEVELS UI — SOURCE CANDIDATE; DIGITAL QUALIFICATION PENDING**

RC25 is canonically signed and digitally qualified. Android CI **#947 / run 36936451831** on source **a17b5f63dbb394bc257c3b6d33fb079205014559** produced the verified `0.5.0-rc25` / 45 homologation APK after the full software and API36 gates. RC26 is the owner-requested Studio presentation successor: Níveis moves to a third navbar button, PK/RMS gain the full useful strip width, and Master receives a wide volume control with its readout below. RC20 remains the physically accepted frozen baseline; RC26 has no qualification claim before its new gate passes.

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

## Next action

Run the exact RC26a source through the controlled `[run ci]` software + API36 gate. Do not declare RC26 digitally qualified, signed or physically accepted before that gate succeeds.

No backend, Demucs, backup or MAIN/CUE algorithm is reopened by this presentation-only correction.

## Predecessors

RC22 signed digital evidence: `history/RC22_DUAL_OUTPUT_CUE_QUALIFICATION_2026-10-01.md`. Accepted RC20 artifact/backend identity remains exclusively in `RELEASE_BASELINE.md` until explicit successor physical acceptance.

RC24 CI #940 reviewed after owner reported failure: build/unit/Lint PASS; API36 Studio group timed out in large-font mixer regression. RC25 follow-up replaces its unbounded scroll helper, uses full geometry and retains target/visibility checks; preserves original CI timeout status. Source stage RC25b; new Android qualification pending. Precise blocked call in old run remains unproven without per-step logs/thread dump.

RC25 CI #942: build/unit/Lint PASS; instrumented mixer large-font test terminates, failing its volume-target-size assertion. RC25c fixes test helper inner-layout vs semantic-bounds confusion and checks effective touch bounds separately. New Android qualification pending; no product layout changes, no CI monitoring.

RC25 CI #943: build/unit/Lint PASS; bounded mixer test identifies Volume semantic overhang at horizontal scroll end. RC25d fixes shared compact Slider padding and thumb measurement to contain expanded semantics and retain 48dp interaction, with physical off-thumb tap preview/commit test. New Android qualification pending. Dock/channel dimensions unchanged.

RC25 CI #944: build/unit/Lint PASS; all five mixer component tests PASS, including large font/off-thumb interaction. Failure advances to fixed-navbar test applying tablet viewport centering to a scrolled phone row. RC25e separates content-relative fixed-slot checks from actual-tablet viewport centering and adds transport invariance to tablet coverage. Navbar geometry unchanged; new Android qualification pending.

RC25 CI #945: build/unit/Lint PASS; navbar regression still read viewport width from a shared scroll/content layout node. RC25f gives the scroll Box and fixed child Row separate layout/semantic identities, retains dimensions and verifies both geometry sizes. Tablet centering and fixed-slot tests remain. New Android qualification pending.

RC25 digital authority: https://github.com/anfalcir/guitarlab/actions/runs/36934260224 (CI #946), source 854aeee0da730d2011e1141fc50a69d59bb13d58. Terminal stage materialize_ci_sources_rc25f.py; previous payloads immutable. Canonical signing pending.
