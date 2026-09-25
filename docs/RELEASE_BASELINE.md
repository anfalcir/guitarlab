# GuitarLab RC20 — Frozen Release Baseline

Updated: 2026-09-25  
Status: **FINAL / PHYSICALLY HOMOLOGATED / FROZEN**

This document is the immutable identity record for the currently accepted GuitarLab release. It is not a development plan.

## Android release

- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc20`;
- versionCode: `40`;
- signed producer commit: `76a832afdd8045ac944046dae2c31d8f6ec00716`;
- U12bx runtime change commit: `2cc2c1d77c1659797927897264dfeae2b4e74e9d`;
- U12bx qualification commit: `a40df1081f170354647ab63cb171353e9b116844`;
- Android CI: **#904 / run 36144821352 — PASS**;
- software gate: PASS;
- API 36 emulator regression: PASS;
- exact-artifact signing gate: PASS;
- unsigned APK SHA-256: `eb607853860c6c18508a0b401dcc0ac103a14d52cd9cbad52c827572e3682c29`;
- signed APK SHA-256: `0d5832666a00484635ef37daecb9bead021ed771ddd053b88d88191a5f029fd2`;
- signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact: `GuitarLabStudio-0.5.0-rc20-homologacao`, artifact id `10869485646`;
- artifact ZIP SHA-256: `79f9868e3cd37cd51825f3f340e1e39678b7b89496f073f2055aaa4b3f72c27c`.

The signing commit contains no file changes relative to its parent; the signing workflow rebuilt/qualified the same materialized U12bx source, retained the exact tested unsigned APK, and signed that artifact without recompiling in the signing job.

## Physical acceptance

Owner acceptance was recorded on **2026-09-25** for the exact signed APK above on the reference hardware line:

- Samsung SM-X230;
- Android 16 / API 36;
- M-VAVE MK-300 over USB;
- hardware loopback OFF.

The final U12bx residual — audible output, waveform and visible playhead presentation alignment — was accepted. Previously accepted physical evidence for unaffected capabilities remains reusable under the project evidence-reuse rule; U12bw waveform fidelity and selective prepared-reference restoration had already been accepted before U12bx.

The consolidated physical result is preserved in `history/RC20_PHYSICAL_HOMOLOGATION_FINAL_2026-09-25.md`.

## Frozen remote-separation backend

- production worker digest: `sha256:14e240cb01b71131cb049dd34e0df078614238da3514325f80126d56f8d5e698`;
- finalist qualification: U7 Cloud Backend **#164 / run 36085821470 — PASS**;
- controlled exact-digest production promotion: U7 Cloud Backend **#165 / run 36087971465 — PASS**;
- promotion producer: `ede5e1356503c3e0e12b4dc8e86c67ca337d18b3`;
- post-cutover real-cloud transaction: PASS, execution `gbw-demucs-dmjm9`;
- engine: `demucs-pytorch`;
- Demucs: `4.1.0`;
- PyTorch: `2.14.0+cpu`;
- NumPy: `1.26.4`;
- model: `htdemucs_6s`;
- checkpoint: `5c90dfd2-34c22ccb.th`;
- checkpoint SHA-256: `34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd`;
- inference recipe: CPU, `--float32`, `--clip-mode none`, shifts `1`, overlap `0.5`, 8 CPU threads;
- canonical strategy: `pytorch-cpu-s1-o0.5-t8`;
- Cloud Run shape: 8 vCPU / 16 GiB / taskCount 1 / parallelism 1 / timeout 1800 s / maxRetries 0.

Prepared References v2 publishes only the validated aligned backing-without-guitar and isolated-guitar references; six Demucs stems remain private worker intermediates.

## Freeze rule

The Android APK and backend digest above form one accepted personal-use baseline.

- Documentation-only commits after this record do not change the producer identity of the accepted APK or worker.
- Do not rebuild, republish, retag or silently replace either frozen artifact for routine freshness.
- Reopen development only for a maintenance trigger defined by `PROJECT_IDENTITY.md` / D-090 / D-096 or an explicit owner-requested feature.
- Any future runtime/source/backend mutation creates a new candidate and receives proportional qualification under `TEST_AND_HOMOLOGATION_POLICY.md`.
