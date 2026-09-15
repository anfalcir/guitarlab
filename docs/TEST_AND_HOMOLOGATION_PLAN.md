# Test and Homologation Plan

Updated: 2026-09-14

## Active candidate
Active line: `0.5.0-rc3` / versionCode `23` / package `studio.guitarlab.app`.

CI #616 at `3051619c219e346daca00d2242f60ef03f2d80db` is the latest fully green signed H0–H6 baseline. Physical Review II H7–H10 is newer and therefore requires one new exact-source manual workflow before promotion.

## Automated coverage
### Retained H0–H6
Transport/loop, Auto seções, countdown, trim handles, split/take lineage, clip deletion/drag-to-trash, recording timing compensation, live waveform frame accounting, persistence, lifecycle, accessibility, geometry and release provenance remain mandatory.

### H7 — state/history/level/recording stop
Required coverage:
- analyze → apply → analyze converges using effective level including current gains;
- repeated apply/undo/redo keeps `canUndo`/`canRedo` synchronized;
- project switch during pending history/analysis work cannot publish stale results into the new project;
- Stop during countdown cancels safely;
- Stop during capture and REC-during-capture use one successful idempotent finalization path;
- duplicate finalization is rejected.

### H8 — workspace/action flow
Required coverage:
- one valid tap on `Cortar` opens Trim;
- blocked trim entry produces explicit feedback, never a silent no-op;
- clip deletion and track-wide clearing have distinct scope/labels;
- one reusable Comparison/Timeline component is used in Mixer header vs closed-Mixer workspace state;
- wide tablet geometry preserves title left and Pin/Close right without overlap.

### H9 — live waveform spatial stability
Required coverage:
- captured frames remain the timebase;
- one uniform temporal bucket resolution applies to both historical and newly captured data;
- rebucketing preserves contiguous covered time and maximum transient;
- renderer uses represented interval width, not a single thin midpoint stroke;
- long/variable callback cadence remains monotonic, bounded and spatially coherent.

### H10 — integrated regression
Required scenario includes repeated level analysis/application, Undo/Redo stress, project switching, Trim entry, REC→Stop, long waveform capture and save/reopen without stuck transport/history state.

## Source materialization
Canonical order remains serial:
`H1 → H2 → H3 → H4 → H5 → H6 → H7 → H8 → H9 → H10`.

H7–H10 are treated as one guarded final-state materialization unit because H10 refines files already touched by H7. Final target hashes recognize an already materialized source and prevent reverse application through later edits.

Requirements: deterministic, idempotence-oriented, fail-fast on drift, identical output for software and API 36 gates.

## Canonical manual gate
The user manually dispatches `.github/workflows/android-ci.yml` on final `main` with `signed_homologation=true`.

Mandatory:
1. diff sanity + materialization;
2. all JVM/unit tests including H7–H10;
3. performance evidence;
4. Android Lint;
5. debug/release assembly;
6. API 36 full connected regression;
7. isolated 1920×1200 tablet geometry;
8. signed homologation from the exact tested unsigned artifact;
9. source/package/version/checksum/signer verification.

Any failure blocks promotion. No green run from another SHA counts.

## Residual physical checks
After the exact H7–H10 automated PASS, only target-only evidence remains: first-tap Trim/tactile handle behavior, level convergence/history responsiveness, Stop-vs-REC recording flow, Mixer-header ergonomics, multi-minute waveform visual stability, real MK-300 routing/isolation/sync, listening and one export smoke.

Use `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` only.

## Closure
M7/M8 close only after zero repeatable P0/P1, exact-source automated PASS, verified signed identity, residual target-device PASS and explicit user approval.