# Implementation Roadmap

Updated: 2026-09-15

## M1 — Foundation — CLOSED
Project model, templates, persistence and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Samsung SM-X230 USB-audio baseline established.

## M3–M4 — Codec/import + Studio foundation — ABSORBED
Consolidated into later milestones.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after physical approval.

## M6 — Measured latency and synchronization — PASS/CLOSED
Closed after physical approval.

## M7 — Production audio polish
Digital scope through H15/H14a is **PASS** at CI #624. H16 is the final polish delta and is PRE-GATE.

## M8 — Release hardening

### Canonical signed baseline — CI #624
Run `35005147318`, exact source `7858dca021a51e0e08835e3fa3f86e6d3b657215`:
- software + performance + Lint + debug/release/provenance: PASS;
- API36 full regression: **22/22 PASS**;
- isolated 1920×1200 geometry: PASS;
- signed homologation: PASS;
- signed APK SHA-256: `82da7c41591c01e304e44e57031ebac6263a2175866ecd1b438d65da0539462b`;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

### H12–H15 + H14a — DIGITAL PASS
H12 global level workflow, H13 Trim ruler, H14/H14a scrollable Mixer with fixed MASTER, and H15 resident same-project return are all digitally homologated by #624.

### H16 — final practice-bar + Trim-overlay polish — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
Practice bar:
- comparison neutral label `Mixer` → `Desativado`;
- top-bar Mixer panel toggle remains `Mixer`;
- `Níveis` moved into a dedicated center **Ajustes** segment;
- docked order `Comparação | Ajustes | Timeline`;
- proportional weights `0.34 / 0.16 / 0.50` for a compact center block and roomy Timeline.

Trim geometry:
- remove the ruler-only 8 dp right shrink that displaced T1/T2 left;
- share exact timeline width with playhead/loop rail;
- compact ruler `46.dp → 20.dp`;
- zero vertical spacing between marker rail and ruler;
- T1/T2 line and precise label overlay the existing marker rail/ruler while Cut is active, with higher visual priority than playhead/loop;
- no dedicated Trim lane and no waveform obstruction.

Regression additions:
- practice-bar test verifies three segments, labels, action placement and proportional widths;
- physical-editing test verifies rail/ruler adjacency, compact height and canonical T1 X projection after drag.

Source part:
`.source-parts/H16FinalUiTrimOverlay.patch.gz`

Decoded patch SHA-256:
`40a4056644707c57291dfd876fde8487d8dbe9c436ba0409ccb7c6a60c31a0bb`

Source validation against exact #624 materialized source: forward/reverse `patch` dry-run, `git apply --check`, application and `git diff --check` all PASS. Android runtime/compilation remains intentionally pending the canonical CI.

## Canonical materialization tail
After H11b:
1. H12 Level Engine
2. H12 Level UI
3. H13 Trim Ruler
4. H14 Mixer Horizontal Scroll
5. H14a Mixer Scroll Viewport Regression
6. H15 Resident Studio Return
7. **H16 Final UI / Trim Overlay**

## Next acceptance gate
The user manually dispatches `GuitarLab Android CI` on the exact final `main` SHA with `signed_homologation=true`.

Promotion rule: H16 becomes DIGITAL PASS only if software/Lint/build/provenance, API36 full regression, tablet geometry and signed homologation all pass on that same source SHA. Until then, #624 remains the last signed digital baseline.

## Gate discipline
The assistant must not dispatch or rerun Actions. `.github/workflows/android-ci.yml` remains manual-only.
