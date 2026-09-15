# H22 — Semantic Physical Audio Routes and Practice-Bar Action Emphasis

## Physical evidence
Physical review of CI #633 on Samsung SM-X230 showed that Android/OEM routing still leaked low-level endpoints into the selectors. Examples included `remote-submix • hsp:...`, repeated `SM-X230` entries and address suffixes such as `0`, `back` and `bottom`.

The defect was not merely naming: H20 canonicalized only a narrow set of built-in speaker types, while Samsung also publishes physical-device endpoints through other logical classes such as `TYPE_BUS`; recording inputs were not canonicalized at all.

## H22 route contract
The user selects a physical audio source/destination, never an Android endpoint.

- Internal recording endpoints belonging to the tablet collapse to `Microfone do tablet`.
- Internal playback endpoints belonging to the tablet collapse to `Alto-falante do tablet`.
- System-only routes such as `TYPE_REMOTE_SUBMIX` and telephony are hidden.
- USB endpoints representing the same physical interface collapse to one device choice; USB card identity is retained where available so two genuinely separate equal-model interfaces can remain distinct.
- Android addresses are retained only internally for resolution/migration and never rendered as labels.
- Bluetooth, HDMI, wired headset/earpiece and other materially distinct transports retain clear semantic labels.
- Existing raw endpoint selections and route2 canonical selections migrate to route3 canonical identities.
- Output duplicates continue to resolve by silent probe plus `routedDevice`; input duplicates are resolved to a stable compatible candidate without exposing candidates to the UI.

## H22 practice-bar contract
The user-requested visual inversion is explicit:
- group/title background is neutral;
- title accent remains in text/indicator/border;
- inactive action buttons receive the group's accent as a subtle container fill inside their own border;
- selected comparison mode remains a solid accent button;
- Comparação = secondary/blue family, Ajustes = primary/teal family, Timeline = tertiary/amber family.

This makes a title read as a title and each action read as a bounded control.

## Regression requirements
- Input policy collapses built-in mic logical endpoints.
- Output policy collapses built-in speaker logical endpoints.
- `remote-submix` is absent from both user-facing lists.
- USB endpoints on one card collapse even when endpoint/device address suffixes differ.
- Different USB cards remain distinct.
- route2/raw stored selections migrate to route3.
- Existing H18/H18a containment and H21 visual geometry remain green.
- Physical tablet verification confirms no technical endpoint labels are visible and each useful physical device appears once.
