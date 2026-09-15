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

## M7 — Production audio polish — DIGITAL BASELINE PASS / FINAL H11 GATE
The active RC includes managed media, SRC/editing domain, fades/crossfades, transactional recording/recovery, fail-closed input, monitoring isolation, takes, practice controls, level analysis, live REC waveform and project/master export.

CI #617 digitally validated the product through H10. H11 changes final Studio interaction/metering behavior and therefore requires one new exact-source digital gate before final physical closure.

## M8 — Release hardening
### H0–H6 — DIGITAL PASS in CI #616
- H0 reproduction/invariants;
- H1 independent trim handles;
- H2 split/take lineage and explicit clip deletion;
- H3 transactional drag/drop and drag-to-trash;
- H4 measured per-session recording startup skew;
- H5 frame-span live waveform;
- H6 integrated regression/materialization/guide synchronization.

### H7–H10 — DIGITAL PASS in CI #617
- **H7:** state/history resynchronization, level-analysis convergence and unified successful recording Stop;
- **H8:** first-valid-tap Trim, clearer destructive scope and first Mixer/practice-controls consolidation;
- **H9:** uniform temporal bucketing for long live REC waveform stability;
- **H10:** project-switch race closure, integrated regression and guide sync.

CI #617 / run ID `34918430241` / source `abc0e2a9f8708dd141735915898b508ce0948f48` passed software, Lint/build, API 36 full instrumentation, isolated 1920×1200 geometry and signed homologation. Signed APK SHA-256: `7f0c303ccc447c5455dfbd49e1bc022af482927582254eb826c9b29f3a84c6b6`.

### H11 — Mixer/waveform/metering refinement — IMPLEMENTED / PRE-GATE
H11 is deliberately one bounded final interaction delta:
- **Mixer bar:** remove redundant title/Pin/X; top-bar Mixer button becomes the only persistent open/close toggle; Mixer header is two equal segments, Comparação and Timeline, with restrained near-square button rounding.
- **Waveform selection:** tapping waveform/audio lane, clip or active live waveform selects the corresponding track through the canonical selected-track state.
- **REC metering:** live capture Peak/RMS is projected onto the recording target's existing track meter and shown both in Mixer PK/RMS and a compact live-waveform overlay.
- **Persistence:** Mixer visibility is stored as `mixer_visible`, with backward migration from former `mixer_pinned`.
- **Regression:** equal-segment geometry, waveform selection, visibility persistence and recording-meter policy receive explicit tests.

Source representation:
- `.source-parts/H11MixerWaveformMetering.patch.gz`;
- materialized after H7–H10 by `scripts/materialize_ci_sources.sh`.

Pre-gate evidence:
- pure recording-meter policy harness PASS;
- exact #617 materialized-source patch reproduction PASS;
- forward/reverse H11 patch validation PASS;
- `git diff --check` PASS.

## Candidate progression
- `0.4.0-rc2` / code 20 — CI #593 PASS.
- `0.5.0-rc1` / code 21 — recording/practice hardening.
- `0.5.0-rc2` / code 22 — timeline/practice refinement.
- `0.5.0-rc3` / code 23 — active release line.
- CI #616 — H0–H6 digital PASS.
- CI #617 — H0–H10 digital PASS.
- H11 — current source delta, next exact-source gate pending.

## Next acceptance gate
The next manually dispatched workflow must prove, for one identical `github.sha`:
1. JVM/unit regression including H11 metering policy;
2. Android Lint;
3. debug/release assembly;
4. API 36 full connected regression including H11 UI interactions;
5. isolated 1920×1200 geometry;
6. signed homologation release;
7. locked signer certificate;
8. package/version/source provenance and APK SHA-256.

After that PASS, only the focused Samsung SM-X230 + M-VAVE MK-300 checklist in `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` remains.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only. Signing cannot bypass software or Android integration gates. No commit in this H11 preparation may auto-dispatch Actions. The #617 APK remains historical evidence through H10, not evidence for H11.
