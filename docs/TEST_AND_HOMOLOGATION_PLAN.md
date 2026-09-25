# Test and Homologation Plan

Updated: 2026-09-24

## Purpose

Qualify GuitarLab in proportion to credible risk for its single owner. Test count is not a release objective; confidence in protected owner outcomes is.

Historical campaigns and exhaustive matrices are retained in `history/TEST_AND_HOMOLOGATION_PLAN_PRE_RC20.md` and related evidence files.

## Protected outcomes

The following are blocking when a candidate can affect them:

- no project, recording or managed-media loss/corruption;
- structurally valid and musically usable prepared audio;
- working search/acquisition → Prepare → Studio path;
- safe save/reopen and transactional publication;
- recovery, quota, idempotency and cloud cleanup correctness;
- no credential exposure or unauthorized access;
- exact APK package/version/provenance/signer identity;
- exact worker digest/engine/model/recipe identity;
- safe production promotion with rollback.

## Evidence selection rule

For each change:

1. identify components and owner flows it can materially affect;
2. run focused tests for the changed path;
3. run adjacent integration smoke for protected boundaries;
4. reuse accepted unrelated evidence;
5. add broad regression only when the affected surface is uncertain or prior evidence is invalidated.

Broad suites may continue to run. A specific unrelated flaky/cosmetic failure may be quarantined with recorded evidence and rationale. Integrity, audio, primary-flow, quota/cost, credential and signing failures may not be quarantined.

## RC20 automated minimum

### Worker/backend

- deterministic source materialization and shell/source checks;
- worker unit tests and exact engine/model/dependency identity;
- structure/finiteness/hard audio-quality gates;
- W3 exact contract plus independent quality acceptance;
- selected CPU8 W3/W4 runtime/cost/bundle evidence from one Cloud Run execution and one Demucs inference;
- U7 digest-pinned shadow qualification;
- post-cutover U4 transactional smoke;
- secret/credential hygiene and retained vulnerability evidence.

Stochastic numeric/hash differences are diagnostic under D-091. CPU4 and other matrices are optional. Warm-probe and standalone model-probe are diagnostic-only and default off in the normal RC20 gate.

### Android

- unit tests relevant to search terminal states and changed domain behavior;
- Android Lint;
- release assembly;
- representative API36 regression for search/acquisition and adjacent Prepare → Studio/save-reopen behavior;
- exact unsigned artifact provenance;
- no-recompile signing and package/version/certificate/SHA verification.

Debug assembly, full screenshot matrices and isolated geometry are required only when relevant to the candidate or when prior evidence is invalidated.

## Human and physical boundaries

Automation cannot establish musical usefulness or target-device behavior completely.

D-092 explicitly waives the additional pre-cutover W5 owner-listening gate. After Android signing, the owner performs final acceptance on the exact signed APK under `RC20_PHYSICAL_HOMOLOGATION.md`.

Do not manually repeat digitally proven claims unless physical behavior can differ materially.

## Vulnerability handling

Retain SBOM/scanner output. A finding blocks when it represents a credible applicable high-risk path in the shipped runtime or credentials. Severity labels without applicable code/path/fix context do not automatically veto the personal appliance.

RC20 keeps its current narrowly evidenced scanner exceptions; this policy does not require building a new generic reachability framework before release.

## Candidate invalidation

A change invalidates evidence only for paths it can materially affect. Examples:

- worker image/model/recipe change: repeat worker shadow and cloud integration; owner listening is repeated only if explicitly reinstated or required by a new observed audio-quality concern;
- separation lifecycle/backend change: repeat U7/U4 and relevant Android recovery smoke;
- search/acquisition change: repeat focused Android search and Prepare adjacency;
- signing/build change: repeat artifact provenance/signing identity;
- recording route/timing change: repeat affected digital and target USB checks;
- documentation-only change: no product qualification.

## Acceptance records

For a promoted candidate retain:

- producer SHA;
- relevant workflow/run identities and conclusions;
- unsigned and signed APK SHA-256;
- package/version/signer;
- worker digest and engine/model/recipe;
- owner-listening decision/waiver;
- physical acceptance decision and included capabilities;
- explicit quarantines or known non-blocking limitations.

`CURRENT_STATE.md` contains only the current record. Superseded evidence belongs under `history/` or immutable CI artifacts.

## Freeze

After final owner acceptance, do not run recurring qualification or rebuild for freshness alone. Reopen testing only with a maintenance trigger or owner-requested feature, and apply this same affected-path rule.


## U12bn RC20 Android completion focus
The next Android qualification must exercise the exact U12bn source and include focused coverage for persistent Prepare search terminal states/suggestion retry, local reference-binding repair, diagnostics navigation/export/redaction/checksums, accepted-manifest retention ordering, and adjacent Prepare → Studio behavior. The final signed artifact remains subject to the consolidated SM-X230/MK-300 physical campaign before freeze.


