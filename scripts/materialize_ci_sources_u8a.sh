#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c4c.sh"
PATCH="$ROOT/.source-parts/U8aUnifiedDriveDomain.patch"
PATCH_SHA256="5cc998a90a373b72acd379db3a7009a6d565099f33c2f5b578149f7938276a3d"
DOMAIN="core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedDriveBackupDomain.kt"
TEST="core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedDriveBackupDomainTest.kt"
DOMAIN_HASH="097d12150a303c30cbb3fa71d7fc768de55c8260"
TEST_HASH="648bcdef51420b61ec5d41600f6f7cb652d6a965"

ready() {
  [[ -f "$ROOT/$DOMAIN" && -f "$ROOT/$TEST" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$DOMAIN")" == "$DOMAIN_HASH" ]] || return 1
  [[ "$(git -C "$ROOT" hash-object "$ROOT/$TEST")" == "$TEST_HASH" ]]
}

if ready; then
  echo "Source patch chain already materialized through U8a"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C4c materializer" >&2; exit 1; }
[[ -f "$PATCH" ]] || { echo "Missing U8a patch payload" >&2; exit 1; }
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
bash "$PREVIOUS"
[[ ! -e "$ROOT/$DOMAIN" && ! -e "$ROOT/$TEST" ]] || {
  echo "U8a refuses to overwrite unexpected source files" >&2
  exit 1
}
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U8a final blob mismatch" >&2; exit 1; }
echo "Source patch chain materialized through U8a with exact blob verification"
