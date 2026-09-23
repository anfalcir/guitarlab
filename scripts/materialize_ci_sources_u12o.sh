#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u12n.sh"
PATCH_B64="$ROOT/.source-parts/U12oPrepareRetryCopyCompilation.patch.b64"
PATCH_B64_BLOB="1920d1ffb09a4cf0122dabfdd4d08faaa7bd4894"
PATCH_BLOB="a4c0b7ba4073239b30e95647ea70ec48ad1a215e"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_PATCH"' EXIT

FILE="app/src/main/java/studio/guitarlab/app/ui/PrepareJourneyPolicy.kt"
HASH="fbebfb6084784699c7f993ea456a33e214cb832b"

ready() {
  [[ -f "$ROOT/$FILE" ]] &&
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$FILE")" == "$HASH" ]]
}

verify_semantics() {
  git -C "$ROOT" diff --check
  grep -q 'versionName = "0.5.0-rc10"' "$ROOT/app/build.gradle.kts"
  grep -q 'val errorCode = job.errorCode' "$ROOT/$FILE"
  grep -q 'errorCode.contains(":UPLOADING:")' "$ROOT/$FILE"
}

[[ -f "$PATCH_B64" ]] || { echo "Missing U12o Prepare retry-copy compilation payload" >&2; exit 1; }
[[ "$(git -C "$ROOT" hash-object "$PATCH_B64")" == "$PATCH_B64_BLOB" ]] || {
  echo "U12o base64 payload blob mismatch" >&2
  exit 1
}
base64 -d "$PATCH_B64" > "$TMP_PATCH"
[[ "$(git -C "$ROOT" hash-object "$TMP_PATCH")" == "$PATCH_BLOB" ]] || {
  echo "U12o decoded patch blob mismatch" >&2
  exit 1
}

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
  echo "Source patch chain already materialized through U12o Prepare retry-copy compilation fix"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U12n materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12o final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$TMP_PATCH"
echo "Source patch chain materialized through U12o Prepare retry-copy compilation fix"
