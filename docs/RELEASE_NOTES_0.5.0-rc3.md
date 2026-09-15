# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-15

## Last signed digital homologation — CI #625
Run `35010012582`, source `476fa740408130adf6a4e9665d166e724a9184dd`, remains the signed DIGITAL PASS through H16. API36 was **22/22 PASS**, isolated 1920×1200 geometry passed, and signed homologation passed.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- signed APK SHA-256 `107795f040ed18246bb130a9519044ba7e07f334a5835566b952cbc7fdb528e6`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## H17 candidate delta — PRE-GATE
Physical review of #625 produced two final presentation corrections.

### CUT
- remove visible T1/T2 time-label boxes;
- keep only short yellow ticks in the time ruler;
- never draw CUT ticks into the sections/playhead rail;
- retain exact timeline X projection and waveform handle editing.

### Practice bar
- keep the three-block `Comparação | Ajustes | Timeline` structure;
- center `Ajustes + Níveis` inside its dedicated center block;
- preserve proportional segment widths while eliminating the visual collision with comparison controls.

### Regression hardening
- CUT regression now requires no visible T1/T2 time text and constrains tick bounds to the time ruler;
- practice-bar regression now requires Níveis to remain inside Ajustes and measures symmetric left/right inset of the center content cluster.

Implementation commit: `41534dd2fb1ba7b0fc18459dbe5c622e78f039cb`.

H17 patch SHA-256: `38b3f494cf528fcc9fc818e6ef38ed0647389e106ec1bcc821e00ee2e65278dc`.

H17 becomes part of the signed baseline only after the next manually dispatched exact-source workflow passes the full software/API36/tablet-geometry/signing gate.
