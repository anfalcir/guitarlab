# Documentation map

Updated: 2026-09-14

For the active `0.5.0-rc3` candidate, use these documents together:
- `CURRENT_STATE.md` — authoritative current project/candidate status;
- `IMPLEMENTATION_ROADMAP.md` — milestone/gate disposition;
- `CANDIDATE_IDENTITY_POLICY.md` — exact version/source/signer/checksum contract;
- `CI_PIPELINE.md` — manual CI/release job graph, performance strategy, caches, artifacts and failure interpretation;
- `RELEASE_NOTES_0.5.0-rc3.md` — RC3 behavior delta;
- `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — only residual manual target-device checks after automated PASS;
- `TEST_AND_HOMOLOGATION_PLAN.md` — canonical automated/manual gate discipline;
- `USER_GUIDE_POLICY.md` — mandatory synchronization contract for the novice-facing in-app `Ajuda` guide;
- `UI_COPY_STYLE.md` — capitalization, punctuation and terminology standard for all user-visible screens;
- `ARCHITECTURE.md` — system structure;
- `DECISIONS.md` — durable product/architecture decisions;
- `PRODUCT_REQUIREMENTS.md` — product requirements;
- `CODEC_SUPPORT_MATRIX.md` — import/export capability claims;
- `MANAGED_MEDIA_POLICY.md` — source/proxy/media lifecycle rules;
- `STUDIO_OPTIONS_AND_MIXER.md` — Studio options/mixer behavior;
- `M8_GLOBAL_DIGITAL_REGRESSION.md` — global automated regression baseline;
- `HISTORICAL_CANDIDATES.md` — historical candidate evidence.

`M7_ALPHA1_HOMOLOGATION_CHECKLIST.md` and older alpha/RC files are historical evidence. They must not be used as the active RC3 checklist when they conflict with the documents above.

The exact RC3 source SHA and APK checksum are intentionally not predeclared before the manual workflow succeeds; the successful workflow `github.sha`, `BUILD_IDENTITY.txt` and `SHA256SUMS.txt` are authoritative for those values.
