#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10zb.sh"
PATCH="$ROOT/.source-parts/U12aPhysicalHomologationCorrectives.patch"
PATCH_SHA256="70f0bd7360f52237b9cd1a4e3ec72a31a3368ce95c4df13510e808fdd878c02b"

declare -a FILES=(
  "app/build.gradle.kts"
  "app/src/main/java/studio/guitarlab/app/backup/BackupScheduler.kt"
  "app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt"
  "app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/ui/ProjectShellScaffold.kt"
  "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt"
  "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
)

declare -a HASHES=(
  "92413d38c748de760c5fa63126158b9456c94a39"
  "65c9b7d5c18d09b3baa6d354c62ec1432343e4ab"
  "79464db547fdd624ec228ef465b3d2dfc42e0cc9"
  "8bae41082d44eeeadc4b9176a147dd9f121dd7a8"
  "fb798328dd823f0f540856d6fcf9275e569aa8af"
  "aecd0c7d70af6966ad787ef89bf4c67fc0cd8561"
  "5095741c9fbb776b8623c2efd73911b896c7e160"
  "085d372eda95b4ad4a45fa435f392186f0536fa8"
  "ae674e4c922f9dde5a2d28c3e1cb25fa135745fc"
  "45515efeb9d88928200c6f58f61a339d395fdf15"
)

ready() {
  for i in "${!FILES[@]}"; do
    [[ -f "$ROOT/${FILES[$i]}" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/${FILES[$i]}")" == "${HASHES[$i]}" ]] || return 1
  done
}

verify_semantics() {
  local backup="$ROOT/app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt"
  local backup_vm="$ROOT/app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt"
  local home="$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt"
  local prepare="$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  local shell="$ROOT/app/src/main/java/studio/guitarlab/app/ui/ProjectShellScaffold.kt"

  git -C "$ROOT" diff --check
  grep -q 'versionCode = 26' "$ROOT/app/build.gradle.kts"
  grep -q 'versionName = "0.5.0-rc6"' "$ROOT/app/build.gradle.kts"
  grep -q 'GuitarLab Studio' "$home"
  grep -q 'Importar projeto' "$home"
  grep -q 'contentDescription = "Início"' "$shell"
  grep -q 'Modifier.align(Alignment.Center).widthIn(max = 520.dp)' "$shell"
  grep -q 'prepare-search-progress' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt"
  grep -q 'Pesquisando fontes para a música' "$prepare"
  grep -q 'cancelCoalesced' "$backup_vm"
  grep -q 'backup-busy-feedback' "$backup"
  if grep -qE 'Diagnóstico de homologação|Executar aceitação U8m|beginU8mRealDriveAcceptance' "$backup" "$backup_vm"; then
    echo "U8m development surface leaked into production backup UI" >&2
    exit 1
  fi
}

[[ -f "$PATCH" ]] || { echo "Missing U12a corrective patch" >&2; exit 1; }
echo "$PATCH_SHA256  $PATCH" | sha256sum -c -

if ready; then
  verify_semantics
  git -C "$ROOT" apply --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U12a physical-homologation correctives"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10zb materializer" >&2; exit 1; }
bash "$PREVIOUS"
git -C "$ROOT" apply --check "$PATCH"
git -C "$ROOT" apply "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U12a final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --check --reverse "$PATCH"
echo "Source patch chain materialized through U12a physical-homologation correctives"
