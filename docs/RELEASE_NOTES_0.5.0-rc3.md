# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-16

## Current signed digital homologation — CI #642
Run `35121955150`, producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632`, is the current signed DIGITAL PASS through H25 only.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- signed APK SHA-256 `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

CI #642 evidence: 269/269 JVM/unit, Lint/build/provenance, API36 **28/28 standard + 1/1 isolated geometry**, signed homologation PASS.

## H25 — DIGITAL PASS
H25 contains rounded-square interaction feedback, dedicated calibration modal, consolidated diagnostics and explicit project-delete confirmation.

## H26 — SAF Cloud Backup — PRE-GATE
New source behavior prepared for the next candidate:
- user-selected SAF target folder with persistable scoped permission and real read/write/delete probe;
- manual full backup and per-project backup;
- automatic incremental/coalesced + periodic WorkManager backup;
- single-version and latest-per-project full restore as independent local copies;
- transactional remote versions with remote re-read, SHA-256/byte validation and strict `COMMITTED` marker;
- configurable cadence/network/charging and target-folder switching/disconnect without deleting old cloud data;
- retention by age plus a minimum protected version count per project;
- cleanup suppression/protection on full or partial upload failure and proper cancellation propagation;
- dedicated Backup/Restauração UI, Settings summary, Home per-project shortcut and synchronized Help;
- new pure/JVM and Android UI test coverage.

H26 is not included in the #642 APK. A fresh manual signed workflow is required before release promotion.
