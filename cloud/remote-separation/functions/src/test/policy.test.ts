import test from "node:test";
import assert from "node:assert/strict";
import {
  ACTIVE_STATES,
  BACKEND_POLICY_REVISION,
  MAX_MONTHLY_JOBS,
  RESULT_RECOVERY_WINDOW_MS,
  inputPath,
  isIdempotentJob,
  monthKey,
  requireAckable,
  retentionAction,
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
  assert.equal(BACKEND_POLICY_REVISION, "rc18-q40-single8-prepared-v2"));

test("retention preserves recent results and expires abandoned imports after recovery window", () => {
  const now = Date.UTC(2026, 8, 23, 12);
  assert.equal(retentionAction("COMPLETED", now - RESULT_RECOVERY_WINDOW_MS + 1, now), null);
  assert.equal(retentionAction("IMPORT_FAILED", now - RESULT_RECOVERY_WINDOW_MS - 1, now), "EXPIRE_AND_PURGE");
  assert.equal(retentionAction("IMPORTED", now - RESULT_RECOVERY_WINDOW_MS - 1, now), "PURGE_ONLY");
  assert.equal(retentionAction("RUNNING", now - RESULT_RECOVERY_WINDOW_MS - 1, now), null);
});
