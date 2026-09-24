#!/usr/bin/env python3
"""Collect a sanitized, read-only U12 Cloud Run separation evidence bundle."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import urllib.parse
import urllib.request
import zipfile


PROJECT = "gbwapp-ef048"
REGION = "us-central1"
JOB_NAME = "gbw-demucs"
SENSITIVE_KEY = re.compile(
    r"authorization|token|password|secret|credential|service.?account|session.?url",
    re.IGNORECASE,
)
EMAIL = re.compile(r"(?<![A-Za-z0-9._%+-])[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}(?![A-Za-z0-9.-])")


def command(*args: str) -> str:
    completed = subprocess.run(
        args,
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        timeout=180,
    )
    return completed.stdout


def sanitize(value: object) -> object:
    if isinstance(value, dict):
        return {
            key: "[REDACTED]" if SENSITIVE_KEY.search(key) else sanitize(item)
            for key, item in sorted(value.items())
        }
    if isinstance(value, list):
        return [sanitize(item) for item in value]
    if isinstance(value, str):
        value = re.sub(r"(?i)bearer\s+[A-Za-z0-9._~+/-]+=*", "Bearer [REDACTED]", value)
        return EMAIL.sub("[REDACTED_EMAIL]", value)
    return value


def write_json(path: Path, value: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(sanitize(value), indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def firestore_get(uid: str, job_id: str) -> dict:
    token = command("gcloud", "auth", "print-access-token").strip()
    document = urllib.parse.quote(f"users/{uid}/jobs/{job_id}", safe="/")
    url = f"https://firestore.googleapis.com/v1/projects/{PROJECT}/databases/(default)/documents/{document}"
    request = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def storage_inventory(prefix: str) -> dict:
    uri = f"gs://{PROJECT}.firebasestorage.app/{prefix}/**"
    completed = subprocess.run(
        ["gcloud", "storage", "ls", "--long", uri],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        timeout=180,
    )
    if completed.returncode == 0:
        return {"state": "PRESENT", "uri": uri, "listing": completed.stdout.splitlines()}
    if "matched no objects" in completed.stderr:
        return {"state": "PURGED", "uri": uri, "reason": "No objects matched the immutable job prefix."}
    raise RuntimeError(f"Storage inventory failed: {completed.stderr.strip()}")


def collect(job_id: str, expected_local_hashes: list[str], output: Path) -> Path:
    query = f'resource.type="cloud_run_job" AND jsonPayload.jobId="{job_id}"'
    logs = json.loads(command(
        "gcloud", "logging", "read", query,
        f"--project={PROJECT}", "--freshness=30d", "--limit=200", "--order=asc", "--format=json",
    ))
    completed_rows = [row for row in logs if row.get("jsonPayload", {}).get("event") == "completed"]
    if len(completed_rows) != 1:
        raise RuntimeError(f"Expected exactly one completed event for {job_id}; found {len(completed_rows)}")
    completed = completed_rows[0]["jsonPayload"]
    labels = completed_rows[0].get("labels", {})
    execution_name = labels.get("run.googleapis.com/execution_name")
    uid = completed.get("uid")
    if not execution_name or not uid:
        raise RuntimeError("Completed log lacks execution name or uid")

    firestore = firestore_get(uid, job_id)
    execution = json.loads(command(
        "gcloud", "run", "jobs", "executions", "describe", execution_name,
        f"--project={PROJECT}", f"--region={REGION}", "--format=json",
    ))
    prefix = f"remote/v1/users/{uid}/jobs/{job_id}"
    storage = storage_inventory(prefix)
    diagnostic = next(
        (row.get("jsonPayload", {}) for row in logs if row.get("jsonPayload", {}).get("event") == "stem_reconstruction_diagnosed"),
        None,
    )
    deliverables = {row["name"]: row for row in completed.get("deliverables", [])}
    remote_hashes = {name: row.get("sha256") for name, row in deliverables.items()}
    exact_local_match = sorted(set(remote_hashes.values()) & set(expected_local_hashes))
    summary = {
        "schemaVersion": 1,
        "mode": "READ_ONLY",
        "project": PROJECT,
        "jobId": job_id,
        "answers": {
            "freshCloudRunExecution": True,
            "execution": execution_name,
            "image": execution["spec"]["template"]["spec"]["containers"][0].get("image"),
            "engine": completed.get("engine"),
            "engineRevision": completed.get("engineRevision"),
            "model": completed.get("model"),
            "modelSha256": completed.get("modelSha256"),
            "inferenceStrategy": completed.get("inferenceStrategy"),
            "inputSha256": completed.get("inputSha256"),
            "manifestSha256": completed.get("resultManifestSha256"),
            "remoteDeliverableSha256": remote_hashes,
            "exactSuppliedLocalHashMatches": exact_local_match,
            "remoteToLocalRelationship": (
                "exact-byte-match" if exact_local_match else
                "not-an-exact-byte-match; Android canonicalization/provenance is required to distinguish legitimate recoding from stale local assets"
            ),
            "recoveryOrAdoption": "No evidence of reuse: a unique successful execution and worker_started event exist for this job.",
            "reconstructionDiagnostic": diagnostic,
            "storageState": storage["state"],
        },
    }

    with tempfile.TemporaryDirectory(prefix="u12-forensics-") as temporary:
        root = Path(temporary)
        write_json(root / "summary.json", summary)
        write_json(root / "firestore" / "job.json", firestore)
        write_json(root / "cloud-run" / "execution.json", execution)
        write_json(root / "logs" / "worker.json", logs)
        write_json(root / "storage" / "inventory.json", storage)
        (root / "README.md").write_text(
            f"# U12 RC19 read-only audio forensics\n\nJob: `{job_id}`\n\n"
            "Collected using only Firestore GET, Cloud Logging read, Cloud Run describe, and Storage list.\n"
            "No job, object, document, quota, deployment, or runtime resource was modified.\n",
            encoding="utf-8",
        )
        integrity = root / "integrity" / "SHA256SUMS.txt"
        integrity.parent.mkdir(parents=True)
        rows = []
        for path in sorted(root.rglob("*")):
            if path.is_file() and path != integrity:
                digest = hashlib.sha256(path.read_bytes()).hexdigest()
                rows.append(f"{digest}  {path.relative_to(root).as_posix()}")
        integrity.write_text("\n".join(rows) + "\n", encoding="utf-8")

        output.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            for path in sorted(root.rglob("*")):
                if path.is_file():
                    info = zipfile.ZipInfo(path.relative_to(root).as_posix(), (1980, 1, 1, 0, 0, 0))
                    info.compress_type = zipfile.ZIP_DEFLATED
                    info.external_attr = 0o100644 << 16
                    archive.writestr(info, path.read_bytes())
    return output


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("job_id")
    parser.add_argument("--local-sha256", action="append", default=[])
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    output = args.output or Path(f"u12-rc19-audio-forensics-{args.job_id}.zip")
    print(collect(args.job_id, args.local_sha256, output))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
