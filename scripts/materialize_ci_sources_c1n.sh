#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1m.sh"
PATCH_B64="$ROOT/.source-parts/C1nHomeResponsiveHero.patch.gz.b64"
PATCH_B64_SHA256="dbaad815744368b05967be2b5ba8b94c08f89dd593fc47c4836cfbc250346c3e"
PATCH_GZ_SHA256="8e52aa67413126f56a05885a799a7a82846f68e41d8aad7b50d4dfacde27be2d"
PATCH_SHA256="b1df2faa1eceb46f71800d7f88964019e5968d70666fa2a6580be2d3686d1638"

TARGET="$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt"
PRE_HASH="9f35766ce34670388fe03b3800a6fb8bddea5a72"
POST_HASH="1d5ec3a310aff7c35763355a9555427d7ebab858"

hash_target() { git -C "$ROOT" hash-object "$TARGET"; }

if [[ -f "$TARGET" ]] && [[ "$(hash_target)" == "$POST_HASH" ]]; then
  echo "Source patch chain already materialized through C1n"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1m materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1n patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
[[ "$(hash_target)" == "$PRE_HASH" ]] || { echo "C1n refuses to patch: HomeScreen is not the exact C1m blob" >&2; exit 1; }

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

[[ "$(hash_target)" == "$POST_HASH" ]] || { echo "C1n final HomeScreen blob mismatch" >&2; exit 1; }
grep -q '^import androidx.compose.foundation.layout.BoxWithConstraints$' "$TARGET" || { echo "C1n responsive container import missing" >&2; exit 1; }
grep -q 'val compactHome = maxWidth < 600.dp' "$TARGET" || { echo "C1n compact Home breakpoint missing" >&2; exit 1; }
grep -q 'if (compactHome)' "$TARGET" || { echo "C1n compact Home branch missing" >&2; exit 1; }

echo "Source patch chain materialized through C1n with exact blob verification"
