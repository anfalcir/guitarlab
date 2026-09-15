# Implementation Roadmap

Updated: 2026-09-14

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

## M7 — Production audio polish — DIGITAL PASS / FINAL PHYSICAL GATE
Managed media, SRC/editing domain, fades/crossfades, transactional recording/recovery, live recording waveform, fail-closed input, monitoring isolation, takes, practice controls, level analysis and project/master export are implemented. Final closure remains tied to the latest active RC after all subsequent hardening.

## M8 — Release hardening
### H0–H6 — DIGITAL PASS in CI #616
- H0 reproduction/invariants;
- H1 independent trim handles;
- H2 split/take lineage and explicit clip deletion;
- H3 transactional drag/drop and drag-to-trash;
- H4 measured per-session recording startup skew;
- H5 frame-span live waveform;
- H6 integrated regression/materialization/guide synchronization.

CI #616 / run `34912716297` at source `3051619c219e346daca00d2242f60ef03f2d80db` passed software, API 36 full instrumentation, isolated 1920×1200 geometry and signed homologation. Signed APK SHA-256: `92e806c6fbfd68b0fd44409570c17a976b922e56f2d206824a308c1fdc15bf9c`.

### Physical Review II — H7–H10 — IMPLEMENTED / PRE-GATE
Physical use of the #616 APK exposed a second hardening delta. The findings were handled as one serial program instead of isolated button fixes.

- **H7 — state/history + level + recording stop:** central resynchronization of derived history/playback/mixer readiness, project/session guards for asynchronous callbacks, effective-level analysis that converges after apply, and one idempotent successful stop path shared by Stop and REC.
- **H8 — workspace flow:** first-valid-tap Trim entry with explicit blocking feedback, clearer clip-vs-track destructive action scope, and Comparison/Timeline integrated into the Mixer header through one reusable component.
- **H9 — live waveform stability:** replaces uneven pairwise historical compaction with uniform temporal bucketing and interval-width rendering so past/future waveform density remains spatially coherent during long capture.
- **H10 — race closure + integrated regression + guide:** closes project-switch races, adds stress/integration coverage and synchronizes the novice-facing guide.

H7–H10 are represented by guarded source patches and are source/materialization validated. They are **not yet digitally homologated**.

## Candidate progression
- `0.4.0-rc2` / code 20 — CI #593 PASS.
- `0.5.0-rc1` / code 21 — recording/practice hardening.
- `0.5.0-rc2` / code 22 — timeline/practice refinement.
- `0.5.0-rc3` / code 23 — active release line.
- CI #613 — earlier signed RC3 PASS.
- CI #614 — test API compatibility failure; signing correctly blocked.
- CI #615 — full pre-H0–H6 PASS.
- CI #616 — authoritative H0–H6 digital PASS.
- Current source after H7–H10: next exact-source gate pending.

## Next acceptance gate
The next manually dispatched workflow must prove, for one identical `github.sha`:
1. JVM/unit regression including H7–H10 state/level/waveform contracts;
2. Android Lint;
3. debug/release assembly;
4. Android test compilation;
5. API 36 full connected regression;
6. isolated 1920×1200 geometry and interaction coverage;
7. signed homologation release;
8. signer certificate equals the locked GuitarLab identity;
9. package/version/source provenance and APK SHA-256 published.

After that PASS, only the focused real Samsung SM-X230 + M-VAVE MK-300 checklist in `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` remains.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only. Signing cannot bypass software or Android integration gates. `scripts/materialize_ci_sources.sh` applies versioned RC3/H1–H10 deltas and fails on patch drift. Documentation distinguishes source review, automated evidence and target-hardware evidence.