import importlib.util
from pathlib import Path
import unittest


MODULE_PATH = Path(__file__).parents[1] / "u12_readonly_audio_forensics.py"
SPEC = importlib.util.spec_from_file_location("u12_readonly_audio_forensics", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class SanitizationTest(unittest.TestCase):
    def test_redacts_sensitive_keys_headers_and_emails(self):
        sanitized = MODULE.sanitize(
            {
                "accessToken": "opaque",
                "nested": {
                    "header": "Authorization: Bearer abcdefghijklmnopqrstuvwxyz",
                    "creator": "worker@example.com",
                    "jobId": "job-1",
                },
            }
        )

        self.assertEqual("[REDACTED]", sanitized["accessToken"])
        self.assertNotIn("abcdefghijklmnopqrstuvwxyz", sanitized["nested"]["header"])
        self.assertEqual("[REDACTED_EMAIL]", sanitized["nested"]["creator"])
        self.assertEqual("job-1", sanitized["nested"]["jobId"])


if __name__ == "__main__":
    unittest.main()
