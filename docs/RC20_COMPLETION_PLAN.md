# U12 — RC19 audio forensics, observability and diagnostics plan

Updated: 2026-09-24 — F1/F2/F3 root cause closed; worker reconstruction active
Status: **ACTIVE / RELEASE-BLOCKING WORKER RECONSTRUCTION**
Current signed candidate under investigation: **0.5.0-rc19 / versionCode 39**
Next signed candidate if source changes are required: **0.5.0-rc20 / versionCode 40**
Repository authority: `anfalcir/guitarlab` / `main`

## 1. Purpose

RC19 closed the authoritative same-generation remote recovery defect and passed its complete digital qualification, but target-device listening exposed a separate release-blocking audio-quality incident after a real Prepare run. F1/F2/F3 subsequently proved that the incident originates in the substituted `demucs.cpp`/GGML inference engine, not in source canonicalization, Firebase transport, Android publication, Studio binding or the prepared-reference renderer.

This plan owns the RC20 corrective and the owner-approved completion features that must remain in scope before the final product freeze. It intentionally separates two tracks:

**Release-critical RC20 corrective**
1. completed forensic proof of what executed in Firebase/Cloud Run;
2. completed audio-content diagnosis and root-cause localization;
3. reconstruction of the worker on the consolidated GBW Linux Demucs baseline;
4. balanced quality/performance/cost qualification;
5. the source-search terminal-state correction required for the owner's normal Prepare flow;
6. quality-gate hardening required to prevent another musically invalid prepared result from being accepted.

**Owner-requested completion features before final freeze**
7. explicit Studio reference reinsertion UX;
8. Prepare technical-detail cleanup/grouping;
9. consolidated Diagnostics in Settings;
10. persistent sanitized application audit journal;
11. exportable diagnostic ZIP/support bundle.

The second track does **not** block W6, the PyTorch backend cutover or backend qualification. W5 owner listening was explicitly waived by the owner under D-092. It does remain committed product scope: these features must be completed before GuitarLab is declared finally frozen unless the owner explicitly removes one of them.

The forensic prerequisite is now satisfied. Do not patch around the defective engine with a DC blocker as the production solution: listening proved that DC was only one symptom of a structurally invalid separation dominated by the `other` stem.

U12 remains open. RC19 is a digitally qualified but physically **not accepted** candidate.

### Personal-use maintenance profile — authoritative scope

GuitarLab is a private, single-owner application for continued music study. It is not a public upload service, commercial product or generally distributed application. Release engineering must therefore maximize dependable day-to-day use and minimize future maintenance, rather than pursue enterprise-style evidence for its own sake.

Before the signed RC20 homologation APK, a finding is release-blocking only when it has a credible path to one or more of:

- lost, overwritten or unrecoverable project/recording media;
- musically unusable or structurally invalid prepared audio;
- broken primary source acquisition for the owner's normal provider path;
- unauthorized cross-user access, exposed secrets or an applicable remote-code/credential risk;
- duplicate quota/cost, uncontrolled retries or an unbounded cloud resource;
- inability to recover/cancel/adopt work safely after interruption;
- failure to build, install, sign, launch or complete the representative owner workflow.

The following remain evidence but are not automatic release vetoes when the representative workflow passes:

- cross-host floating-point/hash differences inside the documented metric envelope;
- scanner severity without contextual applicability, without an available fix, or contradicted by relevant vendor severity; accepted findings remain explicit and narrowly matched;
- performance above the optimal target but inside the acceptable tier;
- CPU/resource matrix cells that are not the selected production configuration;
- p95 claims when the small personal-use sample cannot support a meaningful percentile;
- synthetic-fixture cosmetic or diagnostic differences without structural or audible impact;
- missing convenience diagnostics, consolidated support UI or export tooling that is not required to diagnose a current blocker.

After RC20 is accepted, freeze the signed APK and digest-pinned worker. Do not rebuild, upgrade or rerun qualification merely because dependency/scanner databases changed. Reopen maintenance only for an observed functional regression, platform/provider deprecation, applicable known-exploited vulnerability, credential exposure, unacceptable cost, data-integrity risk, or an owner-requested feature. A rebuilt image must be requalified; an unchanged frozen digest does not require periodic requalification.

---

## 2. Physical incident evidence

Target project:
- name: `WATG - Deadbolt`;
- projectId: `18cf294d-7a76-4f03-8abd-bc55d3344beb`;
- sourceAssetId: `f8c3e5d7-c759-41e1-945b-4968c3a54b0e`;
- source SHA-256: `b0e24e05bf4adfe73a72b1cef961ad0f5322ddd4b995d8af7a84296f8feb324a`.

Physical RC19 run shown by the app:
- source operation: `5db9b4a8-b5ab-4a5e-a517-3c25e2678a7f`;
- processing/job id: `c3ba40ec-6153-483a-801f-9e838b3e3b60`.

User-exported references from that project:
- backing SHA-256: `d9f206f25f9eb28d02c7fb4a4a3e1d667077429e91520eb64763cd0251fe2a8e`;
- guitar SHA-256: `8bbb4b5eb6b4ef8758669c1734b8d095893aadc9792d12a6330d9318eae725fa`;
- both are float32 stereo WAV, 44.1 kHz, 7,914,496 frames, approximately 179.467 s and 63,316,012 bytes;
- no NaN/Inf was found in the supplied files;
- neither file exceeds full-scale;
- backing + guitar peaks at approximately `-1.000 dBFS`, consistent with the current prepared-reference-v2 shared-gain ceiling;
- measured mean/DC component is approximately `-0.0473` per backing channel and approximately `-0.0090` per guitar channel. It was initially diagnostic evidence; F3 later proved it is a symptom of invalid engine output, not the complete root cause.

Observed physical result:
- separation finished and references were imported;
- audio remained subjectively unusable;
- current digital integrity gates did not reject it.

The incident is now diagnosed as an engine/runtime-quality failure, not a WAV-container, transfer, Android publication or renderer failure.

### Completed second-source reproduction

Project `MMF - Misery` independently reproduced unusable audio. Local metrics found backing DC around `-0.0622` and guitar DC around `-0.0125`. The preserved M4A source had SHA-256 `85cdef6e6bd5671e320fc503db582f23eecd1a75097c5cc2b5994f8e693d9ec2`.

An isolated replay retained canonical input and all six stems. The canonical input was physically accepted by listening and had only negligible mean (`-0.00030 / -0.00051`). Every `demucs.cpp` stem acquired a similar negative DC around `-0.013` to `-0.016`; listening found all six stems unusable and most musical content incorrectly concentrated in `other`. A 10 Hz per-stem DC blocker removed the measured offset but did **not** restore musical separation. This proves the defect is the inference output, not merely DC.

