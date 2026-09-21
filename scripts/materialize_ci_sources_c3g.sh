#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c3f.sh"
PATCH_B64="$ROOT/.source-parts/C3gEditableTextAssertion.patch.gz.b64"
PATCH_B64_SHA256="9589e16e98dd7695b6b867c4076da3ed756818ef8c9fb7462ab9b74aaf9da3c7"
PATCH_GZ_SHA256="f6f168ee3bff734df570f18112c0ff2b15318372bba1cabda20bcc634ee4d98c"
PATCH_SHA256="690e90220a0d820de0ec2a82562cb1e61991db590832841cc70f8c769fb8fe19"
TARGET="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
PRE_HASH="94d7bc997fc785e9fa21a13c83011809fa385083"
POST_HASH="1806c8e4f80178794172c040f05313954fb8a0c7"
hash_target() { git -C "$ROOT" hash-object "$TARGET"; }
if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C3g"
  exit 0
fi
[[ -f "$PREVIOUS" ]] || { echo "Missing C3f materializer" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C3g patch payload" >&2; exit 1; }
bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || { echo "C3g refuses to patch: lifecycle test blob mismatch" >&2; exit 1; }
echo "$PATCH_B64_SHA256  $PATCH_B64" | sha256sum -c -
TMP_GZ="$(mktemp)"; TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_GZ" "$TMP_PATCH"' EXIT
base64 --decode "$PATCH_B64" > "$TMP_GZ"
echo "$PATCH_GZ_SHA256  $TMP_GZ" | sha256sum -c -
gzip -dc "$TMP_GZ" > "$TMP_PATCH"
echo "$PATCH_SHA256  $TMP_PATCH" | sha256sum -c -
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
[[ "$(hash_target)" == "$POST_HASH" ]] || { echo "C3g final lifecycle test blob mismatch" >&2; exit 1; }
grep -q 'assertTextContains(projectName)' "$TARGET" || { echo "C3g editable-text assertion missing" >&2; exit 1; }
! grep -q 'assertTextEquals(projectName)' "$TARGET" || { echo "C3g strict text assertion remains" >&2; exit 1; }
echo "Source patch chain materialized through C3g with exact blob verification"
