#!/usr/bin/env python3
"""Isolated official-Demucs benchmark harness for Phase W shadow qualification."""

from __future__ import annotations

import argparse
import json
import resource
import tempfile
import time
from pathlib import Path

import gbw_worker as worker
from demucs_engine import (
    DEMUCS_VERSION,
    ENGINE_NAME,
    ENGINE_REVISION,
    MODEL_BYTES,
    MODEL_NAME,
    MODEL_SHA256,
    PYTORCH_VERSION,
    DemucsEngineConfig,
    DemucsPyTorchRunner,
    EngineError,
)

STRATEGIES = {
    "cpu8_s1_o05": {
        "device": "cpu",
        "cpu_threads": 8,
        "shifts": 1,
        "overlap": 0.5,
    },
}


def strategy(name: str) -> dict:
    try:
        return dict(STRATEGIES[name])
    except KeyError as error:
        raise ValueError(f"unknown benchmark strategy: {name}") from error


def run(args: argparse.Namespace) -> dict:
    from google.cloud import storage

    selected = strategy(args.strategy)
    config = DemucsEngineConfig(
        model_repo=Path(args.model_repo),
        device=selected["device"],
        shifts=selected["shifts"],
        overlap=selected["overlap"],
        cpu_threads=selected["cpu_threads"],
    )
    runner = DemucsPyTorchRunner(config)
    client = storage.Client(project=args.project)
    bucket = client.bucket(args.bucket)
    with tempfile.TemporaryDirectory(prefix="gbw-benchmark-") as tmp:
        root = Path(tmp)
        source = root / "source"
        canonical = root / "input.wav"
        download_started = time.monotonic()
        bucket.blob(args.input_path).download_to_filename(str(source))
        download_ms = round((time.monotonic() - download_started) * 1000)

        canonical_started = time.monotonic()
        worker.canonicalize(source, canonical)
        canonical_ms = round((time.monotonic() - canonical_started) * 1000)

        asset_started = time.monotonic()
        try:
            runner.validate_assets()
        except EngineError as error:
            raise worker.WorkerError(error.code, str(error)) from error
        asset_validation_ms = round((time.monotonic() - asset_started) * 1000)

        inference_started = time.monotonic()
        try:
            raw_outputs = runner.run(canonical, root / "raw", timeout_seconds=args.timeout_seconds)
        except EngineError as error:
            raise worker.WorkerError(error.code, str(error)) from error
        inference_ms = round((time.monotonic() - inference_started) * 1000)

        normalize_started = time.monotonic()
        outputs = worker.normalize_outputs(raw_outputs, root / "final")
        normalize_ms = round((time.monotonic() - normalize_started) * 1000)

        contracts = [worker.wav_contract(path) for path in outputs]
        source_contract = worker.wav_contract(canonical)
        reconstruction_snr_db = worker.sampled_reconstruction_snr_db(canonical, outputs)
        return {
            "engine": ENGINE_NAME,
            "engineRevision": ENGINE_REVISION,
            "demucsVersion": DEMUCS_VERSION,
            "pytorchVersion": PYTORCH_VERSION,
            "model": MODEL_NAME,
            "modelSha256": MODEL_SHA256,
            "modelBytes": MODEL_BYTES,
            "strategy": config.strategy,
            "device": config.device,
            "shifts": config.shifts,
            "overlap": config.overlap,
            "cpuThreads": config.cpu_threads,
            "sourceContract": source_contract,
            "stemContracts": contracts,
            "stemSha256": {path.stem: worker.sha256_file(path) for path in outputs},
            "sampledReconstructionSnrDb": reconstruction_snr_db,
            "downloadMs": download_ms,
            "canonicalizeMs": canonical_ms,
            "assetValidationMs": asset_validation_ms,
            "inferenceMs": inference_ms,
            "normalizeMs": normalize_ms,
            "maxRssKiB": resource.getrusage(resource.RUSAGE_SELF).ru_maxrss,
        }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project", required=True)
    parser.add_argument("--bucket", required=True)
    parser.add_argument("--input-path", required=True)
    parser.add_argument("--strategy", choices=sorted(STRATEGIES), required=True)
    parser.add_argument("--model-repo", default="/opt/demucs/models")
    parser.add_argument("--timeout-seconds", type=int, default=1_700)
    parser.add_argument("--json-out")
    args = parser.parse_args()
    result = run(args)
    encoded = json.dumps(result, sort_keys=True, indent=2, allow_nan=False)
    print(encoded)
    if args.json_out:
        Path(args.json_out).write_text(encoded + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
