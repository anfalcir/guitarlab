# Implementation Roadmap

Updated: 2026-09-16

## Milestones
- **M1 — Foundation:** CLOSED.
- **M2 — Android hardware/audio baseline:** PASS/CLOSED.
- **M3–M4:** absorbed into later milestones.
- **M5 — Reliable recording + Studio + Media I/O:** PASS/CLOSED.
- **M6 — Measured latency/synchronization:** PASS/CLOSED.
- **M7 — Production audio polish:** digital evidence retained; residual physical closure active.
- **M8 — Release hardening:** H24a DIGITAL PASS; H25 PRE-GATE.

## Last signed authority
CI #641 / run `35105065689` / producer `b11769f340f7056c37dfb17d95b062909dad87bf` is the signed DIGITAL PASS through H24a: 269/269 JVM/unit, Lint/build/provenance PASS, API36 **25/25 standard + 1/1 isolated geometry**, signing PASS.

## H25 — UI interaction / settings / destructive-action safety
Status: **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**.

Acceptance goals:
1. icon-button pointer/ripple feedback matches the visible rounded-square chassis;
2. calibration details/controls live in one dedicated modal;
3. main Options page remains concise while exposing calibration status/action;
4. diagnostics are not duplicated and are grouped by domain;
5. project deletion requires explicit confirmation and cannot call delete directly from the overflow menu;
6. Help reflects the current behavior;
7. Android tests cover modal visibility and both cancel/confirm delete paths;
8. materialization remains deterministic, idempotent and fail-closed.

## Canonical materialization tail
`… → H23b → H24 → H24a → H25`.

## Remaining release path
1. Land H25 source/materializer/docs with `[skip ci]` and no automatic Actions.
2. User manually dispatches one full signed workflow on the final H25 `main` SHA.
3. Audit exact unit count, Lint/build/provenance, standard API36 count, isolated geometry, signing and artifact identity.
4. If green, promote H25 to DIGITAL PASS in docs without changing producer identity.
5. Install only that H25 signed candidate on SM-X230.
6. Physically smoke H25 hover/press geometry, calibration modal/diagnostics organization and delete confirmation.
7. Complete retained H24 Home smoke and H23b MK-300 recording-timing checks.
8. Final RC3 approval requires no repeatable P0/P1 and explicit approval of the exact signed candidate.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions.
