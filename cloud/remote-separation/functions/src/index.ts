import {ExecutionsClient, JobsClient} from "@google-cloud/run";
import {Storage} from "@google-cloud/storage";
import {initializeApp} from "firebase-admin/app";
import {FieldValue, getFirestore} from "firebase-admin/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {onSchedule} from "firebase-functions/v2/scheduler";
import {logger} from "firebase-functions";
import {ACTIVE_STATES, BACKEND_POLICY_REVISION, JOB_ID, MAX_INPUT_BYTES, MAX_MONTHLY_JOBS, RESULT_RECOVERY_WINDOW_MS, SHA256, decideEnqueue, isIdempotentJob, monthKey, requireAckable, retentionAction, sameGeneration} from "./policy";

initializeApp();
const db = getFirestore();
const storage = new Storage();
const jobsClient = new JobsClient();
const executionsClient = new ExecutionsClient();
const region = process.env.GBW_REGION || "us-central1";
const project = process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT || "";
const bucketName = process.env.GBW_BUCKET || "";
const runJobName = process.env.GBW_RUN_JOB || `projects/${project}/locations/${region}/jobs/gbw-demucs`;
const allowedUids = new Set((process.env.GBW_ALLOWED_UIDS || "").split(",").map(v => v.trim()).filter(Boolean));
const runtimeServiceAccount = process.env.GBW_ORCHESTRATOR_SA;
const runtimeIdentity = runtimeServiceAccount ? {serviceAccount: runtimeServiceAccount} : {};

type Caller = {uid: string};
function caller(request: {auth?: {uid: string}; app?: unknown}): Caller {
  if (!request.auth) throw new HttpsError("unauthenticated", "Authentication required.");
  if (!request.app && process.env.GBW_REQUIRE_APP_CHECK === "true") throw new HttpsError("failed-precondition", "App Check required.");
  if (!allowedUids.has(request.auth.uid)) throw new HttpsError("permission-denied", "Account is not authorized.");
  return {uid: request.auth.uid};
}
function text(value: unknown, key: string, regex: RegExp): string {
  if (typeof value !== "string" || !regex.test(value)) throw new HttpsError("invalid-argument", `Invalid ${key}.`);
  return value;
}
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

async function cleanupUnregisteredUpload(uid: string, jobId: string): Promise<void> {
  await storage.bucket(bucketName).deleteFiles({prefix: `remote/v1/users/${uid}/jobs/${jobId}/`})
    .catch(error => logger.warn("unregistered upload cleanup failed", {jobId, error: String(error)}));
}

export const findRecoverableRemoteSeparation = onCall({region, enforceAppCheck: false, timeoutSeconds: 60, ...runtimeIdentity}, async request => {
  const {uid} = caller(request);
  const data = request.data || {};
  const projectId = text(data.projectId, "projectId", uuid);
  const sourceAssetId = text(data.sourceAssetId, "sourceAssetId", /^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$/);
  const inputSha256 = text(data.inputSha256, "inputSha256", SHA256);
  const activeSnapshot = await db.collection(`users/${uid}/jobs`).where("state", "in", [...ACTIVE_STATES]).limit(10).get();
  const matches = activeSnapshot.docs.filter(document =>
    sameGeneration({...(document.data() || {}), jobId: document.id}, projectId, sourceAssetId, inputSha256),
  );
  if (matches.length === 0) return {found: false};
  if (matches.length !== 1) {
    throw new HttpsError("failed-precondition", "Remote state is ambiguous.", {reason: "REMOTE_STATE_AMBIGUOUS"});
  }
  const document = matches[0];
  const job = document.data() || {};
  return {
    found: true,
    disposition: "EXISTING_SAME_GENERATION",
    jobId: document.id,
    projectId,
    sourceAssetId,
    inputSha256,
    state: String(job.state || "QUEUED"),
    resultManifestSha256: typeof job.resultManifestSha256 === "string" ? job.resultManifestSha256 : null,
  };
});

