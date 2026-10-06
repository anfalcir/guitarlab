# CUE automatic output negotiation — 2026-10-06

## Goal

Keep the project/timeline sample rate authoritative while allowing a physically distinct CUE sink
to use a different rate when Android/OEM routing requires it. The field case that motivated this
change is a 44.1 kHz project where MAIN advertises 44.1/48 kHz and the wired CUE endpoint advertises
only 48 kHz.

## Runtime policy

- MAIN stays at the project sample rate.
- CUE preflight orders candidate rates from the endpoint capabilities.
- A non-empty CUE sample-rate list is treated as a strong capability hint; unsupported session rates
  are not tried before advertised alternatives.
- Every candidate still has to pass the existing silent physical-route and clock proof.
- A successful pair is cached only in-process and keyed by MAIN physical identity, CUE physical
  identity and project rate.
- Topology or MAIN-selection changes invalidate the cached proof.
- Playback re-proves the fresh runtime routes before musical audio.
- If CUE uses another physical rate, only the CUE mix is linearly resampled in real time. Project
  files, edit proxies, timeline coordinates, recording rate and MAIN output are unchanged.
- CUE clock progress is normalized back to project-rate frames before drift policy is evaluated.
- Any partial write, route convergence/change or excessive drift still fails closed by silencing CUE.

## Diagnostics

CUE preflight schema v2 records attempted CUE sample rates, the negotiated rate and whether runtime
resampling is required. Existing routed-device transition evidence remains authoritative.

## Version

This implementation advances Android homologation identity to 0.5.0-rc30 / versionCode 50.

## Remaining physical qualification

CI/JVM tests can validate negotiation ordering, resampler duration accounting and integration
contracts, but only hardware can prove that a specific OEM exposes two independent physical sinks.
The next physical test should repeat MAIN=speaker + CUE=wired on the SM-X230 and inspect whether the
preflight negotiates 48 kHz and reaches SUPPORTED. If it still converges to MAIN at 48 kHz, treat that
pair as an OEM/HAL limitation rather than silently forcing CUE.
