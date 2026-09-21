#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1b.sh"
PATCH_B64="$ROOT/.source-parts/C1cScrollImport.patch.gz.b64"
PATCH_B64_SHA256="4c4e00de33fcc0dea79393e0d59c3dc30eb37420f19d05a9a861b384f36b20ce"
PATCH_GZ_SHA256="5d88539a07a779d446c7278abd37d580c16627355961e8fe53fd00efb66eb53d"
PATCH_SHA256="c0e6e4d4876eb019fe21073a2702f4420a432192ec0dbb934acf52458ff98123"
TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="04b864b289d1302f1481420508b6dd9fb87f1c73"
POST_HASH="4eacdfb56270bdf44d55a81bea6958aa13527a2b"

hash_target() { git -C "$ROOT" hash-object "$TARGET"; }

if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C1c"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1b materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1c patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || {
  echo "C1c refuses to patch: lifecycle test is not the exact C1b blob" >&2
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
  echo "C1c final lifecycle test blob mismatch" >&2
  exit 1
}
grep -q '^import androidx.compose.ui.test.performScrollTo$' "$TARGET" || {
  echo "C1c performScrollTo import missing" >&2
  exit 1
}

echo "Source patch chain materialized through C1c with exact blob verification"
