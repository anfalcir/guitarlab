#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u6.sh"
PATCH_B64="$ROOT/.source-parts/U6aNewExportTests.patch.gz.b64"
PATCH_B64_SHA256="693df4bf5374f496b4d50f8843ca282cb430ab351b540b659a550e018fabc00a"
PATCH_GZ_SHA256="5c98006e62cd04dff9ae8159ebce4a15ed7447f5e6325f4504a00ceadc3ca168"
PATCH_SHA256="7b1c85cd499d28817b50aa5c3ca44026069f7c0bded1c88b0001de6090cf5546"

CHECKS=(
  "app/src/test/java/studio/guitarlab/app/ui/ExportStoragePolicyTest.kt|fcfbe13185693c7116be20c23bf006b947f05839"
  "app/src/androidTest/java/studio/guitarlab/app/StudyExportInstrumentedTest.kt|4599b854fdbdd0915c1ccff3770bfabfcb6fa332"
)

hash_file() { git -C "$ROOT" hash-object "$1"; }

u6a_ready() {
  local entry relative expected
  for entry in "${CHECKS[@]}"; do
    relative="${entry%%|*}"
    expected="${entry#*|}"
    [[ -f "$ROOT/$relative" ]] || return 1
    [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
  done
}

if u6a_ready; then
  echo "Source patch chain already materialized through U6a"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U6 materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing U6a test payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"

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

u6a_ready || {
  echo "U6a applied but final hashes do not match" >&2
  exit 1
}
echo "Source patch chain materialized through U6a with verified final hashes"
