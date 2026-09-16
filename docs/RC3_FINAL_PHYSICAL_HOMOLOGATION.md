# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-16

## Candidate binding
This checklist is bound to the current signed digital authority:
- CI #650 / run `35154021384`;
- producer `07c99155789774cb39f9b4382829f9e1d16649e3`;
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / code `23`;
- signed APK SHA-256 `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`;
- size `13,737,498` bytes;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

H26/H26e is DIGITAL PASS. Do not substitute another APK or a later documentation HEAD for this producer identity.

## H26 — minimal target-device SAF checklist
On Samsung SM-X230 with Google Drive exposed through the system DocumentsProvider:
- [ ] Open `Backup/Restauração`; choose a Drive folder via SAF; the real read/write/delete probe succeeds and status/label are coherent.
- [ ] Close/reopen GuitarLab and then reboot the tablet; the selected tree remains usable without unnecessary reselection.
- [ ] Run `Backup deste projeto`; confirm a real remote version appears in Drive and app status/last-backup data are coherent.
- [ ] With multiple projects, run `Backup total agora`; confirm independent remote versions are created.
- [ ] Change the target folder; confirm the old folder/data remain untouched and subsequent backups go only to the new target.
- [ ] Disconnect the target; confirm existing remote data remains untouched.
- [ ] Restore one selected version; confirm GuitarLab creates a new independent local project and does not overwrite the existing one.
- [ ] Run restore-all; confirm latest valid versions restore independently and existing local projects are not overwritten.
- [ ] Revoke SAF permission or make the provider unavailable; confirm a clear safe error, no crash and no local project damage.
- [ ] Perform a representative large transfer; confirm long-operation behavior is stable. Exercise cancel/retry if practical and confirm no corrupt committed restore candidate is exposed.
- [ ] Where practical, create enough versions to exercise retention and confirm protected minimum versions survive age cleanup.
- [ ] Edit/save a project with automatic backup enabled; confirm a changed revision uploads and an unchanged revision does not create unnecessary duplicate versions.

## Retained audio/editing smoke — only real-device residuals
With MK-300 where applicable:
- [ ] normal REC with MK-300 and hardware loopback OFF;
- [ ] backing plays but is not printed into the guitar take;
- [ ] live waveform/meters update during recording;
- [ ] selected input/route remains correct and fails closed rather than silently falling back;
- [ ] timing/alignment is not repeatably displaced; include repeated takes, non-zero playhead and loop/punch where applicable;
- [ ] `|<`/transport and recording-state controls remain coherent;
- [ ] representative edit → save → reopen preserves project state;
- [ ] representative WAV/FLAC export succeeds and is audibly/structurally coherent;
- [ ] no repeatable P0/P1 regression from H25/H24/H23b behavior.

## Final PASS
RC3 FINAL requires all applicable boxes above, absence of repeatable P0/P1 and explicit user approval of the **exact** #650 signed APK SHA-256. CI #650 alone is not a final-release declaration.