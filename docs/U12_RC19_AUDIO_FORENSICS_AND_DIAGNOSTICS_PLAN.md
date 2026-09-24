# U12 — RC19 audio forensics, observability and diagnostics plan

Updated: 2026-09-24
Status: **ACTIVE / RELEASE-BLOCKING PHYSICAL INCIDENT**
Current signed candidate under investigation: **0.5.0-rc19 / versionCode 39**
Next signed candidate if source changes are required: **0.5.0-rc20 / versionCode 40**
Repository authority: `anfalcir/guitarlab` / `main`

## 1. Purpose

RC19 closed the authoritative same-generation remote recovery defect and passed its complete digital qualification, but target-device listening exposed a separate release-blocking audio-quality incident after a real Prepare run.

This plan owns the investigation and the related product/observability corrections. It intentionally separates:

1. forensic proof of what actually executed in Firebase/Cloud Run;
2. audio-content diagnosis;
3. Studio reference reinsertion UX;
4. Prepare technical-detail cleanup;
5. durable application/cloud diagnostics;
6. quality-gate hardening.

Do **not** modify the separation algorithm merely to make the observed audio sound different before the evidence phases below identify the failing boundary.

U12 remains open. RC19 is a digitally qualified but physically **not accepted** candidate.

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
- measured mean/DC component is approximately `-0.0473` per backing channel and approximately `-0.0090` per guitar channel. This is diagnostic evidence only; it is **not yet established as the root cause**.

Observed physical result:
- separation finished and references were imported;
- audio remained subjectively unusable;
- current digital integrity gates did not reject it.

The incident must therefore be diagnosed as a content/lineage/runtime-quality problem, not merely a WAV-container or hash-transfer problem.

---

## 3. Current known architecture relevant to the incident

The production v2 contract is intentionally narrow:

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

The current U4/U7 gates therefore prove transactional and numerical integrity, not representative musical separation quality.

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

6. **Do not reintroduce the rejected partitioned Demucs strategy.**
   RC18/RC19 `single8` remains the production baseline unless new evidence justifies a separately qualified change.

7. **No signed successor until forensic and quality gates are green.**

---

## 5. Root-cause decision tree

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

## 6. Phase F1 — read-only production forensics

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

## 8. Phase F2 — local audio forensics

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

## 9. Phase F3 — controlled diagnostic replay

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

---

## 10. Phase Q — quality-gate redesign

The current gates validate data integrity but are insufficient to establish useful musical output.

Add a dedicated real-audio regression corpus with legally/project-appropriate fixtures and deterministic expected properties.

The quality program must cover:
- no non-finite samples;
- format/frame/channel integrity;
- no gross DC contamination introduced by pipeline stages;
- no silent/near-silent stem/reference when source energy is present;
- bounded pathological energy explosions;
- reference recombination behavior;
- stable deterministic output for the pinned worker/model/runtime;
- representative listening/metric comparison against an accepted baseline.

Do **not** re-promote summed-stem SNR as a hard veto without evidence that the chosen metric is valid for the model/runtime.

U4 remains a transactional real-cloud smoke. A separate quality gate must own musical-quality claims.

---

## 11. Phase SEARCH — source-search reliability, terminal UX and spelling suggestions

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

Activity and the new audit journal must record the same terminal state.

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

### 11.6 Mandatory automated coverage

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

The diagnostics journal/export package must include search lifecycle events and provider warning codes without credentials or raw sensitive headers.

---

## 12. Phase S — explicit Studio reference reinsertion

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

## 13. Phase P — Prepare technical-detail redesign

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

## 14. Phase D — consolidated Diagnostics in Settings

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

## 15. Phase J — persistent audit journal

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

## 16. Phase X — exportable diagnostic ZIP

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

## 17. Phase C — cloud structured observability

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

## 18. Test and qualification matrix

### Unit/core
- reference-binding repair semantics;
- active-vs-historical diagnostic grouping;
- journal serialization/rotation/redaction;
- diagnostic ZIP integrity;
- manifest audit persistence;
- audio metric calculations.

### Android/API 36
- Ready state exposes `Recolocar referências no Studio` only when appropriate;
- repair without restart;
- no duplicate reference clips;
- historical asset UI is unambiguous;
- Settings diagnostics navigation;
- ZIP export through SAF/share path;
- process death/reopen preserves journal and accepted-manifest audit.

### Worker/backend
- structured log schema;
- diagnostic replay isolation;
- no secret leakage;
- image/model/runtime evidence;
- audio metric generation.

### Real cloud
- forensic workflow against a known job;
- standard U4 transactional smoke;
- separate representative-audio quality gate;
- no mutation in forensic mode.

### Physical
On SM-X230 / Android 16:
- reproduce/retest real source;
- confirm audible quality;
- verify reference reinsertion without restart;
- export diagnostics ZIP after a real Prepare run;
- verify logs identify the exact Cloud Run job/execution;
- continue remaining U12 MK-300/recording/10-minute residuals only after Prepare audio is usable.

---

## 19. Release gates

No successor APK may be signed until:

1. F1 forensic report is complete;
2. root-cause boundary is identified;
3. required source fix is implemented;
4. source search has explicit non-silent terminal states and typo suggestions are green;
5. Studio reinsertion action is green;
6. Prepare diagnostics are de-duplicated;
7. local audit journal + diagnostic ZIP are green;
8. worker/backend observability is green where required;
9. unit/Lint/build/API 36 pass;
10. U7 verify/shadow/production gates pass when backend changes exist;
11. U4 passes;
12. representative audio-quality gate passes;
13. physical listening acceptance passes.

Any source change after RC19 freeze creates a new candidate identity:
- target: `0.5.0-rc20`;
- versionCode: `40`;
- existing signing identity preserved.

---

## 20. Execution order

Mandatory order:

**F1 production forensics (read-only)**
→ **F2 local audio forensics**
→ if necessary **F3 controlled diagnostic replay**
→ root-cause decision
→ source correction
→ **SEARCH explicit terminal UX + typo suggestions**
→ **S Studio reinsertion**
→ **P Prepare diagnostics cleanup**
→ **D/J/X diagnostics + audit export**
→ **C cloud observability**
→ quality-gate closure
→ Android/U7/U4 digital qualification
→ signed RC20
→ focused physical acceptance
→ remaining U12 final-homologation residuals.

UI/diagnostic improvements may be designed in parallel after F1, but no audio-algorithm correction may bypass the forensic decision point.

---

## 21. Exit criteria

This plan closes only when all of the following are true:

- the physical RC19 job lineage is concretely reconstructed;
- it is known whether a fresh Cloud Run execution occurred;
- the exact worker image/model/strategy and output lineage are proven;
- the bad-audio boundary is identified and corrected;
- a representative real-source gate prevents recurrence;
- source search never terminates silently and high-confidence spelling corrections can be suggested from existing provider evidence;
- prepared references can be reinserted into Studio without restart;
- Prepare details show active vs historical data without ambiguity;
- Settings owns one coherent Diagnostics surface;
- the app can export a sanitized evidence ZIP suitable for future incident analysis;
- the successor signed candidate passes focused physical listening;
- U12 can resume/complete its remaining hardware acceptance with no release-blocking Prepare audio defect.
