# H28 — Backup identity and provider-consistency hardening

Status: **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**
Updated: 2026-09-16

## Trigger
Physical homologation of the signed H27 candidate from CI #651 showed that history semantics were improved but the remote commit path was still not robust enough for an eventually-consistent document provider.

Observed sequence:
- an empty backup destination was confirmed;
- one manual total backup physically created both project packages;
- one of those writes was nevertheless reported as unconfirmed;
- a second unchanged manual run duplicated only the revision that the previous catalog had failed to observe;
- the same false confirmation failure then moved to the other project.

This pattern is consistent with successful remote writes becoming visible through the provider's directory listing later than the direct document URIs returned by the write operation. H28 removes that listing-timing dependency from commit confirmation and deduplication.

## Identity model
### Project identity
`GuitarProject.id` is the canonical immutable project identity.

The display name is never used as project identity:
- rename preserves the same `projectId`;
- backup history therefore remains attached to the same project after a rename;
- an intentional Duplicate operation receives a new project ID;
- restore continues to create an independent local project with a new ID, by design.

This identity already existed in the project model. H28 formalizes it as the authoritative backup identity and adds regression tests so a future rename implementation cannot silently break that contract.

### Revision identity
H28 adds a persisted `revisionId` for every new backup revision:

`r_<projectUpdatedAtEpochMs>_<stateDigest>`

The timestamp is the persisted project edit time at millisecond resolution. The suffix is a truncated SHA-256 digest derived from:
- immutable `projectId`;
- `projectUpdatedAtEpochMs`;
- canonical serialized project state.

Using edit time alone would be insufficient because two distinct states could theoretically share the same timestamp. The state digest closes that collision class while keeping the revision ID readable and deterministic.

`revisionId` identifies the logical project revision. Package SHA-256 remains an independent integrity identity for the exact backup bytes. The deduplication key is therefore effectively `revisionId + package SHA-256`.

## Provider-consistency contract
### Deterministic remote path
New H28 version directories are deterministic:

`v_<revisionId>`

Retrying the same project revision targets the same logical remote location rather than creating another random directory.

### Direct commit verification
After writing metadata, package and commit marker, H28 validates the exact document URIs returned by the provider. It no longer requires an immediate parent-directory relist to prove that the write succeeded.

The package is re-read through its direct URI and verified by size + SHA-256. Metadata and commit marker are also re-read directly and validated as one commit contract.

### Settling lookup before a retry
If the initial catalog does not show the current revision, the coordinator asks the remote store for a targeted `(projectId, revisionId)` lookup before uploading again. The SAF implementation performs a short bounded settling poll. This absorbs normal provider-listing propagation delay without hiding a persistent failure indefinitely.

### Legacy compatibility
H26/H27 format-v1 backups do not contain `revisionId`. They remain readable and receive a deterministic legacy compatibility identity derived from `projectId + projectUpdatedAtEpochMs`.

The incremental policy accepts a matching legacy timestamp only for legacy-format backups. H28-format backups require the stronger revision identity.

## Retention and deduplication
H28 no longer collapses versions solely because their edit timestamp is equal.

Duplicate cleanup uses revision identity plus package hash. This means:
- repeated copies of the same exact revision/content collapse safely;
- two different H28 states created at the same timestamp are not incorrectly treated as duplicates;
- rename remains one project history because grouping is by immutable `projectId`;
- rename after a save is a new revision of the same project, not a new project.

## Regression coverage added
Focused tests cover:
- revision ID deterministic for the same project state;
- different state with the same timestamp produces a different H28 revision ID;
- rename preserves project ID while producing a new revision;
- duplicate project receives a new project ID;
- initial catalog lag followed by targeted revision discovery skips the duplicate upload;
- retention does not collapse different H28 states that share a timestamp;
- legacy v1 metadata remains readable and incrementally compatible.

## Source materialization
H28 source part:
`.source-parts/H28BackupIdentityConsistency.patch.gz.b64`

Decoded gzip SHA-256:
`1abd8101361b241dfb443950c2a635141b41fecca77442d86343eb3f3025d3be`

Decoded patch SHA-256:
`3d06aa851ad1dc88dd078d60bb24ea097bbca7ef4f3e93089f47c9bd4a0a6e71`

Canonical tail:
`… → H25 → H26 → H26a → H26b → H26e → H27 → H28`

Expected terminal message:
`Source patch chain materialized through H28 with verified final hashes`

Final H28 Git blobs verified by the materializer:
- `SafBackupRemoteStore.kt` — `54d6692d48976594105d1e451d211a76dd4150be`
- `BackupDomain.kt` — `4dc0ff93be80f82e5c48f783d5eb788c9388d028`
- `ProjectBackupCoordinator.kt` — `c501c4cbab0ecf4d1dd3f18a770c9ae786782a25`
- `BackupDomainTest.kt` — `027ffb19eeb63518675ab3756f382030ed4b1b4c`
- `FileProjectRepositoryTest.kt` — `2c69c9f7b7624bf1ae2bcd800eb207242ae6b4f6`
- `ProjectBackupCoordinatorTest.kt` — `aa4992d65ce314d1773a80140f3433da72cf6dbb`

## Source validation completed
- H27 baseline → H28 clean application PASS;
- exact final Git blob verification PASS;
- second materializer execution idempotent PASS;
- corrupted H28 source-part fails closed before source mutation;
- `git diff --check` PASS;
- `bash -n scripts/materialize_ci_sources.sh` PASS;
- focused Kotlin compilation checks for the backup domain/coordinator/SAF delta PASS.

Full Gradle/JVM/Lint/API36/release-signing execution is intentionally not claimed locally. A fresh manually dispatched workflow is required before H28 can become DIGITAL PASS.

## Evidence boundary
CI #651 / run `35166195527` / producer `0b6ae1e28214decbcfba622a38d90c0dcbe2acf9` is the signed DIGITAL PASS for H27, not H28.

Its signed APK SHA-256 is `d9ce720194812afcb281ecebd263d482d4320b4285f50044a2771fc6293736fe`.

Physical use of that exact APK exposed the provider-consistency defect described above. It remains valuable H27 digital evidence but is not the final backup candidate.
