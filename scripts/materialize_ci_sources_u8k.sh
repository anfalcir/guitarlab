#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8j.sh"
PAYLOAD="$ROOT/.source-parts/U8kProductionDriveCutover.patch.b64"
PAYLOAD_SHA256="f94e23546e04cd854792b271fa7fee8d216410eca05e55f94aaa3579eb7115d4"
PATCH_SHA256="dd68c00cb51bf79cbacdabf979c8c4111e3ac1b1b35708118ec75eadd10c4fdd"

declare -a FILES=(
  "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt"
  "app/src/main/java/studio/guitarlab/app/backup/AutomaticBackupWorker.kt"
  "app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt"
  "app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/backup/ConfirmedRevisionStore.kt"
  "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveProductionAccess.kt"
  "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveProductionService.kt"
  "app/src/main/java/studio/guitarlab/app/backup/UnifiedDriveV3RemoteStore.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  "app/src/test/java/studio/guitarlab/app/backup/UnifiedDriveV3RemoteStoreTest.kt"
  "core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedDriveProductionStorage.kt"
  "core/project/src/main/kotlin/studio/guitarlab/core/project/UnifiedDriveTransactionJournal.kt"
  "core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedDriveProductionStorageTest.kt"
  "core/project/src/test/kotlin/studio/guitarlab/core/project/UnifiedDriveTransactionJournalTest.kt"
)

declare -a HASHES=(
  "45d6a57a076d476dc91da63cccfe3ba2fba59be3"
  "7912734b828932627ba917c18251d3c266c2c99f"
  "4f9dc7724dfb143cf4ec7e24cd63a8b14006770a"
  "dc829143be2ffa2abb5ca5c39d2dafb979a92af2"
  "0ba93b5ffecd7bda0990fb8fab24f61208697d3f"
  "8b4edc8f1726bb66a3e8e2561da377923bf87d50"
  "13ac8c39d5928bab79e89a2a9ad449128cee055a"
  "eb0fe74995ff2825bd664d689b44e7ada734cd1f"
  "951c25b5bc9188a5b3bff1bd93bd8f78c88516a0"
  "627439bffbe6f630eea9e8825c91b4a5712d481b"
  "1baf6a31cdcedadcd148658bd307785754e5b18b"
  "70a028b8d58ec4d4a7400065b3c4304a56f36628"
  "41f958e91e13857bc7e3b7a4ec7eaef9371da450"
  "25dd7685835ab540b8af897e3a06c4db00b75719"
)

ready() {
  for i in "${!FILES[@]}"; do
    local file="${FILES[$i]}"
    [[ -f "$ROOT/$file" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$file")" == "${HASHES[$i]}" ]] || return 1
  done
}

decode_patch() {
  [[ -f "$PAYLOAD" ]] || { echo "Missing U8k base64 payload" >&2; exit 1; }
  echo "$PAYLOAD_SHA256  $PAYLOAD" | sha256sum -c -
  PATCH_TMP="$(mktemp)"
  trap 'rm -f "$PATCH_TMP"' EXIT
  base64 --decode "$PAYLOAD" > "$PATCH_TMP"
  echo "$PATCH_SHA256  $PATCH_TMP" | sha256sum -c -
}

if ready; then
  decode_patch
  git -C "$ROOT" apply --check --reverse "$PATCH_TMP"
  echo "Source patch chain already materialized through U8k with payload/reverse verification"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U8j materializer" >&2; exit 1; }
decode_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH_TMP"
git -C "$ROOT" apply "$PATCH_TMP"
git -C "$ROOT" diff --check
ready || { echo "U8k final blob mismatch" >&2; exit 1; }
git -C "$ROOT" apply --check --reverse "$PATCH_TMP"
echo "Source patch chain materialized through U8k with exact blob/reverse verification"
