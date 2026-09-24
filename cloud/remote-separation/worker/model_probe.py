#!/usr/bin/env python3
from __future__ import annotations
import json
import os
import time
from pathlib import Path

MODEL_NAME = "htdemucs_6s"
EXPECTED_SOURCES = ("drums", "bass", "other", "vocals", "guitar", "piano")

def main() -> int:
    project = os.environ["GBW_PROBE_PROJECT"]
    bucket_name = os.environ["GBW_PROBE_BUCKET"]
    output_path = os.environ["GBW_PROBE_OUTPUT_PATH"]
    model_repo = Path(os.environ.get("GBW_MODEL_REPO", "/opt/demucs/models"))
    total_started = time.monotonic()
    import_started = time.monotonic()
    from demucs.pretrained import get_model
    import_ms = round((time.monotonic() - import_started) * 1000)
    load_started = time.monotonic()
    model = get_model(MODEL_NAME, model_repo)
    model_load_ms = round((time.monotonic() - load_started) * 1000)
    if tuple(model.sources) != EXPECTED_SOURCES:
        raise RuntimeError(f"unexpected model sources: {tuple(model.sources)}")
    result = {
        "model": MODEL_NAME,
        "modelRepo": str(model_repo),
        "importMs": import_ms,
        "modelLoadMs": model_load_ms,
        "totalProbeMs": round((time.monotonic() - total_started) * 1000),
        "sources": list(model.sources),
    }
    from google.cloud import storage
    storage.Client(project=project).bucket(bucket_name).blob(output_path).upload_from_string(
        json.dumps(result, sort_keys=True, indent=2) + "\n",
        content_type="application/json",
    )
    print(json.dumps(result, sort_keys=True))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
