# RC21 Documentation Audit — 2026-09-25

Status: **COMPLETE / MERGED TO MAIN / PRE-SIGNING**

This audit records the live-documentation changes required by GuitarLab RC21 before canonical-main integration and signed qualification.

## Live authority updated

- `README.md`: RC20 accepted baseline vs RC21 active maintenance candidate;
- `CURRENT_STATE.md`: exact RC21 version/source/CI #910 state, scope and promotion sequence;
- `PROJECT_IDENTITY.md`: accepted-baseline semantics while a proportional successor candidate is open;
- `PRODUCT_REQUIREMENTS.md`: persisted-state verification, legacy take recovery, stereo lineage and Drive catalog/background requirements;
- `ARCHITECTURE.md`: transactional stereo separation, persisted-state compatibility boundary and one-snapshot Drive catalog/cache architecture;
- `BACKUP_IDENTITY_CONTRACT.md`: digest/revision verification before compatibility recovery;
- `DRIVE_BACKUP_CONTRACT.md`: UI-independent automatic backup, one-snapshot catalog and metadata-cache authority rules;
- `PROJECT_PORTABILITY_CONTRACT.md`: persisted representation verified before compatibility repair;
- `TRANSIENT_FEEDBACK_CONTRACT.md`: shared host and immediate consumption/no-navigation-replay semantics;
- `TEST_AND_HOMOLOGATION_POLICY.md`: proportional provider-real evidence for catalog/cache-only change;
- `CANDIDATE_IDENTITY_POLICY.md`: signed maintenance candidate must originate from canonical `main` after temporary branch exception removal;
- `CI_PIPELINE.md`: RC21 terminal materialization, CI #910 authority and pre-signing branch cleanup;
- `DOCUMENTATION_MAP.md`: RC21 candidate evidence mapping;
- `DECISIONS.md`: D-097 durable maintenance decision.

## Candidate evidence added

- `RC21_MAINTENANCE_QUALIFICATION_2026-09-25.md`;
- `RELEASE_NOTES_0.5.0-rc21.md`.

## Intentionally unchanged accepted authority

`RELEASE_BASELINE.md` remains the immutable RC20 accepted-release record until the exact signed RC21 APK passes residual owner acceptance. The RC21 documentation does not claim that signing or physical promotion has already occurred.

The production remote-separation worker identity is unchanged, so no RC20 worker digest/model/recipe authority is rewritten.

## Consistency result

The live documentation now consistently describes:

- RC20 as accepted/frozen baseline;
- RC21 as version `0.5.0-rc21` / code `41`;
- RC21 unsigned qualification authority as Android CI #910 / run `36195905242` on source `ad182678cb2704dc9bbfc622124b4f2ac121fea1`;
- current materialization tail as `scripts/materialize_ci_sources_rc21.py`;
- signing as pending and required from canonical `main`;
- residual physical/provider acceptance as pending;
- the temporary maintenance branch as disposable integration scaffolding, not release identity.

Canonical integration is PR #7, merge commit `2617fe1f1f2351a17389f165ed5d5a8e834e16a2`.

No live document intentionally promotes RC21 before signed/physical acceptance.
