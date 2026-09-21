#!/usr/bin/env python3
import hashlib
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
INVENTORY = ROOT / "integration/u0/inventory.json"
MANIFEST = ROOT / "integration/u0/fixtures/manifest.json"


def load_json(path: pathlib.Path):
    with path.open("r", encoding="utf-8") as stream:
        return json.load(stream)


def fail(message: str):
    raise SystemExit(f"U0 inventory verification failed: {message}")


inventory = load_json(INVENTORY)
if inventory.get("schemaVersion") != 1 or inventory.get("milestone") != "U0":
    fail("unsupported inventory identity")

capabilities = inventory.get("capabilities", [])
ids = [item.get("id") for item in capabilities]
if len(ids) != len(set(ids)) or any(not value for value in ids):
    fail("capability IDs must be present and unique")
for item in capabilities:
    for field in ("sourceOwner", "sourcePaths", "targetOwner", "strategy", "targetMilestone"):
        if not item.get(field):
            fail(f"capability {item['id']} lacks {field}")

manifest = load_json(MANIFEST)
for fixture in manifest.get("fixtures", []):
    path = ROOT / fixture["path"]
    if not path.is_file():
        fail(f"missing fixture {fixture['path']}")
    actual = hashlib.sha256(path.read_bytes()).hexdigest()
    if actual != fixture["sha256"]:
        fail(f"hash mismatch for {fixture['path']}: {actual}")
    if fixture["kind"] == "project-json":
        load_json(path)

print(f"U0 inventory PASS: {len(capabilities)} capabilities, {len(manifest['fixtures'])} fixtures")
