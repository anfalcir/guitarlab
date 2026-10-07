# RC32 code + documentation audit

Updated: 2026-10-07
Status: **COMPLETE**

## Evidence reviewed

The audit used the owner-provided RC31 evidence:

- `GuitarLab-Diagnostics-1791369836868.zip`;
- `Screen_Recording_20261007_072018.mp4`;
- final RC32 source through producer `8360b695b6df8b56bc480479d07d1eee6f1c6add`;
- Android CI #971 predecessor PASS and final Android CI #972 PASS.

The diagnostic bundle contains 13 completed CUE preflights: 6 `SUPPORTED`, 7 `OFFSET_EXCEEDED`. All captured successes use `communicationModeRequired=false`; all captured offset failures use `communicationModeRequired=true`. The final bundle snapshot proves MAIN MK-300 at 44.1 kHz and wired CUE at 48 kHz with adaptive CUE resampling, exact distinct effective routes, `duckingRequested=false` and `strategy=COMMUNICATION_SPLIT`.

The video independently shows:
- a user-visible false-negative path reporting that CUE could not be confirmed;
- a second false-negative path treating transient non-blocking CUE backpressure as fatal;
- real musical playback successfully split between MK-300 MAIN and wired CUE before a later runtime guard disabled CUE;
- development-facing success/failure copy that should not appear in normal product UI.

## Finding → correction matrix

| Finding | RC32 correction | Final state |
| --- | --- | --- |
| Dual-MEDIA probe is known to converge on this Samsung and can perturb policy before the working path | Communication-capable CUE is attempted through `COMMUNICATION_SPLIT` first | Implemented |
| Successful owner runs never required global communication mode; captured offset failures did | Automatic `MODE_IN_COMMUNICATION` escalation removed | Implemented |
| 12 ms mixed physical-route safety with fixed MEDIA↔COMMUNICATION pipeline latency | 12 ms retained for conventional multi-device; Communication Split gets bounded 60 ms startup policy + measured `initialOffsetNs` | Implemented + unit-tested |
| One partial/zero `WRITE_NON_BLOCKING` result killed CUE | Bounded FIFO retains partial/zero writes in order without blocking MAIN; 180 ms maximum backlog | Implemented + unit-tested |
| Runtime drift guard suppressed working CUE after only a few instantaneous checks | Communication drift envelope = 60 ms and must persist 750 ms | Implemented + unit-tested policy |
| Runtime repeated expensive/perturbing silent proof after selection-time admission | Communication runtime reuses the proven profile and performs fresh physical route qualification rather than the complete selection-time clock preflight | Implemented |
| Failure text was development/homologation oriented | Product-facing success/failure copy | Implemented |
| A later generic routing event could overwrite the specific reason | Final producer preserves the specific CUE suppression reason | Implemented and requalified in CI #972 |
| RC31 bundle could not explain runtime suppression after the fact | Diagnostic schema v4 adds `initialOffsetNs` and `lastCueRuntime` with backlog/drift/route detail | Implemented |
| Zero ducking must remain invariant | No CUE duck-focus request, no automatic MAIN gain manipulation, no system-volume balancing | Preserved |
| Removing MULTI_DEVICE entirely could regress other Android devices | Conventional multi-device path remains a compatibility fallback | Preserved |

## Safety review

The stabilization does **not** make route validation permissive. CUE still fails closed for missing, converged, wrong or mirrored physical routes. MAIN remains timing authority and CUE never blocks MAIN. Negative writes, bounded-backlog overflow, physical route loss and sustained drift remain suppression conditions. The wider Communication Split offset/drift windows are quality/stability bounds, not route-identity relaxations.

## Documentation cleanup

The live-document rule is restored: root `docs/` describes current contracts; candidate chronology stays under `docs/history/`.

Updated live authority:
- `PROJECT_IDENTITY.md` — removed RC22-as-current wording;
- `CURRENT_STATE.md` — reduced to RC32 current state, exact signed identity, residual physical gate and future USB work;
- `ARCHITECTURE.md` — current Communication Split/MULTI_DEVICE strategy, bounded FIFO and strategy-specific timing;
- `PRODUCT_REQUIREMENTS.md` — current CUE requirements and zero-duck invariant;
- `STUDIO_OPTIONS_AND_MIXER.md` — current CUE contract; removed embedded RC25/RC27/RC28 chronology;
- `TEST_AND_HOMOLOGATION_POLICY.md` — current CUE digital/physical gates; removed RC27/RC28-as-current sections;
- `CI_PIPELINE.md` — current execution controls only; removed RC23–RC29 chronology and stale materializer tail;
- `UI_VISUAL_SYSTEM.md` — retained current visual invariants and removed RC24/26/27 chronology;
- `DOCUMENTATION_MAP.md` and `history/README.md` — reclassified CUE plans/evidence;
- repository `README.md` — removed RC28/RC29/current-tail claims and restored authority links.

No historical evidence file was rewritten merely to make it look current. Superseded CUE plans and prior RC evidence already reside under `docs/history/`, so no duplicate archive copies were created. The USB multichannel plan remains intentionally open future work.

## Qualification result

Final runtime producer CI #972 is fully green and signed. The subsequent documentation-only closure does not change APK bytes or invalidate the signed producer. Physical RC32 owner requalification remains the only open gate for promoting this Android successor.
