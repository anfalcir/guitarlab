import test from "node:test";
import assert from "node:assert/strict";
import {
  ACTIVE_STATES,
  BACKEND_POLICY_REVISION,
  MAX_MONTHLY_JOBS,
  inputPath,
  isIdempotentJob,
  monthKey,
  requireAckable,
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
  assert.equal(BACKEND_POLICY_REVISION, "rc5-q40-mt4-omp2-v1"));
