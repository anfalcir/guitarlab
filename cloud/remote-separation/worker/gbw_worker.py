#!/usr/bin/env python3
"""GBW remote Demucs worker.

Cloud Run Jobs injects immutable job identity through environment variables. The
worker treats the result manifest as the commit marker: it uploads audio first
and the manifest last, then removes the input only after the complete result is
durable.
"""

from __future__ import annotations

import hashlib
import json
import math
import os
import re
import shutil
import signal
import struct
import subprocess
import sys
import tempfile
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable

STEMS = ("drums", "bass", "other", "vocals", "guitar", "piano")
BACKING_STEMS = ("drums", "bass", "other", "vocals", "piano")
REFERENCE_DELIVERABLES = ("backing", "guitar")
REFERENCE_RECIPE = "prepared-reference-v2"
TARGET_PEAK_DBFS = -1.0
MODEL_NAME = "ggml-model-htdemucs-6s-f16.bin"
MODEL_BYTES = 54_855_129
MODEL_SHA256 = "09704f4ceae204e56e77d5eefd6ac71d7275be81fd507e6913371d59abcee856"
ENGINE_REVISION = "f1206e9adeea103aef4a636b9e62297cf1f8e34e"
MAX_INPUT_BYTES = 1_073_741_824


class WorkerError(RuntimeError):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


@dataclass(frozen=True)
class Config:
    bucket: str
    uid: str
    job_id: str
    project_id: str
    input_path: str
    expected_input_sha256: str
    model_path: Path
    demucs_binary: Path
    blas_threads: int
    demucs_threads: int

    @property
    def prefix(self) -> str:
        return f"remote/v1/users/{self.uid}/jobs/{self.job_id}"

    @classmethod
    def from_env(cls) -> "Config":
        def required(name: str) -> str:
            value = os.environ.get(name, "").strip()
            if not value:
                raise WorkerError("CONFIG_INVALID", f"missing {name}")
            return value

        threads = int(os.environ.get("OPENBLAS_NUM_THREADS", "2"))
        if threads not in (1, 2, 4, 8):
            raise WorkerError("CONFIG_INVALID", "OPENBLAS_NUM_THREADS must be 1, 2, 4, or 8")
        demucs_threads = int(os.environ.get("GBW_DEMUCS_MT_THREADS", "4"))
        if demucs_threads not in (0, 2, 4):
            raise WorkerError("CONFIG_INVALID", "GBW_DEMUCS_MT_THREADS must be 0, 2, or 4")
        config = cls(
            bucket=required("GBW_BUCKET"), uid=required("GBW_UID"),
            job_id=required("GBW_JOB_ID"), project_id=required("GBW_PROJECT_ID"),
            input_path=required("GBW_INPUT_PATH"),
            expected_input_sha256=required("GBW_INPUT_SHA256").lower(),
            model_path=Path(os.environ.get("GBW_MODEL_PATH", f"/model/{MODEL_NAME}")),
            demucs_binary=Path(os.environ.get(
                "GBW_DEMUCS_BINARY",
                "/usr/local/bin/demucs_mt.cpp.main",
            )),
            blas_threads=threads,
            demucs_threads=demucs_threads,
        )
        expected_prefix = f"remote/v1/users/{config.uid}/jobs/{config.job_id}/input/"
        if not config.input_path.startswith(expected_prefix) or ".." in config.input_path:
            raise WorkerError("CONFIG_INVALID", "input path is outside the job namespace")
        if len(config.expected_input_sha256) != 64:
            raise WorkerError("CONFIG_INVALID", "invalid input SHA-256")
        is_mt_binary = config.demucs_binary.name == "demucs_mt.cpp.main"
        if is_mt_binary != (config.demucs_threads > 0):
            raise WorkerError(
                "CONFIG_INVALID",
                "multithreaded Demucs binary and GBW_DEMUCS_MT_THREADS disagree",
            )
        effective_threads = (
            config.blas_threads
            if config.demucs_threads == 0
            else config.blas_threads * config.demucs_threads
        )
        if effective_threads > int(os.environ.get("GBW_VCPU", "8")):
            raise WorkerError("CONFIG_INVALID", "Demucs thread plan oversubscribes configured vCPU")
        return config


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def validate_model(path: Path) -> None:
    if not path.is_file() or path.stat().st_size != MODEL_BYTES or sha256_file(path) != MODEL_SHA256:
        raise WorkerError("MODEL_INVALID", "controlled htdemucs_6s checkpoint failed integrity validation")


