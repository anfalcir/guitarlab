#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1l.sh"
PATCH_B64="$ROOT/.source-parts/C1mHomeExportVisibilityWait.patch.gz.b64"
PATCH_B64_SHA256="66b8d62a395eb2d1400197c4ce019d8bacb1517bc718122f9e3a11ca65cae95f"
PATCH_GZ_SHA256="338012c9a9c317ffff05684de91a6bd37e1fccbc60700d7e5587e575be785b3b"
PATCH_SHA256="a419fe544f7f190fbfb5f187c5eb88f9a3c2776045047f17215af3b66936ce40"

TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="6e80bdce709ba15009f6bd033c86b90bd1955caa"
POST_HASH="9ae0aa1bccee7978022e7ba831f40d65f46b0d94"

hash_target() { git -C "$ROOT" hash-object "$TARGET"; }

if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C1m"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1l materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1m patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || { echo "C1m refuses to patch: lifecycle test is not the exact C1l blob" >&2; exit 1; }

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

[[ "$(hash_target)" == "$POST_HASH" ]] || { echo "C1m final lifecycle test blob mismatch" >&2; exit 1; }
grep -q 'runCatching { composeRule.onNodeWithContentDescription(menuDescription).assertIsDisplayed() }.isSuccess' "$TARGET" || {
  echo "C1m visible Home menu wait missing" >&2
  exit 1
}
if ! sed -n '/runCatching { composeRule.onNodeWithContentDescription(menuDescription).assertIsDisplayed() }.isSuccess/,+5p' "$TARGET" | grep -q 'assertIsDisplayed'; then
  echo "C1m final Home menu visibility assertion missing" >&2
  exit 1
fi

echo "Source patch chain materialized through C1m with exact blob verification"
