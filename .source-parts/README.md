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