IEEE_FLOAT_FORMAT = 0x0003
WAVE_FORMAT_EXTENSIBLE = 0xFFFE
IEEE_FLOAT_SUBFORMAT_GUID = bytes.fromhex("0300000000001000800000aa00389b71")


def wav_contract(path: Path) -> tuple[int, int, int, float]:
    raw = path.read_bytes()
    if len(raw) < 44 or raw[:4] != b"RIFF" or raw[8:12] != b"WAVE":
        raise WorkerError("OUTPUT_INVALID", f"invalid WAV: {path.name}")

    cursor, fmt_body, fmt_size, data_bytes = 12, None, None, None
    while cursor + 8 <= len(raw):
        tag = raw[cursor:cursor + 4]
        size = struct.unpack_from("<I", raw, cursor + 4)[0]
        body = cursor + 8
        if body + size > len(raw):
            raise WorkerError("OUTPUT_INVALID", f"truncated WAV: {path.name}")
        if tag == b"fmt ":
            if size < 16:
                raise WorkerError("OUTPUT_INVALID", f"invalid fmt chunk: {path.name}")
            fmt_body, fmt_size = body, size
        elif tag == b"data":
            data_bytes = size
        cursor = body + size + (size & 1)

    if fmt_body is None or fmt_size is None or data_bytes is None:
        raise WorkerError("OUTPUT_INVALID", f"missing WAV chunks: {path.name}")

    audio_format, channels, sample_rate, _, block_align, bits = struct.unpack_from(
        "<HHIIHH", raw, fmt_body
    )
    float_format = audio_format == IEEE_FLOAT_FORMAT
    extensible_subformat = None
    valid_bits = bits

    if audio_format == WAVE_FORMAT_EXTENSIBLE:
        if fmt_size < 40:
            raise WorkerError(
                "OUTPUT_INVALID",
                f"truncated WAVE_FORMAT_EXTENSIBLE fmt chunk: {path.name}",
            )
        extension_size = struct.unpack_from("<H", raw, fmt_body + 16)[0]
        if extension_size < 22:
            raise WorkerError(
                "OUTPUT_INVALID",
                f"invalid WAVE_FORMAT_EXTENSIBLE extension: {path.name}",
            )
        valid_bits = struct.unpack_from("<H", raw, fmt_body + 18)[0]
        extensible_subformat = raw[fmt_body + 24:fmt_body + 40]
        float_format = extensible_subformat == IEEE_FLOAT_SUBFORMAT_GUID

    contract_ok = (
        float_format
        and channels == 2
        and sample_rate == 44_100
        and bits == 32
        and valid_bits == 32
        and block_align == 8
    )
    if not contract_ok:
        subtype = extensible_subformat.hex() if extensible_subformat is not None else "-"
        raise WorkerError(
            "OUTPUT_INVALID",
            "expected float32 stereo 44.1kHz: "
            f"{path.name}; format={audio_format}, channels={channels}, "
            f"sampleRate={sample_rate}, bits={bits}, validBits={valid_bits}, "
            f"blockAlign={block_align}, subtype={subtype}",
        )

    frames = data_bytes // block_align
    return sample_rate, channels, frames, frames / sample_rate


def canonicalize(source: Path, output: Path) -> None:
    command = ["ffmpeg", "-nostdin", "-v", "error", "-y", "-i", str(source),
               "-map_metadata", "-1", "-vn", "-ac", "2", "-ar", "44100",
               "-c:a", "pcm_f32le", str(output)]
    completed = subprocess.run(command, capture_output=True, text=True, timeout=600)
    if completed.returncode:
        raise WorkerError("INPUT_UNSUPPORTED", completed.stderr[-2000:])


