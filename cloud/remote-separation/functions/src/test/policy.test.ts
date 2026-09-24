import test from "node:test";
import assert from "node:assert/strict";
import {
  ACTIVE_STATES,
  BACKEND_POLICY_REVISION,
  MAX_MONTHLY_JOBS,
  RESULT_RECOVERY_WINDOW_MS,
  WORKER_CONTRACT,
  inputPath,
  isIdempotentJob,
  monthKey,
  requireAckable,
  retentionAction,
  decideEnqueue,
  sameGeneration,
} from "../policy";

test("month key is UTC stable", () =>
  assert.equal(monthKey(new Date("2026-09-30T23:59:59Z")), "2026-09"));

test("input path is namespaced", () =>
  assert.equal(
    inputPath("u", "123e4567-e89b-42d3-a456-426614174000", "MP3"),
    "remote/v1/users/u/jobs/123e4567-e89b-42d3-a456-426614174000/input/source.mp3",
  ));

test("idempotency binds project, source asset, and hash", () => {
  const hash = "a".repeat(64);
  assert.equal(
    isIdempotentJob({projectId: "p", sourceAssetId: "source", inputSha256: hash}, "p", "source", hash),
    true,
  );
  assert.equal(
    isIdempotentJob({projectId: "p", sourceAssetId: "source", inputSha256: hash}, "x", "source", hash),
    false,
  );
  assert.equal(
    isIdempotentJob({projectId: "p", sourceAssetId: "source", inputSha256: hash}, "p", "other-source", hash),
    false,
  );
  assert.equal(
    isIdempotentJob({projectId: "p", inputSha256: hash}, "p", "source", hash),
    false,
  );
});

test("only completed/importing/imported are ackable", () => {
  requireAckable("COMPLETED");
  requireAckable("IMPORTED");
  assert.throws(() => requireAckable("RUNNING"));
});

test("monthly remote separation allowance is 40", () =>
  assert.equal(MAX_MONTHLY_JOBS, 40));

test("cancel requested still occupies the single active-job slot", () =>
  assert.equal(ACTIVE_STATES.has("CANCEL_REQUESTED"), true));

test("backend policy revision is explicit", () =>
  assert.equal(BACKEND_POLICY_REVISION, "rc20-q40-official-demucs-prepared-v2-recovery"));

test("retention preserves recent results and expires abandoned imports after recovery window", () => {
  const now = Date.UTC(2026, 8, 23, 12);
  assert.equal(retentionAction("COMPLETED", now - RESULT_RECOVERY_WINDOW_MS + 1, now), null);
  assert.equal(retentionAction("IMPORT_FAILED", now - RESULT_RECOVERY_WINDOW_MS - 1, now), "EXPIRE_AND_PURGE");
  assert.equal(retentionAction("IMPORTED", now - RESULT_RECOVERY_WINDOW_MS - 1, now), "PURGE_ONLY");
  assert.equal(retentionAction("RUNNING", now - RESULT_RECOVERY_WINDOW_MS - 1, now), null);
});


test("same generation recovery wins without quota consumption or new dispatch", () => {
  const hash = "a".repeat(64);
  const active = [{jobId: "job-good", projectId: "p", sourceAssetId: "source", inputSha256: hash, state: "COMPLETED", workerContract: WORKER_CONTRACT}];
  assert.equal(sameGeneration(active[0], "p", "source", hash), true);
  assert.deepEqual(decideEnqueue(active, 40, "p", "source", hash), {
    kind: "RECOVER_EXISTING",
    jobId: "job-good",
  });
});

test("same generation adoption wins safely even when unrelated active work is present", () => {
  const hash = "a".repeat(64);
  assert.deepEqual(
    decideEnqueue(
      [
        {jobId: "preserved-other", projectId: "other", sourceAssetId: "other-source", inputSha256: "b".repeat(64), state: "RUNNING", workerContract: WORKER_CONTRACT},
        {jobId: "job-good", projectId: "p", sourceAssetId: "source", inputSha256: hash, state: "COMPLETED", workerContract: WORKER_CONTRACT},
      ],
      10,
      "p",
      "source",
      hash,
    ),
    {kind: "RECOVER_EXISTING", jobId: "job-good"},
  );
});

test("different generation and different project are typed active conflicts", () => {
  const hash = "a".repeat(64);
  assert.deepEqual(
    decideEnqueue(
      [{jobId: "other-generation", projectId: "p", sourceAssetId: "source", inputSha256: "b".repeat(64)}],
      10,
      "p",
      "source",
      hash,
    ),
    {kind: "ACTIVE_JOB_CONFLICT", jobId: "other-generation"},
  );
  assert.deepEqual(
    decideEnqueue(
      [{jobId: "other-project", projectId: "other", sourceAssetId: "source", inputSha256: hash}],
      10,
      "p",
      "source",
      hash,
    ),
    {kind: "ACTIVE_JOB_CONFLICT", jobId: "other-project"},
  );
});

test("quota is distinct from active-job conflict and only applies without active work", () => {
  const hash = "a".repeat(64);
  assert.deepEqual(decideEnqueue([], MAX_MONTHLY_JOBS, "p", "source", hash), {
    kind: "MONTHLY_QUOTA_REACHED",
  });
  assert.deepEqual(decideEnqueue([], MAX_MONTHLY_JOBS - 1, "p", "source", hash), {kind: "CREATE"});
});


test("superseded RC19 completed same-generation result is not adopted", () => {
  const hash = "a".repeat(64);
  const legacy = [{
    jobId: "legacy-completed",
    projectId: "p",
    sourceAssetId: "source",
    inputSha256: hash,
    state: "COMPLETED",
    workerContract: "demucs-cpp-single8-v1",
  }];
  assert.deepEqual(decideEnqueue(legacy, 10, "p", "source", hash), {kind: "CREATE"});
});

test("superseded same-generation running work still blocks concurrent inference", () => {
  const hash = "a".repeat(64);
  const legacy = [{
    jobId: "legacy-running",
    projectId: "p",
    sourceAssetId: "source",
    inputSha256: hash,
    state: "RUNNING",
    workerContract: "demucs-cpp-single8-v1",
  }];
  assert.deepEqual(decideEnqueue(legacy, 10, "p", "source", hash), {
    kind: "ACTIVE_JOB_CONFLICT",
    jobId: "legacy-running",
  });
});
