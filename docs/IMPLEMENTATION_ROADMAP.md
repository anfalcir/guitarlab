# Implementation Roadmap

Updated: 2026-09-16

## Milestones
- **M1 — Foundation:** CLOSED.
- **M2 — Android hardware/audio baseline:** PASS/CLOSED.
- **M3–M4:** absorbed into later milestones.
- **M5 — Reliable recording + Studio + Media I/O:** PASS/CLOSED.
- **M6 — Measured latency/synchronization:** PASS/CLOSED.
- **M7 — Production audio polish:** digital scope PASS; residual physical closure active.
- **M8 — Release hardening:** digital scope PASS through H25; residual physical closure active.

## Current signed authority
**CI #642** / run `35121955150` / exact producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632` is the signed DIGITAL PASS through H25.

Evidence:
- **269/269** JVM/unit PASS, 0 failures/errors/skips;
- Android Lint, debug/release build and unsigned provenance PASS;
- API36 **28/28 standard + 1/1 isolated geometry**;
- signed homologation PASS;
- unsigned APK SHA-256 `f6b4f21d0f514bad06b80eabdad141dac5cd236a707842c21258ec2072868c0e`;
- signed APK SHA-256 `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Report Android regression canonically as **28/28 standard + 1/1 isolated geometry**.

## H25 — DIGITAL PASS
Acceptance goals are digitally proven at #642:
1. rounded-square pointer/ripple feedback matches the visible button chassis;
2. calibration details/controls live in one dedicated modal;
3. main Options page remains concise with calibration summary/action;
4. diagnostics are grouped without duplicate entry points;
5. project deletion requires explicit confirmation before repository deletion;
6. Help reflects current behavior;
7. Android tests cover calibration-modal behavior plus delete Cancel/Confirm paths;
8. source materialization remains deterministic, idempotent and fail-closed.

## Canonical materialization tail
`… → H23b → H24 → H24a → H25`.

## Remaining RC3 release path
1. Install the exact CI #642 signed candidate on SM-X230.
2. Physically smoke H25 button feedback, Settings/calibration modal, diagnostics organization and delete confirmation.
3. Run retained H24 Home-library smoke.
4. Confirm retained H22 semantic route UX after MK-300 reconnect.
5. Complete H23b zero-adjustment recording-timing validation on MK-300, including repeated 44.1 kHz takes, non-zero playhead and loop/punch.
6. Use route/rate calibration only if a repeatable residual remains.
7. Run retained backing-isolation/live-waveform/input-fail-closed/edit-save-reopen/export smoke.
8. Final RC3 approval requires no repeatable P0/P1 and explicit approval of signed APK SHA `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`.

No further deterministic CI rerun is required unless source changes.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions without explicit user instruction.