def normalize_outputs(raw_dir: Path, final_dir: Path) -> list[Path]:
    final_dir.mkdir(parents=True, exist_ok=True)
    outputs = []
    for index, stem in enumerate(STEMS):
        candidates = [raw_dir / f"target_{index}_{stem}.wav", raw_dir / f"{stem}.wav"]
        source = next((candidate for candidate in candidates if candidate.is_file()), None)
        if source is None:
            raise WorkerError("OUTPUT_MISSING", f"missing {stem}")
        destination = final_dir / f"{stem}.wav"
        canonicalize(source, destination)
        outputs.append(destination)
    return outputs


def run_ffmpeg(command: list[str], code: str, label: str, timeout: int = 600) -> None:
    completed = subprocess.run(command, capture_output=True, text=True, timeout=timeout)
    if completed.returncode:
        raise WorkerError(code, f"{label}: {completed.stderr[-2000:]}")


def mix_float(inputs: list[Path], output: Path) -> None:
    if not inputs:
        raise WorkerError("REFERENCE_RENDER_FAILED", "mix requires at least one input")
    command = ["ffmpeg", "-nostdin", "-v", "error", "-y"]
    for path in inputs:
        command.extend(["-i", str(path)])
    labels = "".join(f"[{index}:a]" for index in range(len(inputs)))
    command.extend([
        "-filter_complex",
        f"{labels}amix=inputs={len(inputs)}:normalize=0:dropout_transition=0[mix]",
        "-map", "[mix]",
        "-map_metadata", "-1",
        "-vn", "-ac", "2", "-ar", "44100", "-c:a", "pcm_f32le",
        str(output),
    ])
    run_ffmpeg(command, "REFERENCE_RENDER_FAILED", f"mix failed for {output.name}")


def peak_dbfs(path: Path) -> float:
    completed = subprocess.run(
        [
            "ffmpeg", "-nostdin", "-hide_banner", "-v", "info", "-i", str(path),
            "-af", "astats=metadata=1:reset=0", "-f", "null", "-",
        ],
        capture_output=True,
        text=True,
        timeout=600,
    )
    if completed.returncode:
        raise WorkerError("REFERENCE_RENDER_FAILED", f"peak analysis failed: {completed.stderr[-2000:]}")
    matches = re.findall(r"Peak level dB:\s*(-?inf|[-+0-9.]+)", completed.stderr, flags=re.IGNORECASE)
    if not matches:
        raise WorkerError("REFERENCE_RENDER_FAILED", f"peak analysis missing Peak level dB for {path.name}")
    value = matches[-1].lower()
    return float("-inf") if value == "-inf" else float(value)


def apply_gain(source: Path, output: Path, gain_db: float) -> None:
    run_ffmpeg(
        [
            "ffmpeg", "-nostdin", "-v", "error", "-y", "-i", str(source),
            "-map_metadata", "-1", "-vn", "-ac", "2", "-ar", "44100",
            "-af", f"volume={gain_db:.9f}dB", "-c:a", "pcm_f32le", str(output),
        ],
        "REFERENCE_RENDER_FAILED",
        f"gain render failed for {output.name}",
    )


