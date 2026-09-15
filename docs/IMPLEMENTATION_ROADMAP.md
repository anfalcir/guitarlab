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

## M7 — Production audio polish — DIGITAL PASS THROUGH H10 / FINAL PHYSICAL GATE OPEN
Managed media, SRC/editing domain, fades/crossfades, transactional recording/recovery, fail-closed input, monitoring isolation, takes, practice controls, level analysis, live REC waveform and project/master export are implemented. CI #617 is the authoritative H0–H10 digital PASS.

## M8 — Release hardening
### H0–H10 — DIGITAL PASS
CI #617 / run ID `34918430241` / source `abc0e2a9f8708dd141735915898b508ce0948f48` passed software, API 36 full instrumentation, isolated 1920×1200 geometry and signed homologation.

### H11 — Physical Review III — IMPLEMENTED / PRE-GATE
- segmented Comparação/Timeline bar inside Mixer;
- redundant dock title/Pin/X removed;
- top-bar Mixer button becomes persistent open/close toggle;
- waveform/clip/live waveform taps select the track;
- active recording track receives live Peak/RMS in Mixer and waveform overlay.

### CI #618 — diagnostic failure
Run ID `34922529980`, source `e00ae08b1ea3a1d7c5f630d54fd5fb2aec7da3d2`:
- software/unit/Lint/build/provenance: PASS;
- API 36 integration: FAIL on the independent Trim-handle regression;
- signed homologation: skipped.

### H11a — synchronous Trim dispatch — IMPLEMENTED
H11a removed an inherited H8 race by replacing `pendingTrimClipId` + `LaunchedEffect` with synchronous `onBeginTrim(clip.id)` dispatch from the `Cortar` click. The regression test was not weakened.

### CI #619 — diagnostic failure after H11a
Run ID `34923582119`, source `a30a4a04a8ffef2820d8f51745cd172ac6cbba3a`:
- software/unit/Lint/build/provenance: PASS;
- H11 waveform-selection, segmented-bar and persistence instrumentation: PASS;
- API 36 integration: FAIL on the same single independent Trim-handle test;
- signed homologation: skipped.

#619 proved H11a was valid hardening but not the final cause. The remaining defect was H11 clickable semantics merging the TrimHandle descendants out of the merged accessibility tree.

### H11b — waveform-selection semantics isolation — IMPLEMENTED / PRE-GATE
- waveform lane selection moved from an ancestor clickable to a sibling background hit target;
- clip click semantics are installed only outside Trim mode;
- independent TrimHandle semantics remain exposed during Trim;
- waveform/clip/live-waveform track selection remains intact;
- strict #618/#619 Trim regression remains unchanged.

Exact H11b patch SHA-256: `315bbd8d247d25edf8fc4d19adb67fbafae2705f618863d2de98af0d003f570a`.

## Next acceptance gate
A new manually dispatched workflow on the corrected final `main` SHA must pass:
1. complete JVM/unit regression;
2. Android Lint and debug/release assembly;
3. API 36 full connected regression, including independent Trim handles and H11 selection behavior;
4. isolated 1920×1200 geometry;
5. signed homologation;
6. exact source/package/version/signer/checksum provenance.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only. No assistant-triggered reruns. `scripts/materialize_ci_sources.sh` applies H11 → H11a → H11b after H7–H10 and fails closed on patch drift.
