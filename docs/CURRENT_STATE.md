# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last signed DIGITAL PASS: CI #636 / run `35040569179` / exact source `b0a39a765f7cfbb0e9320ee847809300bc1e3d01`.
- Signed APK SHA-256: `b195d8fc4d90fa0f8fa8c826525d08328f65090859386a90eaa37e4bcdf4087e`.
- Unsigned APK SHA-256: `de996298a451cd559320cf71498121a652f9e3ca8054dba8bf2d95a281d08c47`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Workflow remains manual-only: `.github/workflows/android-ci.yml` uses `workflow_dispatch`; the assistant must not dispatch or rerun it.

## Evidence boundary
CI #636 is the authoritative digital baseline for H22/H22a. Physical validation on the Samsung SM-X230 + MK-300 is still required before final physical closure.

### H20/H21 — historical DIGITAL PASS at CI #633
- H20 added output canonicalization but physical review of #633 still exposed low-level Samsung BUS/system endpoints.
- H21 established the app-wide hardware-inspired visual system and passed the complete #633 digital regression.

### H22/H22a — DIGITAL PASS at CI #636
- Canonicalization applies to both recording inputs and playback outputs.
- Samsung/OEM internal BUS endpoints are folded into semantic physical microphone/speaker families.
- Built-in microphone endpoints are presented as one `Microfone do tablet` choice.
- Built-in speaker endpoints are presented as one `Alto-falante do tablet` choice.
- `remote-submix` and telephony/system-only endpoints are excluded from user-facing choices.
- USB endpoints are grouped by physical interface identity; endpoint/device indices do not create duplicate MK-300 choices.
- Low-level addresses such as `0`, `back`, `bottom`, `hsp:...` and endpoint ids are not user-facing labels.
- Legacy raw/H19/H20 selections migrate to the new canonical signatures.
- Input groups resolve internally to a compatible Android endpoint; output groups retain silent probe plus authoritative `routedDevice` resolution.
- Practice-bar titles use the neutral group surface while the action buttons carry the accent fill inside their own borders.
- H22a updates the USB migration regression to require both raw endpoint signatures and the prior `route2:usb:` migration signature; production routing logic is unchanged by H22a.

## CI #636 digital evidence
- Exact source: `b0a39a765f7cfbb0e9320ee847809300bc1e3d01`.
- Unit tests + Android Lint + debug/release assembly + unsigned provenance: PASS.
- `StudioAudioRoutePolicyTest`: 12/12 PASS, including built-in microphone grouping, built-in speaker grouping, `remote-submix` filtering, USB/MK-300 grouping and route2→route3 migration coverage.
- API 36 standard connected regression: 23/23 PASS.
- Isolated target-tablet geometry at 1920×1200 / 240 dpi: 1/1 PASS.
- Signed homologation gate: PASS.
- APK Signature Scheme v2: PASS; v1/v3/v3.1/v4 disabled as designed.
- Signers: 1; RSA 4096; certificate SHA-256 matches the locked GuitarLab homologation certificate.
- Package/version validated from the signed APK: `studio.guitarlab.app`, `0.5.0-rc3`, versionCode `23`.

## Expected physical UX
With no external interface connected, Options → Áudio should expose only meaningful routes, for example:
- Entrada: `Automático`, `Microfone do tablet` (plus genuinely external devices if present).
- Saída: `Automático`, `Alto-falante do tablet` (plus genuinely external devices if present).

With MK-300 connected, each direction should add one `MK-300` physical choice, not endpoint duplicates.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7/M8 through H22/H22a: DIGITAL PASS at CI #636.
- Final physical closure: pending Samsung SM-X230 + MK-300 verification.

## Next gate
Install the exact CI #636 signed APK and perform focused physical validation of input/output route presentation and real routing, disconnect/reconnect behavior, practice-bar visual hierarchy and retained recording/playback smoke. No additional CI is required unless physical validation exposes another product defect.
