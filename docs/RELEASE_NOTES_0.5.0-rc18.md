# GuitarLab Studio 0.5.0-rc18 - Release Notes

Version: `0.5.0-rc18` / versionCode `38`

RC18 corrects the RC17 shadow gate outcome without reverting the real audio safeguards.
RC17 successfully removed the corrupt partitioned Demucs path and deployed the finite-safe
`demucs.cpp.main` / `single8` worker, but isolated shadow run `35988775722` failed during the
prepared-reference contract because the worker treated sampled source-versus-summed-stems SNR as a
hard acceptance rule. Cloud Run logs showed finite sequential output and a diagnostic SNR of
`-1.875 dB`; the failure was the acceptance criterion, not non-finite stem corruption.

The worker now keeps sampled reconstruction SNR in structured logs as a diagnostic only. Publication
still fails closed on objective signal and contract violations: invalid WAV format, rate/channel or
duration mismatch, missing stems, non-finite samples, invalid prepared deliverable count, and peak
ceiling violations after rendering backing/guitar references.

RC18 also bumps the backend policy revision to `rc18-q40-single8-prepared-v2` so production, shadow
and Android evidence cannot be confused with the failed RC17 shadow.

Required promotion gates remain unchanged: exact-source Android CI with signed homologation APK,
U7 verification, isolated shadow, controlled production deployment, U4 cloud integration smoke and
target-tablet physical acceptance of clean backing/guitar audio plus Studio resynchronization.
