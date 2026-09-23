# Final Physical Homologation — GuitarLab 0.5.0-rc14

Updated: 2026-09-23  
Target: `0.5.0-rc14` / versionCode `34`  
Status: digital qualification pending

Use only the signed RC14 artifact produced after all exact-source gates pass.

## Focused invalidation set

- [ ] Notification shade can be opened repeatedly during upload, queueing, Cloud Run processing and import without cancelling or duplicating the job.
- [ ] Home, app switching, screen lock/unlock and return to the project preserve the same remote job.
- [ ] Separation and backup notifications show the GuitarLab monochrome mark legibly in light and dark system themes.
- [ ] The track pencil exposes one take-management route, and every action is consistent with the track-settings entry.
- [ ] Per-take ±1/5/25 ms adjustments persist after save/reopen and affect existing recorded material; global calibration affects only later recordings.
- [ ] Backup catalog shows one row per project ID and a modal containing all retained versions.
- [ ] Explicitly deleted projects show `EXCLUÍDO`; Drive-only projects show `SOMENTE NA NUVEM` and are not scheduled for deletion.
- [ ] Restoring an explicitly deleted project cancels its ten-day deletion lifecycle.
- [ ] Permanent cloud deletion requires confirmation and removes the project from the refreshed catalog.
- [ ] A tombstone at less than ten days remains recoverable; at ten days it is removed during a normal total/automatic backup.

All unrelated accepted RC13 physical evidence may be reused only where the RC14 source delta cannot affect the observed behavior.
