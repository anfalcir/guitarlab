import importlib.util
import math
import struct
import sys
import tempfile
import unittest
from pathlib import Path

MODULE = Path(__file__).parents[1] / "audio_quality.py"
spec = importlib.util.spec_from_file_location("audio_quality", MODULE)
quality = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = quality
spec.loader.exec_module(quality)


def metrics(rms, dc, peak=0.9, non_finite=0):
    return quality.AudioMetrics(
        frames=100,
        finite_samples=200 - non_finite,
        non_finite_samples=non_finite,
        peak_per_channel=(peak, peak),
        rms_per_channel=(rms, rms),
        mean_dc_per_channel=(dc, dc),
    )


class AudioQualityTest(unittest.TestCase):
    def test_measure_float32_stereo_reports_peak_rms_dc_and_finiteness(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "pcm.bin"
            values = [0.5, -0.5, 0.5, -0.5, float("nan"), 0.0]
            path.write_bytes(struct.pack("<" + "f" * len(values), *values))
            measured = quality.measure_float32_stereo(path, 0, len(values) * 4)
            self.assertEqual(measured.frames, 3)
            self.assertEqual(measured.non_finite_samples, 1)
            self.assertEqual(measured.finite_samples, 5)
            self.assertAlmostEqual(measured.peak_per_channel[0], 0.5)
            self.assertAlmostEqual(measured.peak_per_channel[1], 0.5)
            self.assertAlmostEqual(measured.mean_dc_per_channel[0], 0.5)
            self.assertAlmostEqual(measured.mean_dc_per_channel[1], -1.0 / 3.0)
            self.assertTrue(math.isfinite(measured.energy))

    def test_known_rc19_f3_signature_is_rejected_and_other_collapse_is_visible(self):
        evidence = {
            "drums": quality.AudioMetrics(1, 2, 0, (0.990866, 0.977934), (0.1470643, 0.1473445), (-0.0153398, -0.0156896)),
            "bass": quality.AudioMetrics(1, 2, 0, (0.567860, 0.621570), (0.0467919, 0.0476618), (-0.0153052, -0.0147431)),
            "other": quality.AudioMetrics(1, 2, 0, (0.932966, 0.925337), (0.2548659, 0.2539341), (-0.0133531, -0.0138187)),
            "vocals": quality.AudioMetrics(1, 2, 0, (0.947759, 0.930906), (0.2302305, 0.2308942), (-0.0147668, -0.0148184)),
            "guitar": quality.AudioMetrics(1, 2, 0, (0.783335, 0.751910), (0.0866346, 0.0866124), (-0.0146805, -0.0147581)),
            "piano": quality.AudioMetrics(1, 2, 0, (0.331459, 0.376721), (0.0251797, 0.0254132), (-0.0145588, -0.0144452)),
        }
        summary, findings = quality.evaluate_stems(evidence)
        self.assertTrue(summary["systematicDc"]["detected"])
        self.assertEqual(len(summary["systematicDc"]["affectedStems"]), 6)
        self.assertGreater(summary["energyShareByStem"]["other"], 0.40)
        self.assertGreater(summary["otherToGuitarEnergyRatio"], 5.0)
        self.assertTrue(summary["otherConcentrationDiagnostic"]["detected"])
        self.assertIn("SYSTEMATIC_STEM_DC", {row["code"] for row in findings})
        self.assertIn("OTHER_CONCENTRATION", {row["code"] for row in findings})
        with self.assertRaisesRegex(quality.QualityError, "SYSTEMATIC_STEM_DC"):
            quality.require_safe_stems(evidence)

    def test_other_concentration_is_diagnostic_not_universal_veto(self):
        clean = {
            "drums": metrics(0.10, 0.0001),
            "bass": metrics(0.04, -0.0001),
            "other": metrics(0.25, 0.0002),
            "vocals": metrics(0.12, -0.0001),
            "guitar": metrics(0.08, 0.0001),
            "piano": metrics(0.02, 0.0001),
        }
        summary, findings = quality.require_safe_stems(clean)
        self.assertTrue(summary["otherConcentrationDiagnostic"]["detected"])
        diagnostic = [row for row in findings if row["code"] == "OTHER_CONCENTRATION"]
        self.assertEqual(diagnostic[0]["severity"], "diagnostic")

    def test_non_finite_amplitude_explosion_and_silence_are_hard_failures(self):
        base = {name: metrics(0.1, 0.0) for name in quality.STEMS}
        base["drums"] = metrics(0.1, 0.0, non_finite=1)
        base["bass"] = metrics(0.1, 0.0, peak=5.0)
        base["piano"] = metrics(0.0, 0.0, peak=0.0)
        _, findings = quality.evaluate_stems(base)
        codes = {row["code"] for row in findings if row["severity"] == "reject"}
        self.assertEqual(codes, {"NON_FINITE_STEM", "AMPLITUDE_EXPLOSION", "SILENT_STEM"})
        with self.assertRaises(quality.QualityError):
            quality.require_safe_stems(base)

    def test_missing_or_extra_stems_fail_closed(self):
        incomplete = {name: metrics(0.1, 0.0) for name in quality.STEMS[:-1]}
        with self.assertRaisesRegex(quality.QualityError, "exactly six"):
            quality.summarize_stems(incomplete)


if __name__ == "__main__":
    unittest.main()
