# GuitarLab Studio 0.5.0-rc2

This candidate refines the RC1 practice workflow after physical tablet validation.

## Corrected and refined
- section names no longer occupy an independent rail above the timeline;
- sections are rendered in the shared temporal header, aligned with their waveform ranges;
- tapping a section applies its bounds to the loop; its integrated remove action deletes it;
- markers and punch range share the same temporal layer instead of adding another visual lane;
- practice controls are separated into Comparison, Timeline and Punch Recording groups;
- "Section from loop" is clarified as "Create section from loop";
- Reference, My Guitar and Both comparison modes show INCLUDED/HIDDEN feedback on affected tracks in both the timeline sidebar and Mixer;
- the visual comparison state is derived from the same policy used by playback.

## Validation target
- JVM regression and comparison-state policy;
- Android Lint;
- debug APK and Android test APK compilation;
- signed release, package identity, certificate and ZIP integrity;
- final visual/ergonomic validation on the Samsung tablet.