Evidence:
- `history/U12_RC19_F1_AUDIO_FORENSICS_REPORT.md`;
- `history/U12_RC19_F2_LOCAL_AUDIO_FORENSICS_REPORT.md`;
- `history/U12_RC19_F3_DIAGNOSTIC_REPLAY_REPORT.md`.

---

## 3. Current known architecture relevant to the incident

The RC19 production v2 contract is intentionally narrow:

- engine: `demucs.cpp`;
- model: `htdemucs_6s`;
- six internal stems remain worker-local;
- prepared backing = drums + bass + other + vocals + piano;
- prepared guitar = guitar stem;
- one shared gain is applied to backing and guitar;
- final prepared references target a recombined peak ceiling of `-1 dBFS`;
- only `backing.wav`, `guitar.wav` and `result-manifest.json` are remotely committed;
- Android validates ownership/hash/container and republishes the two references locally;
- Android stores provenance including remote job, manifest SHA, model SHA, recipe and remote deliverable hashes.

Important limitation:
`sampled_reconstruction_snr_db` is currently diagnostic. It is logged but is not a publication veto because RC17 proved that summed-stem SNR is not, by itself, a reliable safety criterion for Demucs output.

The current U4/U7 gates therefore prove transactional and numerical integrity, not representative musical separation quality. They permitted a broken engine to publish structurally valid audio.

The consolidated GBW Linux reference does **not** use `demucs.cpp` for its Demucs path. Its known-good Separador B invokes the official PyTorch Demucs CLI:

```text
demucs -n htdemucs_6s --float32 --clip-mode none \
  --shifts 1 --overlap 0.5 -d <cpu|cuda> -o <output> <prepared.wav>
```

The GBW defaults are `demucs_shifts=1` and `demucs_overlap=0.5`. This official PyTorch path, its six-stem naming and float32 output contract are the reconstruction baseline. GBW's separate BS-RoFormer mode remains excluded from GuitarLab.

---

## 4. Non-negotiable investigation rules

1. **Preserve evidence first.**
   No manual Firestore mutation, ACK, cancellation, Storage purge or production-job deletion solely to simplify diagnosis.

2. **Read-only cloud forensics precedes algorithm changes.**
   The first cloud investigation must not deploy or modify resources.

3. **Do not assume v1 reuse or a fresh run.**
   Prove whether job `c3ba40ec-6153-483a-801f-9e838b3e3b60` dispatched a new Cloud Run execution.

4. **Do not apply speculative audio filters.**
   In particular, do not add a DC blocker/high-pass merely because a DC component is visible until the source/stem boundary where it appears is identified.

5. **Do not relax v2 ownership/hash validation.**

6. **Do not revert to the rejected partitioned `demucs.cpp` strategy.**
   It produced non-finite samples. `single8` is also rejected for musical quality. Neither is an eligible RC20 engine.

7. **No signed successor until forensic and quality gates are green.**

---

## 5. Root-cause decision — CLOSED

The investigation resolved to **Case B**.

- fresh Cloud Run execution was proven for the physical job;
- the same source produced deterministic identical remote audio in separate executions;
- Android's 70-byte container delta was explained by canonical WAV rewriting while preserving sample payload;
- a second source reproduced the physical failure;
- isolated replay proved the canonical source is good and all six `demucs.cpp` stems are bad;
- per-stem DC correction removed DC but did not repair separation;
- physical listening confirmed energy is incorrectly concentrated in `other`.

Root cause: replacing the consolidated GBW official PyTorch Demucs path with the `demucs.cpp`/GGML engine broke separation quality. The corrective is worker reconstruction, not result reuse, Android rebinding, renderer changes, or a DC filter.

The historical decision branches are retained below for traceability.

### Case A — no new Cloud Run execution exists for the physical job

Likely class:
- local/remote generation reconciliation;
- recovered prior v2 result;
- stale active-reference selection;
- manifest/job lineage ambiguity.

Action:
- correct job/generation selection and provenance presentation;
- do not modify Demucs.

### Case B — a new execution exists and produced the exact supplied hashes

Likely class:
- worker inference;
- model/runtime behavior;
- source canonicalization;
- worker post-processing.

Action:
- perform diagnostic replay with internal stems and metrics.

### Case C — Cloud Run produced different hashes from the active local references

Likely class:
- Android publication;
- historical active-asset pointer;
- canonicalization/publication lineage;
- Studio binding to an older generation.

Action:
- correct publication/selection/binding and prove exact remote→local hash lineage.

### Case D — raw stems are acceptable but prepared references become bad

Likely class:
- stem-to-backing mix;
- shared-gain render;
- channel/canonicalization;
- post-render publication.

Action:
- fix renderer only after the exact transformation is identified.

---

## 6. Phase F1 — read-only production forensics — COMPLETE

Create a dedicated, fail-closed forensic script/workflow. It must use the repository's existing authenticated GCP/Firebase path and must be **read-only**.

Primary target:
`c3ba40ec-6153-483a-801f-9e838b3e3b60`.

Collect:

### Firestore
- full sanitized job document;
- project/source/input generation;
- state/phase/progress;
- enqueue/completion/import/cleanup timestamps;
- manifest SHA;
- error/recovery fields;
- accepted-job/quota evidence when available.

### Cloud Run Jobs
- matching execution name/id;
- execution creation/start/completion time;
- terminal result;
- task index;
- image reference and immutable digest;
- configured CPU/memory/timeout;
- non-secret runtime strategy variables.

### Cloud Logging
Filter by job id and execution:
- `worker_started`;
- `stem_reconstruction_diagnosed`;
- failure/warning records;
- preparation/publication events;
- task start/end;
- actual inference strategy;
- model/engine revision where logged.

### Storage / manifest
If objects still exist:
- manifest bytes and SHA;
- deliverable object metadata;
- remote backing/guitar SHA and size.

If purge already occurred:
- state this explicitly;
- rely on Firestore, Cloud Logging and locally retained provenance;
- do not recreate evidence from assumptions.

### Mandatory forensic answers

The F1 report must answer:

1. Did this job cause a new Cloud Run execution?
2. Which exact execution processed it?
3. Which image digest executed?
4. Which engine revision/model/model SHA/strategy ran?
5. What input SHA was processed?
6. What manifest SHA was committed?
7. What remote backing/guitar hashes were committed?
8. Do those hashes map to the supplied local references or only to their pre-canonicalization remote originals?
9. What reconstruction diagnostic was recorded?
10. Was any recovery/idempotent adoption path involved?

---

## 7. Forensic evidence bundle

The workflow must produce a downloadable sanitized ZIP, for example:

`u12-rc19-audio-forensics-<jobId>.zip`

Suggested contents:

```text
README.md
summary.json
firestore/job.json
cloud-run/execution.json
cloud-run/runtime.json
logs/worker.jsonl
storage/result-manifest.json        # only if still present
integrity/SHA256SUMS.txt
```

