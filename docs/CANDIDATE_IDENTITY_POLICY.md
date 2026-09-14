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

The exact candidate source is the `github.sha` of the manually dispatched workflow run that passes all mandatory gates. Documentation prepared before the run must not guess or predeclare that SHA. After the run, the unsigned release identity, `BUILD_IDENTITY.txt`, `SHA256SUMS.txt`, workflow metadata and the downloaded signed APK must all agree on the same source commit and version.

The optimized release pipeline compiles the release package once in the software gate, while signing credentials are absent. That unsigned APK is checksummed and uploaded with its source/package/version identity. After the independent software and API 36 gates both pass, the signing job downloads that exact artifact, verifies its checksum and identity, aligns/signs it, then verifies the final signer and application identity. The final job must not recompile source.

## Required gate identity

A deliverable is eligible for physical homologation only when the same manual workflow run proves:
1. software gate PASS, including unsigned release assembly when signed homologation is requested;
2. the staged unsigned release SHA-256 and identity are recorded and tied to the run `github.sha`;
3. API 36 Android integration gate PASS, including the isolated target-tablet geometry pass;
4. final signing/verification PASS using that exact staged release artifact;
5. signer certificate matches the locked SHA-256 above;
6. final APK package, `versionName` and `versionCode` match the active candidate;
7. final APK checksum is recorded in `SHA256SUMS.txt`;
8. `BUILD_IDENTITY.txt` reports the same source commit, version, package, unsigned release SHA-256 and signed APK SHA-256 as the workflow evidence.

Any mismatch blocks delivery. A successful build from a different SHA, version or signer is not the active candidate.
