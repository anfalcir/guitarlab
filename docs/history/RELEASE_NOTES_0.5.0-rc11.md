# Release Notes — GuitarLab Studio 0.5.0-rc11

Updated: 2026-09-23
Status: **DIGITAL PASS — U12 PHYSICAL HOMOLOGATION PENDING**

## Candidate identity
- package: `studio.guitarlab.app`
- versionName: `0.5.0-rc11`
- versionCode: `31`
- exact producer: `34cb60624b2fabf21cfe2c60003b04eac1597418`
- Android CI #806 / run `35885021871`: PASS
- U4 Cloud Integration Smoke #123 / run `35885021902`: PASS
- U7 Cloud Backend #81 / run `35885021969`: PASS
- signed artifact: `GuitarLabStudio-0.5.0-rc11-homologacao`
- artifact id: `10762204176`
- signed APK SHA-256: `3cd9bfe06c9aa1af7f452f0dd8d9e9bead50774bde4a3b22c72f09f51e5ea769`
- signed APK size: `79,965,840` bytes
- artifact ZIP SHA-256: `9f9ad8e16222c4eac1d8dcc27d7946e75e54be6a9a9bfcd243beb0284e14e7b2`
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`

## Why rc11 exists
Physical rc9 diagnostics showed a new Prepare job stuck locally in `UPLOADING`/retry before any Storage object, Firestore document, Firebase callable or Cloud Run execution existed. Android bugreports proved WorkManager was executing the worker and immediately returning RETRY.

The first cloud-auth reproduction returned `ADMIN_ONLY_OPERATION` for anonymous signup. After anonymous auth was temporarily enabled for diagnosis, a real anonymous client could authenticate but the backend correctly rejected its random UID because remote Functions enforce `GBW_ALLOWED_UIDS`.

Historical GBW source established the intended contract: the owner signs in with one personal Firebase Email/Password account, and that account's stable UID is allowlisted. The GuitarLab migration had inadvertently replaced that identity with anonymous auth.

## rc11 corrective
- restores Firebase Email/Password login for remote separation;
- removes anonymous login from the Android remote-separation path;
- adds Opções → Conta e nuvem login UI;
- does not persist the password in GuitarLab; only the Firebase session survives successful login;
- validates the authenticated account against the backend before accepting the session;
- blocks separation enqueue when no stable authorized session exists;
- U7 enforces Email/Password enabled + Anonymous disabled and verifies every `GBW_ALLOWED_UIDS` account exists, is enabled and has the password provider;
- preserves the backend allowlist rather than weakening authorization.

## Firebase/worker hardening retained from rc10
- typed Firebase Auth, Firestore, Functions and Storage failures;
- durable pipeline-stage diagnostics for authentication, remote status, upload and enqueue;
- retry only for transient classes such as network/unavailable/deadline failures;
- bounded worker/cancel retry;
- explicit retry and terminal UI copy rather than indefinite “Preparando…”;
- source remains preserved on permanent remote failure.

## Activity lifecycle corrective
- deleted/missing projects can no longer leave a permanent active Activity card;
- stale project-scoped active records are terminalized;
- `Limpar resolvidos` removes only succeeded/cancelled records;
- failed, retrying, queued and running records remain visible for diagnosis/resolution.

## Digital qualification
Android CI #806 passed:
- unit tests;
- Android Lint;
- debug/release assembly;
- full API 36 grouped instrumentation;
- the explicit Email/Password cloud-account Settings surface;
- signed homologation APK generation and certificate verification.

U4 #123 passed the real six-stem Cloud Run contract.

U7 #81 passed:
- Email/Password Firebase configuration enforcement;
- Anonymous Authentication disabled;
- allowlisted password-backed user validation;
- Functions/schema/script/security checks;
- worker container build.

## Physical acceptance still required
Do not promote rc11 to final release until the owner explicitly passes U12 on the target Samsung SM-X230/MK-300 setup, including:
- account login/authorization;
- real source upload → Function → Cloud Run → six-stem import;
- pending/reboot/cancel reconciliation;
- Activity cleanup behavior;
- USB routing/isolation and no microphone fallback/bleed;
- recording feedback/timing/listening;
- reconnect behavior;
- representative continuous session and final ergonomics.
