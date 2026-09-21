#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c2a.sh"
PATCH_B64="$ROOT/.source-parts/C2bComposeWeightImport.patch.gz.b64"
PATCH_B64_SHA256="346c9686a8020c3982531e25cf46fcc9463b6c3b7a3082934e690654541dba0a"
PATCH_GZ_SHA256="ad800420827583b1058e4b73ad300d8c562cb6d5fa05883721f32904a4a1c3ac"
PATCH_SHA256="2da50eeb86e87583cb7f82e8566b812a85bd12d5431f17ff18b1f76bf10e2cdf"

TARGET="$ROOT/app/src/main/java/studio/guitarlab/app/ui/ProjectShellScaffold.kt"
PRE_HASH="0840cdd4b2729907b2df335011914c550b955edf"
POST_HASH="9cdbfe75a949a5debfb71fb7d36bf1e2b47f8593"

hash_target() { git -C "$ROOT" hash-object "$TARGET"; }

if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C2b"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C2a materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C2b patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || { echo "C2b refuses to patch: ProjectShellScaffold is not the exact C2a blob" >&2; exit 1; }

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

[[ "$(hash_target)" == "$POST_HASH" ]] || { echo "C2b final ProjectShellScaffold blob mismatch" >&2; exit 1; }
if grep -q '^import androidx.compose.foundation.layout.weight$' "$TARGET"; then
  echo "C2b obsolete internal weight import still present" >&2
  exit 1
fi

echo "Source patch chain materialized through C2b with exact blob verification"
