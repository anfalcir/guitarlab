#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c5.sh"
PATCH="$ROOT/.source-parts/U8hAndroidActivityCohesion.patch"
PATCH_SHA256="4fd661a0ad3971e2d7b176ed4da734f0f80c13d49f8a895891aebc148cba73f6"

declare -a FILES=(
  "app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt"
  "app/src/main/java/studio/guitarlab/app/activity/UnifiedActivityStore.kt"
  "app/src/main/java/studio/guitarlab/app/activity/UnifiedActivityViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/backup/AutomaticBackupWorker.kt"
  "app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/ui/AppScreen.kt"
  "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt"
)

declare -a HASHES=(
  "498a341fe165bb7973125a054aa3180bc4472a48"
  "5dbda2f9fc5dfe040b308767d381b97f823f796e"
  "16a28496187b1e493a22913e48e8c16e22d55cbb"
  "2608894a9b69e33f62b63013739ef50896ca1609"
  "d537f5c55a299461641b2342c50caed2015360a9"
  "5096aef652b955b71471dd9dbec69bb2167ba3fc"
  "cbf08d39b7c40f29b7d9e127b96aeec79569ff62"
  "8cfe36e3fc4857d8244c2b54d9dbc517e02aef5b"
  "d35cf2bc5906a13e74f894de2f0909df926fa0ef"
  "01b34e0ee75d04e8ecb640542305c4e6ea82d77b"
)

ready() {
  for i in "${!FILES[@]}"; do
    local file="${FILES[$i]}"
    [[ -f "$ROOT/$file" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$file")" == "${HASHES[$i]}" ]] || return 1
  done
}

if ready; then
  echo "Source patch chain already materialized through U8h"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C5 materializer" >&2; exit 1; }
[[ -f "$PATCH" ]] || { echo "Missing U8h patch payload" >&2; exit 1; }
bash "$PREVIOUS"
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U8h final blob mismatch" >&2; exit 1; }
echo "Source patch chain materialized through U8h with exact blob verification"