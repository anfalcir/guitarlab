# Product Vision

Updated: 2026-09-25

## Mission

GuitarLab is a private, single-owner Android guitar preparation, practice, recording, comparison, mixing, export and backup appliance. It unifies source acquisition and Demucs preparation with Studio so the owner can move from a song source directly into study and recording without a desktop/file-handoff workflow.

The stable product identity and maintenance model are defined by `PROJECT_IDENTITY.md` and D-090.

## Final-state product

The supported personal product includes source acquisition/Prepare, validated Demucs references, one managed project identity, Studio editing/recording/mixing, useful validated import/export formats, safe save/reopen, direct Drive v3 backup/restore and practical tablet UX.

## Product principles

1. Guitar-first, not generic-DAW-first.
2. Private personal appliance, not public platform.
3. Non-destructive editing and transactional publication by default.
4. Reliable before feature-rich.
5. Musical usefulness matters: structurally valid but unusable separated audio is a release failure.
6. Quality gates are proportional to credible risk, not gate count.
7. Explicit capability status: unvalidated features are not advertised as supported.
8. Studio/core remains usable locally; network-dependent operations expose their dependency explicitly.
9. Prepare and Studio share one managed project/asset domain.
10. Keep the accepted exact signed APK and digest-pinned worker frozen until a real maintenance trigger exists.

## Scope guardrail

Existing multi-format import/export support may remain and should not be regressed without reason, but GuitarLab has no obligation to become a universal media-conversion product. Provider-mediated source acquisition and the owner's actual workflows define the practical support boundary.

Historical compatibility is not an open-ended product obligation. Current unified projects, their save/reopen path and data the owner actually needs remain protected; retired GBW/H37/pre-unification migration does not re-enter scope without an explicit later decision.

## Canonical documentation

`PROJECT_IDENTITY.md` defines stable identity; `PRODUCT_REQUIREMENTS.md` defines supported behavior; `CURRENT_STATE.md` records present state; `RELEASE_BASELINE.md` records the exact frozen release identity; `DECISIONS.md` contains durable decisions; `TEST_AND_HOMOLOGATION_POLICY.md` governs future qualification; and `DOCUMENTATION_MAP.md` separates live authority from historical evidence.
