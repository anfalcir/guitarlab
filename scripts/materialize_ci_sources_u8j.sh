#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8i.sh"
PAYLOAD="$ROOT/.source-parts/U8jPathAwareProjectSnapshots.patch.b64"
PAYLOAD_SHA256="d94fa6e07f009681fd0479e0d5a5c513b86f1960e98ba14fae35019411286fc0"
PATCH_SHA256="9cc1549a299bd9cb63b105c0a9e8e6583558c24df94303edae003dbd2e396079"

declare -a FILES=(
  "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveV3RemoteStore.kt"
  "app/src/test/java/studio/guitarlab/app/backup/UnifiedDriveV3RemoteStoreTest.kt"
  "core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedDriveBackupDomain.kt"
  "core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedDriveProjectSnapshot.kt"
  "core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedDriveRestoreCoordinator.kt"
  "core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedDriveBackupDomainTest.kt"
  "core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedDriveProjectSnapshotTest.kt"
)

declare -a HASHES=(
  "59fe762281a4d267b3fb8d5f1d4b7d64858981a6"
  "3798da36020a9bb73bad2fc9b8126d13cdd41778"
  "2200336f5fab562e4beb77a2783f447c03897654"
  "df6edb8137303304b680a01c6443c23c389bf9f0"
  "c8987b270c5e2e9ece3287c8c32ce5e6d491b885"
  "e728d060b6091a5f9067723b03c70a66366a7a86"
  "38db0846a29bf759012b772263eb59726e728718"
)

ready() {
  for i in "${!FILES[@]}"; do
    local file="${FILES[$i]}"
    [[ -f "$ROOT/$file" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$file")" == "${HASHES[$i]}" ]] || return 1
  done
}

decode_patch() {
  [[ -f "$PAYLOAD" ]] || { echo "Missing U8j base64 payload" >&2; exit 1; }
  echo "$PAYLOAD_SHA256  $PAYLOAD" | sha256sum -c -
  PATCH_TMP="$(mktemp)"
  trap 'rm -f "$PATCH_TMP"' EXIT
  base64 --decode "$PAYLOAD" > "$PATCH_TMP"
  echo "$PATCH_SHA256  $PATCH_TMP" | sha256sum -c -
}

if ready; then
  decode_patch
  git -C "$ROOT" apply --check --reverse "$PATCH_TMP"
  echo "Source patch chain already materialized through U8j with payload/reverse verification"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U8i materializer" >&2; exit 1; }
decode_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH_TMP"
git -C "$ROOT" apply "$PATCH_TMP"
git -C "$ROOT" diff --check
ready || { echo "U8j final blob mismatch" >&2; exit 1; }
echo "Source patch chain materialized through U8j with exact blob verification"
