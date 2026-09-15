# Implementation Roadmap

Updated: 2026-09-14

## M1 — Project/model foundation — CLOSED
Project model, templates, persistence baseline and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Original Samsung SM-X230 USB-audio baseline established. Historical Pocket Amp evidence remains valid for the tested combination only.

## M3 — Codec/import foundation — ABSORBED
Codec/import work was consolidated into later milestones.

## M4 — Studio playback/edit/mix foundation — ABSORBED
Timeline, playback, editing, Mixer/Master and project interaction foundations are integrated.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after explicit physical approval of `0.2.0-alpha14`.

## M6 — Measured latency and synchronization — PASS/CLOSED
Closed after explicit physical approval of `0.3.0-alpha1`. Route-scoped calibration, clocks, live Mute/Solo and project rename are no longer pending milestone work. The post-615 RC3 work below hardens per-session startup synchronization without reopening M6.

## M7 — Production audio polish — FINAL PHYSICAL GATE
Implemented scope includes:
- managed immutable sources plus validated editing proxies;
- bounded sample-rate conversion and editing-domain validation;
- non-destructive fades/crossfades with shared playback/export math;
- transactional recording and conservative interrupted-take recovery;
- live waveform during recording;
- fail-closed selected recording input;
- monitoring separated from recorded content;
- take management and active-take policy;
- practice markers, sections, automatic section suggestions and comparison modes;
- transient loop-derived punch recording with pre/post-roll and latency-aware retention;
- level analysis/suggestion;
- project/master export and package round-trip hardening.

M7 is not closed until the exact active RC passes the canonical automated gates and the residual Samsung SM-X230 + M-VAVE MK-300 physical checklist receives explicit approval.

## M8 — Release hardening — H0–H6 IMPLEMENTED / EXACT-SOURCE GATE PENDING
### M8.A — Compatibility + edge cases
Covered by deterministic regression for legacy/current migration, malformed/truncated packages, duplication/media ownership, editing-domain/fade/frame validation, SRC/master render, interrupted import/recording recovery and safe publication/cancellation.

### M8.B — UX + accessibility + performance
Objective coverage includes Compose accessibility/callback checks, lifecycle recreation, target geometry instrumentation, structural/render performance evidence and large/fuzz project regression.

### M8.C — RC3 physical-review hardening
The latest physical findings were converted into one serial hardening program rather than isolated patches:

- **H0 — reproduction/invariants:** fixtures and deterministic contracts for trim, split-lineage move/delete, timing and long live-waveform behavior.
- **H1 — trim interaction:** independent start/end handles with explicit touch targets/semantics and deterministic frame mapping.
- **H2 — clip lifecycle:** split siblings preserve recording-take lineage; canonical take clip is promoted when needed; clip deletion is explicit and confirmed.
- **H3 — drag transaction:** `Move` / `Delete` / `NoOp` intent, source-state revalidation and drag-to-trash using the same domain delete path.
- **H4 — recording synchronization:** per-session capture/backing startup skew is measured separately from accepted route latency and applied exactly once; no hard-coded `-0.5 s` correction.
- **H5 — live REC waveform:** frame-span envelope, bounded compaction preserving transients and conflated/bounded UI publication.
- **H6 — integrated regression/release preparation:** persistence/save-reopen regression, editing instrumentation, timing/waveform tests, guide synchronization and serial source materialization. A final test-assertion defect found during review is isolated in `H6WaveformTestFix.patch` and included in the materialization chain.

H0–H6 are **implemented/pre-gate**, not digitally homologated. Only the next canonical exact-source workflow can promote them.

### M8.D — Candidate progression and evidence
- `0.4.0-rc2` / versionCode 20: full API 36 + target geometry + signed baseline PASS in CI #593.
- `0.5.0-rc1` / versionCode 21: recording/practice hardening candidate.
- `0.5.0-rc2` / versionCode 22: shared timeline/practice visual refinement.
- `0.5.0-rc3` / versionCode 23: active candidate.
- CI #613 / `db5a4208848e4b6ca2163ce715d0c5bb464cfe37`: earlier complete RC3 signed baseline PASS.
- CI #614 / `9be232643f1d34f7e3a08417e44773e4f8b9ef7f`: failed only because a new Compose test used unavailable `assertDoesNotExist`; release signing was correctly blocked.
- CI #615 / `74bf86efbec94d249c4968c3284bf1985cd66b44`: complete software + API 36 + tablet geometry + signed homologation PASS; latest fully green signed baseline. Signed APK SHA-256 `d443cb33a010d2d21dad43e9ff12554d7ada4f2bb783be0c80e3ac11bfe2d6c9`.

Current `main` advanced after #615 through H0–H6. Ordinary commits do not trigger hosted CI. One new exact-source manual run is required before promoting a new APK.

## Active RC3 acceptance gates
RC3 may return to final physical homologation only after one manually dispatched workflow proves all of the following for the same final `github.sha`:
1. unit/JVM regression PASS, including H0/H2/H4/H5/H6 coverage;
2. Android Lint PASS;
3. debug/release assembly PASS;
4. Android test compilation PASS;
5. API 36 connected regression PASS;
6. isolated 1920×1200 geometry PASS, including new trim/delete interaction coverage;
7. signed homologation release PASS;
8. signer certificate equals the locked GuitarLab identity;
9. package/version/source provenance and APK SHA-256 are published.

After that, `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` is the only active manual checklist. It intentionally concentrates on target-only evidence: trim ergonomics, split/move/delete/trash workflow, real MK-300 routing/isolation, guitar-vs-backing synchronization, multi-minute live waveform behavior, listening/latency feel and one representative export/stress smoke.

## Gate discipline
`.github/workflows/android-ci.yml` is canonical and manual-only. The signed homologation job depends on both software and Android integration gates; signing cannot bypass a failed mandatory gate. `scripts/materialize_ci_sources.sh` applies versioned RC3/H1–H6 deltas serially and must fail on patch drift rather than silently producing partial source. Documentation must distinguish source review, JVM/build evidence, emulator evidence and target-hardware evidence.

`CANDIDATE_IDENTITY_POLICY.md` is authoritative for candidate identity. Historical candidate/checklist documents are evidence only and must not override the active RC3 documents listed in `DOCUMENTATION_MAP.md`.
