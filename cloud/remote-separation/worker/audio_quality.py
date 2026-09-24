#!/usr/bin/env python3
"""Stage-level audio metrics and conservative W2 quality gates.

Measurements and policy are deliberately separate. Energy distribution is always
reported, but a high other-stem share remains diagnostic until an accepted
official-Demucs baseline calibrates a safe hard threshold. The known RC19 failure
signature is still fail-closed through systematic same-polarity DC detection.
"""

from __future__ import annotations

import math
import sys
from array import array
from dataclasses import dataclass
from pathlib import Path

STEMS = ("drums", "bass", "other", "vocals", "guitar", "piano")
GROSS_DC_ABS = 0.005
SYSTEMATIC_DC_MIN_STEMS = 4
ABSURD_PEAK = 4.0
SILENT_ENERGY = 1e-12
OTHER_SHARE_DIAGNOSTIC = 0.40
OTHER_TO_GUITAR_DIAGNOSTIC = 5.0


class QualityError(RuntimeError):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


@dataclass(frozen=True)
class AudioMetrics:
    frames: int
    finite_samples: int
    non_finite_samples: int
    peak_per_channel: tuple[float, float]
    rms_per_channel: tuple[float | None, float | None]
    mean_dc_per_channel: tuple[float | None, float | None]

    @property
    def energy(self) -> float:
        values = [value * value for value in self.rms_per_channel if value is not None]
        return sum(values) / len(values) if values else 0.0

    def as_dict(self) -> dict:
        return {
            "frames": self.frames,
            "finiteSamples": self.finite_samples,
            "nonFiniteSamples": self.non_finite_samples,
            "peakPerChannel": list(self.peak_per_channel),
            "rmsPerChannel": list(self.rms_per_channel),
            "meanDcPerChannel": list(self.mean_dc_per_channel),
            "energy": self.energy,
        }


def measure_float32_stereo(path: Path, data_offset: int, data_bytes: int) -> AudioMetrics:
    """Measure a validated stereo float32 PCM data chunk without loading it all."""

    if data_offset < 0 or data_bytes <= 0 or data_bytes % 8:
        raise QualityError("FORMAT_INVALID", "float32 stereo data span is invalid")
    count = [0, 0]
    sums = [0.0, 0.0]
    sums_sq = [0.0, 0.0]
    peaks = [0.0, 0.0]
    finite_samples = 0
    non_finite_samples = 0
    sample_index = 0
    remaining = data_bytes
    with path.open("rb") as handle:
        handle.seek(data_offset)
        while remaining:
            block = handle.read(min(1024 * 1024, remaining))
            if not block or len(block) % 4:
                raise QualityError("FORMAT_INVALID", f"truncated float payload: {path.name}")
            values = array("f")
            values.frombytes(block)
            if sys.byteorder != "little":
                values.byteswap()
            for value in values:
                channel = sample_index & 1
                sample_index += 1
                numeric = float(value)
                if not math.isfinite(numeric):
                    non_finite_samples += 1
                    continue
                finite_samples += 1
                count[channel] += 1
                sums[channel] += numeric
                sums_sq[channel] += numeric * numeric
                peaks[channel] = max(peaks[channel], abs(numeric))
            remaining -= len(block)
    if sample_index * 4 != data_bytes:
        raise QualityError("FORMAT_INVALID", f"float payload length mismatch: {path.name}")

    rms = tuple(
        math.sqrt(sums_sq[channel] / count[channel]) if count[channel] else None
        for channel in range(2)
    )
    mean = tuple(
        sums[channel] / count[channel] if count[channel] else None
        for channel in range(2)
    )
    return AudioMetrics(
        frames=data_bytes // 8,
        finite_samples=finite_samples,
        non_finite_samples=non_finite_samples,
        peak_per_channel=(peaks[0], peaks[1]),
        rms_per_channel=(rms[0], rms[1]),
        mean_dc_per_channel=(mean[0], mean[1]),
    )


