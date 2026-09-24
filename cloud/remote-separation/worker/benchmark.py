#!/usr/bin/env python3
"""Isolated official-Demucs benchmark harness for Phase W shadow qualification."""

from __future__ import annotations

import argparse
import json
import os
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
    "cpu4_s1_o05": {
        "device": "cpu",
        "cpu_threads": 4,
        "shifts": 1,
        "overlap": 0.5,
    },
}
BUNDLE_PREFIX_ROOT = "diagnostics/u12-rc20/w4/"


def strategy(name: str) -> dict:
    try:
        return dict(STRATEGIES[name])
    except KeyError as error:
        raise ValueError(f"unknown benchmark strategy: {name}") from error


def validate_bundle_prefix(prefix: str | None) -> str | None:
    if prefix is None:
        return None
    normalized = prefix.strip("/")
    if not normalized.startswith(BUNDLE_PREFIX_ROOT) or ".." in normalized.split("/"):
        raise ValueError("W4 bundle prefix must stay inside the diagnostic namespace")
    return normalized


def upload_listening_bundle(bucket, prefix: str, root: Path, stems: list[Path],
                            prepared: list[Path], recombined: Path, result: dict) -> None:
    files: list[tuple[str, Path]] = []
    for stem in stems:
        files.append((f"stems/{stem.name}", stem))
    for reference in prepared:
        files.append((f"prepared/{reference.name}", reference))
    files.append(("recombined.wav", recombined))
    result["bundleFiles"] = [
        {
            "path": relative,
            "bytes": path.stat().st_size,
            "sha256": worker.sha256_file(path),
        }
        for relative, path in files
    ]
    metrics = root / "benchmark.json"
    metrics.write_text(
        json.dumps(result, sort_keys=True, indent=2, allow_nan=False) + "\n",
        encoding="utf-8",
    )
    checksum_rows = [
        f"{worker.sha256_file(path)}  {relative}"
        for relative, path in files
    ]
    checksum_rows.append(f"{worker.sha256_file(metrics)}  benchmark.json")
    sums = root / "SHA256SUMS"
    sums.write_text("\n".join(checksum_rows) + "\n", encoding="utf-8")
    upload_files = [*files, ("benchmark.json", metrics), ("SHA256SUMS", sums)]
    for relative, path in upload_files:
        content_type = "audio/wav" if relative.endswith(".wav") else "text/plain"
        if relative.endswith(".json"):
            content_type = "application/json"
        blob = bucket.blob(f"{prefix}/{relative}")
        blob.upload_from_filename(str(path), content_type=content_type)


def env_float(name: str) -> float | None:
    value = os.environ.get(name, "").strip()
    return float(value) if value else None


def env_flag(name: str) -> bool:
    return os.environ.get(name, "").strip().lower() in {"1", "true", "yes", "on"}


