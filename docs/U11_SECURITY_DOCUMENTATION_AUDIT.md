# U11 — Security and Documentation Freeze Audit

Updated: 2026-09-22  
Status: **PASS — PRE-FREEZE**  
Candidate identity: `0.5.0-rc5` / versionCode `25`  
Package: `studio.guitarlab.app`

## Scope

This audit is the pre-freeze evidence required by U11 before the exact signed candidate is triggered.

It covers:
- live-authority documentation consistency;
- deterministic materialization ready-state;
- credential/secret hygiene;
- Drive OAuth scope;
- Firebase client configuration classification;
- tuner exclusion;
- final U11/U12 evidence boundary.

The final freeze SHA is intentionally not recorded here until the canonical gate is triggered.

## 1. Deterministic source/materialization

The exact U10 source artifact from Android CI #781 was downloaded and audited.

Source artifact:
- id `10722206126`;
- digest `sha256:e17472db3fc95fe2ae2e49c56571f06f72a0465c4fbbeecb0ad14e7c0d02210f`.

After restoring a local Git worktree around that exact artifact, the canonical entrypoint completed in ready-state:

`Source patch chain already materialized through U10zb adaptive Studio ruler assertion`

Result: **PASS**.

The active repository entrypoint remains `scripts/materialize_ci_sources.sh` → U10zb.

## 2. Credential and secret hygiene

Repository/source scan checked for:
- JKS/keystore/P12/PEM files;
- `local.properties`;
- `google-services.json`;
- private-key PEM headers;
- service-account private-key JSON;
- client secrets;
- refresh tokens;
- GitHub personal access tokens;
- AWS access-key patterns;
- OpenAI-style secret-key patterns;
- literal password/secret assignments.

Result:
- no committed signing keystore or private-key file found;
- no committed client secret found;
- no committed refresh token found;
- no committed service-account private key found;
- no literal signing password found;
- signing workflow references repository secrets only;
- Google Cloud workflows contain only the service-account **email identifier** and authenticate keylessly through workload identity federation.

Result: **PASS**.

## 3. Firebase client configuration

The Android remote-separation runtime contains a Firebase client Web/API key as part of `FirebaseOptions`.

Classification:
- it is client-side Firebase project configuration required by the Android runtime, not a private authentication credential;
- it is expected to be recoverable from a client APK and is not treated as a server secret;
- authorization is not granted by possession of that value;
- Firestore/Storage access is protected by authenticated-user/security-rule contracts and backend ownership;
- no Firebase Admin/service-account credential is present in the Android source.

The U11 backend verification still performs its own secret-hygiene scan on the exact freeze SHA.

Result: **PASS WITH EXPLICIT CLASSIFICATION** — no private credential leakage found.

## 4. Drive OAuth scope

The source/docs scan found the Drive authorization URI only as:

`https://www.googleapis.com/auth/drive.file`

No broad `https://www.googleapis.com/auth/drive` release scope was found.

The Drive path does not embed:
- client secret;
- refresh token;
- service-account key;
- backend token custody.

Result: **PASS**.

## 5. Tuner exclusion

Application/core/platform source was scanned for tuner/pitch-feature indicators.

The only matching runtime strings were Android hardware type labels:
- `TYPE_FM_TUNER`;
- `TYPE_TV_TUNER`.

These are generic Android audio-device classifications and do not implement a guitar tuner.

No GuitarLab guitar tuner, pitch-detection workflow or automatic tuning feature was found.

Result: **PASS**.

## 6. Live documentation consistency

The live authority set was fetched directly from `main` after U10 closure and checked for stale current-state claims.

Verified:
- U10/C8 = **CLOSED / DIGITAL PASS**;
- U11 = **ACTIVE**;
- U11 candidate = `0.5.0-rc5` / `25`;
- latest signed historical authority = CI #669 / rc4/24 until U11 passes;
- canonical materializer tail = U10zb;
- H37 Drive v3 = closed/absorbed into U8 production path, not a pending current pre-gate;
- U9 migration = retired;
- U12 = consolidated physical-only residual campaign;
- no live authority says the active source tail ends at U2 or U8m;
- no live authority says U10 is still active;
- no live authority treats CI #663 as the current signed authority.

Canonical U11/U12 documents:
- `docs/U11_FINAL_DIGITAL_RELEASE_GATE.md`;
- `docs/U12_FINAL_PHYSICAL_HOMOLOGATION.md`.

Result: **PASS**.

## 7. Remaining exact-source verification

The final freeze commit must still prove, on its own SHA:
- Android software/API36/signed candidate gate;
- real-cloud U4 six-stem smoke;
- U7 backend tests/schema/script/secret-hygiene/container verification.

The final trigger must not deploy the U7 backend from a push event.

## Conclusion

**U11 pre-freeze security/documentation audit: PASS.**

No blocking credential leak or live-document contradiction remains. The repository is eligible for the one exact rc5 freeze trigger, subject to final HEAD verification.
