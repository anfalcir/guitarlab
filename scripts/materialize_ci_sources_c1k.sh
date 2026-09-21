#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1j.sh"
PATCH_B64="$ROOT/.source-parts/C1kHomeExportSemanticClick.patch.gz.b64"
PATCH_B64_SHA256="977af8a57b38ed24e1fc5748b6613e8bac787e068485164c071bbe33da257718"
PATCH_GZ_SHA256="b9cbaeb9751f1045c5e864da1d5d032cd1c492a18bf3bc05951c7d152a60c9ee"
PATCH_SHA256="7a98f99f239a9fd6959975e30b8d7309f39776d958ae4ac9804d9f156c61f3e9"

TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="6e80bdce709ba15009f6bd033c86b90bd1955caa"
POST_HASH="14dae58c71330959e13189f1a2aec13306c12af2"

hash_target() { git -C "$ROOT" hash-object "$TARGET"; }

if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C1k"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1j materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1k patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || { echo "C1k refuses to patch: lifecycle test is not the exact C1j blob" >&2; exit 1; }

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

[[ "$(hash_target)" == "$POST_HASH" ]] || { echo "C1k final lifecycle test blob mismatch" >&2; exit 1; }
grep -q 'onNodeWithContentDescription(menuDescription)' "$TARGET" || { echo "C1k menu semantic node missing" >&2; exit 1; }
if sed -n '/onNodeWithContentDescription(menuDescription)/,+2p' "$TARGET" | grep -q 'assertIsDisplayed'; then
  echo "C1k found obsolete geometry assertion on routing action" >&2
  exit 1
fi

echo "Source patch chain materialized through C1k with exact blob verification"
