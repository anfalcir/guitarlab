#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u8g.sh"
PATCH="$ROOT/.source-parts/C5StudioMediaCohesion.patch"
PATCH_SHA256="a0d49c64d41e11964a42932fd76bed62d5bcd672489cd7e858ade8fa514d63fb"

declare -a FILES=(
  "core/model/src/main/kotlin/studio/guitarlab/core/model/ProjectModels.kt"
  "core/project/src/main/kotlin/studio/guitarlab/core/project/PreparedReferencePipeline.kt"
  "core/project/src/test/kotlin/studio/guitarlab/core/project/PreparedReferencePipelineTest.kt"
  "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt"
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
  "app/src/main/java/studio/guitarlab/app/ui/ProjectMediaDialog.kt"
)

declare -a HASHES=(
  "6a0ccb1e2f9b41694fd815486dab3e0251c2a1e0"
  "ad9644a8bee1c8840109f7b716a9aa3de9c8c440"
  "5b6d2c5076ffd7257bd8ef6b2585ba500f08d884"
  "673949b4773b005ac058597f859796b97cd875ee"
  "a6e54fe33d1905cdc776e775f99f7a77ae2d790a"
  "2039644f5617bbcfac2454bfbf07d6e3d709589c"
  "30d43b56a8d9e92442512a928f800b53b808d840"
)

ready() {
  for i in "${!FILES[@]}"; do
    local file="${FILES[$i]}"
    [[ -f "$ROOT/$file" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$file")" == "${HASHES[$i]}" ]] || return 1
  done
}

if ready; then
  echo "Source patch chain already materialized through C5"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U8g materializer" >&2; exit 1; }
[[ -f "$PATCH" ]] || { echo "Missing C5 patch payload" >&2; exit 1; }
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "C5 final blob mismatch" >&2; exit 1; }
echo "Source patch chain materialized through C5 with exact blob verification"
