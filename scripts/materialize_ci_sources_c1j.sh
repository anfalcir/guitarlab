#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1i.sh"
PATCH_B64="$ROOT/.source-parts/C1jHomeExportFilteredVisibility.patch.gz.b64"
PATCH_B64_SHA256="95d667605d1aeb3b93e7a4e305a6fe9d8eea66c0b3a92625dca0170080f23c2c"
PATCH_GZ_SHA256="e2d6869b226491f0eb89d73c8354e17ca4978aabf2954c9e87b727e5035e7256"
PATCH_SHA256="e8a28f3ed35fdcd251427e838916f534844d44d790a35eba9455354ae768d6dc"

TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="f7da808b7589f5015578511fb0da03f8ddbc0022"
POST_HASH="6e80bdce709ba15009f6bd033c86b90bd1955caa"

hash_target() { git -C "$ROOT" hash-object "$TARGET"; }

if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C1j"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1i materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1j patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || {
  echo "C1j refuses to patch: lifecycle test is not the exact C1i blob" >&2
  exit 1
}

echo "$PATCH_B64_SHA256  $PATCH_B64" | sha256sum -c -
TMP_GZ="$(mktemp)"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_GZ" "$TMP_PATCH"' EXIT
base64 --decode "$PATCH_B64" > "$TMP_GZ"
echo "$PATCH_GZ_SHA256  $TMP_GZ" | sha256sum -c -
gzip -dc "$TMP_GZ" > "$TMP_PATCH"
echo "$PATCH_SHA256  $TMP_PATCH" | sha256sum -c -
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check

[[ "$(hash_target)" == "$POST_HASH" ]] || {
  echo "C1j final lifecycle test blob mismatch" >&2
  exit 1
}

grep -q 'home().updateProjectSearch(project.name)' "$TARGET" || {
  echo "C1j deterministic project filter missing" >&2
  exit 1
}
if grep -q 'performScrollToNode(hasContentDescription(menuDescription))' "$TARGET"; then
  echo "C1j found obsolete semantic LazyColumn scroll" >&2
  exit 1
fi

echo "Source patch chain materialized through C1j with exact blob verification"
