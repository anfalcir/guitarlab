# Test and homologation plan

Updated: 2026-09-09

## Canonical software gate
Every candidate commit on the active branch must pass the repository workflow:
1. `git diff --check` for the candidate delta;
2. source materialization;
3. unit tests;
4. Android Lint;
5. debug APK assembly;
6. diagnostics/artifact upload.

A failure blocks further candidate work until corrected. Do not stack candidate commits while a gate is active.

## Signed homologation gate
Only explicit `[sign-homologation]` candidates (or authorized manual dispatch) may build the homologation release. The job must:
- restore the private signing bundle only in the runner;
- build release;
- verify the APK with `apksigner`;
- compare signer SHA-256 with the locked certificate;
- produce `SHA256SUMS.txt` and `BUILD_IDENTITY.txt`;
- use neutral artifact naming `GuitarLabStudio-${APP_VERSION_NAME}-homologacao`;
- destroy temporary signing files even on failure.

## Active M5 gate
Historical alpha08/alpha09/alpha10 checklists are not current gates. The only active checklist is `M5_ALPHA11_FINAL_HOMOLOGATION_CHECKLIST.md`.

Physical alpha11 focus:
- track reorder + continuous ghost;
- edge autoscroll;
- waveform/clip migration between tracks;
- `Configurar pista` landscape/portrait layout and source metadata;
- Trim regression;
- `Limpar pista` versus `Excluir pista`;
- Undo/Redo after completed drag operations.

Automated tests complement but never replace device validation. M5 remains OPEN until explicit physical approval.
