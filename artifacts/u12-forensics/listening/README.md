# MMF — Misery diagnostic listening package — HISTORICAL RC19 FORENSICS

These files are local diagnostic evidence from the rejected RC19 `demucs.cpp` investigation and are intentionally not release artifacts. They do **not** describe the current production backend. RC20 replaced this inference path with the qualified official PyTorch/Demucs `htdemucs_6s` worker; see `../../../docs/RELEASE_BASELINE.md`.

## `single8-exact`

Exact local replay of the RC19 incident-era production image/model/strategy and v2
renderer using the preserved M4A source. This variant reproduces the systematic
negative DC introduced by every Demucs stem.

## `single8-dcblock10`

The same six `single8` stems with a 10 Hz DC blocker applied independently to
each stem before the unchanged v2 backing mix/shared-gain render.

Programmatic result:

- zero non-finite samples;
- identical rate/channel/frame contract;
- backing mean/DC approximately zero;
- guitar mean/DC approximately zero;
- recombined peak approximately `-1.004 dBFS`;
- no automatic claim of musical acceptance.

Listen to `backing.wav` and `guitar.wav` separately first. `recombined.wav` is
provided only to compare the reconstructed full mix. Human listening approval is
required before this corrective can be considered viable.
