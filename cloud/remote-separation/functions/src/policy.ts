export const ACTIVE_STATES = new Set(["UPLOADING", "READY", "QUEUED", "RUNNING", "COMPLETED", "IMPORTING", "CANCEL_REQUESTED"]);
export const JOB_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
export const SHA256 = /^[a-f0-9]{64}$/;
export const MAX_MONTHLY_JOBS = 40;
export const BACKEND_POLICY_REVISION = "rc5-q40-mt4-omp2-v1";
export const MAX_INPUT_BYTES = 1_073_741_824;

export function monthKey(date = new Date()): string {
  return date.toISOString().slice(0, 7);
}

export function inputPath(uid: string, jobId: string, extension: string): string {
  if (!JOB_ID.test(jobId) || !/^[a-z0-9]{1,8}$/i.test(extension)) throw new Error("invalid identity");
  return `remote/v1/users/${uid}/jobs/${jobId}/input/source.${extension.toLowerCase()}`;
}

export function isIdempotentJob(existing: Record<string, unknown>, projectId: string, hash: string): boolean {
  return existing.projectId === projectId && existing.inputSha256 === hash;
}

export function requireAckable(state: unknown): void {
  if (!["COMPLETED", "IMPORTING", "IMPORTED"].includes(String(state))) throw new Error("job is not ackable");
}