def render_prepared_references(outputs: list[Path], final_dir: Path) -> tuple[list[Path], float]:
    by_name = dict(zip(STEMS, outputs))
    if set(by_name) != set(STEMS):
        raise WorkerError("REFERENCE_RENDER_FAILED", "six-stem set is incomplete")
    final_dir.mkdir(parents=True, exist_ok=True)
    backing_raw = final_dir / ".backing-raw.wav"
    full_raw = final_dir / ".full-raw.wav"
    backing = final_dir / "backing.wav"
    guitar = final_dir / "guitar.wav"

    mix_float([by_name[name] for name in BACKING_STEMS], backing_raw)
    mix_float([backing_raw, by_name["guitar"]], full_raw)

    peaks = [peak_dbfs(backing_raw), peak_dbfs(by_name["guitar"]), peak_dbfs(full_raw)]
    finite_peaks = [value for value in peaks if math.isfinite(value)]
    loudest_dbfs = max(finite_peaks) if finite_peaks else float("-inf")
    shared_gain_db = min(0.0, TARGET_PEAK_DBFS - loudest_dbfs) if math.isfinite(loudest_dbfs) else 0.0

    apply_gain(backing_raw, backing, shared_gain_db)
    apply_gain(by_name["guitar"], guitar, shared_gain_db)

    backing_contract = wav_contract(backing)
    guitar_contract = wav_contract(guitar)
    if backing_contract != guitar_contract:
        raise WorkerError("REFERENCE_RENDER_FAILED", "prepared reference contracts differ")
    backing_raw.unlink(missing_ok=True)
    full_raw.unlink(missing_ok=True)
    return [backing, guitar], shared_gain_db


def demucs_command(config: Config, canonical: Path, raw_dir: Path) -> list[str]:
    command = [
        str(config.demucs_binary),
        str(config.model_path),
        str(canonical),
        str(raw_dir),
    ]
    if config.demucs_threads > 0:
        command.append(str(config.demucs_threads))
    return command


def inference_strategy(config: Config) -> str:
    return (
        f"mt{config.demucs_threads}_omp{config.blas_threads}"
        if config.demucs_threads > 0
        else f"single{config.blas_threads}"
    )


def build_manifest(config: Config, input_sha256: str, outputs: list[Path], shared_gain_db: float,
                   started: str, started_monotonic: float) -> dict:
    contracts = [wav_contract(path) for path in outputs]
    reference = contracts[0]
    if any(contract != reference for contract in contracts[1:]):
        raise WorkerError("OUTPUT_INVALID", "prepared reference duration contracts differ")
    sample_rate, channels, frames, duration = reference
    rows = []
    roles = {"backing": "REFERENCE_BACKING", "guitar": "REFERENCE_GUITAR"}
    for name, path in zip(REFERENCE_DELIVERABLES, outputs):
        rows.append({
            "name": name,
            "role": roles[name],
            "path": f"{config.prefix}/output/prepared/{name}.wav",
            "bytes": path.stat().st_size,
            "sha256": sha256_file(path),
            "sampleRate": sample_rate,
            "channels": channels,
            "frames": frames,
            "encoding": "FLOAT32_LE",
        })
    return {
        "schemaVersion": 2, "jobId": config.job_id, "uid": config.uid,
        "projectId": config.project_id, "inputSha256": input_sha256,
        "engine": "demucs.cpp", "engineRevision": ENGINE_REVISION,
        "model": "htdemucs_6s", "modelSha256": MODEL_SHA256,
        "sampleRate": sample_rate, "channels": channels, "frames": frames,
        "duration": duration, "startedAt": started,
        "completedAt": datetime.now(timezone.utc).isoformat(),
        "wallTimeMs": round((time.monotonic() - started_monotonic) * 1000),
        "vCPU": int(os.environ.get("GBW_VCPU", "8")),
        "blasThreads": config.blas_threads,
        "demucsThreads": config.demucs_threads,
        "inferenceStrategy": inference_strategy(config),
        "referenceRecipe": {
            "version": REFERENCE_RECIPE,
            "targetPeakDbfs": TARGET_PEAK_DBFS,
            "sharedGainDb": round(shared_gain_db, 9),
            "backingStems": list(BACKING_STEMS),
            "guitarStem": "guitar",
        },
        "deliverables": rows,
    }