def _signed_gross_dc(metrics: AudioMetrics) -> int:
    left, right = metrics.mean_dc_per_channel
    if left is None or right is None:
        return 0
    if min(abs(left), abs(right)) < GROSS_DC_ABS:
        return 0
    if left > 0 and right > 0:
        return 1
    if left < 0 and right < 0:
        return -1
    return 0


def summarize_stems(stems: dict[str, AudioMetrics]) -> dict:
    if set(stems) != set(STEMS) or len(stems) != len(STEMS):
        raise QualityError("OUTPUT_MISSING", "quality metrics require exactly six named stems")
    energies = {name: stems[name].energy for name in STEMS}
    total_energy = sum(energies.values())
    shares = {
        name: (energies[name] / total_energy if total_energy > 0.0 else 0.0)
        for name in STEMS
    }
    guitar_energy = energies["guitar"]
    other_to_guitar = energies["other"] / guitar_energy if guitar_energy > SILENT_ENERGY else None
    polarities = {name: _signed_gross_dc(stems[name]) for name in STEMS}
    positive = sorted(name for name, polarity in polarities.items() if polarity > 0)
    negative = sorted(name for name, polarity in polarities.items() if polarity < 0)
    dominant_polarity = 1 if len(positive) >= len(negative) else -1
    affected = positive if dominant_polarity > 0 else negative
    systematic_dc = len(affected) >= SYSTEMATIC_DC_MIN_STEMS
    other_concentration = (
        shares["other"] >= OTHER_SHARE_DIAGNOSTIC
        and other_to_guitar is not None
        and other_to_guitar >= OTHER_TO_GUITAR_DIAGNOSTIC
    )
    return {
        "energyByStem": energies,
        "energyShareByStem": shares,
        "otherToGuitarEnergyRatio": other_to_guitar,
        "systematicDc": {
            "detected": systematic_dc,
            "polarity": "positive" if dominant_polarity > 0 else "negative",
            "affectedStems": affected,
            "thresholdAbsMean": GROSS_DC_ABS,
        },
        "otherConcentrationDiagnostic": {
            "detected": other_concentration,
            "minimumShare": OTHER_SHARE_DIAGNOSTIC,
            "minimumOtherToGuitarRatio": OTHER_TO_GUITAR_DIAGNOSTIC,
        },
    }


def evaluate_stems(stems: dict[str, AudioMetrics]) -> tuple[dict, list[dict]]:
    summary = summarize_stems(stems)
    findings: list[dict] = []
    for name in STEMS:
        metrics = stems[name]
        if metrics.non_finite_samples:
            findings.append({
                "severity": "reject",
                "code": "NON_FINITE_STEM",
                "stem": name,
                "count": metrics.non_finite_samples,
            })
        if max(metrics.peak_per_channel) > ABSURD_PEAK:
            findings.append({
                "severity": "reject",
                "code": "AMPLITUDE_EXPLOSION",
                "stem": name,
                "peak": max(metrics.peak_per_channel),
                "limit": ABSURD_PEAK,
            })
        if metrics.energy <= SILENT_ENERGY:
            findings.append({
                "severity": "reject",
                "code": "SILENT_STEM",
                "stem": name,
                "energy": metrics.energy,
            })
    if summary["systematicDc"]["detected"]:
        findings.append({
            "severity": "reject",
            "code": "SYSTEMATIC_STEM_DC",
            **summary["systematicDc"],
        })
    if summary["otherConcentrationDiagnostic"]["detected"]:
        findings.append({
            "severity": "diagnostic",
            "code": "OTHER_CONCENTRATION",
            "otherShare": summary["energyShareByStem"]["other"],
            "otherToGuitarEnergyRatio": summary["otherToGuitarEnergyRatio"],
        })
    return summary, findings


def require_safe_stems(stems: dict[str, AudioMetrics]) -> tuple[dict, list[dict]]:
    summary, findings = evaluate_stems(stems)
    rejected = [finding for finding in findings if finding["severity"] == "reject"]
    if rejected:
        codes = ",".join(sorted({str(finding["code"]) for finding in rejected}))
        raise QualityError("OUTPUT_QUALITY_INVALID", f"stem quality gate rejected: {codes}")
    return summary, findings
