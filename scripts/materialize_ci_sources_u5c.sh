#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u5b.sh"
PATCH="$ROOT/.source-parts/U5cCoverageClosure.patch"
PATCH_SHA256="e3585744a46897d91a250ff00cafef4974f3bed2ee31289470234ce6fc03d79b"

CHECKS=(
  "core/project/src/test/kotlin/studio/guitarlab/core/project/PreparedReferencePipelineTest.kt|8e3363239f1bc8a8fb41ebd7360f47a22c8d411e"
  "platform/audio-android/src/test/kotlin/studio/guitarlab/platform/audio/android/StudioMasterRenderRequestFactoryTest.kt|bcbefc79c797ec10e67f903526fb2eab0ffe55d6"
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt|df54e5c9021140ce3d2a1dcc8abc7c35a1405694"
)

hash_file() { git -C "$ROOT" hash-object "$1"; }
u5c_ready() {
  local entry relative expected
  for entry in "${CHECKS[@]}"; do
    relative="${entry%%|*}"
    expected="${entry#*|}"
    [[ -f "$ROOT/$relative" ]] || return 1
    [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
  done
}

if u5c_ready; then
  echo "Source patch chain already materialized through U5c"
  exit 0
fi

bash "$PREVIOUS"
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -
patch --dry-run -p1 -d "$ROOT" < "$PATCH" >/dev/null
patch --batch --forward -p1 -d "$ROOT" < "$PATCH"
u5c_ready || {
  echo "U5c applied but final hashes do not match" >&2
  exit 1
}
echo "Source patch chain materialized through U5c with verified final hashes"
