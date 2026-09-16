# Transient Feedback Contract

Updated: 2026-09-16

This is the product contract for Toast/Snackbar-like feedback in GuitarLab. New features must follow it instead of adding ad-hoc transient messages.

## Principle
Transient feedback is scarce attention. A routine action that is already visible in the UI must not create another message.

## Allowed Snackbar classes
1. **Error** — an operation failed and the user needs to know.
2. **Meaningful warning/degradation** — the requested operation continues in a materially degraded mode, for example recording continues without backing after route loss.
3. **Asynchronous completion** — a long-running operation finishes and its result is not otherwise sufficiently obvious, for example project/master export completion.

## Not allowed as Snackbar
- Play/Stop/Pause success;
- entering/exiting CUT/Trim or another editing mode;
- REC countdown, `Gravando`, `Finalizando take` or normal take completion when the resulting clip is visible;
- mute/solo/arm state changes;
- navigation, panel opening/closing, selection, seek and loop toggles;
- routine Undo/Redo success when the visible project state already changed;
- route-selection success;
- normal import/analysis/render progress that already has an inline progress/status surface.

## Placement rules
- **Progress/state** belongs inside the component/screen that owns the operation.
- **Destructive confirmation** uses a dialog.
- **Field validation** stays beside the field when possible.
- **Error/warning/completion** may use Snackbar according to the allowed classes above.
- **Diagnostics** may expose technical identifiers; normal product surfaces may not.

## Audio identifier privacy/UX rule
Normal feedback must never expose raw Android routing details such as device ids, endpoint addresses, `remote-submix`, `hsp:...`, `route2:...`, `route3:...`, or internal suffixes such as `• 0`, `• back`, `• bottom`.

The user-facing label must come from the semantic physical-route layer (`Microfone do tablet`, `Alto-falante do tablet`, `MK-300`, Bluetooth device name, etc.). If an error string contains a low-level identifier and no semantic replacement is available, show a safe generic fallback and leave the raw details to Audio Diagnostics.

## De-duplication
Repeated identical warnings from a continuing condition use a cooldown. H23 defaults to 30 seconds for Studio warnings. A new error with materially different meaning is not suppressed.

## Duration
Default to a short Snackbar. Long-lived conditions should be represented persistently in the owning UI, not by extending Snackbar duration.

## Review checklist for future work
Before adding transient feedback, answer all four questions:
1. Is the operation already visibly reflected in the UI?
2. Is this an error, meaningful degradation, or non-obvious asynchronous completion?
3. Does the text contain implementation/device details that should stay in diagnostics?
4. Could the same condition repeat rapidly and therefore need de-duplication?

If question 1 is yes and question 2 is no, do not add a Snackbar.
