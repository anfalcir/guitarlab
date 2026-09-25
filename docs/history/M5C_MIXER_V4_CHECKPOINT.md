# M5.C — Mixer V4 UX checkpoint

This checkpoint applies the mixer refinements raised during alpha08 Studio homologation without changing the established audio-mix semantics.

Implemented:
- mixer dock height increased from 246 dp to 294 dp (~19.5%), intentionally trading a modest amount of timeline height for clearer touch controls;
- track Arm no longer uses the ambiguous letter `R`; it uses the standard record-circle glyph, dim red when unarmed and vivid red when armed;
- Volume and Pan names plus their live values are integrated into compact control cards, removing dedicated value-only rows;
- Volume retains the existing -60…+12 dB range and 0 dB snap/reference;
- Pan is visually bipolar: center has zero directional fill, movement fills only from center toward left/right, with the center reference retained;
- accessibility descriptions are attached to the interactive Volume/Pan/Arm controls;
- Master uses the same compact integrated gain/value presentation.

Deliberately unchanged:
- persisted gain/pan behavior and playback math;
- M/S/Arm project semantics;
- live metering and clip-latch behavior;
- recording coordinator/countdown remains a separate M5.C gate.

Physical ergonomics (294 dp dock height, strip density, touch targets and remaining timeline area) remain subject to the next signed alpha08 tablet validation.
