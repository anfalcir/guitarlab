# Current State — GuitarLab Studio

Updated: 2026-10-07

## Status

**RC32 COMMUNICATION SPLIT STABILIZATION — DIGITAL PASS / SIGNED; PHYSICAL REQUALIFICATION PENDING**

The current successor candidate is `0.5.0-rc32` / versionCode `52`, produced from commit `8360b695b6df8b56bc480479d07d1eee6f1c6add`. Android CI **#972 / run `37612951851`** passed Unit tests, Android Lint, debug/release assembly, API 36 instrumented regression, exact unsigned provenance, exact-artifact signing and package/certificate verification.

RC32 is not yet the accepted baseline. `RELEASE_BASELINE.md` remains authoritative for the physically accepted RC20 release until owner acceptance of the exact signed RC32 APK.

## Why RC32 exists

RC31 physical evidence changed the diagnosis. The owner-device bundle `GuitarLab-Diagnostics-1791369836868.zip` contains 13 completed CUE preflights: 6 `SUPPORTED` and 7 `OFFSET_EXCEEDED`. All six captured successes use `COMMUNICATION_SPLIT` with `communicationModeRequired=false`, MAIN on MK-300 and CUE on the wired jack. All seven captured offset failures use `communicationModeRequired=true` / `MODE_IN_COMMUNICATION`.

The companion screen recording proves two additional runtime false negatives: a transient non-blocking CUE write was treated as fatal, and a later route/runtime qualification path could replace a specific CUE failure with a generic message. The owner also heard real project audio correctly split between MK-300 MAIN and wired CUE for several seconds before the old drift/backpressure guard disabled CUE.

This is positive physical evidence that the route can exist. RC32 therefore stabilizes validation/runtime policy without weakening physical-route isolation.

## RC32 behavior

- Communication-capable CUE is attempted through `COMMUNICATION_SPLIT` before conventional dual-MEDIA routing.
- `MULTI_DEVICE` remains a compatibility fallback; it was not removed globally.
- Automatic `MODE_IN_COMMUNICATION` escalation is removed.
- Exact canonical MAIN/CUE physical-route proof remains fail-closed.
- Conventional `MULTI_DEVICE` retains the 12 ms startup-origin limit.
- `COMMUNICATION_SPLIT` uses a bounded 60 ms startup static-offset limit and exports the measured `initialOffsetNs`.
- Communication runtime drift uses a 60 ms envelope and must remain outside it for 750 ms before suppression.
- CUE remains non-blocking relative to MAIN. Partial/zero `WRITE_NON_BLOCKING` results are buffered in-order by a bounded FIFO; the configured maximum backlog is 180 ms. Negative writes, backlog overflow, route loss or sustained drift still disable CUE.
- Runtime Communication Split does not repeat the complete selection-time silent clock preflight on the same new tracks. Real playback still performs fresh physical route qualification and continuous route/drift supervision.
- Diagnostics schema v4 exports `initialOffsetNs` and `lastCueRuntime`, including backlog, drift and route evidence for runtime suppression.
- User-facing success/failure text is product-facing. The final producer also preserves the specific CUE suppression reason instead of overwriting it with a generic second routing event.
- Zero ducking remains mandatory. GuitarLab does not request automatic ducking, alter MAIN gain or manipulate Android system-volume groups to create the MAIN/CUE balance.

## Exact signed RC32 identity

- producer SHA: `8360b695b6df8b56bc480479d07d1eee6f1c6add`;
- Android CI: **#972 / run `37612951851` — PASS**;
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc32`;
- versionCode: `52`;
- unsigned APK SHA-256: `d02ecc5f91c6abef86dbc2a38296f45d4c7551187580461829926557494c682d`;
- signed APK SHA-256: `f8cdcd1183d8efd4448f17d911b1284143741f192a03bb3d78b17f78fc4ee0e2`;
- signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact: `GuitarLabStudio-0.5.0-rc32-homologacao`;
- artifact id: `11478632922`;
- artifact ZIP digest: `sha256:b264466155fc37daba632e14639e960fa1bd8f588168d39fa91490ebeb0e84a6`;
- gate recorded by the artifact: `software+android-integration-passed;physical-validation-pending`.

The final signed producer differs from CI #971 only by the focused routing-status fix that preserves a specific CUE suppression reason; CI #972 requalified the complete final source and signed that exact candidate.

## Residual physical gate

Re-test the exact signed RC32 APK on Samsung SM-X230 / Android 16 with:

- MAIN = M-VAVE MK-300;
- CUE = wired headset;
- representative reference-guitar tracks routed to CUE;
- remaining mixer tracks routed to MAIN.

Acceptance requires repeated successful setup, long playback, seek/loop, disconnect/reconnect and representative Play/REC behavior without false CUE suppression, automatic ducking or unacceptable CUE fidelity. A new diagnostic bundle must show stable distinct physical routes and, if a suppression occurs, the schema-v4 `lastCueRuntime` evidence must explain it.

## Active future work

The professional single-device USB multichannel roadmap remains `history/CUE_USB_MULTICHANNEL_IMPLEMENTATION_PLAN_2026-10-06.md`: MAIN on USB 1/2 and CUE on USB 3/4 when a compatible 4+ channel interface is available. It is independent of RC32 and remains the preferred deterministic professional architecture when supported by hardware.

Historical RC/candidate chronology is intentionally not duplicated here. See `docs/history/`, especially:

- `CUE_COMMUNICATION_SPLIT_STABILIZATION_2026-10-07.md`;
- `RC32_COMMUNICATION_SPLIT_DIGITAL_QUALIFICATION_2026-10-07.md`;
- `RC31_COMMUNICATION_SPLIT_DIGITAL_QUALIFICATION_2026-10-06.md`.
