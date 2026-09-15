# Implementation Roadmap

Updated: 2026-09-15

## M1 — Project/model foundation — CLOSED
Project model, templates, persistence baseline and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Samsung SM-X230 USB-audio baseline established.

## M3 — Codec/import foundation — ABSORBED
Consolidated into later milestones.

## M4 — Studio playback/edit/mix foundation — ABSORBED
Timeline, playback, editing, Mixer/Master and project interaction foundations are integrated.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after explicit physical approval.

## M6 — Measured latency and synchronization — PASS/CLOSED
Closed after explicit physical approval. Later timing work hardens per-session startup behavior without reopening M6.

## M7 — Production audio polish — DIGITAL PASS THROUGH H11 / H12–H15+H14a PRE-GATE
Managed media, SRC/editing domain, fades/crossfades, transactional recording/recovery, fail-closed input, monitoring isolation, takes, practice controls, level analysis, live REC waveform, Mixer/selection/metering refinements and project/master export are implemented.

CI #620 is the authoritative active DIGITAL PASS through H11/H11a/H11b. Physical Review IV adds H12–H15 plus H14a and requires one new exact-source canonical gate.

## M8 — Release hardening
### H0–H10 — DIGITAL PASS baseline
CI #617 / run ID `34918430241` / source `abc0e2a9f8708dd141735915898b508ce0948f48` passed software, API36 full instrumentation, tablet geometry and signed homologation.

### H11/H11a/H11b — DIGITAL PASS
CI #620 / run ID `34924500870` / exact source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7` passed software, API36 full instrumentation, unchanged independent Trim regression, H11 interaction coverage, tablet geometry and signed homologation.

Candidate #620 identity:
- version `0.5.0-rc3` / code `23`;
- signed APK SHA-256 `acbe61b006aa4abe8b3063faf35b4a9569ed55aaf7f1a2ca3e1726c927855b3c`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

### H12 — all-track level workflow — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- `Níveis` action integrated into the Comparação practice segment;
- dedicated modal for all project tracks;
- global and per-track analysis/reanalysis;
- global and per-track apply;
- global apply is one transactional project edit / one Undo step;
- stale analysis rejected through exact track + audible-clip snapshot validation;
- explicit per-track/global busy state;
- new UI instrumentation for modal contracts.

### H13 — Trim time-ruler projection — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- waveform-overlapping Trim time bubbles removed;
- T1/T2 projected on the fixed timeline time ruler with short yellow ticks;
- precise floating labels remain above ordinary timeline/playhead drawing priority;
- nearby T1/T2 labels separate vertically;
- existing independent Trim handles retained;
- physical-editing instrumentation extended to require ruler markers.

### H14 — Mixer horizontal overflow — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- track strips use `LazyRow` horizontal scrolling;
- MASTER remains outside the scroll container and fixed at the right edge;
- product regression uses a 10-track Mixer and asserts invariant MASTER bounds.

### CI #621 — diagnostic Physical Review IV failure
Run ID `34998393778`, source `afecde0efd4d22e58115eaedadf60eea0eb3615c`:
- software/unit/performance/Lint/build/provenance: **PASS**;
- API 36: **21/22 PASS**;
- sole failure: `MixerDockInstrumentedTest.overflowingTracksSwipeHorizontallyWhileMasterRemainsAnchored`;
- signed homologation: skipped by the mandatory upstream gate.

The failure came from the regression assuming four swipes were enough on every viewport. H12 all-track levels, H13 Trim ruler and H15 resident same-project return did not fail.

### H14a — viewport-independent physical swipe regression — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- remains a separate source-part after H14 for bisectability;
- retains real `swipeLeft` gestures on the Mixer track scroller;
- checks after each gesture whether the final strip intersects the actual scroller viewport;
- uses a bounded 20-gesture safety ceiling instead of a fixed successful swipe count;
- keeps strict MASTER left/right geometry assertions;
- does not use `scrollToItem`, timeout inflation or weakened assertions.

### H15 — resident Studio navigation return — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- same-project `StudioViewModel.load()` returns from resident state rather than publishing `loading=true` and reloading;
- Home/Options → same Studio preserves resident project/history and avoids the observed double-render flash;
- lifecycle instrumentation asserts object identity and Undo retention after round-trip navigation;
- different-project loading remains on the normal path;
- in-app `Ajuda` is synchronized for the H12–H14 user-facing workflows.

