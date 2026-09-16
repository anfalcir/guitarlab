# Final Physical Homologation — GuitarLab 0.5.0-rc3

Updated: 2026-09-16

## Candidate binding
The previous signed physical candidate is CI #642 / producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632`, SHA-256 `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`, DIGITAL PASS through H25.

**H26 changes source and is PRE-GATE. Do not homologate H26 using the #642 APK.** Bind this checklist to the exact next H26 producer/SHA only after a new manually dispatched signed workflow passes.

## H26 target-device SAF smoke — execute only after H26 DIGITAL PASS
On Samsung SM-X230:
- open Backup/Restauração and select a Google Drive folder through the system SAF picker;
- confirm read/write/delete probe succeeds and selected label/status are coherent;
- restart the app and reboot the tablet; persisted access remains available;
- run `Backup deste projeto` and verify a committed cloud version becomes visible;
- run `Backup total agora` with multiple projects and verify independent versions;
- modify/save a project and confirm automatic incremental backup under selected constraints without duplicate upload for an unchanged revision;
- exercise a representative large project and confirm foreground transfer indication plus safe cancel/retry behavior;
- change target folder: prior folder content remains untouched; subsequent backups target the new folder;
- disconnect target: cloud data remains untouched and automation disables safely;
- restore one selected version: a new independent local project is created, no overwrite;
- restore latest of all: latest valid version per project is restored independently;
- revoke SAF permission/provider availability and confirm clear safe error with no local project damage;
- confirm incomplete/invalid versions are never offered to restore;
- exercise retention with controlled versions and confirm the protected minimum is never deleted.

## Retained H25 physical smoke
- rounded-square icon feedback conforms to button chassis;
- calibration lives in dedicated modal and uncalibrated status alone does not block REC;
- diagnostic organization remains clean;
- project delete requires explicit confirmation; Cancel safe, Confirm selected project only.

## Retained H24/H22/H23b critical smoke
- Home search/filter/sort behavior coherent;
- semantic MK-300 route UX remains correct after reconnect;
- normal REC loopback OFF, fine adjustment 0.0 ms baseline;
- at least three repeated 44.1 kHz takes plus non-zero playhead and loop/punch;
- no repeatable systematic timing displacement/backing leakage;
- live waveform/meters and selected-input fail-closed behavior correct;
- representative edit/save/reopen/WAV-FLAC export smoke passes.

## Final PASS
Requires a new exact H26 signed candidate with complete digital gate PASS, all applicable physical checks above, no repeatable P0/P1 and explicit user approval of that exact signed APK SHA-256.
