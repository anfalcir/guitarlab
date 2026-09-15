# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active candidate
- Canonical repository/branch: `anfalcir/guitarlab` / `main`.
- Active version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Last digitally homologated application/source SHA: `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`.
- Last canonical PASS: CI #620 / run ID `34924500870`.
- #620 signed APK SHA-256: `acbe61b006aa4abe8b3063faf35b4a9569ed55aaf7f1a2ca3e1726c927855b3c`.
- #620 unsigned release SHA-256: `14c4862371871cf6db85548bd6abc3405cdfcd278d726a5a9f097d533183bafd`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- `.github/workflows/android-ci.yml` remains manual-only. Ordinary documentation commits use `[skip ci]`.

## Evidence boundary
CI #620 is the authoritative DIGITAL PASS through H11/H11a/H11b. It supersedes #617 as the active application candidate while preserving #617 as the previous H0–H10 baseline.

CI #618 and #619 remain diagnostic evidence only:
- #618 / source `e00ae08b1ea3a1d7c5f630d54fd5fb2aec7da3d2`: software PASS, API36 FAIL, signing skipped;
- #619 / source `a30a4a04a8ffef2820d8f51745cd172ac6cbba3a`: software PASS, API36 FAIL, signing skipped.

Those runs exposed, respectively, the inherited deferred Trim-dispatch race and the H11 waveform-selection semantics collision. H11a and H11b corrected both without weakening the regression.

## CI #620 — canonical H11 digital PASS
Manual workflow #620 ran on exact source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7` and passed:
1. Unit tests + performance evidence + Android Lint + debug/release assembly + unsigned provenance;
2. API 36 full connected instrumentation, including the unchanged independent Trim-handle regression and H11 interaction coverage;
3. isolated tablet geometry gate;
4. signed homologation using the tested unsigned release artifact.

Signed identity:
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc3`;
- versionCode: `23`;
- source: `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`;
- unsigned APK SHA-256: `14c4862371871cf6db85548bd6abc3405cdfcd278d726a5a9f097d533183bafd`;
- signed APK SHA-256: `acbe61b006aa4abe8b3063faf35b4a9569ed55aaf7f1a2ca3e1726c927855b3c`;
- APK Signature Scheme v2: verified;
- number of signers: 1;
- key: RSA 4096;
- certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact ID: `10380000533`.

The downloaded signed artifact was independently hashed after download and matched `SHA256SUMS.txt` exactly.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7: digital candidate through H11 PASS; final physical gate remains open.
- M8: H0–H11 DIGITAL PASS in CI #620; only real-device/human validation remains for the active candidate.

## Physical Review III — H11 — DIGITAL PASS
### Segmented Mixer practice bar
- redundant dock title `Mixer`, Pin and X removed;
- top-bar Mixer action is the persistent visibility toggle;
- Mixer-open practice bar contains two equal horizontal segments: **Comparação** and **Timeline**;
- restrained near-square corner radii replace pill-like buttons.

### Waveform selects the track
- tapping waveform/audio lane selects the corresponding track;
- clip cards and live-recording waveform use the same selected-track state;
- H11b isolates selection semantics so Trim handles remain independent accessibility/test nodes.

### Live recording Peak/RMS
- raw capture Peak/RMS is projected only to the active recording track;
- Mixer PK/RMS updates through the existing meter path;
- live waveform shows compact PK/RMS feedback;
- meter state resets across recording lifecycle boundaries.

### Trim hardening retained
- H11a dispatches `Cortar` synchronously on the first valid tap;
- H11b prevents waveform/clip click semantics from merging the independent Trim handles;
- the strict regression that failed #618/#619 passed unchanged in #620.

## Final physical gate
Use the #620 signed APK for the remaining physical validation on Samsung SM-X230 + M-VAVE MK300. Focus only on facts automation cannot establish:
- visual harmony/touch ergonomics of the segmented Mixer bar;
- persistence and feel of the top-bar Mixer toggle;
- natural track selection by tapping waveform/clip/live waveform;
- independent physical usability of both Trim handles;
- real MK300 Peak/RMS responsiveness/plausibility during REC;
- retained level-analysis, Stop-during-REC, long-waveform, routing/isolation, synchronization and listening/export smoke.

`RC3_FINAL_PHYSICAL_HOMOLOGATION.md` remains the single final manual checklist.