def report_terminal_failure(config: Config, code: str, message: str = "",
                            firestore_factory: Callable[[], object] | None = None) -> None:
    """Best-effort terminal state publication for failures outside ``run``.

    A Cloud Run execution failure is not itself observed by the Android client.
    Without this update, the Firestore job can remain RUNNING forever and keep
    the per-user concurrency gate locked.  Never overwrite a state written by
    cancellation or by a successfully completed import.
    """
    if firestore_factory is None:
        from google.cloud import firestore
        firestore_factory = firestore.Client
        server_timestamp = firestore.SERVER_TIMESTAMP
    else:
        server_timestamp = datetime.now(timezone.utc).isoformat()
    try:
        document = firestore_factory().document(f"users/{config.uid}/jobs/{config.job_id}")
        snapshot = document.get()
        state = (snapshot.to_dict() or {}).get("state") if snapshot.exists else None
        if state in {"CANCEL_REQUESTED", "CANCELLED", "COMPLETED", "IMPORTING", "IMPORTED"}:
            return
        document.update({
            "state": "FAILED", "phase": "FAILED", "progress": 0,
            "errorCode": code,
            "errorMessage": message[:2000],
            "updatedAt": server_timestamp,
        })
    except Exception as error:  # Reporting must not hide the original worker failure.
        print(json.dumps({"event": "failure_state_update_failed", "error": str(error)}), file=sys.stderr)


