# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active candidate
- Canonical repository/branch: `anfalcir/guitarlab` / `main`.
- Active version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last digitally homologated application/source SHA: `abc0e2a9f8708dd141735915898b508ce0948f48`.
- Last canonical workflow: CI #617 / run ID `34918430241` — SUCCESS.
- #617 signed APK SHA-256: `7f0c303ccc447c5455dfbd49e1bc022af482927582254eb826c9b29f3a84c6b6`.
- #617 unsigned release SHA-256: `2aacc5c027bfd668fb85499bf8992c90b933dbe04d4abcdbfb732db60a77dd78`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- `.github/workflows/android-ci.yml` remains manual-only. Ordinary commits use `[skip ci]`.

## Evidence boundary
CI #617 is the authoritative digital PASS through H10. Physical use of that APK was approved as materially improved, then exposed one final UI/metering refinement grouped as **Physical Review III / H11**. H11 is **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE** and is not covered by the #617 APK.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7: digital baseline through H10 PASS; final closure remains open because H11 changes active Studio behavior.
- M8: H0–H10 DIGITAL PASS in CI #617; H11 IMPLEMENTED / PRE-GATE.

## Retained authoritative evidence — CI #617
Manual workflow #617 passed software, Android Lint/build, API 36 full instrumentation, isolated 1920×1200 geometry and signed homologation against exact source `abc0e2a9f8708dd141735915898b508ce0948f48`.

`BUILD_IDENTITY.txt` records package `studio.guitarlab.app`, version `0.5.0-rc3` / code 23, unsigned SHA `2aacc5c027bfd668fb85499bf8992c90b933dbe04d4abcdbfb732db60a77dd78`, signed SHA `7f0c303ccc447c5455dfbd49e1bc022af482927582254eb826c9b29f3a84c6b6` and the locked signer above. The downloaded artifact checksum was independently recomputed and matched `SHA256SUMS.txt`.

## Physical Review III — H11 — IMPLEMENTED / PRE-GATE
### Segmented Mixer practice bar
- the redundant `Mixer` title, Pin and in-dock Close controls were removed;
- the top-bar Mixer action is now the single persistent visibility toggle: one tap opens, the next tap closes;
- visibility is persisted and migrated from the former `mixer_pinned` preference;
- with Mixer open, the header contains only two equal horizontal segments: **Comparação** and **Timeline**;
- segment/button geometry uses restrained 4–6 dp corner radii instead of pill-like rounding;
- the same practice-control implementation remains shared rather than duplicated.

### Waveform selects the track
- tapping the waveform/audio lane now selects that track through the same selected-track state used by the sidebar and Mixer;
- clip cards and the live-recording waveform surface follow the same selection rule;
- semantic/test identities were added for deterministic interaction coverage.

### Live recording Peak/RMS
- recording progress now projects raw capture Peak/RMS into the active recording track's existing meter state;
- only the recording target receives the live capture meter; unrelated track meters are preserved;
- Mixer PK/RMS therefore updates during REC through the normal track-meter rendering path;
- a compact `PK` / `RMS` overlay is also rendered on the active live-recording waveform;
- meter state is reset on recording start/finalization/error so stale values do not leak between sessions.

### H11 regression evidence before CI
- pure `RecordingTrackMeterPolicy` harness: PASS;
- H11 patch forward/reverse behavior validated against the exact #617 materialized source;
- H11 result reproduces the exact expected application tree;
- `git diff --check`: PASS;
- added tests cover equal Comparison/Timeline segments, waveform-area track selection, Mixer visibility persistence and recording meter projection.

H11 is versioned as `.source-parts/H11MixerWaveformMetering.patch.gz` and is applied after H7–H10 by `scripts/materialize_ci_sources.sh`.

## Next authoritative gate
One new manually dispatched `GuitarLab Android CI` run is required on the final H11 `main` HEAD. Required PASS:
1. complete JVM/unit regression including `RecordingTrackMeterPolicy`;
2. Android Lint and debug/release assembly;
3. API 36 full instrumentation including H11 interaction tests;
4. isolated 1920×1200 geometry;
5. signed homologation;
6. exact package/version/source provenance;
7. locked signer verification and published APK SHA-256.

Do not use the #617 APK as evidence for H11.

## Residual physical gate after H11 digital PASS
Keep manual validation focused on facts automation cannot establish:
- visual harmony and touch ergonomics of the two-segment Mixer bar;
- top-bar Mixer open/close persistence across project/app reopen;
- natural track selection by tapping waveform/clip/live waveform;
- real MK-300 REC Peak/RMS responsiveness and plausibility;
- previously retained trim, level-analysis, Stop-during-REC, long-waveform, routing/isolation, synchronization and listening/export smoke.

`RC3_FINAL_PHYSICAL_HOMOLOGATION.md` remains the single manual checklist after the next exact-source digital PASS.
