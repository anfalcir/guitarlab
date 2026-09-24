from pathlib import Path
import struct
import tempfile
import unittest

from scripts.u12_audio_forensics import metrics, recombination


def wav(path: Path, frames: list[tuple[float, float]]) -> None:
    samples = [sample for frame in frames for sample in frame]
    data = struct.pack("<" + "f" * len(samples), *samples)
    fmt = struct.pack("<HHIIHH", 3, 2, 44100, 352800, 8, 32)
    body = b"fmt " + struct.pack("<I", len(fmt)) + fmt + b"data" + struct.pack("<I", len(data)) + data
    path.write_bytes(b"RIFF" + struct.pack("<I", len(body) + 4) + b"WAVE" + body)


class AudioForensicsTest(unittest.TestCase):
    def test_metrics_expose_dc_peak_rms_and_correlation(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "dc.wav"
            wav(path, [(0.25, 0.25)] * 100)
            result = metrics(path)
            self.assertEqual([0.25, 0.25], result["meanDcPerChannel"])
            self.assertEqual([0.25, 0.25], result["peakPerChannel"])
            self.assertAlmostEqual(1.0, result["channelCorrelation"])
            self.assertEqual(0, result["nonFiniteSamples"])

    def test_recombination_measures_shared_sum(self):
        with tempfile.TemporaryDirectory() as tmp:
            first, second = Path(tmp) / "a.wav", Path(tmp) / "b.wav"
            wav(first, [(0.3, -0.3)] * 10)
            wav(second, [(0.2, -0.2)] * 10)
            result = recombination(first, second)
            self.assertAlmostEqual(0.5, result["peak"], places=6)
            self.assertEqual(20, result["finiteSamples"])


if __name__ == "__main__":
    unittest.main()