### CI #622 — materializer infrastructure failure before gates
Run ID `35000635666`, source `5832c6800a7523a0bed0b64b9400a00c1fa876c2`:
- `Unit tests + Lint + APK build`: failed at `Materialize split source`;
- `API 36 emulator regression`: failed at the same materialization step;
- no compilation or functional test execution occurred;
- signed homologation: correctly skipped;
- shell error: `line 167: unexpected EOF while looking for matching '"'`.

Root cause: `scripts/materialize_ci_sources.sh` was truncated inside the H7–H10 guard. #622 does not invalidate H12–H15 implementation or the #620 baseline.

### CI #623 — repaired chain reaches API36; H14a gesture lane remains
Run ID `35003025673`, source `973ecae78ee3c159e975b4b1d65a2f733aedf59a`:
- source materialization: **PASS**;
- complete software/unit/performance/Lint/debug+release/provenance gate: **PASS**;
- API36: **21/22 PASS**;
- only failure: `MixerDockInstrumentedTest.overflowingTracksSwipeHorizontallyWhileMasterRemainsAnchored`;
- H12, H13 and H15 instrumentation: **PASS**;
- signed homologation: skipped by the mandatory API36 gate.

The H14a retry/viewport logic executed but used Compose `swipeLeft()`, whose default gesture path is the scroller centerline. That line intersects horizontal Mixer sliders. H14a v2 retains physical swipe semantics while moving the gesture to the non-slider track-header band using explicit `swipe(start, end)`, keeping the bounded retry loop, viewport-intersection assertion and fixed-MASTER bounds.

## Physical Review IV source-validation evidence after #623 H14a v2
Required order after H11b:
1. `H12LevelEngine.patch`
2. `H12LevelUi.patch`
3. `H13TrimRuler.patch`
4. `H14MixerHorizontalScroll.patch`
5. `H14aMixerScrollViewportRegression.patch`
6. `H15ResidentStudioReturn.patch`

Evidence:
- repaired materializer Git blob `de488110153b8a800b69b520a29860230bcb5838`;
- repaired materializer SHA-256 `612f171be1345b59e0f81e7f0e8cfbd78de0dcbc981fe4edcf22130b1b61779f`;
- `bash -n scripts/materialize_ci_sources.sh`: **PASS**;
- six Physical Review IV source parts dry-run/applied serially from the exact #620 materialized source snapshot: **PASS**;
- `git diff --check`: **PASS**;
- final source/test/help blobs match the expected audited tree, including `MixerDockInstrumentedTest.kt` `bf81aa9414a376679634f8ddf3f0b9bbf58fde5d`.

Patch SHA-256:
- H12 engine `39c6a42bca192b1e6a829bd52c46ddb7e546d7cad09500d850e257dec57bc37c`
- H12 UI `db9a7e448a22f79e8be22b9b795d4a4008bd92b4bff9754ad640eb957895308d`
- H13 `4fd47e9d55de072be9ccfbc64361f3e87f9e38d68aa57a76a5084085bce399b0`
- H14 `af3bf0808a5724f8aab3f6f17dec15b041591871ae26e51283d85a03a28a3e43`
- H14a v1 `eb347d29e3bdfa61af6da984d85d985ec0871bc8165ce6961a4043fdbb03c658`
- H14a v2 `0ecdfc2922311a3f6b0c773ece8cb4a27eb049780e8affa793bc2f496b71cef7`
- H15 `79e4a98f996f86cb2c0916f1c152ef8a34529e369f7db1622ea4a5a7f427a1c9`

The materializer is a clean-checkout materialization contract. Whole-script rerun against an already fully materialized #620 artifact is not the supported idempotence path because historical RC3 guards deliberately validate repository baselines. Supported idempotence is provided by patch-level reverse-match detection plus explicit final-blob guards; unexpected drift fails closed.

#623 provides real CI evidence that the repaired materializer and full software/build path pass. The H14a v2 coordinate-lane refinement is newer than #623 and remains source-validated/pre-gate until the next exact-source API36/signing run.

## Next acceptance gate
After final consolidation on `main`, manually dispatch `GuitarLab Android CI` with `signed_homologation=true` on the exact final SHA. It must pass:
1. complete JVM/unit regression;
2. performance evidence;
3. Android Lint and debug/release assembly;
4. API36 full connected regression, including H12/H13/H14/H14a/H15 and retained H11 Trim contracts;
5. isolated 1920×1200 geometry;
6. signed homologation;
7. exact source/package/version/signer/checksum provenance.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only. No assistant-triggered dispatch or rerun. H12–H15/H14a are not DIGITAL PASS until that new user-dispatched exact-source workflow succeeds.
