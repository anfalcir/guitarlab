#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8h.sh"
PATCH="$ROOT/.source-parts/U8iConfirmedSyncDeepLinks.patch"
PATCH_SHA256="dfd26a13c827e4a97be3ccc38e6f3f5021a4921d8ee83c0693d366994e7ea9a2"

declare -a FILES=(
  "app/src/androidTest/java/studio/guitarlab/app/ActivityNotificationDeepLinkInstrumentedTest.kt"
  "app/src/main/java/studio/guitarlab/app/MainActivity.kt"
  "app/src/main/java/studio/guitarlab/app/activity/ActivityScreen.kt"
  "app/src/main/java/studio/guitarlab/app/activity/AppNotificationDeepLink.kt"
  "app/src/main/java/studio/guitarlab/app/backup/AutomaticBackupWorker.kt"
  "app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/backup/ConfirmedRevisionStore.kt"
  "app/src/main/java/studio/guitarlab/app/ui/AppScreen.kt"
  "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  "app/src/test/java/studio/guitarlab/app/ui/AppRouteCodecTest.kt"
  "app/src/test/java/studio/guitarlab/app/ui/ProjectSyncLabelTest.kt"
  "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectBackupCoordinator.kt"
  "core/project/src/test/kotlin/studio/guitarlab/core/project/ProjectBackupCoordinatorTest.kt"
)

declare -a HASHES=(
  "935794d2527643914a49080d40a843d6c3ac82e2"
  "6ab4755178706bc801b4aba78b892bc9e9a5e1f0"
  "009cb2719a41400377c682453db07b87216a8ab7"
  "227c1d0cf00dcb6551c2a0dac8737e11b24490b6"
  "23cf21443af610302904be2912f61e650b2108d5"
  "47e2d97f3dfcc9be659c0d29397647b852f4d339"
  "24a12d032d8179dfa0d6248b05224fb04f44f7d8"
  "f377e80b93fd7316f66418e23d8c8b134739b6ce"
  "94155ba8b8767ee1e777e0cb19ff00f885b5dcee"
  "15d94ba2143e5f02ef21e2f1ecd5213e08f5e108"
  "ebec5c287806035f28e6809d71dc0b8f28b15ed2"
  "939e7ad62c70e57e295ba3b165e355e2351c11cb"
  "a28b2ba95585ab0e4014c92870a47e27955c3fdf"
  "035429cf4eebce78165a57090171f58bd9b9afff"
  "b9ead1dcd990bf774b234cea72c1c34a2d60fca4"
)

ready() {
  for i in "${!FILES[@]}"; do
    local file="${FILES[$i]}"
    [[ -f "$ROOT/$file" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$file")" == "${HASHES[$i]}" ]] || return 1
  done
}

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U8i patch payload" >&2; exit 1; }
  echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
}

if ready; then
  verify_patch
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U8i with payload/reverse verification"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U8h materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U8i final blob mismatch" >&2; exit 1; }
echo "Source patch chain materialized through U8i with exact blob verification"