## RC20 physical candidate identity

The exact APK to install for final physical validation is `GuitarLabStudio-0.5.0-rc20-homologacao.apk`, signed APK SHA-256 `e82fdc75896564ea10c28e072fd186913320eca293b6fad9ea4b7c8fa0468468`, producer commit `fd63413ea440ed96a227b0203768b113bf256e98`. Do not rebuild between this digital pass and physical homologation.


## U12bu Settings/Activity delta

Before the next physical candidate is signed, Android qualification must additionally verify: Settings no longer exposes “Projeto e Studio”, “Importação” or the redundant “Ver atividade” row under “Conta e nuvem”; Home still exposes the primary Activity entry point; every active Activity record exposes a Cancel action; local live jobs are cancelled through the process registry when present; exact source acquisition ownership is cancelled without touching a newer operation; separation cancellation reaches Firebase/Cloud by jobId including the orphan fallback; automatic backup work is cancelled without permanently disabling the future schedule; truly orphaned records become CANCELLED and leave the active set. No APK has been generated for U12bu yet.


## U12bv signed frontend candidate

Install `GuitarLabStudio-0.5.0-rc20-U12bv-homologacao.apk` for the final frontend physical validation. Producer commit: `833142435b2e2c1e74f39bf5cee85d8049595748`; signed APK SHA-256: `6e8d2af6dad105ba4f27a703747b4c04873cdb36d3affe29e18074b2816db838`. Validate the U12bu/U12bv Settings cleanup and Activity cancellation behavior; final documentation/freeze must wait for owner acceptance.


## U12bw waveform/reference qualification

The next Android candidate must qualify:
- exact waveform frame-window generation, including non-divisible durations with no empty trailing bucket;
- GLW2 cache hit/miss behavior across media replacement, trim/split window changes, resolution/algorithm changes and orphan pruning;
- 4096-bin peak-preserving reduction to screen columns;
- same-project reentry: unchanged persisted snapshot preserves resident state/Undo; externally changed snapshot reloads and clears stale history;
- static timeline geometry remains frame-faithful without horizontal waveform padding or artificial clip-duration width;
- aggregate and stereo channel envelopes stay coherent after edits, Undo/Redo, recording finalization and recovery;
- Prepare always offers reference restoration when prepared assets exist;
- modal can restore Base, Guitar or both; selected structural lanes return to canonical prepared media while unrelated tracks/takes/mixer/markers/sections and the unselected family remain intact;
- restored reference media must naturally invalidate/regenerate only the affected waveform cache entries.

Physical homologation should specifically replace/edit a backing, restore Base only, confirm the canonical audio and waveform return together, then repeat for Guitar and verify recorded takes are unchanged.


## U12bw signed waveform/reference candidate

Install `GuitarLabStudio-0.5.0-rc20-U12bw-homologacao.apk` for physical validation. Producer commit: `99cd47022a9babd7accee39c4478fe7dbc2fd1d3`; signed APK SHA-256: `529834d908a0bf69c09182ebe08ca3a6269f7d776c57b37a76dbabcedae5c60f`. Validate: waveform refresh after media/reference replacement; visual alignment against playback across the song; trimmed/split clips; recorded takes; same-project re-entry after Prepare changes; always-visible prepared-reference restore button; Base-only, Guitar-only and both restore selections; preservation of takes, other tracks and mixer state. Final freeze waits for owner acceptance.


## U12bx playback-presentation synchronization qualification

The next Android qualification must cover the exact U12bx source:
- before the AudioTimestamp-derived presentation origin, presented frames remain zero and the visible playhead stays at the requested start frame;
- at 48 kHz, 100 ms of presentation time maps to 4,800 frames; a start at frame 240,000 therefore maps to frame 244,800;
- presentation-derived progress is clamped to frames already accepted by AudioTrack;
- seek/flush resets the presentation anchor before progress resumes;
- playbackHeadPosition remains a bounded fallback when a stable AudioTimestamp cannot be obtained;
- non-loop end/drain does not jump the visual cursor ahead of the presentation clock;
- existing loop/start/seek transport regression and API36 coverage remain green.

After software + API36 pass, produce a new exact-artifact signed RC20 homologation candidate. Physical acceptance must check strong transients from frame zero and mid-song seek, repeated Play/Stop, pause/resume semantics, loop behavior, and compare MK-300 USB with internal output only if needed for diagnosis. The acceptance criterion is no consistent perceptible offset between audible output, waveform and playhead in normal use. Final documentation/freeze remains pending owner acceptance.
