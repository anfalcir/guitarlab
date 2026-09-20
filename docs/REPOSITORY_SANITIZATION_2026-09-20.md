# Repository Sanitization Audit — 2026-09-20

## Goal
Reduce the repository to the canonical `main` branch without losing unique product work.

## Canonical branch
- repository default branch: `main`;
- audited main HEAD before this docs-only sanitation commit: `3a0ec59556e8aea54e1f02292ae97937ac5d06d8`;
- exact current signed producer: CI #659 / run `35512894518` / `a6a53e8ba9e75b32565e451870758c7c65ad687f`;
- signed APK SHA-256: `e7ddce638a83d92af0ef2a01e46a10152f8c0eb4c535f0ddbc8671d9b94a1295`;
- open pull requests: **none**.

## Branch ancestry audit
The following branches are fully contained in `main` (`ahead_by = 0`) and contain no commits that would be lost by deleting the branch reference:

| Branch | Behind main |
|---|---:|
| `codex/recovery-rc3-20260913` | 279 |
| `dev/parallel-m3-m5` | 288 |
| `docs/post-h28-hardening-roadmap` | 9 |
| `h23b-staging` | 47 |
| `h23b-staging-2` | 47 |
| `h23b-staging-3` | 47 |
| `scratch-h27-inspect` | 22 |
| `scratch-h27-tree` | 22 |
| `scratch-h27-tree2` | 22 |
| `scratch-h27-tree3` | 22 |
| `scratch-h27-tree4` | 22 |
| `scratch-h27-tree5` | 22 |
| `staging/fix-622-materializer` | 80 |
| `staging/fix-623-h14a` | 73 |
| `work/h11-mixer-waveform-metering` | 115 |
| `work/h12-h15-final-polish` | 89 |
| `work/physical-review-ii` | 137 |

## Diverged branch audit — work/physical-review-iv
This branch is 10 commits ahead of its merge base and 110 commits behind `main`. The 10 exclusive commits were inspected individually.

They consist only of the old Physical Review IV H12-H16 packaging: H12 batch level analysis, H13 Trim timeline-ruler, H14 Mixer overflow, H15 resident Studio re-entry, H16 integrated polish, and their materializer/documentation PRE-GATE commits.

Current `main` already carries the canonical/superseding implementation through:
- `H12LevelEngine.patch` + `H12LevelUi.patch`;
- `H13TrimRuler.patch`;
- `H14MixerHorizontalScroll.patch` + `H14aMixerScrollViewportRegression.patch`;
- `H15ResidentStudioReturn.patch`;
- `H16FinalUiTrimOverlay.patch.gz`;
- all subsequent H17-H35a work.

The exact-source CI line has since advanced through #659. Current materialized source still contains the relevant behaviors/tests: project-wide `Níveis das pistas`, fixed Trim ruler T1/T2 semantics, horizontally scrollable Mixer tracks with anchored MASTER, resident Studio-return hardening and later integrated regressions.

Conclusion: the 10 commits are **superseded historical packaging, not unique product functionality**. Deleting `work/physical-review-iv` does not lose a current requirement or implementation.

## Cleanup conclusion
All 18 non-main branches are safe to delete after this audit. Retain only `main`.

Branch deletion removes branch references only; the canonical main history, current source-parts, CI evidence and signed candidate identity remain intact.