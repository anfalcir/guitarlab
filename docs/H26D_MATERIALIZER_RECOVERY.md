# H26d — historical materializer recovery

Status: **SUPERSEDED by H26e**
Updated: 2026-09-16

H26d was a recovery attempt for the fragile H26c incremental patch metadata. It is retained only as historical diagnostic evidence; it is **not** the current materialization authority and future work must not return to H26c/H26d unless a new defect specifically requires historical investigation.

Historical H26d evidence:
- source patch SHA-256: `97fb62c177519bbb6b8790fe173639cbed2142994ee4fe0eec374c9aa1d37760`;
- intended final `BackupScreenInstrumentedTest.kt` Git blob: `8ebac066a3bef1b93ee0316cdb5dcc726fe4b056`;
- exact CI #646 H26b source snapshot → H26d: locally proven;
- full historical H25 → H26 → H26a → H26b → H26d chain: locally proven;
- corrupted H26d source-part: fail-closed before mutation.

The current `scripts/materialize_ci_sources.sh` instead uses H26e full-source replacement after H26b:
`… → H25 → H26 → H26a → H26b → H26e`.

H26e authoritative source:
- `.source-parts/H26eBackupScreenInstrumentedTest.kt.b64`;
- decoded SHA-256 `7f974aef7ad8b4052cd01b6a66ffe75cee78fb27c5e94585b9eda44e2bf322da`;
- final Git blob `8ebac066a3bef1b93ee0316cdb5dcc726fe4b056`.

CI #650 / run `35154021384` / producer `07c99155789774cb39f9b4382829f9e1d16649e3` passed end to end and is the current H26/H26e DIGITAL PASS authority.