import hashlib
import importlib.util
import os
import struct
import sys
import tempfile
import unittest
from unittest import mock
from pathlib import Path
from types import SimpleNamespace

MODULE = Path(__file__).parents[1] / "gbw_worker.py"
spec = importlib.util.spec_from_file_location("gbw_worker", MODULE)
worker = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = worker
spec.loader.exec_module(worker)


def float_wav(path: Path, frames: int = 441):
    data = b"\0" * frames * 8
    fmt = struct.pack("<HHIIHH", 3, 2, 44100, 352800, 8, 32)
    body = b"fmt " + struct.pack("<I", len(fmt)) + fmt + b"data" + struct.pack("<I", len(data)) + data
    path.write_bytes(b"RIFF" + struct.pack("<I", len(body) + 4) + b"WAVE" + body)


def extensible_float_wav(path: Path, frames: int = 441, subtype: bytes | None = None):
    data = b"\0" * frames * 8
    subtype = subtype or worker.IEEE_FLOAT_SUBFORMAT_GUID
    fmt = (
        struct.pack("<HHIIHH", worker.WAVE_FORMAT_EXTENSIBLE, 2, 44100, 352800, 8, 32)
        + struct.pack("<H", 22)
        + struct.pack("<H", 32)
        + struct.pack("<I", 3)
        + subtype
    )
    body = b"fmt " + struct.pack("<I", len(fmt)) + fmt + b"data" + struct.pack("<I", len(data)) + data
    path.write_bytes(b"RIFF" + struct.pack("<I", len(body) + 4) + b"WAVE" + body)


