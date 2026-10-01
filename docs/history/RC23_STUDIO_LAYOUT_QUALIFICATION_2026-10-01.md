# RC23 Studio navbar / complete narrow Mixer qualification

Date: 2026-10-01

## Scope and design

Owner-requested stable navbar slots for Comparison/Adjustments/Timeline and narrower per-track Mixer controls, preserving every control/readout and persistent pinning. Channel width is the priority: 232 → 168 dp (about 28%); state buttons form a 2×2 grid of 48 dp targets. Height follows complete control geometry rather than being the main density goal. MAIN/CUE domain/audio behavior is unchanged from RC22.

Reference rationale and normative authority are recorded in D-099, `STUDIO_OPTIONS_AND_MIXER.md`, `STUDIO_WORKSPACE_GUIDELINES.md` and `UI_VISUAL_SYSTEM.md`. No generic reference certifies the owner-device physical ergonomics.

## Source and chronology

- canonical base: `58ea3b5bd99e0a54db6ecb510e26e84530b652d5`;
- PR #9: https://github.com/anfalcir/guitarlab/pull/9;
- CI #937 / run `36910864961`, source `726b85642f4d35daa212eaeb8e599037014ba4fc`: software PASS; instrumented APK compilation failed because five screenshot extension calls omitted the Compose rule receiver. No instrumented test executed in that failed gate;
- corrective `922e1c0523355ac34f50b2701c205c55d56cc451` fixes all five receivers and reseals tests/payload;
- CI #938 / run `36912170713`: software and API36 PASS;
- visual review found a loading frame in the pinned full-app capture despite passing semantic geometry. `2aefcd1b2c4268c3ab7fb8d945031cf3cd2db162` adds UIAutomator quiescence before that test's full-display screenshot and retires the temporary branch trigger; runtime bytes remain identical to #938;
- canonical PR merge / exact signed producer: `b39be540f56fa700338602408a7be92d9076e10b`;
- CI #939 / run `36914477871`: **software PASS / API36 PASS / signing PASS** — https://github.com/anfalcir/guitarlab/actions/runs/36914477871.

## Materialization

The new stage `scripts/materialize_ci_sources_rc23.py` follows immutable RC22. Payload `.source-parts/RC23StudioSpaceLayout.patch` Git blob: `7c66064d0a8c8dc6fb87609d4129008e219e3ee9`. Thirteen terminal source/test blobs are locked. Local cold RC22→RC23 construction, repeated materialization and reverse-apply checks passed. Prior protected payloads are not rewritten.

## Relevant coverage and visual review

- fixed navbar geometry across comparison/Mixer/pinning/height states;
- anchored comparison changes without closing, Níveis callback and narrow row reachability;
- independent persistent visibility/pinning/height;
- real, non-overlapping 48 dp Mute/Solo/CUE/Arm and CLIP targets;
- CUE/Arm transport locks and gain/pan control separation;
- large-font width/height growth with reachable controls;
- horizontal overflow with fixed separate Master;
- actual Loop→REC flow through the new Timeline trigger;
- five full default channels plus Master on 1920×1200 / 240 dpi;
- existing broader adjacent API36 and software gates remain enabled.

The final `rc23-studio-five-channels-pinned.png` was visually inspected in the canonical #939 integration artifact: all five complete channels and Master render, fixed mode reserves its own space, and the permanent practice row is absent. The #938 loading PNG is not pinned-layout visual evidence. Device physical comfort remains for the owner's acceptance.

## Signed artifact

- artifact: `GuitarLabStudio-0.5.0-rc23-homologacao`, GitHub artifact ID `11189558593`;
- file: `GuitarLabStudio-0.5.0-rc23-homologacao.apk`, `80133832` bytes;
- package: `studio.guitarlab.app`; versionName `0.5.0-rc23`; versionCode `43`;
- unsigned SHA-256: `219f7c4f9c5323ec0879dedb8e07b80421b919e52a79aedf5f019669e38bfd12`;
- signed SHA-256: `d2630d935c255a543f122b5226edb96bf9bbe10badca6b02308c94fd8f95f3ee`;
- certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signing job validates package/version/certificate and APK Signature Scheme v2, without recompiling the tested unsigned release;
- downloaded signed bytes were hashed locally and match both producer identity and SHA256SUMS metadata.

## Acceptance boundary

Signed digital qualification is closed. Owner tablet UX validation and inherited residual physical MAIN/CUE acceptance remain pending. RC20 stays the accepted frozen baseline until explicit physical acceptance; this report does not redefine it. The owner requested no workflow monitoring; completed results are consulted only after the owner reports completion.
