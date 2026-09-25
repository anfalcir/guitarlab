# GuitarLab remote separation backend

Updated: 2026-09-25  
Current release state: **RC20 frozen**

This tree originated from the GBW backend baseline, but GuitarLab now owns its source, tests and deployment. Existing `GBW_*` environment variables, Google Cloud resource names and compatible manifest fields remain infrastructure contracts where changing them would create unnecessary control-plane risk.

Exact frozen production identity is recorded in `docs/RELEASE_BASELINE.md`.

## Responsibility boundary

Only remote source separation runs in this backend. Android projects, Studio editing/recording, exports and direct Google Drive v3 backup remain client-side concerns. Firebase Storage is temporary transport, not durable project backup.

Components:

- `worker/`: official PyTorch/Demucs inference, quality validation and Prepared References v2 rendering;
- `functions/`: authenticated, allowlisted, quota-limited control plane;
- `firebase/`: deny-by-default Firestore/Storage rules and indexes;
- `schemas/`: versioned job/result contracts;
- `scripts/`: reproducible verification, deployment and real-cloud smoke tooling.

## Frozen inference contract

The accepted RC20 worker uses:

- engine: `demucs-pytorch`;
- Demucs `4.1.0`;
- PyTorch `2.14.0+cpu`;
- NumPy `1.26.4`;
- model: `htdemucs_6s`;
- model checkpoint: `5c90dfd2-34c22ccb.th`;
- checkpoint SHA-256: `34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd`;
- device: CPU;
- `--float32`;
- `--clip-mode none`;
- shifts `1`;
- overlap `0.5`;
- CPU threads `8`;
- canonical engine strategy: `pytorch-cpu-s1-o0.5-t8`;
- Cloud Run Job shape: 8 vCPU / 16 GiB / taskCount 1 / parallelism 1 / timeout 1800 s / maxRetries 0.

The exact production image is digest-pinned; never replace it by tag equivalence or rebuild during a promotion.

## Prepared References v2

Demucs produces six private worker-local stems: drums, bass, other, vocals, guitar and piano. They are intermediate inference data, not Android project assets.

The committed remote result contains exactly two aligned validated Float32 WAV references:

1. backing without guitar;
2. isolated guitar.

Publication verifies container/format, duration consistency, finite samples and objective amplitude/quality safeguards before the result becomes importable. Android independently verifies integrity and canonicalizes accepted project-managed references.

## Stochastic inference policy

Independent `--shifts 1` runs are not expected to be sample-identical. Exact engine/model/config/source identity and hard structural/finiteness/quality gates are blocking; cross-run numeric/hash differences are retained as evidence rather than universal vetoes under D-091.

## Lifecycle and cleanup

The normal lifecycle is source upload → enqueue → Cloud Run → validated result → durable Android import → ACK → immediate remote purge.

- same-generation recovery must not consume a second accepted quota/job;
- cancellation and retry exhaustion end in explicit terminal state;
- source and result cleanup are idempotent;
- bucket lifecycle is only an orphan safety net;
- no service-account key is stored in GitHub or Android.

## Deployment

`.github/workflows/u7-cloud-backend.yml` performs backend verification and keyless GitHub WIF authentication. Shadow qualification precedes production mutation when worker content changes.

Production promotion uses the exact prequalified image digest, records the previous production image and runs runtime verification plus the U4 transactional smoke. Failure must restore the captured rollback image.

The frozen RC20 worker does not require periodic rebuild/redeployment. Reopen only under the maintenance policy in `docs/PROJECT_IDENTITY.md` / D-090 / D-096.

## Rejected historical path

The RC16–RC19 `demucs.cpp`/GGML `single8` and partitioned experiments are historical forensic evidence only. They are not the current production engine and must not be described as the frozen baseline.
