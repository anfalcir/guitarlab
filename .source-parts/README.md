# Source materialization payloads

This directory contains the versioned payloads used by the CI source materializers.
They are intentionally kept in the repository: the materializer scripts apply these
patches in a fixed order and verify their SHA-256 and terminal Git blobs before a
build proceeds.

Naming conventions:

- `H*` — historical GuitarLab hardening and release-line payloads;
- `U*` — unified GuitarLab/GBW integration payloads;
- `C*` — unified product-cohesion payloads;
- `.partNN` and `.b64` files — split or encoded payloads used when a patch is large;
- plain `.patch`/`.gz` files — directly consumable payloads.

Do not edit a payload in place. Add a new corrective payload and materializer step
when a source change must remain reproducible.

## Frozen baseline note

The accepted RC20 source tail is U12bx, but this README intentionally does not hardcode the active materializer filename as operational authority; use `docs/CI_PIPELINE.md` and `scripts/materialize_ci_sources.sh`.

After freeze, do not edit an existing payload merely for cleanup or dependency freshness. A real maintenance source change adds a new corrective payload/materializer step, preserving the reproducible history of the accepted baseline.
