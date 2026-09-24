#!/usr/bin/env python3
"""Engine-neutral boundary for the official PyTorch Demucs worker.

The production worker is responsible for Cloud/Firebase lifecycle and prepared-reference
rendering. This module owns only the inference engine contract: immutable model assets,
exact CLI arguments, bounded execution, cancellation, and discovery of the six private stems.
"""

from __future__ import annotations

import hashlib
import os
import signal
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

STEMS = ("drums", "bass", "other", "vocals", "guitar", "piano")
ENGINE_NAME = "demucs-pytorch"
DEMUCS_VERSION = "4.1.0"
PYTORCH_VERSION = "2.14.0+cpu"
ENGINE_REVISION = f"demucs={DEMUCS_VERSION};torch={PYTORCH_VERSION}"
MODEL_NAME = "htdemucs_6s"
MODEL_SIGNATURE = "5c90dfd2"
MODEL_FILE = "5c90dfd2-34c22ccb.th"
MODEL_BYTES = 54_996_327
MODEL_SHA256 = "34c22ccb381c6f9fdbf324f04e1e2fe21aaaf293f5ded163a162697ff9a02ddd"
MODEL_BAG_FILE = "htdemucs_6s.yaml"
MODEL_BAG_CONTENT = "models: ['5c90dfd2']\n"
DEFAULT_MODEL_REPO = Path("/opt/demucs/models")


class EngineError(RuntimeError):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


@dataclass(frozen=True)
class DemucsEngineConfig:
    model_repo: Path = DEFAULT_MODEL_REPO
    device: str = "cpu"
    shifts: int = 1
    overlap: float = 0.5
    cpu_threads: int = 8
    python_executable: str = sys.executable

    def validate(self) -> None:
        if self.device not in {"cpu", "cuda"}:
            raise EngineError("CONFIG_INVALID", f"unsupported Demucs device: {self.device}")
        if self.shifts < 1:
            raise EngineError("CONFIG_INVALID", "Demucs shifts must be >= 1")
        if not 0.0 <= self.overlap < 1.0:
            raise EngineError("CONFIG_INVALID", "Demucs overlap must be >= 0 and < 1")
        if self.cpu_threads < 1:
            raise EngineError("CONFIG_INVALID", "Demucs CPU threads must be >= 1")
        if not self.python_executable.strip():
            raise EngineError("CONFIG_INVALID", "Python executable must be explicit")

    @property
    def strategy(self) -> str:
        overlap = format(self.overlap, ".3f").rstrip("0").rstrip(".")
        return f"pytorch-{self.device}-s{self.shifts}-o{overlap}-t{self.cpu_threads}"


class DemucsPyTorchRunner:
    def __init__(self, config: DemucsEngineConfig):
        config.validate()
        self.config = config
        self._process: subprocess.Popen[str] | None = None

    def validate_assets(self) -> None:
        repo = self.config.model_repo
        checkpoint = repo / MODEL_FILE
        bag = repo / MODEL_BAG_FILE
        if not repo.is_dir():
            raise EngineError("MODEL_INVALID", f"Demucs model repo missing: {repo}")
        if not checkpoint.is_file():
            raise EngineError("MODEL_INVALID", f"Demucs checkpoint missing: {checkpoint.name}")
        if checkpoint.stat().st_size != MODEL_BYTES:
            raise EngineError("MODEL_INVALID", "htdemucs_6s checkpoint size mismatch")
        if sha256_file(checkpoint) != MODEL_SHA256:
            raise EngineError("MODEL_INVALID", "htdemucs_6s checkpoint SHA-256 mismatch")
        if not bag.is_file() or bag.read_text(encoding="utf-8") != MODEL_BAG_CONTENT:
            raise EngineError("MODEL_INVALID", "htdemucs_6s local bag definition mismatch")

    def command(self, source: Path, output_root: Path) -> list[str]:
        return [
            self.config.python_executable,
            "-m",
            "demucs.separate",
            "--repo",
            str(self.config.model_repo),
            "-n",
            MODEL_NAME,
            "--float32",
            "--clip-mode",
            "none",
            "--shifts",
            str(self.config.shifts),
            "--overlap",
            format(self.config.overlap, ".6g"),
            "-d",
            self.config.device,
            "-o",
            str(output_root),
            str(source),
        ]

    def environment(self) -> dict[str, str]:
        env = os.environ.copy()
        threads = str(self.config.cpu_threads)
        for name in ("OMP_NUM_THREADS", "MKL_NUM_THREADS", "OPENBLAS_NUM_THREADS", "NUMEXPR_NUM_THREADS"):
            env[name] = threads
        env.setdefault("PYTHONUNBUFFERED", "1")
        return env

    def cancel(self) -> None:
        process = self._process
        if process is None or process.poll() is not None:
            return
        try:
            os.killpg(process.pid, signal.SIGTERM)
        except ProcessLookupError:
            return

    def run(self, source: Path, output_root: Path, timeout_seconds: int = 1_700) -> list[Path]:
        self.validate_assets()
        if not source.is_file():
            raise EngineError("INPUT_UNSUPPORTED", f"canonical input missing: {source}")
        output_root.mkdir(parents=True, exist_ok=True)
        command = self.command(source, output_root)
        process = subprocess.Popen(
            command,
            env=self.environment(),
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            start_new_session=True,
        )
        self._process = process
        try:
            stdout, _ = process.communicate(timeout=timeout_seconds)
        except subprocess.TimeoutExpired:
            self.cancel()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                try:
                    os.killpg(process.pid, signal.SIGKILL)
                except ProcessLookupError:
                    pass
                process.wait(timeout=10)
            raise
        finally:
            self._process = None

        if process.returncode:
            tail = (stdout or "")[-2_000:]
            raise EngineError("DEMUCS_FAILED", f"Demucs exited {process.returncode}: {tail}")

        track_dir = output_root / MODEL_NAME / source.stem
        outputs = [track_dir / f"{stem}.wav" for stem in STEMS]
        missing = [path.name for path in outputs if not path.is_file()]
        if missing:
            raise EngineError("OUTPUT_MISSING", f"official Demucs missing stems: {', '.join(missing)}")
        return outputs
