# U1a — Kotlin compile corrective

Canonical Android CI #667 materialized U1 successfully, then both unit and emulator jobs failed at Kotlin compilation in `UnifiedProjectDomain.kt`.

The compiler identified three source-boundary defects:

- `require(false)` inferred `Unit`/`Any` in a `GuitarProject` `when` expression;
- `asset.provenance` could not be smart-cast across the model-module API boundary;
- `project.preparation` could not be smart-cast across the same boundary.

U1a uses `error(...)`, whose return type is `Nothing`, and captures each nullable property through `let` before copying it. No schema, serialization, validation, revision or reachability behavior changes.

Local source-chain evidence:

- clean U1 → U1a application PASS;
- repeated materialization is idempotent;
- terminal `UnifiedProjectDomain.kt` Git blob is `27fedd8cb7c6f9d9ec04d83f8fee333f9062db53`;
- patch SHA-256 is `f6dc617e5bf17d3fc81976cd73686640bba17bb869b1d4bb45ca8464f420bc8a`;
- deliberate patch corruption is rejected before source mutation.

U1 remains SOURCE PRE-GATE until a new manual canonical workflow passes.
