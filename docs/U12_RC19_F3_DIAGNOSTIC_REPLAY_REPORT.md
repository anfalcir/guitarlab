# U12 RC19 — F3 isolated diagnostic replay report

Date: 2026-09-24
Execution: local, isolated, no Firebase/Firestore/project mutation

## Identity

- preserved source: M4A SHA-256 `85cdef6e6bd5671e320fc503db582f23eecd1a75097c5cc2b5994f8e693d9ec2`;
- canonical input SHA-256: `440c8e022c78d2290637293b42246c50ab306b5e0328f9efafbf3b0be6e3beb7`;
- production image digest: `sha256:a70bd221ab50ef092508781c609cbfbecfe6b71ba4c8a722fc5f087b09dbd550`;
- engine revision: `f1206e9adeea103aef4a636b9e62297cf1f8e34e`;
- model SHA-256: `09704f4ceae204e56e77d5eefd6ac71d7275be81fd507e6913371d59abcee856`;
- strategy: `single8`.

The source is the M4A source generation captured from diagnostic job
`e7b570f2-6f1c-4113-9e28-be27fce3a5bd`. The earlier physical Misery job used
the WEBM source SHA `87c052...`; results are therefore comparable by boundary
behavior, not claimed byte-identical to that earlier job.

## Boundary localization

Canonical input mean/DC was only `-0.00030451 / -0.00050812`.

Every raw/normalized Demucs stem acquired a similar negative DC component:

| Stem | Left mean | Right mean |
|---|---:|---:|
| drums | -0.01533978 | -0.01568958 |
| bass | -0.01530522 | -0.01474309 |
| other | -0.01335314 | -0.01381874 |
| vocals | -0.01476676 | -0.01481838 |
| guitar | -0.01468050 | -0.01475814 |
| piano | -0.01455877 | -0.01444522 |

The unchanged renderer then summed five contaminated backing stems. Final exact
replay means were backing `-0.06386502 / -0.06403169` and guitar
`-0.01278674 / -0.01285436` after shared gain. This matches the physical pattern.

Root-cause boundary: **the pinned demucs.cpp inference output introduces the DC
into every stem**. Input canonicalization, Android publication and the v2 renderer
do not originate it. The renderer predictably amplifies the backing symptom by
summing five affected stems.

## Diagnostic corrective candidate

A 10 Hz high-pass/DC blocker was applied to each stem before the unchanged v2
render. Result:

- backing mean/DC: `-4.3e-8 / +2.0e-7`;
- guitar mean/DC: approximately `0 / +3.2e-9`;
- zero non-finite samples;
- recombined peak: `-1.00446 dBFS`;
- frame/rate/channel contract preserved.

This is a diagnostic candidate, not an approved production correction. Listening
acceptance is mandatory and the blocker must not hide wider separation-quality
defects.

## Strategy/performance decision

`single8` fixed the non-finite-output failure of the prior partitioned
`mt4_omp2` path but did not produce acceptable physical audio and increased
runtime. A direct rollback to the known non-finite strategy is prohibited.
Follow-up qualification must compare:

1. `single8` plus the proven DC corrective;
2. a known-good reference implementation/baseline;
3. any corrected parallel strategy only in diagnostic/shadow mode.

Each alternative must pass finiteness, stage metrics, representative musical
quality, deterministic output and measured runtime before production promotion.
