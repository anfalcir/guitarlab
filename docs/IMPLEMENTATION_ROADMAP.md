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

CI #620 is the authoritative active DIGITAL PASS through H11. Physical Review IV adds H12–H15 and requires one new exact-source gate.

## M8 — Release hardening
### H0–H10 — DIGITAL PASS baseline
CI #617 / run ID `34918430241` / source `abc0e2a9f8708dd141735915898b508ce0948f48` passed software, API36 full instrumentation, tablet geometry and signed homologation.

### H11/H11a/H11b — DIGITAL PASS
CI #620 / run ID `34924500870` / exact source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7` passed software, API36 full instrumentation, unchanged independent Trim regression, H11 interaction coverage, tablet geometry and signed homologation.

Candidate #620 identity:
- version `0.5.0-rc3` / code `23`;
- signed APK SHA-256 `acbe61b006aa4abe8b3063faf35b4a9569ed55aaf7f1a2ca3e1726c927855b3c`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

### H12 — all-track level workflow — IMPLEMENTED / PRE-GATE
- `Níveis` action integrated into the Comparação practice segment;
- dedicated modal for all project tracks;
- global and per-track analysis/reanalysis;
- global and per-track apply;
- global apply is one transactional project edit / one Undo step;
- stale analysis rejected through exact track + audible-clip snapshot validation;
- explicit per-track/global busy state;
- new UI instrumentation for modal contracts.

### H13 — Trim time-ruler projection — IMPLEMENTED / PRE-GATE
- waveform-overlapping Trim time bubbles removed;
- T1/T2 projected on the fixed timeline time ruler with short yellow ticks;
- precise floating labels remain above ordinary timeline/playhead drawing priority;
- nearby T1/T2 labels separate vertically;
- existing independent Trim handles retained;
- physical-editing instrumentation extended to require ruler markers.

### H14 — Mixer horizontal overflow — IMPLEMENTED / PRE-GATE
- track strips use `LazyRow` horizontal scrolling;
- MASTER remains outside the scroll container and fixed at the right edge;
- regression swipes a 10-track Mixer to the final track and asserts invariant MASTER bounds.

### CI #621 — diagnostic Physical Review IV failure
Run ID `34998393778`, source `afecde0efd4d22e58115eaedadf60eea0eb3615c`:
- software/unit/performance/Lint/build/provenance: **PASS**;
- API 36: **21/22 PASS**;
- sole failure: `MixerDockInstrumentedTest.overflowingTracksSwipeHorizontallyWhileMasterRemainsAnchored`;
- signed homologation: skipped by the mandatory upstream gate.

The failure came from the regression assuming four swipes were enough on every viewport. H12 all-track levels, H13 Trim ruler and H15 resident same-project return did not fail.

### H14a — viewport-independent physical swipe regression — IMPLEMENTED / PRE-GATE
- retains real `swipeLeft` gestures on the Mixer track scroller;
- checks after each gesture whether the final strip is composed and intersects the actual scroller viewport;
- uses a bounded 20-gesture safety ceiling instead of a fixed successful swipe count;
- keeps strict MASTER left/right geometry assertions;
- does not use `scrollToItem`, timeout inflation or weakened assertions.

### H15 — resident Studio navigation return — IMPLEMENTED / PRE-GATE
- same-project `StudioViewModel.load()` returns from resident state rather than publishing `loading=true` and reloading;
- Home/Options → same Studio preserves resident project/history and avoids the observed double-render flash;
- lifecycle instrumentation asserts object identity and Undo retention after round-trip navigation;
- in-app `Ajuda` is synchronized for the H12–H14 user-facing workflows.

## Physical Review IV source-validation evidence
H12→H15 were applied serially against the exact #620 materialized source artifact; after #621, H14a was folded into the revised H14 patch and revalidated from the same exact #620 baseline through H15. Patch dry-run/application succeeded, `git diff --check` passed, and the resulting changed/new source/test/help files matched the independently developed final tree byte-for-byte.

Patch SHA-256:
- H12 engine `39c6a42bca192b1e6a829bd52c46ddb7e546d7cad09500d850e257dec57bc37c`
- H12 UI `db9a7e448a22f79e8be22b9b795d4a4008bd92b4bff9754ad640eb957895308d`
- H13 `4fd47e9d55de072be9ccfbc64361f3e87f9e38d68aa57a76a5084085bce399b0`
- H14 `af3bf0808a5724f8aab3f6f17dec15b041591871ae26e51283d85a03a28a3e43`
- H14 revised/H14a folded-in `2a96869617205b56b94a5e7d97dac99a3859071c90605a7eed9715efac9e6b51`
- H15 `79e4a98f996f86cb2c0916f1c152ef8a34529e369f7db1622ea4a5a7f427a1c9`

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