def run(config: Config, storage_factory: Callable[[], object] | None = None) -> dict:
    if not config.demucs_binary.is_file():
        raise WorkerError("CONFIG_INVALID", "Demucs binary is missing")
    if storage_factory is None:
        from google.cloud import storage
        storage_factory = storage.Client
    client = storage_factory()
    bucket = client.bucket(config.bucket)
    firestore_client = None
    if storage_factory.__module__.startswith("google."):
        from google.cloud import firestore
        firestore_client = firestore.Client()
    job_document = firestore_client.document(f"users/{config.uid}/jobs/{config.job_id}") if firestore_client else None
    if job_document:
        from google.cloud import firestore
        current = job_document.get()
        current_state = (current.to_dict() or {}).get("state") if current.exists else None
        if current_state in {"CANCEL_REQUESTED", "CANCELLED"}:
            raise WorkerError("JOB_CANCELLED", "job was cancelled before worker start")
        job_document.update({
            "state": "RUNNING",
            "phase": "RUNNING",
            "progress": 1,
            "startedAt": firestore.SERVER_TIMESTAMP,
            "updatedAt": firestore.SERVER_TIMESTAMP,
        })
    print(json.dumps({
        "event": "worker_started",
        "jobId": config.job_id,
        "inferenceStrategy": inference_strategy(config),
        "blasThreads": config.blas_threads,
        "demucsThreads": config.demucs_threads,
    }, separators=(",", ":")))
    validate_model(config.model_path)
    started, started_monotonic = datetime.now(timezone.utc).isoformat(), time.monotonic()
    cancelled = False
    def terminate(_signum: int, _frame: object) -> None:
        nonlocal cancelled
        cancelled = True
        raise WorkerError("JOB_CANCELLED", "worker terminated")
    signal.signal(signal.SIGTERM, terminate)

    with tempfile.TemporaryDirectory(prefix="gbw-") as tmp:
        root = Path(tmp)
        source = root / "source"
        input_blob = bucket.blob(config.input_path)
        input_blob.reload()
        if int(input_blob.size or 0) <= 0 or int(input_blob.size) > MAX_INPUT_BYTES:
            raise WorkerError("INPUT_UNSUPPORTED", "input size outside allowed range")
        input_blob.download_to_filename(str(source))
        actual_input_sha = sha256_file(source)
        if actual_input_sha != config.expected_input_sha256:
            raise WorkerError("INPUT_HASH_MISMATCH", "downloaded input digest differs")
        canonical = root / "input.wav"
        canonicalize(source, canonical)
        raw_dir = root / "raw"
        raw_dir.mkdir()
        env = os.environ.copy()
        env["OPENBLAS_NUM_THREADS"] = str(config.blas_threads)
        env["OMP_NUM_THREADS"] = str(config.blas_threads)
        command = demucs_command(config, canonical, raw_dir)
        completed = subprocess.run(command, env=env, timeout=1_700)
        if cancelled:
            raise WorkerError("JOB_CANCELLED", "worker terminated")
        if completed.returncode:
            raise WorkerError("DEMUCS_FAILED", f"Demucs exited {completed.returncode}")
        outputs = normalize_outputs(raw_dir, root / "final")
        if job_document:
            from google.cloud import firestore
            job_document.update({
                "phase": "PREPARING_REFERENCES",
                "progress": 85,
                "updatedAt": firestore.SERVER_TIMESTAMP,
            })
        prepared, shared_gain_db = render_prepared_references(outputs, root / "prepared")
        manifest = build_manifest(config, actual_input_sha, prepared, shared_gain_db, started, started_monotonic)
        if job_document:
            from google.cloud import firestore
            job_document.update({
                "phase": "PUBLISHING_RESULTS",
                "progress": 95,
                "updatedAt": firestore.SERVER_TIMESTAMP,
            })
        for path in prepared:
            if job_document:
                state = (job_document.get().to_dict() or {}).get("state")
                if state in {"CANCEL_REQUESTED", "CANCELLED"}:
                    raise WorkerError("JOB_CANCELLED", "job was cancelled while publishing prepared references")
            blob = bucket.blob(f"{config.prefix}/output/prepared/{path.name}")
            blob.upload_from_filename(str(path), content_type="audio/wav")
            blob.metadata = {
                "sha256": sha256_file(path),
                "jobId": config.job_id,
                "resultContract": "prepared-reference-v2",
            }
            blob.patch()
        manifest_bytes = json.dumps(manifest, separators=(",", ":"), sort_keys=True).encode()
        manifest_sha = hashlib.sha256(manifest_bytes).hexdigest()
        if job_document:
            state = (job_document.get().to_dict() or {}).get("state")
            if state in {"CANCEL_REQUESTED", "CANCELLED"}:
                raise WorkerError("JOB_CANCELLED", "job was cancelled before result commit")
        manifest_blob = bucket.blob(f"{config.prefix}/output/result-manifest.json")
        manifest_blob.metadata = {"sha256": manifest_sha, "jobId": config.job_id}
        manifest_blob.upload_from_string(manifest_bytes, content_type="application/json")
        if job_document:
            from google.cloud import firestore
            job_document.update({
                "state": "COMPLETED", "phase": "COMPLETED", "progress": 100,
                "resultManifestPath": f"{config.prefix}/output/result-manifest.json",
                "resultManifestSha256": manifest_sha,
                "completedAt": firestore.SERVER_TIMESTAMP,
                "wallTimeMs": manifest["wallTimeMs"],
                "blasThreads": config.blas_threads,
                "demucsThreads": config.demucs_threads,
                "inferenceStrategy": inference_strategy(config),
            })
        input_blob.delete()
        manifest["resultManifestSha256"] = manifest_sha
        return manifest


def main() -> int:
    config = None
    try:
        config = Config.from_env()
        result = run(config)
        print(json.dumps({"event": "completed", **result}, separators=(",", ":")))
        return 0
    except WorkerError as error:
        if config is not None:
            report_terminal_failure(config, error.code, str(error))
        print(json.dumps({"event": "failed", "code": error.code, "message": str(error)}), file=sys.stderr)
        return 2
    except subprocess.TimeoutExpired:
        if config is not None:
            report_terminal_failure(config, "JOB_TIMEOUT", "Cloud Run worker exceeded the inference timeout")
        print(json.dumps({"event": "failed", "code": "JOB_TIMEOUT"}), file=sys.stderr)
        return 3
    except Exception as error:
        if config is not None:
            report_terminal_failure(config, "WORKER_INTERNAL", str(error))
        print(json.dumps({"event": "failed", "code": "WORKER_INTERNAL", "message": str(error)}), file=sys.stderr)
        return 4


if __name__ == "__main__":
    raise SystemExit(main())
