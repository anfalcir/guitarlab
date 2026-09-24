#!/usr/bin/env python3
"""Run the RC20 official-Demucs path against a local mounted fixture for W3 parity."""

from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path

sys.path.insert(0, "/app")

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
)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--image-identity", required=True)
    args = parser.parse_args()

    args.output.mkdir(parents=True, exist_ok=True)
    canonical = args.output / "input.wav"
    worker.canonicalize(args.input, canonical)
    canonical_metrics = worker.measure_wav(canonical)
    if canonical_metrics.non_finite_samples:
        raise SystemExit("W3 local canonical input contains non-finite samples")

    config = DemucsEngineConfig(
        model_repo=Path("/opt/demucs/models"),
        device="cpu",
        shifts=1,
        overlap=0.5,
        cpu_threads=8,
    )
    runner = DemucsPyTorchRunner(config)
    runner.validate_assets()

    started = time.monotonic()
    raw = runner.run(canonical, args.output / "raw", timeout_seconds=1_700)
    inference_ms = round((time.monotonic() - started) * 1000)
    outputs = worker.normalize_outputs(raw, args.output / "stems")
    stem_metrics = {name: worker.measure_wav(path) for name, path in zip(worker.STEMS, outputs)}
    quality_summary, quality_findings = worker.evaluate_stems(stem_metrics)
    rejected = [row for row in quality_findings if row.get("severity") == "reject"]
    if rejected:
        raise SystemExit(f"W3 local quality rejection: {rejected}")

    prepared, shared_gain_db, prepared_metrics = worker.render_prepared_references(
        outputs, args.output / "prepared"
    )
    result = {
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
        "imageIdentity": args.image_identity,
        "sourceContract": worker.wav_contract(canonical),
        "canonicalInputMetrics": canonical_metrics.as_dict(),
        "stemContracts": [worker.wav_contract(path) for path in outputs],
        "stemSha256": {path.stem: worker.sha256_file(path) for path in outputs},
        "stemMetrics": {name: stem_metrics[name].as_dict() for name in worker.STEMS},
        "qualitySummary": quality_summary,
        "qualityFindings": quality_findings,
        "preparedSha256": {path.stem: worker.sha256_file(path) for path in prepared},
        "preparedMetrics": prepared_metrics,
        "sharedGainDb": shared_gain_db,
        "inferenceMs": inference_ms,
    }
    (args.output / "parity.json").write_text(
        json.dumps(result, sort_keys=True, indent=2, allow_nan=False) + "\n",
        encoding="utf-8",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
