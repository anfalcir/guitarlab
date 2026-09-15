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
CI #633 remains authoritative through H20/H21. Physical review of the exact #633 APK showed that the Samsung device exposes additional low-level audio endpoints not covered by H20. H22 is implemented/source-validated and requires a new exact-source full gate.

### H20/H21 — historical DIGITAL PASS at CI #633
- H20 added output canonicalization but real SM-X230 hardware still exposed BUS/system endpoints such as `0`, `back`, `bottom` and `remote-submix`.
- H21 established the app-wide hardware-inspired visual system and passed the complete #633 digital regression.

### H22 — semantic physical audio routes + practice-bar action emphasis — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- Canonicalization now applies to both recording inputs and playback outputs.
- Samsung/OEM `TYPE_BUS` endpoints whose product identifies the current Android device are folded into the built-in physical microphone/speaker families.
- Built-in microphone endpoints are shown as one user-facing `Microfone do tablet` route.
- Built-in speaker endpoints are shown as one user-facing `Alto-falante do tablet` route.
- `remote-submix` and telephony/system-only endpoints are removed from user-facing choices.
- USB endpoints are grouped by physical interface identity; endpoint/device indices no longer create duplicate MK-300 choices.
- Low-level addresses (`0`, `back`, `bottom`, `hsp:...`, endpoint ids) are never used as user-facing labels.
- Legacy raw/H19/H20 selections migrate to the new canonical signatures instead of being silently lost.
- Input groups resolve internally to a stable compatible Android endpoint; output groups retain silent probe + authoritative `routedDevice` resolution.
- Practice-bar titles no longer own the tinted background; inactive action buttons now have a subtle accent fill inside their own borders, making title/group/action hierarchy clearer.
- Route-policy smoke reproducing the physical evidence topology passes locally.
- H22 source patch forward/reverse round-trip, `git diff --check`, encoded gzip integrity, materializer `bash -n`, first materialization and idempotent second materialization all pass locally.

## H22 expected physical UX
With no external interface connected, Options → Áudio should expose only meaningful routes, e.g.:
- Entrada: `Automático`, `Microfone do tablet` (plus genuinely external devices if present).
- Saída: `Automático`, `Alto-falante do tablet` (plus genuinely external devices if present).

With MK-300 connected, each direction should add one `MK-300` physical choice, not endpoint duplicates.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7/M8 through H21: DIGITAL PASS at CI #633.
- H22: PRE-GATE; final physical closure remains pending.

## Next gate
Run the full workflow manually on the exact H22 source. DIGITAL PASS requires software/unit/Lint/build/provenance, route-policy regressions, API36 connected regression, isolated target-tablet geometry and signed homologation all green on the same SHA.