Security requirements:
- no Firebase auth/App Check/access/refresh tokens;
- no passwords;
- no Authorization headers;
- no resumable-upload session URLs;
- no service-account keys;
- redact user email when not required;
- preserve job/project/source/hash evidence.

Exit gate:
- bundle reproducible;
- SHA256SUMS verifies;
- all ten mandatory questions answered or explicitly marked unavailable with reason.

---

## 8. Phase F2 — local audio forensics — COMPLETE

Analyze:
- original managed source;
- supplied backing;
- supplied guitar;
- any recoverable remote manifest/provenance.

Metrics:
- container/encoding/rate/channels/frames;
- SHA-256;
- finite-sample count;
- absolute peak;
- RMS;
- mean/DC offset per channel;
- crest factor;
- channel correlation;
- silence/dropout windows;
- inter-reference alignment;
- backing + guitar recombination metrics;
- spectral sanity summaries sufficient to identify obvious DC/low-frequency contamination.

Do not convert subjective listening into a hard numeric threshold prematurely.

Exit:
- determine whether the suspicious DC component exists in source, is introduced by inference, or appears during post-processing.

---

## 9. Phase F3 — controlled diagnostic replay — COMPLETE

If F1/F2 do not isolate the defect, run a **separate diagnostic job** on the same source without mutating the user's project state.

The diagnostic path may retain worker-local artifacts temporarily in a diagnostic-only namespace with bounded TTL.

Capture all six stems before deletion:
- SHA-256;
- duration/format;
- peak/RMS/DC;
- finite-sample counts;
- per-channel metrics;
- reconstruction diagnostic;
- backing raw;
- guitar raw;
- final prepared backing/guitar;
- shared gain;
- final hashes.

Also capture the canonicalized input WAV.

The replay must not:
- increment normal user quota unless explicitly unavoidable and documented;
- become the project's authoritative active job;
- ACK/purge the physical project job;
- overwrite active project references.

If needed, compare against a separately controlled known-good Demucs reference implementation/baseline using the exact same source and model family. Any comparison result is diagnostic evidence, not an automatic production-engine replacement decision.

F3 exit result:
- input canonicalization is good;
- `demucs.cpp` corrupts separation semantics at inference;
- all stems acquire systematic DC and `other` captures most content;
- renderer behavior is deterministic and is not the originating defect;
- a DC blocker is insufficient by listening.

---

## 9A. Phase W — reconstruct the Cloud Run worker from the consolidated GBW baseline

This is the next mandatory source phase. Replace the rejected `demucs.cpp`/GGML inference boundary with the official PyTorch Demucs path proven by the consolidated GBW Linux application.

### 9A.1 Required engine contract

- engine family: official Demucs PyTorch CLI/library;
- model: `htdemucs_6s`;
- output: six float32 stems with clipping disabled;
- baseline parameters: `shifts=1`, `overlap=0.5`;
- device selected explicitly and recorded (`cpu` or qualified accelerator);
- exact Demucs, PyTorch, model, CUDA/runtime and container digests pinned;
- no runtime model/package download in a production job;
- model and Python wheels baked into or immutably mounted by the image;
- input canonicalization remains stereo float32 44.1 kHz;
- existing prepared-reference-v2 renderer and manifest ownership/hash contract remain unchanged unless evidence requires a separately reviewed change;
- BS-RoFormer remains excluded.

### 9A.2 Cloud/Firebase integration invariants

Preserve:
- callable enqueue/adoption/recovery semantics;
- stable uid/project/source/input generation identity;
- one accepted job and quota accounting boundary;
- immutable image digest dispatch;
- source upload validation;
- Firestore phase/state progression;
- result manifest committed last;
- ACK-before-purge and whole-prefix cleanup;
- cancellation/process-death recovery;
- only final backing/guitar/manifest exposed to Android;
- Android remote-to-local validation and canonical publication.

Change only the worker inference implementation and the observability/quality evidence needed to qualify it. Do not couple the engine migration to unrelated Android product changes.

### 9A.3 Performance policy

Musical quality is release-critical; performance is a bounded usability constraint for one representative song around three to four minutes:

- **optimal:** wall time `<= 5 minutes`;
- **acceptable:** wall time `> 5 and <= 15 minutes`;
- **alert/review required:** wall time `> 15 minutes`;
- a fast result that fails musical-quality gates is rejected regardless of time;
- a high-quality result above 15 minutes is not production-ready without an explicit resource/architecture review;
- the reported wall time must separate provisioning/cold start, model initialization, inference, render and publication.

Record one cold and one warm execution for the selected production shape. Additional repetitions and p50/p95 are useful only when sample count makes them meaningful or observed variance requires investigation. Runtime claims must name song duration, device/resource shape, shifts, overlap and image digest.

### 9A.4 Resource/strategy qualification matrix

Benchmark in isolated shadow jobs, never by mutating the production job in place. The selected CPU8 baseline is blocking; alternative matrix cells are comparative evidence and do not block when the selected shape already meets quality, cost and the acceptable time tier:

1. official Demucs CPU baseline using current `8 vCPU / 16 GiB` where viable;
2. tuned CPU shapes/threads supported by Cloud Run Jobs and project quota;
3. accelerator-backed execution only if available, quota-approved and materially better in time/cost;
4. `shifts=1`, `overlap=0.5` as the quality baseline;
5. parameter reductions only if AB metrics and human listening remain accepted;
6. concurrency/parallelism kept at one song per task until memory and interference are measured.

For each cell collect:
- provisioning/model-load/inference/render/publish durations;
- peak memory and CPU/accelerator utilization where available;
- estimated per-song cost;
- stem and prepared-reference metrics;
- hashes and determinism evidence;
- human listening result.

Select the least costly configuration inside the best jointly satisfied quality/time tier. Do not optimize cost by crossing a quality gate.

### 9A.5 Container and supply-chain design

- multi-stage, digest-pinned base image;
- lock Python, Demucs, PyTorch and audio dependencies with hashes;
- verify the exact `htdemucs_6s` checkpoint SHA during build and startup;
- run as non-root with read-only root filesystem where Cloud Run permits;
- no package manager/network model fetch at job runtime;
- bounded temporary disk sized for canonical input, six stems and prepared references;
- deterministic source materialization and semantic guards;
- SBOM/vulnerability scan retained as CI evidence;
- logs must expose versions/digests but no secrets.

### 9A.6 Reconstruction stages

**W0 — baseline freeze**
- extract and hash the supplied GBW v5.23 reference;
- record exact official CLI contract and defaults;
- add representative legally usable quality fixtures plus the private local physical fixture outside Git.

