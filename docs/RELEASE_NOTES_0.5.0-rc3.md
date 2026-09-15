# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-15

## Last signed digital homologation — CI #626
Run `35017084625`, source `f187ab2ba7596c4aa04d223f007409b2fb39f490`, is the signed DIGITAL PASS through H17. API36 was **22/22 PASS**, isolated 1920×1200 geometry was **1/1 PASS**, and signed homologation passed.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- signed APK SHA-256 `93a7ed1ebfdedf7421cf21183db84a529d2d095c9564caafc87952506cb5426b`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## Physical review after #626
The CUT presentation is physically accepted: numeric T1/T2 labels are gone and CUT ticks are confined to the time ruler. Ajustes/Níveis is also in the desired center location.

Two further defects were identified:
- the final `Ambas` comparison control could be clipped by the fixed docked segment proportions;
- MK-300 output could appear twice because Android exposed more than one logical USB output endpoint for the same physical interface, with one observed endpoint producing no sound.

## H18 candidate delta — adaptive practice bar — PRE-GATE
- fixed docked percentage allocation removed from the critical path;
- Comparação/Ajustes size to content;
- Timeline flexes into the remaining width and owns its own overflow;
- narrow docked widths scroll the whole strip instead of clipping a control;
- instrumentation now requires every comparison control to stay fully inside Comparação.

Patch SHA-256: `fba48ae2b0eedd2c87c269738197542f84a2772bac1aa55e0646ee722ef3627e`.

## H19 candidate delta — canonical USB output routing — PRE-GATE
- one physical USB interface is represented once in the output selector even when Android publishes multiple logical endpoints;
- old endpoint signatures migrate to one canonical route signature;
- non-USB profiles remain distinct;
- duplicate candidate endpoints are resolved by a silent stereo route probe and actual `routedDevice` confirmation rather than list order;
- unconfirmed duplicates fail closed to the existing automatic-route fallback;
- stale persistent selections are cleared on device refresh;
- deterministic JVM tests cover canonicalization, migration, route separation and ranking.

Patch SHA-256: `21373fa0d85d55ee180fed29308e5677e7bde90b1b555e366c61a872a388eb06`.

H18/H19 become part of the signed baseline only after the next manually dispatched exact-source workflow passes the full software/API36/tablet-geometry/signing gate. Real MK-300 endpoint consolidation/output remains a final hardware check.
