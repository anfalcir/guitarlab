#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c4b.sh"
PAYLOAD="$ROOT/.source-parts/C4cGuideScrollTest.patch.gz.b64"
PAYLOAD_SHA256="ec02331fc835754745776766ee559f32123f83c1d2ee1abb13642ef48ad12555"
GZ_SHA256="d46e67985542fa6e2b5260c557b2215b048a7b1e1e713b752373d1f085b7c4a9"
PATCH_SHA256="e962000ca60d42aa3e129441a1635e62653c7007555c4b9347fb1432c131c0e3"
TARGET="app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="1806c8e4f80178794172c040f05313954fb8a0c7"
POST_HASH="9905c8283872ede0f195284b5f3017bd1953ab99"

hash_target() { git -C "$ROOT" hash-object "$ROOT/$TARGET"; }
if [[ -f "$ROOT/$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C4c"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C4b materializer" >&2; exit 1; }
[[ -f "$PAYLOAD" ]] || { echo "Missing C4c patch payload" >&2; exit 1; }
echo "$PAYLOAD_SHA256  $PAYLOAD" | sha256sum -c -
bash "$PREVIOUS"
[[ -f "$ROOT/$TARGET" ]] || { echo "C4c refuses to patch: missing target" >&2; exit 1; }
[[ "$(hash_target)" == "$PRE_HASH" ]] || { echo "C4c refuses to patch: pre-blob mismatch" >&2; exit 1; }

TMP_GZ="$(mktemp)"; TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_GZ" "$TMP_PATCH"' EXIT
base64 --decode "$PAYLOAD" > "$TMP_GZ"
echo "$GZ_SHA256  $TMP_GZ" | sha256sum -c -
gzip -dc "$TMP_GZ" > "$TMP_PATCH"
echo "$PATCH_SHA256  $TMP_PATCH" | sha256sum -c -
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
[[ "$(hash_target)" == "$POST_HASH" ]] || { echo "C4c final blob mismatch" >&2; exit 1; }
grep -q 'performScrollTo().assertIsDisplayed()' "$ROOT/$TARGET" || { echo "C4c guide scroll assertion missing" >&2; exit 1; }
echo "Source patch chain materialized through C4c with exact blob verification"
