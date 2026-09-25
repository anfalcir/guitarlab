# M5.C foundation checkpoint — structural editing + rotation safety

This checkpoint implements the first software gate defined by `M5C_UX_IMPLEMENTATION_PLAN.md` before the recording coordinator/UI batch.

Implemented:
- app navigation route is persisted with `rememberSaveable`, so an open Studio project and contextual Options/diagnostic destinations survive Android configuration recreation such as landscape ↔ portrait;
- clip metadata can move between tracks without copying source media;
- clip duplication creates a new clip identity while intentionally sharing the same immutable managed source;
- split-at-playhead core operation creates contiguous left/right timeline ranges and advances the right source offset exactly;
- track reordering moves stable track identity and therefore carries all track content without rewriting clip references;
- bounded `ProjectHistory` stores project metadata snapshots only and provides deterministic Undo/Redo primitives without touching managed audio bytes;
- unit tests cover media-reference invariants, split boundaries, track ordering, history capacity and redo invalidation.

Not claimed by this checkpoint:
- drag gesture UI, overflow menu, Undo/Redo transport buttons and source metadata dialog are not wired yet;
- M5.C capture coordination/countdown is not wired yet;
- no new physical homologation claim is made until the complete alpha08 gate passes CI and Samsung SM-X230 + Pocket Amp testing.