W0 evidence captured on 2026-09-24 (phase remains OPEN until fixture/adapter validation completes):
- canonical `main` baseline before worker writes: `46ed4ce4d760fda039799262f7a2f5474303c2a9`;
- `temporario/Guitar_Backing_Wizard_v5.23_Linux.zip` was inspected in-memory only; no loose extraction was retained or committed;
- the currently versioned ZIP is 423,767 bytes and hashes to `f56eee55c71a88083bbfb2651fc65df2b9986aafc9bb217b24b6140f1c421ea6`, which does **not** match the handoff-declared `ca4e0b1b95e9f308deb9ae8bccce673a091631105cb4fbd020909f6fef64ce4a`; preserve the archive unchanged and keep this discrepancy visible rather than silently normalizing it;
- the archive's GBW v5.23 Separador B invokes official Demucs with `htdemucs_6s`, `--float32`, `--clip-mode none`, configurable shifts/overlap, explicit `-d cpu|cuda`, output directory and the prepared WAV; its CPU defaults are `shifts=1` and `overlap=0.5`;
- reconstruction baseline pins official `demucs==4.1.0` and the `htdemucs_6s` checkpoint `5c90dfd2-34c22ccb.th` (54,996,327 bytes, SHA-256 `34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd`);
- the first qualification target remains CPU, one song/task, `shifts=1`, `overlap=0.5`; accelerator use remains a later evidence/cost decision, not a W0 assumption.

**W1 — worker adapter**
- introduce an engine-neutral runner boundary;
- implement official PyTorch Demucs runner;
- preserve cancellation and typed failures;
- retain six stems only inside worker/diagnostic scope.

W1 execution evidence (2026-09-24) — **CLOSED**:
- official engine boundary is `DemucsPyTorchRunner`, pinned to Demucs `4.1.0`, PyTorch `2.14.0+cpu`, checkpoint SHA-256 `34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd`;
- the image contains the checkpoint at build time, validates size/SHA, runs non-root and performs no model download during a production task;
- prepared-reference v2, six-stem-private boundary, cancellation and typed failures remain intact;
- U7 verification #136/#137 and subsequent W2 backend gates passed on canonical `main`.

**W2 — stage metrics and fail-closed quality checks**
- canonical input plus per-stem peak/RMS/DC/finiteness/energy;
- stem energy-distribution sanity so pathological `other` concentration is detected;
- backing/guitar raw and final metrics;
- no summed-stem SNR veto unless separately validated.

W2 execution evidence (2026-09-24) — **CLOSED for programmatic worker gates**:
- `audio_quality.py` measures canonical/stem finiteness, peak, RMS, mean/DC and energy distribution;
- hard rejects cover non-finite stems, gross amplitude explosion, silent stems and systematic same-polarity DC; `other` concentration is recorded diagnostically pending an accepted musical baseline rather than promoted to an unvalidated universal veto;
- backing/guitar raw/final and recombined metrics are retained; sampled summed-stem SNR remains diagnostic only;
- U7 #138–#141 passed; recovery is provenance-gated so RC19 `demucs.cpp` results cannot be adopted as RC20 work;
- Android U12ap now accepts prepared-reference v2 only from the official RC20 engine/checkpoint/CPU recipe while preserving schema-v1 legacy decoding.

W3/W4 qualification evidence (2026-09-24):
- U12aq introduced the deterministic royalty-free 180 s technical fixture and listening bundle; the synthetic fixture remains a performance/integrity tool and does **not** satisfy W5 musical listening acceptance;
- U7 #151 on digest `sha256:d8a53dd989c0d2c1bda22979f25d93456ec47137e750b1e0b8d648077d0e434f` passed verify, digest-pinned shadow deployment, runtime contract, U4 prepared-reference v2 transactional smoke and the CPU8/16 GiB technical W4 cell;
- #151 W4 metrics: `totalMs=325126`, inference `107613 ms`, render `116929 ms`, estimated cost `$0.057222176`, max RSS `126176 KiB`, `other` energy share `0.14379256`, zero reject findings; the full SHA-verified listening artifact was retained;
- U7 #152 reproduced a cross-host numeric delta of about 0.37% on a drum peak between the same digest on GitHub-hosted CPU and Cloud Run. The original 0.2% parity tolerance was therefore rejected as too narrow rather than treated as an audio failure;
- U12bd initially calibrated W3 to a 1% relative / 5e-6 absolute metric envelope while keeping provenance/quality gates exact and hashes diagnostic only;
- U7 #153 / run `36070537579` passed source/container verification, authentication, hardened image publication, dependency-closure capture, SBOM generation and the Trivy scan, then stopped before runtime/W3/W4 because Trivy inherited NVD's CRITICAL rating for `CVE-2023-45853` in Debian `zlib1g`; Debian marks it `will_not_fix`, supplies no fixed version and does not ship the affected unsupported `contrib/minizip` component;
- U12be keeps the complete scan as evidence and permits only that exact CVE/package/status/no-fix tuple. Any other CRITICAL, a changed package/status, or an available fixed version remains blocking;
- U7 #154 / run `36071506915` correctly kept the gate closed: the full report contained ten additional CRITICAL findings inherited by the Debian 12/FFmpeg package closure, so the MiniZip exception alone passed while the other findings remained blocking. A same-date controlled scan of the digest-pinned Python 3.12.14 Debian 13 base reported zero CRITICAL findings; U12bf therefore updates only the pinned OS base from Bookworm to Trixie while preserving the engine/model/recipe;
- U7 #155 / run `36072897237` passed build/publication/SBOM/scan on Debian 13 and reduced the full-image result to one NVD-scored CRITICAL: `CVE-2026-6653` in `libxml2`, with no fixed version. Red Hat and Ubuntu both rate the Linux finding MEDIUM. U12bg accepts only that exact CVE/package/status/no-fix/NVD-source/vendor-rating tuple and keeps the full finding visible in evidence;
- U7 #156 / run `36073588591` passed the supply-chain gate and produced exact engine/model/config/source contracts with zero quality rejects on local/container and Cloud Run for digest `sha256:6a02516afc215a66731b7d3d4f258cc062ebaaa235cd5e67e92e94bdcc1f2abc`, then failed only because 46/84 numeric comparisons exceeded the prior envelope. Official Demucs `--shifts 1` deliberately chooses random shifts, so sample closeness across independent executions is not a deterministic runtime promise; all six energy-share deltas nevertheless stayed below one percentage point;
- U7 #157 / run `36075604507` then proved the corrected D-091 W3 contract: backend/auth/supply-chain/runtime/U4 and W3 all passed. The CPU8 W4 Cloud Run execution itself completed and the downloaded bundle passed SHA256SUMS, but the local validator failed on a strategy-name assertion because the benchmark report exposes the canonical engine strategy (`pytorch-cpu-s1-o0.5-t8`) while the wrapper passes the matrix alias (`cpu8_s1_o05`). Artifact `u12-rc20-w4-shadow-f815e5dc...` / id `10840374632` was retained. This is a W4 validator-contract defect, not a demonstrated audio-quality failure; W5 remains blocked until the validator is corrected and W4 requalifies.
- U12bh corrects W3 to veto contract, structure, finiteness and independent quality failures, while retaining every numeric/hash delta and prior-envelope miss as audit evidence. This preserves detection of broken separation without making expected stochastic variation a release blocker;
- U12bi corrects only the W4 wrapper contract so the matrix alias `cpu8_s1_o05` is validated against the canonical engine strategy `pytorch-cpu-s1-o0.5-t8`;
- U7 #158 / run `36078452293` is **PASS** on producer `90428c81feca5eea4593ab384280846bd1175ccb`. Exact finalist shadow image: `sha256:6a6d5017e0e2c9bf3800bca3a3c3988ed9398add4ef2c3097241a7a260893dbe`. Backend/auth, dependency closure, CycloneDX SBOM, accepted narrow Trivy gate, runtime contract, real prepared-reference v2 smoke, corrected W3 and W4 all passed;
- #158 W4 CPU8 metrics: `totalMs=280648`, inference `100839 ms`, warm inference `89875 ms`, render `84971 ms`, max RSS `127600 KiB`, estimated cost `$0.049394048`, `other` energy share `0.1438911088`, zero quality findings; artifact id `10842185288`, artifact digest `sha256:198027e57f2aae4206b6f065ecc476994867bf8f438b39b1d33e1835cdf99523`;
- the #158 technical listening bundle is the deterministic synthetic 180 s fixture. It proves W4 integrity/performance but **cannot** satisfy W5 musical acceptance;
- W3 and W4 are **CLOSED**. CPU4 remains optional. **W5 owner musical listening is OPEN and is the only remaining Phase W gate before W6.** Production remains unchanged.

