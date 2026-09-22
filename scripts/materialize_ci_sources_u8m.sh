#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8l.sh"
PAYLOAD="$ROOT/.source-parts/U8mRealDriveGate.patch.b64"
PAYLOAD_SHA256="d1f18b0d4bfa3e994c87808293dbce101d54fd4aba3ab4ecd3a4e7f83c367a2d"
PATCH_SHA256="51a56dd12f73fe9686e9d41457bca366168a568c1ce7aa34ce8d845c5c9a52ab"

declare -a FILES=(
  "app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt"
  "app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/backup/DriveV3Protocol.kt"
  "app/src/main/java/studio/guitarlab/app/backup/U8mRealDriveAcceptance.kt"
  "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveProductionService.kt"
  "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveV3RemoteStore.kt"
  "app/src/test/java/studio/guitarlab/app/backup/U8mRealDriveAcceptanceTest.kt"
)

declare -a HASHES=(
  "7f6745634e2ffcb7b9df27ba88e54269845b8416"
  "739b4246554ae8d534ffc6571a36ec141b072398"
  "ed17e99189da2af11b259ed5489f65d6c50eea23"
  "2d5e5fc0b3f58049356c57a6ca5fd40207b01bf0"
  "d964b8ff60d501c77c0447cdc510eb60b06ca53d"
  "7b633232e9b453925f0d7cd27634a86d7f0190aa"
  "9556a83cd95e287d4afe9d67c12f3305b3dde005"
)

ready() {
  for i in "${!FILES[@]}"; do
    local file="${FILES[$i]}"
    [[ -f "$ROOT/$file" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$file")" == "${HASHES[$i]}" ]] || return 1
  done
}

decode_patch() {
  [[ -f "$PAYLOAD" ]] || { echo "Missing U8m base64 payload" >&2; exit 1; }
  echo "$PAYLOAD_SHA256  $PAYLOAD" | sha256sum -c -
  PATCH_TMP="$(mktemp)"
  trap 'rm -f "$PATCH_TMP"' EXIT
  base64 --decode "$PAYLOAD" > "$PATCH_TMP"
  echo "$PATCH_SHA256  $PATCH_TMP" | sha256sum -c -
}

if ready; then
  decode_patch
  git -C "$ROOT" apply --check --reverse "$PATCH_TMP"
  echo "Source patch chain already materialized through U8m with payload/reverse verification"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U8l materializer" >&2; exit 1; }
decode_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH_TMP"
git -C "$ROOT" apply "$PATCH_TMP"
git -C "$ROOT" diff --check
ready || { echo "U8m final blob mismatch" >&2; exit 1; }
git -C "$ROOT" apply --check --reverse "$PATCH_TMP"
echo "Source patch chain materialized through U8m with exact blob/reverse verification"
