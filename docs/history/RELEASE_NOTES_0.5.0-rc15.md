# GuitarLab Studio 0.5.0-rc15 — Release Notes

Version: `0.5.0-rc15` / versionCode `35`

RC15 is the focused physical-homologation corrective following RC14. It preserves the remote
Prepared References v2 result and fixes import of standards-compliant float32 WAV files whose
container uses `WAVE_FORMAT_EXTENSIBLE` with the IEEE-float subtype.

The correction validates the complete extensible subtype GUID rather than accepting the generic
container marker alone. PCM and IEEE-float classic WAV remain supported; unknown extensible
subtypes remain rejected. Regression coverage exercises a valid extensible float32 file, an
unsupported subtype and a malformed GUID.

Remote separation enqueue and import-resume entrypoints are now synchronized so repeated taps
cannot pass the local check/save boundary concurrently. The backend's existing single-active-job
rule remains the authoritative second boundary.

Physical evidence: RC14 job `99d3099f-618d-42ca-afe6-a24471553460` downloaded and passed remote
integrity validation after the Storage-rule correction, then failed specifically at
`WavStructure.read` on the legacy format-tag restriction. Replacement job
`66464e42-937f-4d78-a5bd-dabc7c9687f9` completed successfully and is intentionally preserved for
RC15's direct **Retomar importação** acceptance; Demucs must not be rerun for that validation.
