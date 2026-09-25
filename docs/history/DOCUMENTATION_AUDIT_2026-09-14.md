# Documentation audit — 2026-09-14

## Scope and rule
This audit reviewed `README.md` and every Markdown document under `docs/` after the RC3 physical-review corrections. Active documents were checked against current source, product behavior, CI evidence and candidate identity. Historical candidate/checkpoint files were intentionally preserved as historical evidence; old statements inside a historical file are not treated as current project truth.

Current-source facts after this consolidation: `0.5.0-rc3` / versionCode 23; CI #613 at `db5a4208848e4b6ca2163ce715d0c5bb464cfe37` is the latest fully green signed baseline; source has advanced afterward and needs one new manual exact-source run before a replacement APK is promoted.

`README.md` — **updated** to the RC3/#613 baseline and post-#613 pending-gate state.

## Document-by-document review

| Document | Classification | Audit result |
|---|---|---|
| `ALPHA05_BUILD_NOTE.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `ALPHA05_CANDIDATE.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `ALPHA06_CANDIDATE.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `ALPHA13_CANDIDATE.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `ALPHA13_GATE_SUMMARY.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `ALPHA13_HOMOLOGATION_HANDOFF.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `ALPHA13_RELEASE_NOTES.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `ARCHITECTURE.md` | Ativo/canônico | Atualizado nesta revisão |
| `CANDIDATE_IDENTITY_POLICY.md` | Ativo/canônico | Revisado; sem alteração necessária |
| `CI_PIPELINE.md` | Ativo/canônico | Atualizado nesta revisão |
| `CODEC_SUPPORT_MATRIX.md` | Ativo/canônico | Revisado; sem alteração necessária |
| `CURRENT_STATE.md` | Ativo/canônico | Atualizado nesta revisão |
| `DECISIONS.md` | Ativo/canônico | Atualizado nesta revisão |
| `DOCUMENTATION_MAP.md` | Ativo/canônico | Atualizado nesta revisão |
| `GITLAB_CI_MIGRATION.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `HISTORICAL_CANDIDATES.md` | Ativo/canônico | Revisado; sem alteração necessária |
| `IMPLEMENTATION_ROADMAP.md` | Ativo/canônico | Atualizado nesta revisão |
| `M2_HOMOLOGATION_EVIDENCE.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M4_ALPHA05_HOMOLOGATION_CHECKLIST.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M4_ALPHA06_HOMOLOGATION_CHECKLIST.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M4_ALPHA07_HOMOLOGATION_CHECKLIST.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M4_CLIP_MANAGEMENT_CHECKPOINT.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M4_COMMERCIAL_POLISH_CHECKPOINT.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M4_METERING_MASTER_CHECKPOINT.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M4_PLAYBACK_CHECKPOINT.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M4_TRACK_MIX_CHECKPOINT.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5C_ALPHA09_UX_CONSOLIDATION.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5C_COMPLETE_CHECKPOINT.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5C_FOUNDATION_CHECKPOINT.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5C_MIXER_V4_CHECKPOINT.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5C_RECORDING_COORDINATOR_PLAN.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5C_UX_IMPLEMENTATION_PLAN.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5_ALPHA08_HOMOLOGATION_CHECKLIST.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5_ALPHA09_HOMOLOGATION_CHECKLIST.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5_ALPHA10_CORRECTIVE_CHECKPOINT.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5_ALPHA11_FINAL_HOMOLOGATION_CHECKLIST.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5_ALPHA12_FINAL_HOMOLOGATION_CHECKLIST.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5_ALPHA13_FINAL_HOMOLOGATION_CHECKLIST.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5_ALPHA13_SCOPE_DELTA.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5_ALPHA13_VALIDATION_STATUS.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5_ALPHA14_FINAL_HOMOLOGATION_CHECKLIST.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5_CAPTURE_ENGINE_CHECKPOINT.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5_FINAL_CANDIDATE_POLICY.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5_MEDIA_IO_CONSOLIDATION.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M5_RECORDING_IMPLEMENTATION_PLAN.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M6_ALPHA1_HOMOLOGATION_CHECKLIST.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M7_ALPHA1_HOMOLOGATION_CHECKLIST.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `M8_GLOBAL_DIGITAL_REGRESSION.md` | Ativo/canônico | Revisado; sem alteração necessária |
| `MANAGED_MEDIA_POLICY.md` | Ativo/canônico | Revisado; sem alteração necessária |
| `MEDIA_IO_SUPPORT_CLAIM_RULE.md` | Ativo/canônico | Revisado; sem alteração necessária |
| `PRODUCT_REQUIREMENTS.md` | Ativo/canônico | Atualizado nesta revisão |
| `PRODUCT_VISION.md` | Ativo/canônico | Revisado; sem alteração necessária |
| `PROJECT_PORTABILITY_CONTRACT.md` | Ativo/canônico | Revisado; sem alteração necessária |
| `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` | Ativo/canônico | Atualizado nesta revisão |
| `RELEASE_NOTES_0.4.0-alpha2.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `RELEASE_NOTES_0.5.0-rc1.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `RELEASE_NOTES_0.5.0-rc2.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `RELEASE_NOTES_0.5.0-rc3.md` | Ativo/canônico | Atualizado nesta revisão |
| `SHARE_MODAL_CONTRACT.md` | Ativo/canônico | Revisado; sem alteração necessária |
| `STUDIO_OPTIONS_AND_MIXER.md` | Ativo/canônico | Atualizado nesta revisão |
| `STUDIO_VIDEO_REVIEW_2026-09-08.md` | Histórico/superseded | Revisado e preservado como evidência histórica; não reescrito para fingir estado atual |
| `STUDIO_WORKSPACE_GUIDELINES.md` | Ativo/canônico | Atualizado nesta revisão |
| `TEST_AND_HOMOLOGATION_PLAN.md` | Ativo/canônico | Atualizado nesta revisão |
| `TIMELINE_INTERACTION_GUIDELINES.md` | Ativo/canônico | Revisado; sem alteração necessária |
| `UI_COPY_STYLE.md` | Ativo/canônico | Revisado; sem alteração necessária |
| `USER_GUIDE_POLICY.md` | Ativo/canônico | Atualizado nesta revisão |

## Closure criteria
The documentation set is considered synchronized when:
1. active/canonical docs do not claim #613 validates post-#613 source;
2. Home/Studio shared guide, fixed Auto-sections slot, project-end section bounds and 3-second overlay countdown are represented in product/UX/test/release docs;
3. historical files remain clearly non-authoritative through `DOCUMENTATION_MAP.md` and this audit;
4. the next successful manual workflow updates exact source/APK identity evidence without retroactively changing historical records.
