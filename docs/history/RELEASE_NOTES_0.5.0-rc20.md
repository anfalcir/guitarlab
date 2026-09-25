# GuitarLab 0.5.0-rc20 — Final Release Notes

Released/frozen: 2026-09-25  
Status: **physically homologated personal-use baseline**

RC20 closes the unified GuitarLab product line and replaces the rejected RC19 separation backend.

## Final product

- Home/Prepare/Studio/Export/Backup/Activity/Settings operate as one project-oriented application;
- local Studio remains usable without login;
- direct Google Drive API v3 backup/restore is the supported backup transport;
- remote separation uses official PyTorch Demucs `htdemucs_6s`;
- Prepared References v2 publishes aligned backing-without-guitar and isolated-guitar references while six source stems remain worker-private;
- Studio supports managed media, non-destructive timeline editing, recording/takes, mixer, loop/practice controls, validated waveform display and master export;
- no tuner/pitch-detection functionality is included.

## RC20 corrective highlights

- replaced the musically broken RC19 `demucs.cpp`/GGML path with the official Demucs/PyTorch CPU route;
- qualified and promoted the exact worker digest without rebuild;
- retained hard audio structure/finiteness/quality gates and transaction/recovery/quota/ACK-purge checks;
- completed source-search terminal-state handling and bounded suggestion retry;
- added local prepared-reference reinsertion and later selective Base/Guitar canonical restoration;
- consolidated Prepare technical details, Settings diagnostics, bounded sanitized journal and diagnostic ZIP;
- cleaned obsolete Settings surfaces and made Activity cancellation actionable across local/WorkManager/cloud ownership;
- upgraded waveform cache/rendering to media/window-aware, frame-faithful peak-preserving behavior;
- corrected Studio same-project revision reconciliation after Prepare changes;
- anchored visible playback progress to sink presentation timing with bounded fallback, resolving the final playhead/audio perception issue.

## Final identity

See `../RELEASE_BASELINE.md` for exact APK hashes, signer, producer SHA, CI run, worker digest, Demucs/model identity and physical acceptance provenance.

## Maintenance

The release is frozen. No routine rebuild or recurring qualification is required. Reopen only under the maintenance triggers in `../PROJECT_IDENTITY.md` / D-090 / D-096 or an explicit owner-requested feature.
