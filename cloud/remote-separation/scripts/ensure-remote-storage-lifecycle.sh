#!/usr/bin/env bash
set -euo pipefail

: "${GBW_BUCKET:?Set GBW_BUCKET}"

current="$(mktemp)"
merged="$(mktemp)"
verified="$(mktemp)"
trap 'rm -f "$current" "$merged" "$verified"' EXIT

token="$(gcloud auth print-access-token)"
curl -fsS   -H "Authorization: Bearer $token"   "https://storage.googleapis.com/storage/v1/b/$GBW_BUCKET?fields=lifecycle"   > "$current"

python3 - "$current" "$merged" <<'PY'
import json, sys
current_path, merged_path = sys.argv[1:]
with open(current_path, encoding="utf-8") as f:
    current = json.load(f)
rules = current.get("lifecycle", {}).get("rule", [])
remote_prefix = "remote/v1/users/"
preserved = []
for rule in rules:
    prefixes = rule.get("condition", {}).get("matchesPrefix", [])
    if remote_prefix in prefixes:
        continue
    preserved.append(rule)
preserved.append({
    "action": {"type": "Delete"},
    "condition": {"age": 3, "matchesPrefix": [remote_prefix]},
})
with open(merged_path, "w", encoding="utf-8") as f:
    json.dump({"rule": preserved}, f, separators=(",", ":"))
PY

gcloud storage buckets update "gs://$GBW_BUCKET" --lifecycle-file="$merged" >/dev/null

token="$(gcloud auth print-access-token)"
curl -fsS   -H "Authorization: Bearer $token"   "https://storage.googleapis.com/storage/v1/b/$GBW_BUCKET?fields=lifecycle"   > "$verified"

python3 - "$verified" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as f:
    data = json.load(f)
rules = data.get("lifecycle", {}).get("rule", [])
matches = []
for rule in rules:
    action = rule.get("action", {})
    condition = rule.get("condition", {})
    prefixes = condition.get("matchesPrefix", [])
    if "remote/v1/users/" in prefixes:
        matches.append(rule)
assert len(matches) == 1, f"expected exactly one remote lifecycle rule: {matches!r}"
rule = matches[0]
assert rule.get("action", {}).get("type") == "Delete", rule
assert int(rule.get("condition", {}).get("age", -1)) == 3, rule
print("remote_storage_lifecycle=PASS age_days=3 prefix=remote/v1/users/")
PY
