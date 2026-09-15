# Documentation Audit — Physical Review III / H11

Updated: 2026-09-15

## Scope
This audit records the final post-CI-#617 interaction/metering refinement. It does not invalidate the #617 evidence through H10; it defines the exact H11 delta that requires a new exact-source gate.

## Evidence boundary
- CI #617 / source `abc0e2a9f8708dd141735915898b508ce0948f48`: authoritative H0–H10 DIGITAL PASS.
- H11: IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE.
- The #617 signed APK must not be cited as evidence for H11 behavior.

## Requested H11 behavior
1. Mixer dock header becomes a visually segmented practice bar, not a title row with controls inserted into it.
2. Redundant dock title `Mixer`, Pin and X are removed.
3. Top-bar Mixer action becomes the single persistent open/close toggle.
4. Comparação and Timeline occupy equal horizontal segments with restrained near-square button rounding.
5. Tapping waveform/audio content selects the corresponding track.
6. The actively recorded track exposes real-time Peak and RMS during REC.

## Source representation
- `.source-parts/H11MixerWaveformMetering.patch.gz`
- `scripts/materialize_ci_sources.sh` applies H11 after H7–H10.

H11 source-part SHA-256 before publication: `c483c7a08af0f34bdd63a8b22fb11bd33e7ba10f034e2e1d7fde484c9dd56dbc`.
Decoded H11 patch SHA-256: `f956f87e4f4e12850fbf527bce4b0d6b036e1a37c9ce07150b89bee8d7e74f7d`.

## H11 automated contracts added
- recording Peak/RMS projection updates only the recording target and preserves unrelated meter entries;
- two docked practice segments have equal geometry;
- waveform/audio area tap selects the corresponding track;
- Mixer visibility persists across preference-store recreation;
- existing Mixer behavior tests are adapted to removal of Pin/X/title ownership.

## Pre-gate validation
- pure recording-meter policy harness: PASS;
- H11 patch applies cleanly to the exact #617 materialized source;
- H11 patch reverse-match succeeds after application;
- result reproduces the exact expected `app/` tree;
- `git diff --check`: PASS.

A broader historical materializer re-entry limitation exists in old early RC3 guards when the script is rerun against an already fully materialized later snapshot; H11 does not introduce that limitation. H11's own terminal source-part is independently forward/reverse guarded, while the canonical CI path materializes once from repository state. Any future global materializer-idempotence refactor must preserve the currently proven one-pass CI contract.

## Documents synchronized
- `CURRENT_STATE.md`
- `IMPLEMENTATION_ROADMAP.md`
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`
- `RELEASE_NOTES_0.5.0-rc3.md`
- this audit

## Closure criteria
H11 moves to DIGITAL PASS only when one manually dispatched canonical workflow on the final `main` HEAD passes software, Android integration/geometry and signed homologation with matching package/version/source/signer/checksum. After that, only real-device visual ergonomics, persistent toggle feel, live MK-300 metering plausibility and the retained physical smoke remain.
