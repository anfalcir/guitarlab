#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8h.sh"
PAYLOAD="$ROOT/.source-parts/U8iConfirmedSyncDeepLinks.patch.b64"
PAYLOAD_SHA256="29a7df7508a3deaeb44e1c5dc0d73d131e628a2f179f5989faa8bf73b3ba4a35"
PATCH_SHA256="6df0637000bf730a5cf5ee0413ab32526bd4ffb32cad5fb404ce3738ca964a59"

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
  "scripts/ci_run_api36_regression_groups.sh"
)

declare -a HASHES=(
  "19acda46286ff05fadb544aaf83f9c30daefeee0"
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
  "d74b57a8c18e3855caf3d44201e639f988e68c9d"
)

ready() {
  for i in "${!FILES[@]}"; do
    local file="${FILES[$i]}"
    [[ -f "$ROOT/$file" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$file")" == "${HASHES[$i]}" ]] || return 1
  done
}

decode_patch() {
  [[ -f "$PAYLOAD" ]] || { echo "Missing U8i base64 payload" >&2; exit 1; }
  echo "$PAYLOAD_SHA256  $PAYLOAD" | sha256sum -c -
  PATCH_TMP="$(mktemp)"
  trap 'rm -f "$PATCH_TMP"' EXIT
  base64 --decode "$PAYLOAD" > "$PATCH_TMP"
  echo "$PATCH_SHA256  $PATCH_TMP" | sha256sum -c -
}

if ready; then
  decode_patch
  git -C "$ROOT" apply --check --reverse "$PATCH_TMP"
  echo "Source patch chain already materialized through U8i with payload/reverse verification"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U8h materializer" >&2; exit 1; }
decode_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH_TMP"
git -C "$ROOT" apply "$PATCH_TMP"
git -C "$ROOT" diff --check
ready || { echo "U8i final blob mismatch" >&2; exit 1; }
echo "Source patch chain materialized through U8i with exact blob verification"
