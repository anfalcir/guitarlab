# Documentation Audit — Physical Review II H7–H10

Updated: 2026-09-14

## Scope
This audit is the current interpretation boundary for the post-CI-#616 physical findings, H7–H10 implementation, and CI #617 evidence.

## Evidence boundary
- CI #616 / source `3051619c219e346daca00d2242f60ef03f2d80db`: authoritative H0–H6 digital PASS.
- CI #617 / source `abc0e2a9f8708dd141735915898b508ce0948f48`: authoritative H7–H10 and current H0–H10 digital PASS.
- Signed #617 APK SHA-256: `7f0c303ccc447c5455dfbd49e1bc022af482927582254eb826c9b29f3a84c6b6`.
- Unsigned #617 APK SHA-256: `2aacc5c027bfd668fb85499bf8992c90b933dbe04d4abcdbfb732db60a77dd78`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## Canonical documents reviewed/updated
- `CURRENT_STATE.md` — #617 becomes the active digital evidence.
- `IMPLEMENTATION_ROADMAP.md` — H7–H10 promoted from PRE-GATE to DIGITAL PASS.
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — exact #617 source/APK/signer identity locked for the residual pass.
- `RELEASE_NOTES_0.5.0-rc3.md` — #617 validation evidence recorded.
- `TEST_AND_HOMOLOGATION_PLAN.md` — existing automation/manual separation remains binding.
- `STUDIO_WORKSPACE_GUIDELINES.md` — H7–H10 UX contracts remain binding.
- `DECISIONS.md` — D-071…D-075 remain current.
- `CI_PIPELINE.md` — H1–H10 materialization/manual-only gate contract remains current.
- `DOCUMENTATION_MAP.md` — this audit remains the current H7–H10 audit.

## Source/materialization audit
Physical Review II is represented by:
- `.source-parts/H7StateLevelTransport.patch.gz`
- `.source-parts/H8WorkspaceFlow.patch.gz`
- `.source-parts/H9LiveWaveformStability.patch.gz`
- `.source-parts/H10StateRaceGuide.patch.gz`
- `scripts/materialize_ci_sources.sh` final-state guard for H7–H10.

Expected order remains H7 → H8 → H9 → H10. CI #617 proved materialization of this chain from exact repository SHA `abc0e2a9f8708dd141735915898b508ce0948f48` in both software and API36 jobs.

## Physical findings disposition
- missed first `Cortar` tap → H8 — digitally covered;
- repeated level correction after apply → H7 — digitally covered;
- `Excluir clipe` vs track-clear scope ambiguity → H8 — digitally covered;
- Undo/Redo/transport stale state after repeated edits → H7/H10 — digitally covered;
- Comparison/Timeline vertical-space optimization → H8 — digitally covered;
- Stop successfully finishing REC → H7 — digitally covered;
- sparse-history/dense-recent live waveform → H9 — digitally covered;
- project-switch async race → H10 — digitally covered.

## CI #617 closure evidence
Run #617 / ID `34918430241` passed:
1. Unit tests + Lint + APK build;
2. API 36 emulator regression;
3. Signed homologation APK;
4. exact source/package/version provenance;
5. signer/certificate verification;
6. artifact upload and checksum publication.

The downloaded signed APK checksum was recomputed independently and matched `SHA256SUMS.txt`. APK signing verification reported v2=true, one signer, RSA 4096 and the locked certificate fingerprint.

## Remaining closure criteria
Documentation is internally consistent when:
1. H0–H10 are marked DIGITAL PASS for CI #617;
2. the exact #617 application/source SHA and APK checksum are the active physical candidate;
3. later documentation-only evidence commits are explicitly distinguished from the locked application source identity;
4. only target-device/ergonomic facts remain open;
5. final M7/M8 closure requires zero repeatable P0/P1 and explicit approval of the exact #617 APK.

Older audits/checklists remain historical evidence and must not override the documents above.
