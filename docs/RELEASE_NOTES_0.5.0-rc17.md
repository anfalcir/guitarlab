# GuitarLab Studio 0.5.0-rc17 — Release Notes

Version: `0.5.0-rc17` / versionCode `37`

RC17 replaces the remote inference strategy after the RC16 shadow gate exposed a concrete
numerical defect in the upstream partitioned Demucs executable. In a controlled A/B reproduction
using the same pinned model and canonical input, `demucs_mt.cpp.main` / `mt4_omp2` emitted 154,350
non-finite samples in every stem; `demucs.cpp.main` / `single8` emitted zero.

The worker image now contains only the continuous sequential executable, uses all eight allocated
BLAS/OMP threads and rejects any runtime configuration that attempts to restore partitioned
inference. The manifest schema, deployment postcondition and backend policy revision all pin
`single8`. Existing finiteness, peak and sampled reconstruction checks remain mandatory, so an
invalid separation cannot be published to Android.

RC17 retains RC16's strict Android decoding/canonicalization and idempotent Studio synchronization
fixes. It requires exact-source Android CI and U7 verification, a successful isolated shadow run,
controlled production deployment/U4, and physical listening plus delete/reinsert acceptance before
promotion.