**W3 — local/container parity**
- same source and pinned image produce contract-equivalent stems across local controlled and Cloud Run shadow execution;
- engine/model/config/source identity, media structure, finiteness and independent quality acceptance are blocking;
- hashes, peaks, RMS, DC, energy, shared gain and energy-share deltas are retained as evidence but are not cross-run vetoes because `--shifts 1` is intentionally stochastic.

**W4 — shadow benchmark matrix**
- qualify the selected CPU8 shape; run alternative cells only when they can materially improve an unacceptable result or cost;
- require quality before ranking time/cost;
- retain a downloadable listening bundle for the selected finalist.

**W5 — human listening acceptance**
- owner listens to backing, guitar, individual stems and recombined reference;
- programmatic PASS without listening PASS cannot promote a worker.

**W6 — production cutover**
- deploy digest-pinned winner only after U7 verify/shadow PASS;
- verify runtime contract;
- execute representative quality gate;
- preserve automatic rollback to the last known production image;
- then run U4 transactional smoke.

### 9A.7 Phase W exit gate

Phase W closes only when:
- official Demucs output passes the retained structural/audio quality gates;
- no systematic DC/non-finite/pathological energy defect exists;
- the exact candidate digest passes the required proportional shadow qualification;
- representative wall time is `<= 15 minutes`, with `<= 5 minutes` preferred;
- cost/resource evidence is recorded;
- Firebase/Firestore/Storage lifecycle invariants remain green;
- production remains untouched until shadow qualification and rollback evidence pass.

---

## 10. Phase Q — quality-gate redesign

The current gates validate data integrity but are insufficient to establish useful musical output.

Use the existing legal technical fixture plus the owner's private representative listening source. A larger permanent real-audio corpus is optional and must not become a maintenance burden for this personal-use release.

The quality program must cover:
- no non-finite samples;
- format/frame/channel integrity;
- no gross DC contamination introduced by pipeline stages;
- no silent/near-silent stem/reference when source energy is present;
- bounded pathological energy explosions;
- reference recombination behavior;
- stable deterministic output for the pinned worker/model/runtime;
- plausible energy distribution across the six stems, including rejection of the observed failure where most content collapses into `other`;
- representative listening/metric comparison against an accepted baseline.

The quality program must also enforce the Phase W temporal tiers (`<=5 min` optimal, `>5–15 min` acceptable, `>15 min` alert) without allowing speed to override musical acceptance.

Do **not** re-promote summed-stem SNR as a hard veto without evidence that the chosen metric is valid for the model/runtime.

U4 remains a transactional real-cloud smoke. A separate quality gate must own musical-quality claims.

---

## 11. Phase SEARCH — primary-flow reliability; spelling assistance is optional

A target-device screen recording on 2026-09-24 exposed a separate release-blocking Prepare search defect:

- project title shown: `MMF - Misery`;
- artist entered: `memphys may fire`;
- song entered: `misery`;
- the UI entered `Pesquisando fontes compatíveis…` and showed progress for roughly 15 seconds;
- the operation then returned to the idle `Pesquisar fontes` button;
- no candidate, zero-result message, provider error, retry guidance or spelling suggestion remained visible.

This is unacceptable even if an Activity record exists in the background. **A search operation may never fail or complete silently in the active Prepare surface.**

### 11.1 Explicit terminal-state contract

Every search attempt must finish visibly in exactly one user-facing terminal class:

1. **RESULTS**
   - one or more compatible ranked candidates are shown;
   - count/status remains visible.

2. **NO_EXACT_MATCH**
   - providers were reached successfully;
   - no exact compatible candidate survived ranking;
   - show a persistent message such as `Nenhuma fonte compatível encontrada para esta busca.`;
   - if a high-confidence correction exists, show it in the same surface.

3. **DID_YOU_MEAN**
   - no exact match;
   - provider evidence contains a plausible artist/title correction;
   - show `Você quis dizer …?` with an explicit action to apply/research;
   - never silently rewrite the user's text and never auto-acquire a source.

4. **PROVIDER_FAILURE**
   - all/critical providers failed or validation failed;
   - show typed failure/retry copy;
   - never report this as an empty successful search.

5. **TIMEOUT**
   - show a persistent timeout message and explicit retry action.

6. **CANCELLED/REPLACED**
   - user cancellation is explicit;
   - an old search replaced by a new query may close quietly only because the replacement search itself is visibly active and later reaches one of the states above.

Returning directly from busy state to an indistinguishable idle form is forbidden.

### 11.2 Reuse existing matching intelligence

Do not introduce an LLM, remote spelling service or heavyweight dependency.

Reuse the existing core search machinery:
- `SourceSearchRules.normalize()`;
- `meaningfulTokens()`;
- `textSimilarity()`;
- existing Levenshtein implementation;
- provider candidate metadata (`title`, `uploader`, provider);
- existing ranking/official/duration signals.

The current exact ranking remains authoritative for source selection. Fuzzy logic is **suggestion-only** unless a later explicit contract changes it.

### 11.3 Candidate-derived correction strategy

When exact ranking is empty and providers were otherwise healthy:

