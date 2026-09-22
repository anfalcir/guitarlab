#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8f.sh"
PATCH="$ROOT/.source-parts/U8gUnifiedActivityDomain.patch"
PATCH_SHA256="2405818b632b87556bc93fd343c791ed16eee188d2c7a9454dd37d72790ccde4"
DOMAIN="core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedOperationDomain.kt"
TEST="core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedOperationDomainTest.kt"
DOMAIN_HASH="244e800cde3f52e893b267d16668d751f8e9e3aa"
TEST_HASH="564d387aa39cf5a6b0aea28ecbbb7fce5c8d5929"

ready() {
  [[ -f "$ROOT/$DOMAIN" && -f "$ROOT/$TEST" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$DOMAIN")" == "$DOMAIN_HASH" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TEST")" == "$TEST_HASH" ]]
}

if ready; then
  echo "Source patch chain already materialized through U8g"
  exit 0
fi
[[ -f "$PREVIOUS" ]] || { echo "Missing U8f materializer" >&2; exit 1; }
[[ -f "$PATCH" ]] || { echo "Missing U8g patch payload" >&2; exit 1; }
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
bash "$PREVIOUS"
[[ ! -e "$ROOT/$DOMAIN" && ! -e "$ROOT/$TEST" ]] || { echo "U8g refuses to overwrite unexpected source files" >&2; exit 1; }
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U8g final blob mismatch" >&2; exit 1; }
echo "Source patch chain materialized through U8g with exact blob verification"
