#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8k.sh"
PAYLOAD="$ROOT/.source-parts/U8lNetworkFaultGate.patch.b64"
PAYLOAD_SHA256="daac2fa9e2c81ad451fcc6bbf3db208e61d26292e74905c7c1b3d2aa8d964afb"
PATCH_SHA256="dfbe639c886383449320ca24584cc4bd9fe34564adaabc2436e36970cacb03e3"

declare -a FILES=(
  "app/src/main/java/studio/guitarlab/app/backup/AutomaticBackupWorker.kt"
  "app/src/main/java/studio/guitarlab/app/backup/DriveV3Protocol.kt"
  "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveProductionAccess.kt"
  "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveProductionService.kt"
  "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveV3RemoteStore.kt"
  "app/src/test/java/studio/guitarlab/app/backup/DriveV3NetworkFaultTest.kt"
  "app/src/test/java/studio/guitarlab/app/backup/DriveV3ProtocolTest.kt"
  "app/src/test/java/studio/guitarlab/app/backup/UnifiedDriveV3RemoteStoreTest.kt"
)

declare -a HASHES=(
  "a3b63b708271a0c929941325ce936dc84e01673d"
  "5e3e1c66d8a8d028ac922624fad163a26cf6a85a"
  "e450220a850d60ac9a45f8a3542e2598b554c999"
  "dfac08b30e9025100472d57196263976ee175755"
  "252421a2a090711724a1c7180640b59220a902fd"
  "5a985554d71819d68f55a51bde7bdc28a4903ddc"
  "3e75b4a2c0d883dd849668f6b91addff68b68b0f"
  "6935a349b7c5e47dc9e1256fcb777586a3a85995"
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
