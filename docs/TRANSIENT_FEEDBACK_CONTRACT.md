# Transient Feedback Contract

Updated: 2026-09-16

This is the product contract for Toast/Snackbar-like feedback in GuitarLab. New features must use this contract instead of adding ad-hoc transient messages.

## Principle
Transient feedback is scarce attention. If the result is already obvious in the owning UI, do not duplicate it in a Snackbar or Toast.

## Classification matrix

| Class | Correct channel | Duration / behavior | De-duplication | Allowed examples | Prohibited examples |
|---|---|---|---|---|---|
| **Error** | Snackbar for recoverable screen-level errors; inline field error when local to a field; blocking dialog only when recovery requires an explicit decision | Short by default; persistent UI if the condition itself persists | Do not suppress materially different errors | import/export failure, unavailable required input, operation failed | raw stack/driver/device identifiers |
| **Warning / meaningful degradation** | Snackbar plus persistent owner-state when degradation remains active | Short Snackbar; persistent state in owner UI | Identical continuing warnings use cooldown; Studio default 30 s | recording continues without backing after output loss | route-selection success, ordinary fallback that is already visually represented and has no user consequence |
| **Async success** | Snackbar only when completion is non-obvious and useful | One short completion message | Collapse duplicate completion events for the same operation | export/master/project package completed | Play started, Cut mode entered, REC started |
| **Persistent state** | Owning screen/component, icon, label, meter or control state | Visible while state is true | Not applicable | Playing/Paused, Armed, Muted, Solo, selected route, recording state | mirroring every state change in Snackbar |
| **Progress** | Owning screen/component progress indicator/status | Updated in place; removed on completion/error | Not applicable | import %, waveform generation, `Medição 3/5`, export progress | repeated Toast/Snackbar progress ticks |
| **Confirmation** | Dialog for destructive/irreversible action; inline confirm affordance when appropriate | Remains until explicit user action | Not applicable | delete project/track, destructive clear | destructive action confirmed by Snackbar alone |
| **Diagnostic-only** | Audio Diagnostics / technical logs | As needed in diagnostic context | Not applicable | device id, endpoint address, raw `AudioDeviceInfo`, clock details | any low-level identifier in normal Studio transient UX |

## Routine actions that must stay transient-silent
No Snackbar/Toast for successful:
- Play, Stop, Pause;
- entering/exiting CUT/Trim or another editing mode;
- REC countdown, start, normal capture state or normal take completion when the take is visible;
- mute, solo, arm;
- selection, navigation, panel open/close, seek/playhead movement;
- route-selection success;
- loop toggle;
- routine Undo/Redo when the visible project state already demonstrates the change.

`clipStatus`, `importStatus`, `exportStatus` and equivalent owner-state fields may still be used **inline** when they communicate persistent state or progress. They do not automatically qualify for Snackbar display.

## Audio identity sanitization
Normal transient feedback must never expose directly:
- `deviceId` or device index;
- endpoint address / endpoint index;
- raw `AudioDeviceInfo` or `productName=...` representation;
- `route2:...`, `route3:...`;
- `remote-submix`;
- `hsp:...`;
- internal suffixes such as `• 0`, `• back`, `• bottom`;
- any other Android/OEM low-level routing identity.

Preferred behavior:
1. use the H22 semantic physical-route layer (`Microfone do tablet`, `Alto-falante do tablet`, `MK-300`, friendly Bluetooth name);
2. if a raw technical error reaches the transient boundary and no reliable semantic substitution is available, use a safe generic user-facing fallback;
3. preserve raw details only in diagnostics/logging.

H23b applies the sanitizer to both centralized transient notices **and raw Studio error messages** before Snackbar display.

## Warning cooldown
Repeated identical warnings from a continuing condition use cooldown. H23/H23b default Studio warning cooldown is 30 seconds. A materially different error/warning is not suppressed merely because another message was recently shown.

## Review policy for future code
Before adding any transient message, answer:
1. Is the result already visible in the owning UI?
2. Is this an Error, meaningful Warning/Degradation, or non-obvious Async Success?
3. Does the message contain implementation/device details that belong only in diagnostics?
4. Can the condition repeat rapidly and need cooldown/de-duplication?
5. Is a dialog or persistent owner-state more appropriate?

If #1 is yes and #2 is no, do not add a Snackbar/Toast.

## Test policy
Central policy tests must continue to assert:
- `OPERATIONAL_STATUS` is not Snackbar-eligible;
- Error, Warning and Async Completion are eligible;
- semantic route labels remain unchanged;
- representative low-level route identifiers are rejected/sanitized;
- normal Studio display passes both notices and errors through the policy boundary.
