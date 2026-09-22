#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8k.sh"
PAYLOAD="$ROOT/.source-parts/U8lNetworkFaultGate.patch.b64"
PAYLOAD_SHA256="63102c22b674c071d70aa0870a7d80dcc5ed7b16ef696a768c40833624e8f7bb"
PATCH_SHA256="dada4d9fdf65aa681aede7ae4e0accafaae16ddcbcafe9bca861cfda6594d886"

declare -a FILES=(
  "app/src/main/java/studio/guitarlab/app/backup/DriveV3Protocol.kt"
  "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveProductionAccess.kt"
  "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveV3RemoteStore.kt"
  "app/src/test/java/studio/guitarlab/app/backup/DriveV3NetworkFaultTest.kt"
  "app/src/test/java/studio/guitarlab/app/backup/DriveV3ProtocolTest.kt"
  "app/src/test/java/studio/guitarlab/app/backup/UnifiedDriveV3RemoteStoreTest.kt"
)

declare -a HASHES=(
  "d5c0188e9cb901e4c53b77b25ab05ee4742fa960"
  "e450220a850d60ac9a45f8a3542e2598b554c999"
  "252421a2a090711724a1c7180640b59220a902fd"
  "5a985554d71819d68f55a51bde7bdc28a4903ddc"
  "b142bb391dde9d452be0db4bd46c10fc2998fa59"
  "4f9bca498f3942071be20c45a229b192dacedbbb"
)

ready() {
  for i in "${!FILES[@]}"; do
    local file="${FILES[$i]}"
    [[ -f "$ROOT/$file" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$file")" == "${HASHES[$i]}" ]] || return 1
  done
}

decode_patch() {
  [[ -f "$PAYLOAD" ]] || { echo "Missing U8l base64 payload" >&2; exit 1; }
  echo "$PAYLOAD_SHA256  $PAYLOAD" | sha256sum -c -
  PATCH_TMP="$(mktemp)"
  trap 'rm -f "$PATCH_TMP"' EXIT
  base64 --decode "$PAYLOAD" > "$PATCH_TMP"
  echo "$PATCH_SHA256  $PATCH_TMP" | sha256sum -c -
}

if ready; then
  decode_patch
  git -C "$ROOT" apply --check --reverse "$PATCH_TMP"
  echo "Source patch chain already materialized through U8l with payload/reverse verification"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U8k materializer" >&2; exit 1; }
decode_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH_TMP"
git -C "$ROOT" apply "$PATCH_TMP"
git -C "$ROOT" diff --check
ready || { echo "U8l final blob mismatch" >&2; exit 1; }
git -C "$ROOT" apply --check --reverse "$PATCH_TMP"
echo "Source patch chain materialized through U8l with exact blob/reverse verification"
