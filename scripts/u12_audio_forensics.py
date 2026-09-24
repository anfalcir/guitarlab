#!/usr/bin/env python3
"""Streaming local WAV metrics for the U12 RC19 audio investigation."""

from __future__ import annotations

import argparse
from array import array
import hashlib
import json
import math
from pathlib import Path
import struct
import sys


FLOAT = 3
EXTENSIBLE = 0xFFFE
FLOAT_GUID = bytes.fromhex("0300000000001000800000aa00389b71")


def wav_layout(path: Path) -> dict:
    with path.open("rb") as stream:
        header = stream.read(12)
        if len(header) != 12 or header[:4] != b"RIFF" or header[8:] != b"WAVE":
            raise ValueError(f"invalid WAV: {path}")
        fmt = None
        data_offset = data_bytes = None
        while chunk := stream.read(8):
            if len(chunk) != 8:
                raise ValueError("truncated WAV chunk")
            chunk_id, size = chunk[:4], struct.unpack("<I", chunk[4:])[0]
            body = stream.tell()
            if chunk_id == b"fmt ":
                fmt = stream.read(size)
            elif chunk_id == b"data":
                data_offset, data_bytes = body, size
                stream.seek(size, 1)
            else:
                stream.seek(size, 1)
            if size & 1:
                stream.seek(1, 1)
        if fmt is None or len(fmt) < 16 or data_offset is None or data_bytes is None:
            raise ValueError("missing WAV fmt/data")
        encoding, channels, rate, _, align, bits = struct.unpack_from("<HHIIHH", fmt)
        float_encoding = encoding == FLOAT
        if encoding == EXTENSIBLE and len(fmt) >= 40:
            float_encoding = struct.unpack_from("<H", fmt, 16)[0] >= 22 and fmt[24:40] == FLOAT_GUID
        if not float_encoding or bits != 32 or align != channels * 4 or data_bytes % align:
            raise ValueError("only aligned float32 WAV is supported")
        return {
            "encoding": "FLOAT32_LE",
            "sampleRate": rate,
            "channels": channels,
            "frames": data_bytes // align,
            "dataOffset": data_offset,
            "dataBytes": data_bytes,
        }


def sample_blocks(path: Path, layout: dict, block_frames: int = 16_384):
    with path.open("rb") as stream:
        stream.seek(layout["dataOffset"])
        remaining = layout["dataBytes"]
        block_bytes = block_frames * layout["channels"] * 4
        while remaining:
            raw = stream.read(min(block_bytes, remaining))
            if not raw:
                raise ValueError("truncated WAV data")
            values = array("f")
            values.frombytes(raw)
            if sys.byteorder != "little":
                values.byteswap()
            yield values
            remaining -= len(raw)


def metrics(path: Path) -> dict:
    layout = wav_layout(path)
    channels = layout["channels"]
    count = [0] * channels
    sums = [0.0] * channels
    squares = [0.0] * channels
    peaks = [0.0] * channels
    cross = left_sq = right_sq = 0.0
    finite = non_finite = silent_frames = longest_silence = current_silence = 0
    threshold = 10 ** (-80 / 20)
    for values in sample_blocks(path, layout):
        frames = len(values) // channels
        for frame in range(frames):
            frame_peak = 0.0
            for channel in range(channels):
                value = float(values[frame * channels + channel])
                if not math.isfinite(value):
                    non_finite += 1
                    continue
                finite += 1
                count[channel] += 1
                sums[channel] += value
                squares[channel] += value * value
                peaks[channel] = max(peaks[channel], abs(value))
                frame_peak = max(frame_peak, abs(value))
            if channels == 2:
                left, right = float(values[frame * 2]), float(values[frame * 2 + 1])
                if math.isfinite(left) and math.isfinite(right):
                    cross += left * right
                    left_sq += left * left
                    right_sq += right * right
            if frame_peak <= threshold:
                silent_frames += 1
                current_silence += 1
                longest_silence = max(longest_silence, current_silence)
            else:
                current_silence = 0
    means = [sums[i] / count[i] if count[i] else None for i in range(channels)]
    rms = [math.sqrt(squares[i] / count[i]) if count[i] else None for i in range(channels)]
    crest = [peaks[i] / rms[i] if rms[i] else None for i in range(channels)]
    correlation = cross / math.sqrt(left_sq * right_sq) if channels == 2 and left_sq and right_sq else None
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return {
        "path": str(path),
        "sha256": digest.hexdigest(),
        "bytes": path.stat().st_size,
        **{key: layout[key] for key in ("encoding", "sampleRate", "channels", "frames")},
        "durationSeconds": layout["frames"] / layout["sampleRate"],
        "finiteSamples": finite,
        "nonFiniteSamples": non_finite,
        "peakPerChannel": peaks,
        "rmsPerChannel": rms,
        "meanDcPerChannel": means,
        "crestFactorPerChannel": crest,
        "channelCorrelation": correlation,
        "silentFrameFractionBelowMinus80Dbfs": silent_frames / layout["frames"] if layout["frames"] else None,
        "longestSilenceSecondsBelowMinus80Dbfs": longest_silence / layout["sampleRate"],
    }


def recombination(first: Path, second: Path) -> dict:
    left, right = wav_layout(first), wav_layout(second)
    contract = (left["sampleRate"], left["channels"], left["frames"])
    if contract != (right["sampleRate"], right["channels"], right["frames"]):
        raise ValueError("reference contracts differ")
    peak = energy = samples = non_finite = 0
    for one, two in zip(sample_blocks(first, left), sample_blocks(second, right), strict=True):
        if len(one) != len(two):
            raise ValueError("reference block alignment differs")
        for a, b in zip(one, two):
            value = float(a) + float(b)
            if not math.isfinite(value):
                non_finite += 1
                continue
            peak = max(peak, abs(value))
            energy += value * value
            samples += 1
    return {
        "peak": peak,
        "peakDbfs": 20 * math.log10(peak) if peak else None,
        "rms": math.sqrt(energy / samples) if samples else None,
        "finiteSamples": samples,
        "nonFiniteSamples": non_finite,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("audio", nargs="+", type=Path)
    parser.add_argument("--recombine", action="store_true")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = {"schemaVersion": 1, "files": [metrics(path) for path in args.audio]}
    if args.recombine:
        if len(args.audio) != 2:
            parser.error("--recombine requires exactly two WAV files")
        result["recombination"] = recombination(*args.audio)
    encoded = json.dumps(result, indent=2, ensure_ascii=False) + "\n"
    if args.output:
        args.output.write_text(encoded, encoding="utf-8")
    else:
        print(encoded, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