1. retain a bounded sanitized pool of raw provider drafts before strict artist/title rejection;
2. compare requested artist/title to candidate uploader/title metadata;
3. derive at most a small number of high-confidence corrections;
4. prefer corrections supported by multiple candidates/providers or strong official/uploader evidence;
5. avoid suggestions based only on generic song titles;
6. require a calibrated minimum similarity and a meaningful improvement over the typed text;
7. expose the correction for user confirmation.

For the reproduced typo, a provider candidate containing `Memphis May Fire` should be able to yield:
`Você quis dizer “Memphis May Fire”?`

If the first exact provider query yields no usable metadata, one **bounded fallback discovery pass** may reuse the existing providers with a relaxed query solely to obtain suggestion metadata. It must:
- be rate/budget bounded;
- remain cancellable;
- not download media;
- not create a source operation;
- not turn a distant fuzzy match into an automatic candidate;
- preserve provider-failure typing.

No new external spelling API is required.

### 11.4 Search result model

Replace ad-hoc `candidates + warnings + global message/error` ambiguity with an explicit search outcome model carrying:
- operation id;
- normalized request;
- terminal state;
- candidates;
- provider warnings;
- zero-result reason;
- optional correction suggestions;
- typed provider/timeout error;
- start/end timestamps.

The active Prepare screen must render this outcome locally and persistently until:
- the user starts another search;
- applies a suggestion;
- imports/selects a source;
- explicitly dismisses/clears the result.

Activity must record the same terminal state. If the optional audit journal ships, it records that state too.

### 11.5 Suggestion UX

Recommended presentation directly below the search fields:

`Nenhuma correspondência exata foi encontrada.`

`Você quis dizer Memphis May Fire?`

Actions:
- **Usar “Memphis May Fire” e pesquisar novamente**
- **Manter minha busca** / edit fields

Requirements:
- preserve the song field unless its own correction is accepted;
- show artist/song corrections separately if both exist;
- never auto-submit an acquisition;
- accessible semantics and ≥48 dp touch targets;
- no raw provider exception text in primary copy.

### 11.6 Proportional automated coverage

Release-blocking coverage is limited to visible RESULTS/NO_EXACT_MATCH/PROVIDER_FAILURE/TIMEOUT behavior, cancellation/replacement correctness and one representative successful owner query. The deterministic typo-suggestion cases below are required only if DID_YOU_MEAN ships in RC20; otherwise they remain follow-up scope and the user can correct the query manually.

Core:
- `Memphys May Fire` vs candidate `Memphis May Fire` produces a high-confidence artist suggestion;
- exact `Memphis May Fire` produces no redundant suggestion;
- one-character and transposition typos are handled deterministically;
- distant/unrelated artists do not produce misleading suggestions;
- generic title collisions alone cannot suggest an unrelated artist;
- normalization remains accent/case/punctuation insensitive;
- suggestion ordering is deterministic.

Platform:
- healthy providers + raw near-match + zero strict ranking -> DID_YOU_MEAN;
- healthy providers + truly empty pool -> NO_EXACT_MATCH;
- provider exceptions -> PROVIDER_FAILURE, never NO_EXACT_MATCH;
- timeout -> TIMEOUT;
- bounded fallback suggestion pass executes at most once;
- cancellation stops both primary and fallback discovery.

Android/API 36:
- reproduced `memphys may fire` + `misery` flow never returns silently to idle;
- visible terminal message remains after busy indicator disappears;
- accepting suggestion updates the artist field and runs/arms the corrected search explicitly;
- rejecting suggestion preserves typed text;
- Activity receives the same terminal classification;
- navigation/recomposition does not erase the terminal result unexpectedly.

If the optional diagnostics journal/export package ships, it includes search lifecycle events and provider warning codes without credentials or raw sensitive headers.

---

## 12. Phase S — explicit Studio reference reinsertion — owner-requested completion feature before final freeze

Add a secondary action beside `Abrir Studio` in the Ready state:

**Recolocar referências no Studio**

Contract:
- local-only;
- zero Cloud Run;
- zero Firebase download;
- zero quota effect;
- uses current `activeBackingAssetId` / `activeGuitarAssetId`;
- repairs missing reference bindings/clips;
- idempotent;
- preserves user recordings/takes;
- preserves unrelated clips;
- preserves mixer/loop/sections;
- works immediately after the user deletes prepared-reference clips;
- does not require app restart.

Domain semantics must distinguish:
- **new prepared revision available**;
- **current prepared revision acknowledged**;
- **current prepared revision bindings/clips missing and repairable**.

Automated tests:
- delete backing clip -> action reappears -> backing restored;
- delete guitar L/R/reference clips -> restored;
- repeat action -> no duplicates;
- unrelated audio on target/reference lanes is not destructively replaced without explicit contract;
- reopen remains consistent.

---

## 13. Phase P — Prepare technical-detail redesign — owner-requested completion feature before final freeze

The current `PrepareDiagnostics` must stop presenting every historical asset as an undifferentiated flat list.

Normal view:

### Source active
- format/size;
- shortened SHA;
- copy full SHA action.

### Latest/active processing
- job id;
- state;
- engine/model;
- strategy;
- recipe/contract;
- manifest SHA;
- timestamps where available.

### Active references
- active backing;
- active guitar;
- clear local SHA;
- clear remote SHA when provenance contains one;
- active job/generation label.

Advanced/historical view:
- group by job/generation;
- distinguish `ACTIVE`, `HISTORICAL`, `SUPERSEDED`, `DERIVED CHANNEL`;
- label guitar L/R derivatives as children of the guitar reference;
- never label all three as identical "Guitarra de referência";
- do not duplicate the same source line without generation context.

Tests must assert ordering/grouping and that active references are unambiguous.

---

## 14. Phase D — consolidated Diagnostics in Settings — owner-requested completion feature before final freeze

Create one dedicated surface:

**Configurações → Diagnóstico**

Organize without duplicated information:

1. **Aplicativo e dispositivo**
   - versionName/versionCode;
   - source/producer identity where embedded;
   - Android/device summary;
   - storage health.

2. **Áudio e USB**
   - selected/effective input/output;
   - rate/buffer;
   - underruns;
   - route changes;
   - calibration/session health.

3. **Separação em nuvem**
   - recent jobs;
   - project/source generation;
   - job state;
   - engine/model/strategy/recipe;
   - manifest/provenance;
   - typed failures.

4. **Projeto atual**
   - active asset graph;
   - active reference bindings;
   - detected inconsistencies;
   - repairable missing-reference state.

5. **Backup e nuvem**
   - latest backup/restore status;
   - provider state;
   - typed errors.

6. **Registro de eventos**
   - human-readable recent journal;
   - clear/export actions with explicit scope.

No raw token/session/security data is ever shown.

---

## 15. Phase J — persistent audit journal — owner-requested completion feature before final freeze

Implement a structured local event journal instead of relying only on Logcat.

