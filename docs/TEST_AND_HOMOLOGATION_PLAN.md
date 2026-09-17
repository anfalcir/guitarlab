# Test and Homologation Plan

Updated: 2026-09-16

## Evidence boundary
The last signed DIGITAL PASS is **CI #651** / run `35166195527` / producer `0b6ae1e28214decbcfba622a38d90c0dcbe2acf9`, through H27.

Signed APK SHA-256: `d9ce720194812afcb281ecebd263d482d4320b4285f50044a2771fc6293736fe`.

#651 passed 287/287 JVM/unit tests, Android Lint, debug/release assembly, unsigned provenance, 32/32 standard API36 tests, 1/1 isolated target-tablet geometry test and signed provenance. Physical backup testing then exposed the provider-consistency defect fixed by H28.

H28 is **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**. Do not reuse #651 as H28 evidence.

## Mandatory H28 automated gate
A fresh manually dispatched signed workflow must verify:
- deterministic source materialization ending in `Source patch chain materialized through H28 with verified final hashes`;
- all JVM/unit suites, including new project/revision identity, provider-lag and compatibility tests;
- performance evidence;
- Android Lint;
- debug + release assembly and unsigned identity/provenance;
- standard API36 instrumentation;
- isolated 1920×1200 geometry;
- signing of the exact tested unsigned artifact;
- package/version, zipalign, v2 signature, expected certificate and signing cleanup.

Actual counts and hashes must be taken from that run, not predicted.

## H28 regression assertions
Automated or programmatic coverage must prove:
- rename preserves `projectId`;
- duplicate project receives a new `projectId`;
- revision ID is deterministic for unchanged canonical state;
- revision ID embeds the persisted edit timestamp and changes when canonical state changes, even if timestamp is equal;
- v2 dedup never reduces two distinct same-timestamp states to one;
- v1 backup metadata remains readable;
- an initial stale catalog followed by targeted revision discovery performs zero duplicate commit calls;
- exact remote write verification checks metadata, marker and package integrity without requiring immediate directory-list visibility;
- retention remains bounded per project and fail-safe on partial/total failure.

## Reduced physical residual after a passing H28 CI
Use the exact signed H28 APK only:
- start with an empty destination and refresh: zero versions visible;
- press `Backup total agora` once: each changed project produces exactly one usable version and no false failure;
- immediately press it again without editing: zero additional versions and both projects reported current;
- rename one project, save and back up: one new revision appears under the same project identity/history, never as a separate project;
- edit/save only one project and back up: only that project gains one revision;
- with maximum 3, create >3 distinct revisions and confirm only the three newest unique revisions remain after a safe run;
- refresh/restart/reboot and confirm the catalog converges without creating duplicates;
- restore one and restore all; existing local projects are not silently overwritten;
- change/disconnect destination and revoke provider access; failures remain safe and understandable;
- exercise one representative large transfer/cancel/retry if practical; incomplete versions never become restorable.

## Retained final smoke
On the final exact candidate, retain a concise real-device audio/editing smoke: REC route, no backing printed into take, live waveform/meters, timing/alignment, transport including `|<`, edit→save→reopen and representative WAV/FLAC export.

## Final rule
RC3 FINAL requires a fresh H28-or-later signed DIGITAL PASS, the reduced physical residual above, no repeatable P0/P1 and explicit approval of the exact final signed APK SHA-256.
