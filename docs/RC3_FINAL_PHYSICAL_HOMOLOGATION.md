# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-16

## Candidate binding
**Not currently bound to a final candidate.**

CI #650 / producer `07c99155789774cb39f9b4382829f9e1d16649e3` remains a valid H26/H26e DIGITAL PASS, but target-device backup validation exposed behavior corrected by H27. Do not approve final backup/history behavior using the #650 APK.

After H27 passes a fresh manually dispatched signed workflow, record here before testing:
- CI/run;
- exact producer SHA;
- package/versionCode/versionName;
- signed APK SHA-256 and size;
- signer certificate SHA-256.

## H27 backup/history target-device checklist
On the next exact signed H27 candidate:
- [ ] Select a real backup folder and confirm it remains usable after app restart and tablet reboot.
- [ ] With at least two projects, run `Backup total agora` once; each changed/unprotected project gets at most one new version.
- [ ] Run `Backup total agora` again without editing/saving either project; no additional same-revision versions appear.
- [ ] Run `Backup deste projeto` repeatedly on an unchanged project; no duplicate version appears.
- [ ] Edit and save exactly one project; the next backup creates one new version only for that changed revision.
- [ ] Configure maximum 3 versions; create more than three distinct saved revisions and confirm history is bounded to the three newest unique revisions after a safe run.
- [ ] If duplicate same-revision rows left by #650 are present, a safe H27 run removes the older duplicates while preserving the newest copy.
- [ ] Confirm the screen uses end-user wording: no Storage Access Framework/SAF/COMMITTED/SHA-256 protocol copy, and the control reads `Máximo de versões por projeto`.
- [ ] If a project backup fails, the status identifies that project/reason; its existing older versions are not destructively cleaned by that partial run.
- [ ] Change the destination folder and confirm prior remote data remains untouched; new backups use the new destination.
- [ ] Disconnect destination and confirm remote data remains untouched.
- [ ] Restore one version and restore all; existing local projects are not silently overwritten.
- [ ] Revoke provider access and confirm a safe, understandable error with no local project damage.
- [ ] Exercise a representative large transfer and cancel/retry if practical; no invalid/incomplete version becomes restorable.

## Retained audio/editing smoke
On the final candidate, retain a concise regression smoke for the previously validated real-device path:
- [ ] normal REC with the intended USB audio device and hardware loopback OFF;
- [ ] backing is not printed into the guitar take;
- [ ] live waveform/meters update during recording;
- [ ] selected input/route remains correct and fails closed rather than silently falling back;
- [ ] timing/alignment shows no repeatable systematic displacement;
- [ ] transport including `|<` remains coherent;
- [ ] representative edit → save → reopen is preserved;
- [ ] representative WAV/FLAC export succeeds and is coherent;
- [ ] no repeatable P0/P1 regression.

## Final PASS
RC3 FINAL requires a fresh H27 (or later) signed DIGITAL PASS, all applicable boxes above, no repeatable P0/P1 and explicit user approval of that exact signed APK SHA-256.
