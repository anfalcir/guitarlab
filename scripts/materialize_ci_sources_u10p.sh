#!/usr/bin/env bash
# U10p corrective seal: persist C8 screenshots in shared emulator storage across test teardown.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10o.sh"
PATCH="$ROOT/.source-parts/U10pSharedScreenshotStorageCorrective.patch"
PATCH_BLOB="e22b9cd22e2d67d326d1a87fe41200f89d981da6"

declare -a FILES=(
  "app/src/androidTest/java/studio/guitarlab/app/CohesionScreenshotArtifacts.kt"
  "scripts/ci_run_api36_regression_groups.sh"
)

declare -a HASHES=(
  "7730e7016229899ff1eaf6220b3f28d4f8c11c0b"
  "5517979b0a9363d81ff5a1c4f15af0c1c9b01dd9"
)

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10p shared screenshot storage patch" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10p screenshot storage patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  for i in "${!FILES[@]}"; do
    local file="${FILES[$i]}"
    [[ -f "$ROOT/$file" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$file")" == "${HASHES[$i]}" ]] || return 1
  done
}

verify_semantics() {
  local helper="$ROOT/app/src/androidTest/java/studio/guitarlab/app/CohesionScreenshotArtifacts.kt"
  local ci="$ROOT/scripts/ci_run_api36_regression_groups.sh"

  grep -q 'COHESION_SCREENSHOT_ROOT = "/sdcard/guitarlab-ci-screenshots"' "$helper" || {
    echo "U10p shared screenshot root missing" >&2; exit 1;
  }
  grep -q 'screencap -p \$remotePath' "$helper" || {
    echo "U10p shell screencap backend missing" >&2; exit 1;
  }
  grep -q 'SCREENSHOT_OK' "$helper" || {
    echo "U10p screenshot runtime verification missing" >&2; exit 1;
  }
  grep -q 'remote_root="/sdcard/guitarlab-ci-screenshots"' "$ci" || {
    echo "U10p CI shared screenshot root missing" >&2; exit 1;
  }
  grep -q 'adb pull "\$remote" "\$dest/\$name"' "$ci" || {
    echo "U10p adb pull collector missing" >&2; exit 1;
  }
  if grep -q 'run-as studio.guitarlab.app.*ci-screenshots' "$ci"; then
    echo "U10p stale app-private screenshot collector remains" >&2
    exit 1
  fi
  grep -q '^verify_visual_matrix$' "$ci" || {
    echo "U10p fail-closed visual matrix guard regression" >&2; exit 1;
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10p shared screenshot storage"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10o materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10p final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U10p shared screenshot storage"
