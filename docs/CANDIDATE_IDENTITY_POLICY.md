# Candidate identity policy

Updated: 2026-09-14

This policy defines the identity contract for the active GuitarLab homologation candidate. Historical alpha-specific identity notes are evidence only and do not override this file.

## Active candidate
- `versionName`: `0.5.0-rc3`
- `versionCode`: `23`
- canonical branch: `main`
- package: `studio.guitarlab.app`
- signed artifact name: `GuitarLabStudio-0.5.0-rc3-homologacao.apk`
- expected homologation certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`

## Exact-source rule
The exact candidate source is the `github.sha` of the manually dispatched workflow run that passes all mandatory gates. Documentation prepared before the run must not guess or predeclare that SHA. After the run, `BUILD_IDENTITY.txt`, `SHA256SUMS.txt`, workflow metadata and the downloaded APK must all agree on the same source commit and version.

## Required gate identity
A deliverable is eligible for physical homologation only when the same manual workflow run proves:
1. software gate PASS;
2. API 36 Android integration gate PASS, including the isolated target-tablet geometry pass;
3. signed release assembly PASS;
4. signer certificate matches the locked SHA-256 above;
5. APK checksum is recorded in `SHA256SUMS.txt`;
6. `BUILD_IDENTITY.txt` reports the same commit, versionName and versionCode as the source snapshot.

Any mismatch blocks delivery. A successful build from a different SHA, version or signer is not the active candidate.
