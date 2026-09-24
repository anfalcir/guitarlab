#!/usr/bin/env python3
"""Isolated Cloud Run benchmark for GBW Demucs execution strategies.

This program never mutates Firestore and never publishes stems. It downloads one
existing real GBW input object, runs one inference strategy inside a temporary
working directory, validates all six output WAV contracts, writes a small JSON
result to Cloud Storage, and deletes local audio on exit.
"""

from __future__ import annotations

import json
import os
import resource
import subprocess
import tempfile
import time
from pathlib import Path

import gbw_worker as worker


STRATEGIES = {
    "single8": {
        "binary": "/usr/local/bin/demucs.cpp.main",
        "blas_threads": 8,
        "mt_threads": 0,
    },
}


def strategy(name: str) -> dict:
    try:
        return STRATEGIES[name]
    except KeyError as error:
        raise ValueError(f"unknown benchmark strategy: {name}") from error


def run() -> dict:
    bucket_name = os.environ["GBW_BUCKET"].strip()
    input_path = os.environ["GBW_BENCH_INPUT_PATH"].strip()
    strategy_name = os.environ["GBW_BENCH_STRATEGY"].strip()
    result_path = os.environ["GBW_BENCH_RESULT_PATH"].strip()
    config = strategy(strategy_name)
    model_path = Path(os.environ.get("GBW_MODEL_PATH", f"/model/{worker.MODEL_NAME}"))

    if not input_path or ".." in input_path:
        raise ValueError("invalid benchmark input path")
    if not result_path.startswith("benchmarks/") or ".." in result_path:
        raise ValueError("invalid benchmark result path")

    binary = Path(config["binary"])
    if not binary.is_file():
        raise RuntimeError(f"benchmark binary missing: {binary}")

    from google.cloud import storage

    started_wall = time.monotonic()
    storage_client = storage.Client()
    bucket = storage_client.bucket(bucket_name)

    with tempfile.TemporaryDirectory(prefix="gbw-benchmark-") as tmp:
        root = Path(tmp)
        source = root / "source"
        canonical = root / "input.wav"
        raw_dir = root / "raw"
        raw_dir.mkdir()

        worker.validate_model(model_path)

        input_blob = bucket.blob(input_path)
        input_blob.reload()
        input_bytes = int(input_blob.size or 0)
        if input_bytes <= 0 or input_bytes > worker.MAX_INPUT_BYTES:
            raise RuntimeError(f"benchmark input size outside contract: {input_bytes}")
        download_started = time.monotonic()
        input_blob.download_to_filename(str(source))
        download_ms = round((time.monotonic() - download_started) * 1000)

        canonical_started = time.monotonic()
        worker.canonicalize(source, canonical)
        canonical_ms = round((time.monotonic() - canonical_started) * 1000)
        sample_rate, channels, frames, duration = worker.wav_contract(canonical)

        env = os.environ.copy()
        env["OPENBLAS_NUM_THREADS"] = str(config["blas_threads"])
        env["OMP_NUM_THREADS"] = str(config["blas_threads"])

        command = [str(binary), str(model_path), str(canonical), str(raw_dir)]
        if config["mt_threads"]:
            command.append(str(config["mt_threads"]))

        before = resource.getrusage(resource.RUSAGE_CHILDREN)
        inference_started = time.monotonic()
        completed = subprocess.run(command, env=env, timeout=1_700)
        inference_ms = round((time.monotonic() - inference_started) * 1000)
        after = resource.getrusage(resource.RUSAGE_CHILDREN)
        if completed.returncode:
            raise RuntimeError(f"Demucs benchmark exited {completed.returncode}")

        normalize_started = time.monotonic()
        outputs = worker.normalize_outputs(raw_dir, root / "final")
        contracts = [worker.wav_contract(path) for path in outputs]
        if any(contract != contracts[0] for contract in contracts[1:]):
            raise RuntimeError("benchmark stem contracts differ")
        normalize_ms = round((time.monotonic() - normalize_started) * 1000)

        total_ms = round((time.monotonic() - started_wall) * 1000)
        result = {
            "schemaVersion": 1,
            "strategy": strategy_name,
            "binary": binary.name,
            "vCPU": int(os.environ.get("GBW_VCPU", "8")),
            "memoryGiB": int(os.environ.get("GBW_MEMORY_GIB", "16")),
            "blasThreads": config["blas_threads"],
            "mtThreads": config["mt_threads"],
            "sourceObject": input_path,
            "sourceBytes": input_bytes,
            "sourceSampleRate": sample_rate,
            "sourceChannels": channels,
            "sourceFrames": frames,
            "sourceDurationSeconds": duration,
            "downloadMs": download_ms,
            "canonicalizeMs": canonical_ms,
            "inferenceMs": inference_ms,
            "normalizeMs": normalize_ms,
            "totalMs": total_ms,
            "childUserCpuSeconds": max(0.0, after.ru_utime - before.ru_utime),
            "childSystemCpuSeconds": max(0.0, after.ru_stime - before.ru_stime),
            "childMaxRssKiB": after.ru_maxrss,
            "stemBytes": {name: path.stat().st_size for name, path in zip(worker.STEMS, outputs)},
            "stemSha256": {name: worker.sha256_file(path) for name, path in zip(worker.STEMS, outputs)},
        }

        payload = json.dumps(result, sort_keys=True, separators=(",", ":"))
        bucket.blob(result_path).upload_from_string(payload, content_type="application/json")
        print(json.dumps({"event": "benchmark_result", **result}, separators=(",", ":")))
        return result


if __name__ == "__main__":
    run()
