# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-16

## Candidate binding
**Not currently bound to a final candidate.**

CI #651 / producer `0b6ae1e28214decbcfba622a38d90c0dcbe2acf9` is a valid H27 DIGITAL PASS, but target-device backup validation exposed the provider-consistency behavior corrected by H28. Do not approve final backup behavior using the #651 APK.

After H28 passes a fresh manual signed workflow, record here before testing: CI/run, exact producer SHA, package/version, signed APK SHA-256/size and signer certificate SHA-256.

## H28 backup target-device checklist
On the exact new signed H28 candidate:
- [ ] Empty the test destination and refresh; zero versions are shown.
- [ ] With at least two projects, run `Backup total agora` once; each changed project creates exactly one usable version and no false confirmation failure appears.
- [ ] Immediately run it again without edits; no additional version appears and both projects are treated as current.
- [ ] Rename one project, save and back up; the new revision stays in the same project history/identity.
- [ ] Edit/save only one project; only it gains a new revision.
- [ ] Configure maximum 3 versions; after >3 distinct revisions only the three newest unique revisions remain following a safe run.
- [ ] Refresh, restart the app and reboot the tablet; converged remote history remains stable and no retry creates duplicates.
- [ ] Change/disconnect destination without deleting prior remote data.
- [ ] Restore one version and restore all without silently overwriting an existing local project.
- [ ] Revoke provider access and confirm safe, understandable failure with no local project damage.
- [ ] Exercise a representative large transfer and cancel/retry if practical; incomplete data never becomes restorable.

## Retained audio/editing smoke
- [ ] normal REC with intended USB audio device and hardware loopback OFF;
- [ ] backing is not printed into the guitar take;
- [ ] live waveform/meters update;
- [ ] route fails closed rather than silently falling back;
- [ ] no repeatable systematic timing displacement;
- [ ] transport including `|<` remains coherent;
- [ ] representative edit → save → reopen survives;
- [ ] representative WAV/FLAC export succeeds;
- [ ] no repeatable P0/P1 regression.

## Final PASS
RC3 FINAL requires a fresh H28-or-later signed DIGITAL PASS, all applicable checks above, no repeatable P0/P1 and explicit approval of that exact signed APK SHA-256.
