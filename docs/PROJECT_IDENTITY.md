# GuitarLab — Project Identity

Updated: 2026-09-25

This document defines the stable identity, support boundary, quality philosophy and maintenance policy of GuitarLab. It operationalizes `D-090` in `docs/DECISIONS.md`. If a later numbered decision explicitly supersedes this document, the later decision wins.

## Purpose

GuitarLab is a private Android workspace for guitar preparation, study, recording, comparison, mixing, export and backup. Its primary product flow is:

**search/acquisition → Prepare → Studio → study/recording → save/reopen → export**

Backup/restore is part of the supported personal workflow when the owner uses it.

## Owner and distribution model

- GuitarLab has one owner/user.
- It is for private personal use.
- It is not a commercial service, public distribution, store product, SDK, multi-user platform or 24/7 service.
- There is no SLA, public support matrix or obligation to support arbitrary third-party inputs, devices, providers or historical versions.
- Provider-mediated acquisition, especially the providers actually used by the application, is the normal source path.

## Supported environment

The reference environment is the environment actually used by the owner:

- Android/tablet as the primary client;
- current target device: Samsung SM-X230 / Android 16 / API 36;
- Firebase/Cloud Run separation and Google Drive v3 backup only as used by GuitarLab;
- M-VAVE MK-300 over USB when USB recording is part of the capability being homologated or a related defect is under investigation.

Existing codec/device capabilities may remain supported when they are already implemented and validated. Their existence does not create an obligation to expand into universal interoperability or exhaustive hardware/format matrices.

## Quality philosophy

Release rigor is proportional to credible harm to the owner.

The following remain release-blocking when applicable:

- loss or corruption of projects, recordings or managed media;
- structurally invalid or musically unusable prepared audio;
- failure of the primary search/acquisition → Prepare → Studio flow;
- persistence, recovery, backup or restore failures that can lose or duplicate state;
- duplicate jobs, quota consumption or unacceptable cost behavior;
- credential exposure or unauthorized access;
- wrong APK package/version/signer/provenance;
- wrong worker image/model/runtime identity;
- unsafe production cutover without a usable rollback path.

The following are normally evidence or conditional checks rather than automatic vetoes:

- unrelated flaky/cosmetic tests;
- optional environment matrices;
- statistics without enough samples to support the claim;
- cross-run numeric/hash equality where the qualified algorithm is intentionally stochastic;
- scanner findings without a credible applicable path to material harm;
- convenience diagnostics, journals and export bundles;
- cosmetic refinement that does not impair the owner's normal workflow;
- historical compatibility with data the owner does not need.

Existing CI and vulnerability workflows are not weakened merely to implement this policy. Broad coverage may continue to run. Quarantine is allowed only for a specific, evidenced, unrelated failure and never for data/media integrity, audio validity, primary flow, quota/cost, credential or signing failures.

## Freeze policy

`RELEASE_BASELINE.md` records the exact immutable identity of the currently accepted baseline. RC20 remains that accepted baseline while RC21 proceeds through maintenance qualification; opening a successor candidate does not mutate the accepted artifact identity. For every accepted baseline:

- freeze the exact signed APK;
- record and freeze its producer SHA, package/version, APK SHA-256 and signer certificate;
- freeze the production worker by immutable digest and record its qualified engine/model/recipe;
- retain the production configuration needed to identify and, when a real future change is made, roll back the affected backend path;
- do not rebuild merely for dependency freshness, scanner-database churn, optional matrix expansion, percentile collection or maintenance-prevention aesthetics.

## Maintenance triggers

Development reopens only for one or more of:

1. an observed regression in real owner use;
2. real provider/platform/API deprecation that breaks the supported path;
3. an applicable high-risk vulnerability, especially a known-exploited path relevant to GuitarLab;
4. credential exposure or authorization failure;
5. unacceptable integrity, quota or cost risk;
6. a new feature explicitly requested by the owner.

A future change requalifies only the paths it can materially affect, plus adjacent smoke needed to prove safe integration. Previously accepted unrelated evidence remains reusable.

The current RC21 maintenance line is an example of this rule: an owner-observed Studio/project-integrity regression plus backup/feedback issues reopened only the affected Android paths. It does not reopen the frozen Demucs worker or unrelated physical evidence. Until RC21 is signed and physically accepted, RC20 remains the promoted baseline.

## Release principle

A physical campaign evaluates the **exact signed candidate intended for promotion**. Worker listening/quality qualification may occur before signing, but application homologation occurs after the tested unsigned artifact has been signed and its identity verified. Documentation-only commits after acceptance do not redefine the frozen producer or artifact identity.