def run(args: argparse.Namespace) -> dict:
    from google.cloud import storage

    started_total = time.monotonic()
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
        canonical_metrics = worker.measure_wav(canonical)
        if canonical_metrics.non_finite_samples:
            raise worker.WorkerError("INPUT_UNSUPPORTED", "canonical benchmark input contains non-finite samples")

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

        quality_started = time.monotonic()
        stem_metrics = {name: worker.measure_wav(path) for name, path in zip(worker.STEMS, outputs)}
        quality_summary, quality_findings = worker.evaluate_stems(stem_metrics)
        quality_ms = round((time.monotonic() - quality_started) * 1000)
        rejected = [row for row in quality_findings if row["severity"] == "reject"]
        if rejected:
            codes = ",".join(sorted({str(row["code"]) for row in rejected}))
            raise worker.WorkerError("OUTPUT_QUALITY_INVALID", f"benchmark quality gate rejected: {codes}")

        reconstruction_started = time.monotonic()
        reconstruction_snr_db = worker.sampled_reconstruction_snr_db(canonical, outputs)
        reconstruction_ms = round((time.monotonic() - reconstruction_started) * 1000)

        render_started = time.monotonic()
        prepared, shared_gain_db, prepared_metrics = worker.render_prepared_references(outputs, root / "prepared")
        render_ms = round((time.monotonic() - render_started) * 1000)

        contracts = [worker.wav_contract(path) for path in outputs]
        source_contract = worker.wav_contract(canonical)
        total_ms = round((time.monotonic() - started_total) * 1000)
        cpu_seconds = time.process_time()
        cost = None
        if args.vcpu_price_per_second is not None and args.memory_price_per_gib_second is not None:
            elapsed_seconds = total_ms / 1000.0
            cost = (
                elapsed_seconds * selected["cpu_threads"] * args.vcpu_price_per_second
                + elapsed_seconds * args.memory_gib * args.memory_price_per_gib_second
            )
        recombined = root / "recombined.wav"
        worker.mix_float(prepared, recombined)
        worker.wav_contract(recombined)
        warm_probe = None
        if args.warm_probe:
            warm_started = time.monotonic()
            try:
                warm_raw = runner.run(canonical, root / "warm-raw", timeout_seconds=args.timeout_seconds)
            except EngineError as error:
                raise worker.WorkerError(error.code, str(error)) from error
            warm_inference_ms = round((time.monotonic() - warm_started) * 1000)
            warm_outputs = worker.normalize_outputs(warm_raw, root / "warm-final")
            warm_metrics = {
                name: worker.measure_wav(path)
                for name, path in zip(worker.STEMS, warm_outputs)
            }
            warm_summary, warm_findings = worker.evaluate_stems(warm_metrics)
            rejected_warm = [row for row in warm_findings if row["severity"] == "reject"]
            if rejected_warm:
                codes = ",".join(sorted({str(row["code"]) for row in rejected_warm}))
                raise worker.WorkerError(
                    "OUTPUT_QUALITY_INVALID",
                    f"warm-cache quality gate rejected: {codes}",
                )
            warm_probe = {
                "inferenceMs": warm_inference_ms,
                "qualityFindings": warm_findings,
                "qualitySummary": warm_summary,
                "stemHashEquality": {
                    warm.stem: worker.sha256_file(warm) == worker.sha256_file(cold)
                    for warm, cold in zip(warm_outputs, outputs)
                },
            }

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
            "sourceContract": source_contract,
            "canonicalInputMetrics": canonical_metrics.as_dict(),
            "stemContracts": contracts,
            "stemSha256": {path.stem: worker.sha256_file(path) for path in outputs},
            "stemMetrics": {name: stem_metrics[name].as_dict() for name in worker.STEMS},
            "qualitySummary": quality_summary,
            "qualityFindings": quality_findings,
            "sampledReconstructionSnrDb": reconstruction_snr_db,
            "preparedSha256": {path.stem: worker.sha256_file(path) for path in prepared},
            "preparedMetrics": prepared_metrics,
            "sharedGainDb": shared_gain_db,
            "downloadMs": download_ms,
            "canonicalizeMs": canonical_ms,
            "assetValidationMs": asset_validation_ms,
            "inferenceMs": inference_ms,
            "normalizeMs": normalize_ms,
            "qualityMs": quality_ms,
            "reconstructionDiagnosticMs": reconstruction_ms,
            "renderMs": render_ms,
            "totalMs": total_ms,
            "processCpuSeconds": cpu_seconds,
            "memoryGiB": args.memory_gib,
            "estimatedCostUsd": cost,
            "maxRssKiB": resource.getrusage(resource.RUSAGE_SELF).ru_maxrss,
            "imageIdentity": args.image_identity,
            "pricingBasis": args.pricing_basis,
            "warmProbe": warm_probe,
            "wallMsIncludingWarmProbe": round((time.monotonic() - started_total) * 1000),
        }
        bundle_prefix = validate_bundle_prefix(args.bundle_prefix)
        if bundle_prefix is not None:
            result["bundlePrefix"] = bundle_prefix
            upload_listening_bundle(bucket, bundle_prefix, root, outputs, prepared, recombined, result)
        return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project", default=os.environ.get("GBW_BENCH_PROJECT"), required=not bool(os.environ.get("GBW_BENCH_PROJECT")))
    parser.add_argument("--bucket", default=os.environ.get("GBW_BENCH_BUCKET"), required=not bool(os.environ.get("GBW_BENCH_BUCKET")))
    parser.add_argument("--input-path", default=os.environ.get("GBW_BENCH_INPUT_PATH"), required=not bool(os.environ.get("GBW_BENCH_INPUT_PATH")))
    parser.add_argument("--strategy", choices=sorted(STRATEGIES), default=os.environ.get("GBW_BENCH_STRATEGY"), required=not bool(os.environ.get("GBW_BENCH_STRATEGY")))
    parser.add_argument("--model-repo", default=os.environ.get("GBW_MODEL_REPO", "/opt/demucs/models"))
    parser.add_argument("--timeout-seconds", type=int, default=int(os.environ.get("GBW_BENCH_TIMEOUT_SECONDS", "1700")))
    parser.add_argument("--memory-gib", type=float, default=float(os.environ.get("GBW_BENCH_MEMORY_GIB", "16")))
    parser.add_argument("--vcpu-price-per-second", type=float, default=env_float("GBW_BENCH_VCPU_PRICE_PER_SECOND"))
    parser.add_argument("--memory-price-per-gib-second", type=float, default=env_float("GBW_BENCH_MEMORY_PRICE_PER_GIB_SECOND"))
    parser.add_argument("--bundle-prefix", default=os.environ.get("GBW_BENCH_BUNDLE_PREFIX"))
    parser.add_argument("--image-identity", default=os.environ.get("GBW_BENCH_IMAGE_IDENTITY"))
    parser.add_argument("--pricing-basis", default=os.environ.get("GBW_BENCH_PRICING_BASIS"))
    parser.add_argument("--warm-probe", action="store_true", default=env_flag("GBW_BENCH_WARM_PROBE"))
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
