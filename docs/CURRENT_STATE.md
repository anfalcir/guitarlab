# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed DIGITAL PASS: CI #633 / run `35033323990` / exact source `2204e0272f9e6e2f218bebd36889db424e006e03`.
- Signed APK SHA-256: `f40b24cb36b4cb1299efcb28a35b54e66b107af2ec3d578a870c70e2966ff52e`.
- Unsigned APK SHA-256: `58165dc53cacaf39357a1f28315b6312ada3ed2b6669b59e32ddbe8e4d53c25d`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Workflow remains manual-only: `.github/workflows/android-ci.yml` uses `workflow_dispatch`; the assistant must not dispatch or rerun it.

## Evidence boundary
CI #633 is authoritative through H20/H21.

### H20 — physical output canonicalization v2 — DIGITAL PASS
- Built-in speaker endpoints such as `SM-X230`, `SM-X230 • 0`, `SM-X230 • back` and `SM-X230 • bottom` are canonicalized as one physical output.
- Earpiece, Bluetooth profiles, HDMI and other materially distinct route classes remain separate.
- H19 USB canonicalization is preserved.
- Duplicate candidate resolution continues to use an inaudible `AudioTrack` probe plus authoritative `routedDevice` confirmation.
- Route-policy unit tests, build, Lint and regression gates passed in CI #633.

### H21 — Studio visual system overhaul — DIGITAL PASS
- Global hardware-inspired geometry contract: 6dp controls, 8dp internal cards/rows, 10dp panels/dialogs.
- Practice groups `Comparação`, `Ajustes` and `Timeline` have explicit independent chassis and distinct title/action hierarchy.
- Functional accents remain blue = Comparação, teal = Ajustes, amber = Timeline.
- App-wide geometry/theme changes compiled and passed the complete CI #633 regression.

## CI #633 evidence
- Source: `2204e0272f9e6e2f218bebd36889db424e006e03`.
- Software/unit/Lint/build/provenance: PASS.
- API 36 standard connected suite: 23/23 PASS.
- Isolated target-tablet 1920×1200 geometry: 1/1 PASS.
- Signed homologation job: PASS.
- Package/version identity: `studio.guitarlab.app` / `0.5.0-rc3` / code 23.
- Signed APK SHA-256: `f40b24cb36b4cb1299efcb28a35b54e66b107af2ec3d578a870c70e2966ff52e`.
- Certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7/M8 through H21: DIGITAL PASS at CI #633.
- Physical validation remains pending for the real MK-300 route behavior, reconnect behavior, and final visual review on the tablet.

## Next physical review
Use the exact CI #633 signed APK. Verify:
1. one user-facing built-in speaker route;
2. one user-facing MK-300 route when connected;
3. audible playback through the selected route before and after reconnect;
4. final app-wide visual hierarchy/geometry on the physical tablet;
5. focused recording/routing smoke with MK-300.
