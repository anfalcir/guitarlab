import importlib.util
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).parents[1]
WORKER = ROOT / "gbw_worker.py"
BENCH = ROOT / "benchmark.py"

worker_spec = importlib.util.spec_from_file_location("gbw_worker", WORKER)
worker = importlib.util.module_from_spec(worker_spec)
sys.modules[worker_spec.name] = worker
worker_spec.loader.exec_module(worker)

spec = importlib.util.spec_from_file_location("gbw_benchmark", BENCH)
benchmark = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = benchmark
spec.loader.exec_module(benchmark)


class BenchmarkContractTest(unittest.TestCase):
    def test_strategy_matrix_uses_exactly_eight_cpu_threads(self):
        single = benchmark.strategy("single8")
        self.assertEqual((single["mt_threads"], single["blas_threads"]), (0, 8))

    def test_strategy_matrix_uses_expected_binaries(self):
        self.assertTrue(benchmark.strategy("single8")["binary"].endswith("demucs.cpp.main"))

    def test_unknown_strategy_is_rejected(self):
        with self.assertRaises(ValueError):
            benchmark.strategy("oversubscribed")


if __name__ == "__main__":
    unittest.main()
