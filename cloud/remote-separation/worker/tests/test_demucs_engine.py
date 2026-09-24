import hashlib
import importlib.util
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

MODULE = Path(__file__).parents[1] / "demucs_engine.py"
spec = importlib.util.spec_from_file_location("demucs_engine", MODULE)
engine = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = engine
spec.loader.exec_module(engine)


class DemucsEngineContractTest(unittest.TestCase):
    def test_command_matches_rc20_official_baseline(self):
        config = engine.DemucsEngineConfig(
            model_repo=Path("/models"),
            device="cpu",
            shifts=1,
            overlap=0.5,
            cpu_threads=8,
            python_executable="/usr/local/bin/python",
        )
        runner = engine.DemucsPyTorchRunner(config)
        self.assertEqual(
            runner.command(Path("/tmp/input.wav"), Path("/tmp/out")),
            [
                "/usr/local/bin/python", "-m", "demucs.separate",
                "--repo", "/models", "-n", "htdemucs_6s",
                "--float32", "--clip-mode", "none",
                "--shifts", "1", "--overlap", "0.5",
                "-d", "cpu", "-o", "/tmp/out", "/tmp/input.wav",
            ],
        )
        self.assertEqual(config.strategy, "pytorch-cpu-s1-o0.5-t8")

    def test_runtime_packages_require_exact_versions(self):
        runner = engine.DemucsPyTorchRunner(engine.DemucsEngineConfig())
        versions = {
            "demucs": engine.DEMUCS_VERSION,
            "torch": engine.PYTORCH_VERSION,
            "numpy": engine.NUMPY_VERSION,
        }
        with mock.patch.object(engine.metadata, "version", side_effect=lambda name: versions[name]):
            runner.validate_runtime_packages()

        versions["numpy"] = "2.0.0"
        with mock.patch.object(engine.metadata, "version", side_effect=lambda name: versions[name]):
            with self.assertRaisesRegex(engine.EngineError, "unexpected numpy version"):
                runner.validate_runtime_packages()

    def test_model_assets_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo = Path(tmp)
            checkpoint = repo / engine.MODEL_FILE
            checkpoint.write_bytes(b"wrong")
            (repo / engine.MODEL_BAG_FILE).write_text(engine.MODEL_BAG_CONTENT, encoding="utf-8")
            runner = engine.DemucsPyTorchRunner(engine.DemucsEngineConfig(model_repo=repo))
            with self.assertRaisesRegex(engine.EngineError, "size mismatch"):
                runner.validate_assets()

    def test_model_assets_accept_exact_checkpoint_contract(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo = Path(tmp)
            checkpoint = repo / engine.MODEL_FILE
            checkpoint.write_bytes(b"x")
            (repo / engine.MODEL_BAG_FILE).write_text(engine.MODEL_BAG_CONTENT, encoding="utf-8")
            runner = engine.DemucsPyTorchRunner(engine.DemucsEngineConfig(model_repo=repo))
            with mock.patch.object(engine, "MODEL_BYTES", 1), \
                 mock.patch.object(engine, "MODEL_SHA256", hashlib.sha256(b"x").hexdigest()):
                runner.validate_assets()

    def test_run_requires_all_six_private_stems(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "input.wav"
            source.write_bytes(b"audio")
            repo = root / "models"
            repo.mkdir()
            checkpoint = repo / engine.MODEL_FILE
            checkpoint.write_bytes(b"x")
            (repo / engine.MODEL_BAG_FILE).write_text(engine.MODEL_BAG_CONTENT, encoding="utf-8")
            output = root / "out"
            track = output / engine.MODEL_NAME / source.stem
            track.mkdir(parents=True)
            for stem in engine.STEMS[:-1]:
                (track / f"{stem}.wav").write_bytes(b"wav")

            process = mock.Mock()
            process.communicate.return_value = ("ok", None)
            process.returncode = 0
            with mock.patch.object(engine, "MODEL_BYTES", 1), \
                 mock.patch.object(engine, "MODEL_SHA256", hashlib.sha256(b"x").hexdigest()), \
                 mock.patch.object(subprocess, "Popen", return_value=process):
                runner = engine.DemucsPyTorchRunner(engine.DemucsEngineConfig(model_repo=repo))
                with mock.patch.object(runner, "validate_runtime_packages"):
                    with self.assertRaisesRegex(engine.EngineError, "piano"):
                        runner.run(source, output)

    def test_cpu_thread_environment_is_explicit(self):
        runner = engine.DemucsPyTorchRunner(engine.DemucsEngineConfig(cpu_threads=4))
        env = runner.environment()
        self.assertEqual(env["OMP_NUM_THREADS"], "4")
        self.assertEqual(env["MKL_NUM_THREADS"], "4")
        self.assertEqual(env["OPENBLAS_NUM_THREADS"], "4")
        self.assertEqual(env["NUMEXPR_NUM_THREADS"], "4")

    def test_invalid_device_is_rejected(self):
        with self.assertRaisesRegex(engine.EngineError, "unsupported Demucs device"):
            engine.DemucsPyTorchRunner(engine.DemucsEngineConfig(device="auto"))

    def test_cancel_terminates_whole_process_group(self):
        process = mock.Mock()
        process.poll.return_value = None
        process.pid = 123
        runner = engine.DemucsPyTorchRunner(engine.DemucsEngineConfig())
        runner._process = process
        with mock.patch.object(os, "killpg") as killpg:
            runner.cancel()
        killpg.assert_called_once_with(123, engine.signal.SIGTERM)


if __name__ == "__main__":
    unittest.main()
