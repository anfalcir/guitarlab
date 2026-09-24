#!/usr/bin/env python3
"""Generate a deterministic, royalty-free technical W4 benchmark fixture."""

from __future__ import annotations

import argparse
import math
import random
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 44_100
MOTIF_SECONDS = 12


def clamp(value: float) -> int:
    return max(-32767, min(32767, int(round(value * 32767.0))))


def motif_bytes() -> bytes:
    frames = SAMPLE_RATE * MOTIF_SECONDS
    out = bytearray()
    rng = random.Random(0x4755495441524C4142)
    roots = (55.0, 65.406, 73.416, 49.0)
    chord_ratios = ((1.0, 1.25, 1.5), (1.0, 1.2, 1.5), (1.0, 1.25, 1.6), (1.0, 1.2, 1.5))
    for index in range(frames):
        t = index / SAMPLE_RATE
        beat = (t * 2.0) % 1.0
        eighth = (t * 4.0) % 1.0
        bar = int(t // 2.0) % 4
        root = roots[bar]
        kick_env = math.exp(-10.0 * beat)
        kick = 0.28 * kick_env * math.sin(2.0 * math.pi * (48.0 + 22.0 * kick_env) * t)
        beat_number = int(t * 2.0) % 4
        snare_phase = ((t - 0.5) * 2.0) % 2.0
        snare_env = math.exp(-14.0 * (snare_phase % 1.0)) if beat_number in (1, 3) else 0.0
        noise = rng.uniform(-1.0, 1.0)
        snare = 0.12 * snare_env * noise
        hat = 0.035 * math.exp(-24.0 * eighth) * rng.uniform(-1.0, 1.0)
        bass = 0.15 * (
            math.sin(2.0 * math.pi * root * t)
            + 0.35 * math.sin(2.0 * math.pi * root * 2.0 * t)
        )
        chord_env = 0.55 + 0.45 * math.exp(-2.0 * (t % 2.0))
        chord = 0.0
        for ratio in chord_ratios[bar]:
            chord += math.sin(2.0 * math.pi * root * ratio * 4.0 * t)
        chord *= 0.035 * chord_env
        pluck_phase = (t * 3.0) % 1.0
        pluck_env = math.exp(-5.5 * pluck_phase)
        guitar = 0.085 * pluck_env * (
            math.sin(2.0 * math.pi * root * 6.0 * t)
            + 0.45 * math.sin(2.0 * math.pi * root * 9.0 * t)
        )
        lead_freq = root * (8.0 if (int(t) % 4) < 2 else 10.0)
        vibrato = 1.0 + 0.008 * math.sin(2.0 * math.pi * 5.2 * t)
        lead = 0.065 * math.sin(2.0 * math.pi * lead_freq * vibrato * t)
        left = kick + snare * 0.85 + hat * 0.65 + bass * 0.95 + chord * 1.1 + guitar * 1.15 + lead * 0.75
        right = kick + snare * 1.15 + hat * 1.35 + bass * 1.05 + chord * 0.9 + guitar * 0.85 + lead * 1.25
        out += struct.pack("<hh", clamp(left), clamp(right))
    return bytes(out)


def generate(path: Path, seconds: int) -> None:
    if seconds < 1 or seconds > 600:
        raise ValueError("fixture duration must be between 1 and 600 seconds")
    motif = motif_bytes()
    motif_frames = SAMPLE_RATE * MOTIF_SECONDS
    total_frames = SAMPLE_RATE * seconds
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as handle:
        handle.setnchannels(2)
        handle.setsampwidth(2)
        handle.setframerate(SAMPLE_RATE)
        full, remaining = divmod(total_frames, motif_frames)
        for _ in range(full):
            handle.writeframesraw(motif)
        if remaining:
            handle.writeframesraw(motif[: remaining * 4])
        handle.writeframes(b"")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("output", type=Path)
    parser.add_argument("--seconds", type=int, default=180)
    args = parser.parse_args()
    generate(args.output, args.seconds)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
