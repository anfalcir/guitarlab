# U7 cloud backend consolidation

Updated: 2026-09-22
Status: CLOSED / DIGITAL PASS

## Closure evidence

U7 closed on 2026-09-22 with cloud source SHA
`e459e5e413ab2dea00b61a9f2f60375d9823e048`:

- hosted source/container verification: U7 Cloud Backend run `35675871322` PASS;
- WIF was narrowed to the immutable GuitarLab repository id, `main` and the
  exact U4/U7 workflow refs while retaining the frozen GBW rollback path;
- shadow deploy: run `35676232948` PASS, execution
  `guitarlab-demucs-shadow-q58kx`, six-stem manifest/integrity, source cleanup
  and idempotent result purge PASS;
- production cutover: run `35677341933` PASS, execution
  `gbw-demucs-p9mzx`, digest-pinned worker, Firebase rules/Functions, runtime
  contract, six-stem manifest/integrity, source cleanup and idempotent result
  purge PASS;
- the production workflow captured the prior image before mutation and retains
  an automatic image-restore path. No rollback was invoked because every
  post-cutover check passed;
- Android CI run `35675370500` PASS at port source
  `42abc548d06a2fb794421c7f75c3ad502b4d9484`, covering unit/Lint/build and the
  complete API 36 regression before the later workflow-only hardening commit.

The production manifest evidence SHA-256 is
`8eee1e249e4424aba414d54b8edca3d0cf77c0659c7f662116da0d033658f180`.
Image digests remain in the restricted workflow artifact rather than being
duplicated in product documentation.

## Source authority

`cloud/remote-separation/` is the GuitarLab-owned copy of the frozen GBW RC5
backend at `48af553518c44b46b913aad73c22e3da04368ea5`. The initial port preserves
the worker, Functions policy/control plane, Firebase rules, schemas, deployment
scripts and tests byte-for-byte except for ownership documentation, package
identity and the parameterized Cloud Run job name.

Existing `GBW_*` environment keys, `gbw-demucs`, bucket paths and schema fields
remain compatibility contracts for the live production plane. They are not
shown in GuitarLab primary UX and are not evidence of a second application.

## Deployment stages

1. `verify`: Python worker tests, Functions tests/build, JSON schemas, shell
   syntax, secret-pattern audit and a local container build.
2. `shadow`: build a digest-pinned image and deploy only
   `guitarlab-demucs-shadow`; never update Functions/rules or production job.
3. `production`: record the exact prior `gbw-demucs` image, deploy the newly
   built digest plus Firebase rules/Functions, verify the runtime contract and
   automatically restore the prior image if post-deploy verification fails.
4. Run the real six-stem smoke and retain its execution/result evidence before
   U7 can close.

Both cloud modes require manual `workflow_dispatch`, the immutable GuitarLab
repository/owner IDs, `main`, the exact workflow ref, keyless WIF and an explicit
`DEPLOY` confirmation. Production additionally requires `PRODUCTION`.

## Security boundary

- no service-account JSON key, refresh token or client secret is stored;
- WIF keeps the frozen GBW workflow only for rollback and adds the accepted U4
  smoke plus the exact U7 workflow;
- Android/Firebase temporary separation remains distinct from direct Drive v3
  backup;
- `GBW_ALLOWED_UIDS` remains a GitHub secret consumed only during production
  Functions deployment;
- model identity, quota, isolation, temporary-object cleanup and result hashes
  remain enforced by the preserved RC5 contracts.

## Closure criteria satisfied

- verification workflow PASS on the exact GuitarLab source;
- shadow deploy + six-stem comparison PASS;
- production cutover and rollback readiness evidence PASS;
- post-cutover real-cloud smoke PASS;
- Android CI PASS proving no app/source regression;
- documentation updated with run IDs, image digests and exact source SHA.