Recommended contract:
- JSON Lines;
- schema versioned;
- UTC timestamp + monotonic time where meaningful;
- bounded rotating storage;
- default retention: 14 days or 8 MiB, whichever comes first;
- append-only during normal operation;
- sanitized at write time.

Core fields:
- event type;
- app version/versionCode/source identity;
- projectId;
- operationId;
- jobId;
- sourceAssetId/input SHA;
- pipeline stage;
- typed result/error;
- relevant audio metadata;
- route identity when applicable;
- remote engine/model/recipe where applicable.

Events to include at minimum:
- source acquisition lifecycle;
- enqueue/adopt/recovery;
- worker-status transitions observed by client;
- manifest accepted;
- reference downloads accepted;
- local reference publication;
- ACK requested/confirmed;
- binding repair;
- recording route/start/stop/error;
- backup start/complete/error;
- app/process lifecycle markers relevant to recovery.

Never persist:
- passwords;
- auth/App Check/access/refresh tokens;
- Authorization headers;
- service-account data;
- resumable session URLs.

---

## 16. Phase X — exportable diagnostic ZIP — owner-requested completion feature before final freeze

Expose:
**Exportar pacote de diagnóstico**

Default ZIP contains metadata/logs only, not song audio.

Suggested structure:

```text
GuitarLab-Diagnostics-<timestamp>.zip
├── README.txt
├── app.json
├── device.json
├── events.jsonl
├── audio-route.json
├── project/
│   ├── summary.json
│   ├── active-assets.json
│   └── reference-bindings.json
├── separation/
│   ├── jobs.json
│   ├── manifests/
│   └── audio-metrics.json
├── backup/
│   └── status.json
└── integrity/
    └── SHA256SUMS.txt
```

Important:
- persist a **sanitized local copy of every accepted remote manifest before ACK/purge**;
- retain enough provenance to reconstruct remote→local reference lineage after remote cleanup;
- audio files are excluded by default;
- any future "include media" option must be explicit and separate.

Tests:
- deterministic ZIP structure;
- SHA256SUMS correctness;
- redaction scans;
- malformed journal tolerance;
- bounded storage/rotation;
- export works offline.

---

## 17. Phase C — cloud structured observability — extend only when needed

Extend worker/backend logs so one job id reconstructs the complete lifecycle.

Structured events should include:
- worker execution start;
- image digest/revision;
- engine/model/model SHA;
- strategy/thread plan;
- input SHA;
- canonical input metrics;
- each stem's non-sensitive audio metrics;
- reconstruction diagnostic;
- prepared-reference metrics;
- sharedGainDb;
- final deliverable hashes;
- manifest SHA;
- publication commit;
- Firestore state transition;
- ACK/purge events.

Cloud logs must remain free of secrets and authentication material.

---

## 18. Proportional test and qualification matrix

Only tests covering changed code or the representative release path are mandatory for RC20. Existing broad regression remains valuable and may continue to execute unchanged; an unrelated flaky/cosmetic assertion may be quarantined with evidence rather than indefinitely block a personal-use candidate. No known data-loss, audio-integrity, primary-flow, quota/cost, credential or signing failure may be quarantined.

### Mandatory RC20 worker/backend evidence
- official PyTorch Demucs/GBW runner contract and dependency locks;
- `demucs.cpp` absent from the RC20 production path;
- exact engine/model/recipe/source identity;
- frame/channel/finite-sample structure;
- independent hard audio-quality acceptance, including systematic-DC rejection;
- selected CPU8 cold/warm performance breakdown with the `<=15 min` release ceiling;
- finalist listening bundle and owner musical acceptance;
- no secret leakage;
- digest-pinned shadow/runtime evidence.

### Mandatory RC20 Android evidence
- source search reaches a visible terminal result: results, no exact match, provider failure, timeout or explicit cancel/replacement;
- provider failure is never collapsed into successful empty search;
- relevant unit/Lint/build/API36 regression for the actual Android changes and adjacent primary flow;
- save/reopen and Prepare → Studio handoff remain healthy where touched by the corrective.

DID_YOU_MEAN-specific tests are required only if that optional enhancement ships in RC20.

### Mandatory real-cloud evidence
- U7 verify/shadow for the finalist backend;
- controlled production cutover with rollback target preserved;
- post-cutover U4 transactional smoke;
- representative audio-quality evidence on the finalist digest;
- no duplicate quota/job or broken ACK/purge behavior.

### Owner-requested completion evidence — required before final freeze, but not before backend cutover
The following evidence is required when the corresponding owner-requested completion feature is implemented. These items do not block W5/W6 or the PyTorch production cutover by themselves:

- reference-binding repair/reinsertion tests;
- active-vs-historical Prepare diagnostics grouping;
- consolidated Settings diagnostics;
- persistent journal serialization/rotation/redaction;
- diagnostic ZIP integrity/export;
- accepted-manifest local audit persistence required by the diagnostic package.

### Conditional evidence — only when needed for an observed blocker
- extended structured cloud logging;
- alternative CPU/resource/hardware/duration matrices.

### Physical release minimum
On the exact signed RC20 APK on SM-X230 / Android 16:
- reproduce/retest a representative real source;
- confirm audible prepared-reference quality;
- complete the owner's normal search/acquire → Prepare → Studio → save/reopen path;
- confirm an interrupted/retried Prepare does not duplicate quota or lose project state;
- approve the exact signed APK intended for freeze.

MK-300, Drive target-device smoke and other capability-specific checks are included when that capability is part of the frozen baseline being accepted or when the RC20 delta can materially affect it. Diagnostics ZIP/journal and exhaustive matrices are not prerequisites by themselves.

---

## 19. Release gates

The RC20 **signed homologation candidate** may be produced when:

1. F1/F2/F3 forensic work has identified the root-cause boundary;
2. Phase W official-Demucs worker reconstruction is implemented;
3. the exact finalist worker passes owner listening and the `<=15 min` performance ceiling;
4. the owner's primary search/acquisition path reaches a visible terminal state; typo suggestion is desirable but not blocking when the correct query works and errors do not disappear silently;
5. relevant unit/Lint/build/API 36 tests pass, with unrelated quarantines explicitly justified;
6. U7 verify/shadow passes for the finalist digest;
7. the controlled production cutover passes with the previous production digest retained as rollback target;
8. post-cutover U4 transactional smoke passes;
9. representative audio-quality evidence passes;
10. the exact qualified unsigned Android artifact is signed without recompilation and package/version/certificate/SHA evidence agrees.

**Final physical homologation occurs after step 10**, on that exact signed APK. It requires owner acceptance of the representative normal workflow and any capability-specific physical checks that are actually part of the frozen baseline.

Studio reinsertion, Prepare diagnostics reorganization, consolidated Diagnostics, audit journal, diagnostic ZIP, extended structured observability and exhaustive matrices are not prerequisites for the signed personal homologation APK. They remain backlog items and become blocking only if shipped in RC20 or needed to resolve an observed release-critical defect.

