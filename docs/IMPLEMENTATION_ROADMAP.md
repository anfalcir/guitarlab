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
Closed after explicit physical approval of `0.3.0-alpha1`. The RC3 H4 work hardens per-session startup synchronization without reopening M6.

## M7 — Production audio polish — DIGITAL PASS / FINAL PHYSICAL GATE
Implemented scope includes managed immutable media, validated SRC/editing domain, fades/crossfades, transactional recording/recovery, live recording waveform, fail-closed selected input, monitoring isolation, take management, practice sections/markers/comparison, transient loop punch, level analysis and project/master export.

The exact active RC has now passed the canonical automated gate. M7 remains open only for the residual Samsung SM-X230 + M-VAVE MK-300 physical checklist and explicit approval.

## M8 — Release hardening — H0–H6 DIGITAL PASS / PHYSICAL CLOSURE PENDING
### M8.A — Compatibility + edge cases
Covered by deterministic regression for legacy/current migration, malformed/truncated packages, duplication/media ownership, editing-domain/fade/frame validation, SRC/master render, interrupted import/recording recovery and safe publication/cancellation.

### M8.B — UX + accessibility + performance
Covered by Compose accessibility/callback checks, lifecycle recreation, target geometry instrumentation, structural/render performance evidence and large/fuzz project regression.

### M8.C — RC3 physical-review hardening
H0–H6 were implemented serially and are now digitally homologated in CI #616:
- **H0:** reproduction/invariants for trim, split-lineage move/delete, timing and long live waveform.
- **H1:** independent start/end trim handles with explicit touch targets/semantics and deterministic frame mapping.
- **H2:** safe split/take lineage, deterministic canonical sibling promotion and explicit confirmed clip deletion.
- **H3:** `Move` / `Delete` / `NoOp` drag intent plus drag-to-trash through the same domain delete path.
- **H4:** per-session capture/backing startup skew separated from route calibration/punch offsets; no fixed `-0.5 s` compensation.
- **H5:** frame-span live waveform with bounded compaction and conflated/bounded UI publication.
- **H6:** integrated persistence/instrumentation/timing/waveform/guide/materialization regression.

### M8.D — Candidate progression and evidence
- `0.4.0-rc2` / versionCode 20 — CI #593: signed/API36/geometry PASS.
- `0.5.0-rc1` / versionCode 21 — recording/practice hardening.
- `0.5.0-rc2` / versionCode 22 — timeline/practice visual refinement.
- `0.5.0-rc3` / versionCode 23 — active candidate.
- CI #613 — earlier RC3 signed baseline PASS.
- CI #614 — blocked by an incompatible Compose test assertion; signing correctly blocked.
- CI #615 / `74bf86efbec94d249c4968c3284bf1985cd66b44` — full signed baseline PASS before H0–H6.
- **CI #616 / run ID `34912716297` / source `3051619c219e346daca00d2242f60ef03f2d80db` — authoritative H0–H6 digital PASS.**
  - software gate PASS;
  - API 36 full instrumentation PASS;
  - isolated 1920×1200 geometry PASS;
  - signed homologation PASS;
  - package/version/provenance PASS;
  - official signer PASS;
  - signed APK SHA-256 `92e806c6fbfd68b0fd44409570c17a976b922e56f2d206824a308c1fdc15bf9c`.

## Remaining closure gate
`RC3_FINAL_PHYSICAL_HOMOLOGATION.md` is now active for the exact CI #616 APK. The residual pass intentionally concentrates only on target-only evidence:
1. trim-handle ergonomics;
2. split/move/delete/drag-to-trash workflow;
3. real MK-300 routing and recording isolation;
4. guitar-vs-backing synchronization;
5. multi-minute live-waveform stability;
6. concise listening/transport/export/stress smoke.

M7/M8 close only after zero repeatable P0/P1 and explicit user approval of this exact signed APK.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only. The signed homologation job depends on both software and Android integration gates. `scripts/materialize_ci_sources.sh` applies versioned RC3/H1–H6 deltas serially and fails on patch drift rather than silently producing partial source. Documentation-only evidence commits after CI #616 do not change the locked application binary/source identity above.

`CANDIDATE_IDENTITY_POLICY.md` is authoritative for identity rules; `CURRENT_STATE.md` and `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` record the active #616 evidence.
