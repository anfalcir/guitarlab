#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u6c.sh"
PATCH_B64="$ROOT/.source-parts/C1CanonicalExport.patch.gz.b64"
PATCH_B64_SHA256="f0dccbe7a81b2b988909d9d34ca1133893c3a0f7a7aac673384893428de2449d"
PATCH_GZ_SHA256="eb89fd1248d1da4c57ad493ae2d52204e3e19c917591d458052498a55dcbfbe7"
PATCH_SHA256="5107b0a4f69788980e01c93391a5e032fb59d687dc3f34be1121b13f7bdc0e25"

PRE_CHECKS=(
  "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt|71263db275adb2322cca682baba8f8f92dbf40e4"
  "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt|679d8557d77fbb304a71b0967db62b4064082722"
  "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt|a72b17e2360a7e15547b37af73a666f2ad3e305c"
  "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt|ed03b956ee8aa49551bfa2786cf8b060d9bca56e"
  "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt|6c98b72b674743eeeebfe3991c0636e824ef8a09"
  "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt|67749191944adcfd411df28941dd638dd41d575a"
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt|528ea83199fa0465ad4518da12abce45302ba473"
  "app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt|28a49b2b81baf9f86cff59de18e0b15973285986"
)
POST_CHECKS=(
  "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt|fa571c6b4ef86035c4affe1f98756d2ac4ba9b29"
  "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt|6466e47a1a5a44ed3c6e11b5de767bee927505d9"
  "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt|db253e899906a7ed0b3a98f72104eca4039cc784"
  "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt|46d9066d329eee6ce9ce73f777b13e23e14bae41"
  "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt|4b89c816c399e584bb37ae56e8fccaef14421709"
  "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt|f2711e34928c28c0c49762c2e0828ddf4a460e7d"
  "app/src/main/java/studio/guitarlab/app/ui/ExportEntryPointPolicy.kt|c485a3fe17ce106651d7c43e54f88529a3b27763"
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt|b6fa99e435233490fe9e9958b4917e1b021acfc4"
  "app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt|ae39b6b4ec94d3b4543106d9e47ee2b55e8fc7e7"
  "app/src/test/java/studio/guitarlab/app/ui/ExportEntryPointPolicyTest.kt|c3b51c85afecc0e6270609918ef8e2f94ed289fb"
)

hash_file() { git -C "$ROOT" hash-object "$1"; }
checks_ready() {
  local array_name="$1" entry relative expected
  local -n checks_ref="$array_name"
  for entry in "${checks_ref[@]}"; do
    relative="${entry%%|*}"
    expected="${entry#*|}"
    [[ -f "$ROOT/$relative" ]] || return 1
    [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
  done
}

semantic_contract() {
  ! grep -R -q 'SaveAndExportDialog' "$ROOT/app/src/main/java" || return 1
  ! grep -R -q 'Salvar e exportar\|Study Exports\|Studio Master\|Área de exportação' "$ROOT/app/src/main/java" || return 1
  grep -q 'onExport = { navigate(ExportEntryPointPolicy.destination(current.projectId)) }' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt" || return 1
  grep -q 'Text("Projeto portátil"' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt" || return 1
  grep -q 'Text("Arquivos para estudo"' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt" || return 1
  grep -q 'Text("Mix final do Studio"' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt" || return 1
}

if checks_ready POST_CHECKS && semantic_contract; then
  echo "Source patch chain already materialized through C1 canonical export cohesion"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U6c materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1 patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"
checks_ready PRE_CHECKS || {
  echo "C1 refuses to patch: materialized U6 inputs do not match the exact CI #694 blobs" >&2
  exit 1
}
[[ ! -e "$ROOT/app/src/main/java/studio/guitarlab/app/ui/ExportEntryPointPolicy.kt" ]] || { echo "Unexpected pre-existing ExportEntryPointPolicy.kt" >&2; exit 1; }
[[ ! -e "$ROOT/app/src/test/java/studio/guitarlab/app/ui/ExportEntryPointPolicyTest.kt" ]] || { echo "Unexpected pre-existing ExportEntryPointPolicyTest.kt" >&2; exit 1; }

echo "$PATCH_B64_SHA256  $PATCH_B64" | sha256sum -c -
TMP_GZ="$(mktemp)"
TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_GZ" "$TMP_PATCH"' EXIT
base64 --decode "$PATCH_B64" > "$TMP_GZ"
echo "$PATCH_GZ_SHA256  $TMP_GZ" | sha256sum -c -
gzip -dc "$TMP_GZ" > "$TMP_PATCH"
echo "$PATCH_SHA256  $TMP_PATCH" | sha256sum -c -
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
checks_ready POST_CHECKS || {
  echo "C1 applied but final git blobs do not match the locked outputs" >&2
  exit 1
}
semantic_contract || {
  echo "C1 final blobs matched, but the export cohesion semantic contract failed" >&2
  exit 1
}

echo "Source patch chain materialized through C1 with exact pre/post blob verification"
