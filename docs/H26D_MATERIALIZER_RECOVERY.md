# H26d — materializer recovery

H26d supersedes the broken H26c materialization metadata while preserving the H26 product implementation.

- Source patch SHA-256: `97fb62c177519bbb6b8790fe173639cbed2142994ee4fe0eec374c9aa1d37760`
- Final `BackupScreenInstrumentedTest.kt` git blob: `8ebac066a3bef1b93ee0316cdb5dcc726fe4b056`
- Exact CI #646 H26b source snapshot → H26d: PASS
- Full H25 → H26 → H26a → H26b → H26d chain: PASS
- Idempotent rerun: PASS
- Corrupted H26d source-part: fail-closed before mutation

CI #648 failed before Gradle/tests because the prior H26c expected patch hash did not match the published source-part. H26d bypasses the H26c source-part entirely and is the new materialization authority. CI #642 remains the last signed authority until a fresh H26d run passes end to end.
