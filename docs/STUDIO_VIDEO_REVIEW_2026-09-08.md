# Studio video review — 2026-09-08

Evidence reviewed: the supplied ~69s tablet recording of GuitarLab Studio plus the supplied DAW-style reference screenshot.

## Findings
1. The Studio is vertically stacked and requires page-level scrolling to reach core controls and project content.
2. Timeline rows, a separate Clips section and a separate Track structure section repeat the same project state in three places.
3. Permanent explanatory/status prose consumes high-value workspace and reads like diagnostics rather than a production Studio.
4. The time ruler is decorative/static instead of derived from actual project duration.
5. Loop boundary markers remain visible when loop is disabled, creating inactive visual controls.
6. Trim does have an `Apply trim` text action in the captured build, but its low-emphasis text-link presentation is easy to miss and functionally reads as if no confirmation button exists.
7. Trim confirmation moves with page content instead of being anchored while the edit draft is active.
8. The waveform is displayed in the separate Clips manager rather than being the primary visual content of the timeline clip itself.
9. Clip actions are separated from the timeline representation, forcing unnecessary context switching.
10. Track import actions exist in timeline rows, but the later Track structure repetition makes the page look as if track controls are duplicated.
11. Marker controls occupy a tall standalone section rather than behaving as a compact ruler directly above the timeline.
12. The transport occupies a top section while the main timeline sits lower on the page; a fixed transport surface and central timeline hierarchy are more appropriate for tablet DAW use.

## Accepted corrective direction
The implementation must follow `STUDIO_WORKSPACE_GUIDELINES.md`: one fixed workspace, timeline-first composition, no duplicate Clips/Track structure sections, contextual clip/import actions, loop markers only while active, and an anchored high-affordance trim Apply/Cancel bar.
