# H37c — Signing provenance corrective

Status: **SOURCE/Pipeline corrective, pending manual CI**
Date: 2026-09-21

## CI #665 evidence
Manual CI #665 / run `35546264450` executed producer `01b2371310fb872eb1583231728941beb93c1a8e`.

Passed:
- Unit tests + Lint + APK build;
- API 36 emulator regression;
- unsigned artifact checksum verification.

Unsigned artifact identity:
- source SHA: `01b2371310fb872eb1583231728941beb93c1a8e`
- package: `studio.guitarlab.app`
- versionName: `0.5.0-rc4`
- versionCode: `24`
- unsigned APK SHA-256: `343bd7423275481039fc0475527bcb6a22c259a69136a5e052b6adbe5b0421f2`

## Failure
The signed homologation job failed at **Verify release candidate provenance and identity**, before restoring the keystore or signing anything.

The raw checkout contains the pre-materialization base metadata `0.5.0-rc3` / versionCode `23`. The tested APK was correctly built from the deterministic H37b materialized source as `0.5.0-rc4` / versionCode `24`. The signing job compared those two different source representations and failed.

This was a release-pipeline provenance bug, not a signing-secret, keystore, certificate or APK-integrity failure.

## Corrective
H37c adds a dedicated `Materialize exact source identity` step to the signed homologation job immediately after checkout:

```bash
bash scripts/materialize_ci_sources.sh
git diff --check
```

Only after deterministic materialization does the job compare source SHA, package, versionName and versionCode against `RELEASE_BUILD_IDENTITY.txt`.

The tested unsigned APK is not rebuilt or modified. The job continues to sign only the exact artifact produced by the already-passed software gate.

CI remains manual-only. CI #663 remains the latest signed DIGITAL PASS until a fresh manual run completes all three gates.
