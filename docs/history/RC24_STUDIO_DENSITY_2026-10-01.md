# RC24 Studio density correction

Date: 2026-10-01. Status: implemented, qualification pending.

## Evidence and design

Owner video `141016.mp4` (25.57 s, 1728×1080) shows menus during approximately 0–12 s and fixed/height behavior afterwards. RC23 Mixer occupies nearly half the screen, obscuring the second waveform lane; channels have unused space beside their two button rows, framed sliders dominate, Master is largely empty. Comparison/Adjustments/Timeline share one overlay width despite unequal content. RC23 digital PASS remains historical evidence and is not physical UX acceptance.

D-100 and `STUDIO_OPTIONS_AND_MIXER.md` define the correction. Four contiguous 48 dp targets require 192 dp content width; 4 dp side padding produces a 200 dp strip (still 13.8% narrower than original 232 dp). Five channels, four 6 dp gaps, 144 dp Master, 6 dp separator and 16 dp outer padding total 1190 dp: fit in the 1280 dp qualification viewport and 1200 dp logical viewport at normal fonts. Complete 252 dp is 28.4% shorter than RC23 default 352; minimum 172 dp recovers another 80 dp. Visible button faces 46 dp remove large decorative margins without overlapping targets.

Navbar side slots are equal and fixed. Comparison is a compact vertical choice list; Timeline is a vertical action menu with missing-loop guidance and separate clear action. Níveis lives in Master. Mixer always docks; a single mode button replaces Pin/Height. New mode defaults complete, preserves legacy visibility, and is independent of visibility.

Both modes retain the four direct buttons and volume. Full pan/numeric metering/clipping remain exposed in complete; minimum opens the same complete strip from its name. Material Slider continues owning gestures/semantics/commit behavior, with a thin custom drawing. No audio engine/domain modifications.

## Validation

Focused existing tests updated for inline non-overlapping targets and near-target visual faces, structural locks, gain/pan separation, horizontal overflow, font scaling, menu fit, stable navbar, minimum/details interaction, Níveis access and preference migration. Target-tablet test now uses valid managed WAV audio in five clips plus waveform cache, asserts full lane visibility and height recovery, and captures complete/minimum/hidden states. Existing Loop→REC flow still exercises the real Timeline entry point. Android runtime results and captures must be reviewed after owner reports CI completion.

Local validation PASS: Kotlin grammar for all changed Kotlin files, shell syntax, Python stage compilation, whitespace/diff sanity, isolated canonical RC23 → RC24 reconstruction, repeated execution/idempotence, all 11 terminal blob hashes and reverse patch applicability. Prior source stages/payloads remain unchanged. The local workspace has no Android SDK/Gradle installation, so Android compilation, Lint and instrumented results remain explicitly pending CI. Exact commit/run provenance will be added from completed qualification evidence. No APK, signature, CI PASS or physical comfort is predeclared. Source stages RC20–RC23 remain immutable. Temporary qualification branch trigger must be removed before canonical integration/signing. Do not monitor workflows.
