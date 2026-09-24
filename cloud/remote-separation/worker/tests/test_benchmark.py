import importlib.util
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).parents[1]
sys.path.insert(0, str(ROOT))
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
    def test_cpu_baseline_is_official_phase_w_contract(self):
        selected = benchmark.strategy("cpu8_s1_o05")
        self.assertEqual(selected["device"], "cpu")
        self.assertEqual(selected["cpu_threads"], 8)
        self.assertEqual(selected["shifts"], 1)
        self.assertEqual(selected["overlap"], 0.5)

    def test_unknown_strategy_is_rejected(self):
        with self.assertRaises(ValueError):
            benchmark.strategy("single8")


if __name__ == "__main__":
    unittest.main()
