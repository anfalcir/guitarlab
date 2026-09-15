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
Managed media, SRC/editing domain, fades/crossfades, transactional recording/recovery, fail-closed input, monitoring isolation, takes, practice controls, level analysis, live REC waveform and project/master export are implemented and digitally validated for the active RC3 candidate.

Final M7 closure requires only the focused Samsung SM-X230 + M-VAVE MK-300 physical checklist and explicit approval.

## M8 — Release hardening — H0–H10 DIGITAL PASS / PHYSICAL CLOSURE PENDING
### H0–H6
- H0 reproduction/invariants;
- H1 independent trim handles;
- H2 split/take lineage and explicit clip deletion;
- H3 transactional drag/drop and drag-to-trash;
- H4 measured per-session recording startup skew;
- H5 frame-span live waveform;
- H6 integrated regression/materialization/guide synchronization.

CI #616 / run `34912716297` at source `3051619c219e346daca00d2242f60ef03f2d80db` remains the authoritative historical H0–H6 evidence. The current candidate is newer.

### Physical Review II — H7–H10 — DIGITAL PASS in CI #617
Physical use of the #616 APK exposed a second hardening delta. It was implemented as one serial program:
- **H7 — state/history + level + recording stop:** derived history/playback/mixer state resynchronization, project/session guards, effective-level convergence and one successful Stop/REC finalization path.
- **H8 — workspace flow:** first-valid-tap Trim entry with explicit blocking feedback, clearer clip-vs-track destructive scope and Comparison/Timeline integrated into the Mixer header through one reusable component.
- **H9 — live waveform stability:** uniform temporal bucketing and interval-width rendering replace uneven historical pairwise compaction.
- **H10 — race closure + integrated regression + guide:** project-switch races closed, stress/integration coverage added and in-app guide synchronized.

CI #617 / run ID `34918430241` / source `abc0e2a9f8708dd141735915898b508ce0948f48` passed:
- software gate;
- H7–H10 JVM/unit regression;
- performance evidence;
- Android Lint;
- debug/release assembly;
- API 36 full instrumentation;
- isolated 1920×1200 geometry;
- signed homologation;
- exact package/version/source provenance;
- locked signer verification.

Canonical #617 identity:
- unsigned APK SHA-256 `2aacc5c027bfd668fb85499bf8992c90b933dbe04d4abcdbfb732db60a77dd78`;
- signed APK SHA-256 `7f0c303ccc447c5455dfbd49e1bc022af482927582254eb826c9b29f3a84c6b6`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact ID `10377695262`.

## Candidate progression
- `0.4.0-rc2` / code 20 — CI #593 PASS.
- `0.5.0-rc1` / code 21 — recording/practice hardening.
- `0.5.0-rc2` / code 22 — timeline/practice refinement.
- `0.5.0-rc3` / code 23 — active release line.
- CI #613 — earlier signed RC3 PASS.
- CI #614 — test API compatibility failure; signing correctly blocked.
- CI #615 — full pre-H0–H6 PASS.
- CI #616 — H0–H6 digital PASS.
- **CI #617 — authoritative H0–H10 digital PASS and current physical-homologation candidate.**

## Remaining closure gate
Only the focused real Samsung SM-X230 + M-VAVE MK-300 checklist in `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` remains. M7/M8 close after zero repeatable P0/P1 and explicit approval of the exact #617 APK.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only. Signing cannot bypass software or Android integration gates. `scripts/materialize_ci_sources.sh` applies versioned RC3/H1–H10 deltas and fails on patch drift. Documentation-only evidence commits after CI #617 do not change the locked application source/binary identity above.
