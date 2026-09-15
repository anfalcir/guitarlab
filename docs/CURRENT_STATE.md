# Current State — GuitarLab Studio

Updated: 2026-09-14

## Active candidate
- Canonical repository/branch: `anfalcir/guitarlab` / `main`.
- Active version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last digitally homologated application source: `3051619c219e346daca00d2242f60ef03f2d80db` from CI #616.
- CI #616 signed APK SHA-256: `92e806c6fbfd68b0fd44409570c17a976b922e56f2d206824a308c1fdc15bf9c`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- `.github/workflows/android-ci.yml` remains manual-only. Ordinary commits use `[skip ci]`.

## Evidence boundary
CI #616 is the authoritative digital PASS for H0–H6 only. Physical review of that APK exposed a new delta, Physical Review II H7–H10. H7–H10 are now **implemented/source-validated but PRE-GATE** and therefore require one new exact-source workflow before a replacement APK can be promoted.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7: previous digital scope PASS; final physical closure remains open because H7–H10 change active application behavior.
- M8: H0–H6 DIGITAL PASS in CI #616; H7–H10 IMPLEMENTED / PRE-GATE.

## CI #616 retained evidence
Run #616 / ID `34912716297` passed software, API 36 integration, isolated 1920×1200 geometry and signed homologation against exact source `3051619c219e346daca00d2242f60ef03f2d80db`. Package/version/provenance and the locked certificate matched. This evidence remains valid for unaffected behavior but does not validate the newer H7–H10 source.

## Physical Review II — H7–H10
### H7 — state/history, level analysis and recording stop — IMPLEMENTED / PRE-GATE
- project mutations now resynchronize history-derived `canUndo`/`canRedo`, playback/mixer/readiness state instead of leaving stale UI state;
- asynchronous history/analysis work is guarded against project switches so a result from one project cannot be published into another;
- level analysis reasons about the effective level after current track/clip gain, so apply → re-analyze converges instead of proposing the same correction repeatedly;
- recording Stop and REC-during-capture use the same idempotent successful finalization path; Stop cancels countdown and cannot double-finalize.

### H8 — workspace flow and action semantics — IMPLEMENTED / PRE-GATE
- `Cortar` must open on the first valid tap; blocked entry produces explicit user feedback instead of a silent no-op;
- clip-level `Excluir clipe` remains contextual; track-wide destructive content clearing is presented as `Limpar toda a pista` in track configuration rather than as a second neighboring trash action;
- the same Comparison/Timeline control surface is embedded into the Mixer header on wide layouts, preserving Mixer title left and Pin/Close right; when Mixer is closed the same component returns to workspace flow.

### H9 — live REC waveform spatial stability — IMPLEMENTED / PRE-GATE
- the old pairwise global compaction behavior exposed by the physical video was replaced by uniform temporal bucketing;
- old and new waveform material share one current temporal resolution instead of leaving sparse history and dense recent samples;
- renderer spans each bucket across its represented time interval rather than drawing only a thin center stroke;
- long/variable-cadence regression checks monotonic time coverage, stable bucket resolution and transient retention.

### H10 — race closure, integrated regression and guide sync — IMPLEMENTED / PRE-GATE
- history/analysis callbacks are session/project guarded;
- integrated regression covers repeated level apply/analyze, history stress, project switching, first-tap trim entry, recording stop and long live waveform behavior;
- `StudioUserGuideDialog` is synchronized with Stop-during-REC, analysis convergence, clip-vs-track deletion scope and Mixer-integrated practice controls;
- H7–H10 are a guarded materialization unit under `.source-parts` with final-hash idempotence checks.

## Next authoritative gate
The next promoted candidate must be produced by one manually dispatched `GuitarLab Android CI` run on the final `main` HEAD after documentation consolidation. Required PASS:
1. complete JVM/unit regression including H7–H10;
2. Android Lint;
3. debug/release assembly;
4. API 36 full instrumentation;
5. isolated 1920×1200 tablet geometry;
6. signed homologation;
7. exact package/version/source provenance;
8. locked signer verification;
9. published APK SHA-256.

Do not reuse the #616 APK to validate H7–H10.

## Residual physical gate after the new automated PASS
Physical validation should be focused on facts automation cannot establish:
- first-tap reliability of `Cortar` and trim-handle ergonomics;
- level analysis → apply → analyze convergence and Undo/Redo responsiveness on the tablet;
- transport/history controls remaining responsive after repeated edits and project switching;
- Stop and REC both ending capture successfully;
- Mixer-header Comparison/Timeline ergonomics and no layout collision;
- 2–3 minute live REC waveform remaining spatially/temporally stable;
- MK-300 route isolation, guitar-vs-backing alignment, listening for pops/dropouts and one representative export smoke.

`RC3_FINAL_PHYSICAL_HOMOLOGATION.md` is the only active manual checklist after the new exact-source automated PASS.