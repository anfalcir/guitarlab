#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c4.sh"
PAYLOAD="$ROOT/.source-parts/C4bDiagnosticsJobIdentity.patch.gz.b64"
PAYLOAD_SHA256="0108bfc17dce8b2991e7c6fe41f000cf932b9870533e061357dc2eabb42aa957"
GZ_SHA256="c152996b91f2223ac4d7d5cec54ce8b14828d8db76d85f43ba0ced88f569244a"
PATCH_SHA256="ee09553733309f451f69a7928e1ce241e4e771740c351495d85f66ca388f62b9"
TARGET="app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt"
PRE_HASH="ba79da3430acbdec72818ed609fc1b88b5dd6d35"
POST_HASH="8ddf049f133c0c201f4751d7905c0271ccacebf2"

hash_target() { git -C "$ROOT" hash-object "$ROOT/$TARGET"; }
if [[ -f "$ROOT/$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C4b"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C4 materializer" >&2; exit 1; }
[[ -f "$PAYLOAD" ]] || { echo "Missing C4b patch payload" >&2; exit 1; }
echo "$PAYLOAD_SHA256  $PAYLOAD" | sha256sum -c -
bash "$PREVIOUS"
[[ -f "$ROOT/$TARGET" ]] || { echo "C4b refuses to patch: missing target" >&2; exit 1; }
[[ "$(hash_target)" == "$PRE_HASH" ]] || { echo "C4b refuses to patch: pre-blob mismatch" >&2; exit 1; }

TMP_GZ="$(mktemp)"; TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_GZ" "$TMP_PATCH"' EXIT
base64 --decode "$PAYLOAD" > "$TMP_GZ"
echo "$GZ_SHA256  $TMP_GZ" | sha256sum -c -
gzip -dc "$TMP_GZ" > "$TMP_PATCH"
echo "$PATCH_SHA256  $TMP_PATCH" | sha256sum -c -
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
[[ "$(hash_target)" == "$POST_HASH" ]] || { echo "C4b final blob mismatch" >&2; exit 1; }
grep -q 'it.identity.jobId' "$ROOT/$TARGET" || { echo "C4b durable job diagnostics contract missing" >&2; exit 1; }
echo "Source patch chain materialized through C4b with exact blob verification"
