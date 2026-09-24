export const ACTIVE_STATES = new Set(["UPLOADING", "READY", "QUEUED", "RUNNING", "COMPLETED", "IMPORTING", "CANCEL_REQUESTED"]);
export const JOB_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
export const SHA256 = /^[a-f0-9]{64}$/;
export const MAX_MONTHLY_JOBS = 40;
export const BACKEND_POLICY_REVISION = "rc19-q40-single8-prepared-v2-recovery";
export const RESULT_RECOVERY_WINDOW_MS = 72 * 60 * 60 * 1000;
export const MAX_INPUT_BYTES = 1_073_741_824;

export function monthKey(date = new Date()): string {
  return date.toISOString().slice(0, 7);
}

export function inputPath(uid: string, jobId: string, extension: string): string {
  if (!JOB_ID.test(jobId) || !/^[a-z0-9]{1,8}$/i.test(extension)) throw new Error("invalid identity");
  return `remote/v1/users/${uid}/jobs/${jobId}/input/source.${extension.toLowerCase()}`;
}

export function isIdempotentJob(existing: Record<string, unknown>, projectId: string, sourceAssetId: string, hash: string): boolean {
  return existing.projectId === projectId && existing.sourceAssetId === sourceAssetId && existing.inputSha256 === hash;
}

export function requireAckable(state: unknown): void {
  if (!["COMPLETED", "IMPORTING", "IMPORTED"].includes(String(state))) throw new Error("job is not ackable");
}


export type RetentionAction = "PURGE_ONLY" | "EXPIRE_AND_PURGE" | null;

export function retentionAction(state: unknown, updatedAtMs: number, nowMs: number): RetentionAction {
  if (!Number.isFinite(updatedAtMs) || nowMs - updatedAtMs < RESULT_RECOVERY_WINDOW_MS) return null;
  const normalized = String(state);
  if (["COMPLETED", "IMPORTING", "IMPORT_FAILED"].includes(normalized)) return "EXPIRE_AND_PURGE";
  if (["IMPORTED", "CANCELLED", "FAILED", "EXPIRED"].includes(normalized)) return "PURGE_ONLY";
  return null;
}


export type EnqueueDecision =
  | {kind: "CREATE"}
  | {kind: "RECOVER_EXISTING"; jobId: string}
  | {kind: "ACTIVE_JOB_CONFLICT"; jobId: string}
  | {kind: "MONTHLY_QUOTA_REACHED"};

export type ActiveJobRecord = {
  jobId: string;
  projectId?: unknown;
  sourceAssetId?: unknown;
  inputSha256?: unknown;
};

export function sameGeneration(
  job: ActiveJobRecord,
  projectId: string,
  sourceAssetId: string,
  inputSha256: string,
): boolean {
  return job.projectId === projectId &&
    job.sourceAssetId === sourceAssetId &&
    job.inputSha256 === inputSha256;
}

export function decideEnqueue(
  activeJobs: ActiveJobRecord[],
  acceptedJobs: number,
  projectId: string,
  sourceAssetId: string,
  inputSha256: string,
): EnqueueDecision {
  const recoverable = activeJobs.filter(job => sameGeneration(job, projectId, sourceAssetId, inputSha256));
  if (recoverable.length === 1 && activeJobs.length === 1) {
    return {kind: "RECOVER_EXISTING", jobId: recoverable[0].jobId};
  }
  if (activeJobs.length > 0) {
    return {kind: "ACTIVE_JOB_CONFLICT", jobId: activeJobs[0].jobId};
  }
  if (acceptedJobs >= MAX_MONTHLY_JOBS) return {kind: "MONTHLY_QUOTA_REACHED"};
  return {kind: "CREATE"};
}