export const enqueueRemoteSeparation = onCall({region, enforceAppCheck: false, timeoutSeconds: 60, ...runtimeIdentity}, async request => {
  const {uid} = caller(request);
  const data = request.data || {};
  const jobId = text(data.jobId, "jobId", JOB_ID);
  const projectId = text(data.projectId, "projectId", uuid);
  const sourceAssetId = text(data.sourceAssetId, "sourceAssetId", /^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$/);
  const inputSha256 = text(data.inputSha256, "inputSha256", SHA256);
  const inputPath = text(data.inputPath, "inputPath", new RegExp(`^remote/v1/users/${uid}/jobs/${jobId}/input/source\\.[a-z0-9]{1,8}$`));
  const jobRef = db.doc(`users/${uid}/jobs/${jobId}`);
  const usageRef = db.doc(`users/${uid}/usage/${monthKey()}`);
  const [metadata] = await storage.bucket(bucketName).file(inputPath).getMetadata();
  const size = Number(metadata.size || 0);
  if (size <= 0 || size > MAX_INPUT_BYTES || !String(metadata.contentType || "").match(/^(audio\/|application\/octet-stream)/)) {
    const registered = await jobRef.get();
    if (!registered.exists) await cleanupUnregisteredUpload(uid, jobId);
    throw new HttpsError("invalid-argument", "Invalid input object.");
  }
  if (metadata.metadata?.sha256 !== inputSha256 || metadata.metadata?.projectId !== projectId || metadata.metadata?.jobId !== jobId) {
    const registered = await jobRef.get();
    if (!registered.exists) await cleanupUnregisteredUpload(uid, jobId);
    throw new HttpsError("failed-precondition", "Input metadata mismatch.");
  }

  const decision = await db.runTransaction(async transaction => {
    const [jobSnapshot, usageSnapshot, activeSnapshot] = await Promise.all([
      transaction.get(jobRef),
      transaction.get(usageRef),
      transaction.get(db.collection(`users/${uid}/jobs`).where("state", "in", [...ACTIVE_STATES]).limit(10)),
    ]);
    if (jobSnapshot.exists) {
      const existing = jobSnapshot.data() || {};
      if (!isIdempotentJob(existing, projectId, sourceAssetId, inputSha256)) {
        throw new HttpsError("already-exists", "jobId conflict.");
      }
      return {
        kind: "IDEMPOTENT" as const,
        effectiveJobId: jobId,
        state: String(existing.state || "QUEUED"),
        resultManifestSha256: typeof existing.resultManifestSha256 === "string" ? existing.resultManifestSha256 : null,
      };
    }
    const activeJobs = activeSnapshot.docs.map(document => ({...(document.data() || {}), jobId: document.id}));
    const selected = decideEnqueue(
      activeJobs,
      Number(usageSnapshot.data()?.acceptedJobs || 0),
      projectId,
      sourceAssetId,
      inputSha256,
    );
    if (selected.kind === "RECOVER_EXISTING") {
      const existingDocument = activeSnapshot.docs.find(document => document.id === selected.jobId);
      const existing = existingDocument?.data() || {};
      return {
        kind: "RECOVER_EXISTING" as const,
        effectiveJobId: selected.jobId,
        state: String(existing.state || "QUEUED"),
        resultManifestSha256: typeof existing.resultManifestSha256 === "string" ? existing.resultManifestSha256 : null,
      };
    }
    if (selected.kind !== "CREATE") return selected;
    transaction.create(jobRef, {schemaVersion: 2, resultContract: "prepared-reference-v2", uid, projectId, sourceAssetId, inputPath, inputSha256, state: "QUEUED", progress: 0,
      phase: "QUEUED", createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(), remoteCleanupState: "NONE"});
    transaction.set(usageRef, {acceptedJobs: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp()}, {merge: true});
    return {kind: "CREATE" as const};
  });

  if (decision.kind === "RECOVER_EXISTING") {
    await cleanupUnregisteredUpload(uid, jobId);
    return {
      requestedJobId: jobId,
      effectiveJobId: decision.effectiveJobId,
      disposition: "EXISTING_SAME_GENERATION",
      state: decision.state,
      resultManifestSha256: decision.resultManifestSha256,
    };
  }
  if (decision.kind === "ACTIVE_JOB_CONFLICT") {
    await cleanupUnregisteredUpload(uid, jobId);
    throw new HttpsError("resource-exhausted", "Another remote separation is active.", {
      reason: "ACTIVE_JOB_CONFLICT",
      activeJobId: decision.jobId,
    });
  }
  if (decision.kind === "MONTHLY_QUOTA_REACHED") {
    await cleanupUnregisteredUpload(uid, jobId);
    throw new HttpsError("resource-exhausted", "Monthly remote quota reached.", {reason: "MONTHLY_QUOTA_REACHED"});
  }
  if (decision.kind === "IDEMPOTENT") {
    return {
      requestedJobId: jobId,
      effectiveJobId: jobId,
      disposition: "IDEMPOTENT",
      state: decision.state,
      resultManifestSha256: decision.resultManifestSha256,
    };
  }

  try {
    const [operation] = await jobsClient.runJob({name: runJobName, overrides: {taskCount: 1, containerOverrides: [{env: [
      {name: "GBW_BUCKET", value: bucketName}, {name: "GBW_UID", value: uid}, {name: "GBW_JOB_ID", value: jobId},
      {name: "GBW_PROJECT_ID", value: projectId}, {name: "GBW_INPUT_PATH", value: inputPath}, {name: "GBW_INPUT_SHA256", value: inputSha256},
    ]}]}});
    const executionName = operation.name || "pending";
    await jobRef.update({
      state: "QUEUED",
      phase: "STARTING",
      executionName,
      dispatchedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    return {
      requestedJobId: jobId,
      effectiveJobId: jobId,
      disposition: "CREATED",
      state: "QUEUED",
      resultManifestSha256: null,
      executionName,
      phase: "STARTING",
    };
  } catch (error) {
    await jobRef.update({state: "FAILED", errorCode: "JOB_START_FAILED", updatedAt: FieldValue.serverTimestamp()});
    throw new HttpsError("internal", "Could not start separation.");
  }
});
export const cancelRemoteSeparation = onCall({region, enforceAppCheck: false, timeoutSeconds: 60, ...runtimeIdentity}, async request => {
  const {uid} = caller(request);
  const jobId = text(request.data?.jobId, "jobId", JOB_ID);
  const ref = db.doc(`users/${uid}/jobs/${jobId}`);
  const snapshot = await ref.get();
  if (!snapshot.exists) {
    await storage.bucket(bucketName).deleteFiles({prefix: `remote/v1/users/${uid}/jobs/${jobId}/`})
      .catch(error => logger.warn("orphan cancel cleanup failed", {jobId, error: String(error)}));
    return {jobId, state: "CANCELLED", idempotent: true, orphan: true};
  }
  const current = snapshot.data() || {};
  if (["CANCELLED", "FAILED", "EXPIRED", "IMPORTED"].includes(current.state)) return {jobId, state: current.state, idempotent: true};
  await ref.update({state: "CANCEL_REQUESTED", updatedAt: FieldValue.serverTimestamp()});
  if (typeof current.executionName === "string" && current.executionName.startsWith("projects/")) {
    await executionsClient.cancelExecution({name: current.executionName}).catch(error => logger.warn("cancel execution failed", {jobId, error: String(error)}));
  }
  await storage.bucket(bucketName).deleteFiles({prefix: `remote/v1/users/${uid}/jobs/${jobId}/`}).catch(error => logger.warn("cancel cleanup failed", {jobId, error: String(error)}));
  await ref.update({state: "CANCELLED", phase: "CANCELLED", updatedAt: FieldValue.serverTimestamp()});
  return {jobId, state: "CANCELLED"};
});

export const acknowledgeRemoteImport = onCall({region, enforceAppCheck: false, timeoutSeconds: 120, ...runtimeIdentity}, async request => {
  const {uid} = caller(request);
  const jobId = text(request.data?.jobId, "jobId", JOB_ID);
  const projectId = text(request.data?.projectId, "projectId", uuid);
  const manifestSha = text(request.data?.resultManifestSha256, "resultManifestSha256", SHA256);
  const ref = db.doc(`users/${uid}/jobs/${jobId}`);
  const snapshot = await ref.get();
  if (!snapshot.exists) throw new HttpsError("not-found", "Job not found.");
  const job = snapshot.data() || {};
  if (job.state === "IMPORTED" && job.remoteCleanupState === "PURGED") return {jobId, state: "IMPORTED", cleanup: "PURGED", idempotent: true};
  try { requireAckable(job.state); } catch { throw new HttpsError("failed-precondition", "Job is not ready for import acknowledgement."); }
  if (job.projectId !== projectId || job.resultManifestSha256 !== manifestSha) throw new HttpsError("failed-precondition", "Manifest identity mismatch.");
  await ref.update({state: "IMPORTED", importedAt: job.importedAt || FieldValue.serverTimestamp(), remoteCleanupState: "PURGING", updatedAt: FieldValue.serverTimestamp()});
  try {
    await storage.bucket(bucketName).deleteFiles({prefix: `remote/v1/users/${uid}/jobs/${jobId}/`});
    await ref.update({remoteCleanupState: "PURGED", remotePurgedAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()});
    return {jobId, state: "IMPORTED", cleanup: "PURGED"};
  } catch (error) {
    logger.error("purge failed", {jobId, error: String(error)});
    await ref.update({remoteCleanupState: "FAILED", updatedAt: FieldValue.serverTimestamp()});
    return {jobId, state: "IMPORTED", cleanup: "PENDING"};
  }
});

export const remoteBackendStatus = onCall({region, enforceAppCheck: false, ...runtimeIdentity}, async request => {
  const {uid} = caller(request);
  const usage = await db.doc(`users/${uid}/usage/${monthKey()}`).get();
  return {
    schemaVersion: 2,
    resultContract: "prepared-reference-v2",
    available: true,
    region,
    acceptedJobs: Number(usage.data()?.acceptedJobs || 0),
    limit: MAX_MONTHLY_JOBS,
    policyRevision: BACKEND_POLICY_REVISION,
    backendRevision: process.env.GBW_BACKEND_REVISION || "unknown",
  };
});

export const purgeStaleRemoteArtifacts = onSchedule(
  {
    region,
    schedule: "every 24 hours",
    timeZone: "UTC",
    timeoutSeconds: 300,
    ...runtimeIdentity,
  },
  async () => {
    const nowMs = Date.now();
    const cutoff = new Date(nowMs - RESULT_RECOVERY_WINDOW_MS);
    let examined = 0;
    let purged = 0;
    let expired = 0;
    for (const uid of allowedUids) {
      const snapshot = await db.collection(`users/${uid}/jobs`)
        .where("updatedAt", "<", cutoff)
        .limit(200)
        .get();
      for (const document of snapshot.docs) {
        examined += 1;
        const job = document.data() || {};
        const updatedAtMs = typeof job.updatedAt?.toMillis === "function" ? job.updatedAt.toMillis() : 0;
        const action = retentionAction(job.state, updatedAtMs, nowMs);
        if (!action) continue;
        const jobId = document.id;
        await storage.bucket(bucketName).deleteFiles({
          prefix: `remote/v1/users/${uid}/jobs/${jobId}/`,
        });
        const update: Record<string, unknown> = {
          remoteCleanupState: "PURGED",
          remotePurgedAt: FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp(),
        };
        if (action === "EXPIRE_AND_PURGE") {
          update.state = "EXPIRED";
          update.phase = "EXPIRED";
          update.errorCode = "RESULT_EXPIRED";
          expired += 1;
        }
        await document.ref.update(update);
        purged += 1;
      }
    }
    logger.info("remote artifact janitor completed", {examined, purged, expired});
  },
);