Any source change after RC19 freeze creates a new candidate identity:
- target: `0.5.0-rc20`;
- versionCode: `40`;
- existing signing identity preserved.

---

## 20. Execution order

Mandatory order:

**F1 production forensics (read-only)**
→ **F2 local audio forensics**
→ **F3 controlled diagnostic replay**
→ **Case B / demucs.cpp root cause CLOSED**
→ **W official PyTorch Demucs worker reconstruction**
→ **W selected-shape shadow quality/performance/cost qualification**
→ **W owner listening acceptance**
→ controlled production cutover + rollback proof
→ **SEARCH non-silent terminal UX + representative owner acquisition**
→ relevant Android/U7/U4 qualification
→ signed RC20
→ focused physical acceptance.

After homologation, and only as needed or explicitly desired:

**DID_YOU_MEAN enhancement**
→ **S Studio reinsertion**
→ **P Prepare diagnostics cleanup**
→ **D/J/X diagnostics + audit export**
→ **C extended cloud observability**
→ remaining optional U12 residuals.

UI/diagnostic improvements may be designed in parallel after F1, but no audio-algorithm correction may bypass the forensic decision point.

---

## 21. Exit criteria

The personal-use RC20 release line closes when all of the following are true:

- the physical RC19 job lineage is concretely reconstructed;
- it is known whether a fresh Cloud Run execution occurred;
- the exact worker image/model/strategy and output lineage are proven;
- the `demucs.cpp` bad-audio boundary is replaced by the qualified official PyTorch Demucs worker;
- owner listening accepts the exact finalist stems/backing/guitar;
- representative worker time is `<=15 minutes`, preferably `<=5 minutes`, with single-run timing/cost evidence; historical warm evidence is retained but is not rerun by default;
- a representative real-source gate prevents recurrence;
- source search never terminates silently and the owner's representative acquisition succeeds;
- the successor signed candidate passes focused physical listening;
- normal Prepare → Studio → save/reopen use has no release-blocking defect.

After the signed RC20 backend/primary-flow homologation, spelling suggestions and extended cloud observability may remain follow-up scope. However, the owner-requested completion features — reference reinsertion, Prepare diagnostics cleanup, consolidated Diagnostics, persistent journal and diagnostic ZIP — remain planned work and must be completed before the final product freeze unless the owner explicitly removes them.


### W6 promotion identity update — 2026-09-24/25

- U7 #164 / run `36085821470`: PASS;
- qualified worker: `us-central1-docker.pkg.dev/gbwapp-ef048/gbw/remote-worker@sha256:14e240cb01b71131cb049dd34e0df078614238da3514325f80126d56f8d5e698`;
- rollback target before cutover: production digest `sha256:a70bd221ab50ef092508781c609cbfbecfe6b71ba4c8a722fc5f087b09dbd550`;
- D-092 waives W5;
- D-095 requires W6 to promote the exact qualified digest with no rebuild, followed by runtime-contract verification and full U4 transactional smoke with automatic rollback on failure.


### RC20 Android completion implementation — U12bn

Implemented before final freeze:
- persistent typed Prepare search terminal states: results, healthy zero-match, bounded deterministic suggestion, provider failure and timeout;
- explicit retry and “Você quis dizer…” UX, never silent return to idle;
- local-only idempotent “Recolocar referências no Studio” using the canonical binding policy, with zero cloud/quota use;
- Prepare technical details grouped by active source, latest processing, active references/derived channels and historical assets;
- consolidated Settings → Diagnostics surface;
- sanitized JSONL event journal bounded to 14 days / 8 MiB;
- diagnostic ZIP with app/device/events/audio route/activity/project/separation/manifest/backup metadata plus SHA256SUMS; song audio excluded by default;
- sanitized accepted remote manifests retained locally after validation and before ACK/purge;
- RC20 Android identity 0.5.0-rc20 / versionCode 40.

U12bn is source-materialized fail-closed. Digital Android/API36 qualification remains required before signed homologation candidate creation.


### RC20 signed homologation candidate

- digital qualification: Android CI #893 PASS;
- signed candidate run: Android CI #894 PASS;
- producer commit: `fd63413ea440ed96a227b0203768b113bf256e98`;
- package: `studio.guitarlab.app`;
- version: `0.5.0-rc20` / versionCode `40`;
- unsigned APK SHA-256: `adc2deab513d8b3d311015887199a6229eb46518daaa351837f25e40dc8b39b3`;
- signed APK SHA-256 `e82fdc75896564ea10c28e072fd186913320eca293b6fad9ea4b7c8fa0468468`;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- remaining gate: consolidated physical homologation only; no rebuild is allowed before that validation.


### U12bu post-candidate delta

After the first signed RC20 physical candidate, the owner requested one additional frontend/support cleanup before physical acceptance. U12bu removes dead/redundant Settings surfaces and adds actionable cancellation to Activity, including Android and Firebase/Cloud orphan cleanup. The implementation is committed and source-materialized but deliberately has not triggered Android CI or produced a new APK. Therefore the earlier signed RC20 artifact remains historical pre-U12bu evidence, not the final physical artifact for the new HEAD.


### U12bw waveform and reference restoration

Implemented as the final post-validation hardening candidate before another physical pass:

- waveform cache v2 validates the exact represented media/window rather than trusting clip id alone;
- stale/legacy cache entries regenerate lazily; unchanged entries cause zero WAV reads on normal reopen;
- waveform envelopes follow the exact playback source window and use 4096 bins;
- rendering collapses bins to visible columns by maximum peak, keeping transient fidelity with bounded draw cost;
- same-project Studio reentry compares the persisted project snapshot before deciding whether resident media/waveforms are still valid;
- static clip waveform geometry shares the exact timeline width, without horizontal inset or artificial 92 dp duration;
- aggregate and stereo L/R waveform state refresh together across load, edits, Undo/Redo, takes and recovery;
- “Recolocar referências no Studio” is available whenever prepared references exist and offers explicit Base/Guitar selection;
- selected reference lanes are restored canonically from the active prepared assets; user recordings/takes, mixer state, markers/sections and unselected references are preserved.

The implementation must pass software + API36 gates and then be signed as the next homologation APK. Final documentation/freeze remains contingent on owner physical acceptance.


### U12bw waveform/reference candidate

U12bw completed canonical Android qualification in CI #902 and produced an exact signed homologation artifact. Waveform cache v2/media-window identity, exact frame-window envelope generation, 4096-bin peak-preserving rendering, timeline geometry correction, same-project revision reconciliation and selective canonical reference restore are digitally closed. Physical acceptance of this exact APK is the remaining release gate before final documentation/freeze. Signed APK SHA-256: `529834d908a0bf69c09182ebe08ca3a6269f7d776c57b37a76dbabcedae5c60f`.
