# GuitarLab remote separation backend

This tree was absorbed from the frozen GBW RC5 production baseline
`48af553518c44b46b913aad73c22e3da04368ea5`. GuitarLab now owns its source,
tests and deployment. Existing `GBW_*` environment variables, Google Cloud
resource names and manifest fields remain stable infrastructure contracts so a
source migration does not become an unsafe data/control-plane migration.

This tree moves only Demucs inference to a private Cloud Run Job. Android projects,
exports and SAF/Drive backup remain local. Firebase Storage is a temporary transport.

Components:

- `worker/`: pinned demucs.cpp container and commit-marker manifest;
- `functions/`: authenticated, allowlisted, quota-limited control plane;
- `firebase/`: deny-by-default Firestore and Storage rules;
- `schemas/`: versioned job/result contracts;
- `scripts/`: reproducible provisioning, deploy and smoke commands.

The normal cleanup path is Android durable import → ACK → immediate purge. Bucket
lifecycle at one day is only an orphan safety net. No service-account key is used.

Deployment is controlled by `.github/workflows/u7-cloud-backend.yml` after the
one-time owner-operated WIF bootstrap in `scripts/bootstrap_u7_github_wif.sh`.
The workflow verifies the complete backend first, supports an isolated shadow
job, records the previous production image digest and rolls back that image if
post-deploy verification fails. No service-account key is stored in GitHub.


Production RC17 inference contract after the RC16 shadow audio-integrity failure:

- Cloud Run Job: 8 vCPU / 16 GiB;
- binary: `demucs.cpp.main`;
- strategy: `single8` (one continuous inference × 8 BLAS/OMP threads);
- model: pinned `htdemucs_6s`;
- monthly logical allowance: 40 accepted jobs per authorized user;
- policy revision: `rc17-q40-single8-prepared-v2`.

The upstream partitioned `demucs_mt.cpp.main` path is deliberately absent from the runtime
image. A controlled A/B reproduction with the same model and canonical input produced 154,350
non-finite samples per stem in `mt4_omp2`, while `single8` produced zero. Output finiteness,
peak and sampled reconstruction checks remain mandatory before any result is published.