class WorkerContractTest(unittest.TestCase):
    def test_sha256(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "x"
            path.write_bytes(b"gbw")
            self.assertEqual(worker.sha256_file(path), hashlib.sha256(b"gbw").hexdigest())

    def test_wav_contract(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "x.wav"
            float_wav(path)
            self.assertEqual(worker.wav_contract(path), (44100, 2, 441, 0.01))

    def test_accepts_wave_format_extensible_ieee_float(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "extensible.wav"
            extensible_float_wav(path)
            self.assertEqual(worker.wav_contract(path), (44100, 2, 441, 0.01))

    def test_rejects_wave_format_extensible_pcm_subtype(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "extensible-pcm.wav"
            pcm_subtype = bytes.fromhex("0100000000001000800000aa00389b71")
            extensible_float_wav(path, subtype=pcm_subtype)
            with self.assertRaisesRegex(worker.WorkerError, "subtype="):
                worker.wav_contract(path)

    def test_rejects_pcm16(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "x.wav"
            float_wav(path)
            raw = bytearray(path.read_bytes())
            raw[20:22] = struct.pack("<H", 1)
            path.write_bytes(raw)
            with self.assertRaises(worker.WorkerError):
                worker.wav_contract(path)

    def test_normalize_requires_six_stems(self):
        with tempfile.TemporaryDirectory() as tmp:
            raw, final = Path(tmp) / "raw", Path(tmp) / "final"
            raw.mkdir()
            for index, stem in enumerate(worker.STEMS[:-1]):
                float_wav(raw / f"target_{index}_{stem}.wav")
            with mock.patch.object(worker, "canonicalize", side_effect=lambda source, destination: destination.write_bytes(source.read_bytes())):
                with self.assertRaisesRegex(worker.WorkerError, "piano"):
                    worker.normalize_outputs(raw, final)

    def test_failure_reporting_releases_running_job(self):
        updates = []
        document = SimpleNamespace(
            get=lambda: SimpleNamespace(exists=True, to_dict=lambda: {"state": "RUNNING"}),
            update=lambda value: updates.append(value),
        )
        client = SimpleNamespace(document=lambda _path: document)
        config = SimpleNamespace(uid="user", job_id="job")

        worker.report_terminal_failure(
            config,
            "DEMUCS_FAILED",
            "Demucs exited 9",
            lambda: client,
        )

        self.assertEqual(updates[0]["state"], "FAILED")
        self.assertEqual(updates[0]["phase"], "FAILED")
        self.assertEqual(updates[0]["errorCode"], "DEMUCS_FAILED")
        self.assertEqual(updates[0]["errorMessage"], "Demucs exited 9")

    def test_failure_reporting_preserves_cancelled_job(self):
        updates = []
        document = SimpleNamespace(
            get=lambda: SimpleNamespace(exists=True, to_dict=lambda: {"state": "CANCELLED"}),
            update=lambda value: updates.append(value),
        )
        client = SimpleNamespace(document=lambda _path: document)
        config = SimpleNamespace(uid="user", job_id="job")

        worker.report_terminal_failure(config, "DEMUCS_FAILED", "boom", lambda: client)

        self.assertEqual(updates, [])

    def test_production_mt4_omp2_command_contract(self):
        env = {
            "GBW_BUCKET": "bucket",
            "GBW_UID": "user",
            "GBW_JOB_ID": "00000000-0000-4000-8000-000000000001",
            "GBW_PROJECT_ID": "00000000-0000-4000-8000-000000000002",
            "GBW_INPUT_PATH": "remote/v1/users/user/jobs/00000000-0000-4000-8000-000000000001/input/source.wav",
            "GBW_INPUT_SHA256": "a" * 64,
            "GBW_MODEL_PATH": "/model/" + worker.MODEL_NAME,
            "GBW_DEMUCS_BINARY": "/usr/local/bin/demucs_mt.cpp.main",
            "GBW_DEMUCS_MT_THREADS": "4",
            "GBW_VCPU": "8",
            "OPENBLAS_NUM_THREADS": "2",
        }
        with mock.patch.dict(os.environ, env, clear=True):
            config = worker.Config.from_env()
        command = worker.demucs_command(config, Path("/tmp/input.wav"), Path("/tmp/raw"))
        self.assertEqual(command[-1], "4")
        self.assertEqual(config.blas_threads, 2)
        self.assertEqual(config.demucs_threads, 4)
        self.assertEqual(worker.inference_strategy(config), "mt4_omp2")

    def test_thread_plan_rejects_oversubscription(self):
        env = {
            "GBW_BUCKET": "bucket",
            "GBW_UID": "user",
            "GBW_JOB_ID": "00000000-0000-4000-8000-000000000001",
            "GBW_PROJECT_ID": "00000000-0000-4000-8000-000000000002",
            "GBW_INPUT_PATH": "remote/v1/users/user/jobs/00000000-0000-4000-8000-000000000001/input/source.wav",
            "GBW_INPUT_SHA256": "a" * 64,
            "GBW_DEMUCS_BINARY": "/usr/local/bin/demucs_mt.cpp.main",
            "GBW_DEMUCS_MT_THREADS": "4",
            "GBW_VCPU": "8",
            "OPENBLAS_NUM_THREADS": "4",
        }
        with mock.patch.dict(os.environ, env, clear=True):
            with self.assertRaisesRegex(worker.WorkerError, "oversubscribes"):
                worker.Config.from_env()

    def test_mt_binary_requires_mt_thread_count(self):
        env = {
            "GBW_BUCKET": "bucket",
            "GBW_UID": "user",
            "GBW_JOB_ID": "00000000-0000-4000-8000-000000000001",
            "GBW_PROJECT_ID": "00000000-0000-4000-8000-000000000002",
            "GBW_INPUT_PATH": "remote/v1/users/user/jobs/00000000-0000-4000-8000-000000000001/input/source.wav",
            "GBW_INPUT_SHA256": "a" * 64,
            "GBW_DEMUCS_BINARY": "/usr/local/bin/demucs_mt.cpp.main",
            "GBW_DEMUCS_MT_THREADS": "0",
            "GBW_VCPU": "8",
            "OPENBLAS_NUM_THREADS": "2",
        }
        with mock.patch.dict(os.environ, env, clear=True):
            with self.assertRaisesRegex(worker.WorkerError, "disagree"):
                worker.Config.from_env()


    def test_manifest_v2_exposes_only_prepared_deliverables(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            backing = root / "backing.wav"
            guitar = root / "guitar.wav"
            float_wav(backing)
            float_wav(guitar)
            config = SimpleNamespace(
                prefix="remote/v1/users/u/jobs/00000000-0000-4000-8000-000000000001",
                job_id="00000000-0000-4000-8000-000000000001",
                uid="u",
                project_id="00000000-0000-4000-8000-000000000002",
                blas_threads=2,
                demucs_threads=4,
            )
            with mock.patch.dict(os.environ, {"GBW_VCPU": "8"}, clear=False):
                manifest = worker.build_manifest(
                    config,
                    "a" * 64,
                    [backing, guitar],
                    -0.75,
                    "2026-09-23T00:00:00+00:00",
                    0.0,
                )
            self.assertEqual(manifest["schemaVersion"], 2)
            self.assertNotIn("stems", manifest)
            self.assertEqual([row["name"] for row in manifest["deliverables"]], ["backing", "guitar"])
            self.assertEqual(
                [row["role"] for row in manifest["deliverables"]],
                ["REFERENCE_BACKING", "REFERENCE_GUITAR"],
            )
            self.assertEqual(manifest["referenceRecipe"]["version"], "prepared-reference-v2")
            self.assertEqual(manifest["referenceRecipe"]["sharedGainDb"], -0.75)

    def test_reference_render_uses_one_shared_gain_for_backing_and_guitar(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            stems = []
            for stem in worker.STEMS:
                path = root / f"{stem}.wav"
                float_wav(path)
                stems.append(path)

            def fake_mix(_inputs, output):
                float_wav(output)

            def fake_gain(source, output, _gain_db):
                output.write_bytes(source.read_bytes())

            with mock.patch.object(worker, "mix_float", side_effect=fake_mix), \
                 mock.patch.object(worker, "peak_dbfs", side_effect=[-0.2, -3.0, -0.1]), \
                 mock.patch.object(worker, "apply_gain", side_effect=fake_gain):
                outputs, gain_db = worker.render_prepared_references(stems, root / "prepared")

            self.assertAlmostEqual(gain_db, -0.9, places=6)
            self.assertEqual([path.name for path in outputs], ["backing.wav", "guitar.wav"])
            self.assertEqual(worker.wav_contract(outputs[0]), worker.wav_contract(outputs[1]))


if __name__ == "__main__":
    unittest.main()
