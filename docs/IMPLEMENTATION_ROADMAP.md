# Implementation Roadmap

Updated: 2026-09-15

## M1 — Project/model foundation — CLOSED
Project model, templates, persistence baseline and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Samsung SM-X230 USB-audio baseline established.

## M3 — Codec/import foundation — ABSORBED
Consolidated into later milestones.

## M4 — Studio playback/edit/mix foundation — ABSORBED
Timeline, playback, editing, Mixer/Master and project interaction foundations are integrated.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after explicit physical approval.

## M6 — Measured latency and synchronization — PASS/CLOSED
Closed after explicit physical approval. Later timing work hardens per-session startup behavior without reopening M6.

## M7 — Production audio polish — DIGITAL PASS THROUGH H11 / FINAL PHYSICAL GATE OPEN
Managed media, SRC/editing domain, fades/crossfades, transactional recording/recovery, fail-closed input, monitoring isolation, takes, practice controls, level analysis, live REC waveform, H11 Mixer/selection/metering refinements and project/master export are implemented.

CI #620 is the authoritative active digital candidate.

## M8 — Release hardening
### H0–H10 — DIGITAL PASS baseline
CI #617 / run ID `34918430241` / source `abc0e2a9f8708dd141735915898b508ce0948f48` passed software, API 36 full instrumentation, tablet geometry and signed homologation.

### H11 — Physical Review III — DIGITAL PASS
Implemented scope:
- segmented Comparação/Timeline bar inside Mixer;
- redundant dock title/Pin/X removed;
- top-bar Mixer button is the persistent open/close toggle;
- waveform/clip/live waveform taps select their track;
- active recording track receives live Peak/RMS in Mixer and waveform overlay.

### H11a — synchronous Trim dispatch — DIGITAL PASS
Removed the inherited H8 `pendingTrimClipId` + `LaunchedEffect` handoff. `Cortar` now invokeses Trim synchronously from the first valid click.

### H11b — waveform-selection semantics isolation — DIGITAL PASS
- waveform lane selection uses a sibling background hit target rather than an ancestor clickable;
- clip click semantics are absent during Trim;
- independent TrimHandle nodes remain exposed;
- strict #618/#619 Trim regression remains unchanged.

### Diagnostic runs retained
- CI #618 / run `34922529980` / source `e00ae08b1ea3a1d7c5f630d54fd5fb2aec7da3d2`: software PASS, API36 FAIL, signing skipped.
- CI #619 / run `34923582119` / source `a30a4a04a8ffef2820d8f51745cd172ac6cbba3a`: software PASS, API36 FAIL, signing skipped.

### Canonical acceptance run — CI #620 — PASS
Run ID `34924500870`, exact source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`:
1. complete JVM/unit regression: PASS;
2. performance evidence: PASS;
3. Android Lint + debug/release assembly: PASS;
4. API 36 full connected regression: PASS;
5. independent Trim-handle regression: PASS unchanged;
6. H11 selection/segmented-bar/persistence coverage: PASS;
7. isolated 1920×1200 geometry: PASS;
8. signed homologation: PASS;
9. package/version/source/signer/checksum provenance: PASS.

Candidate identity:
- version `0.5.0-rc3` / code `23`;
- signed APK SHA-256 `acbe61b006aa4abe8b3063faf35b4a9569ed55aaf7f1a2ca3e1726c927855b3c`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## Remaining work
No further digital implementation gate is required for H11. Proceed only with final physical validation on the #620 APK and fix new defects only if that real-device gate exposes objective regressions.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only. No assistant-triggered reruns. Documentation-only commits after #620 do not change the digitally homologated application source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`.
