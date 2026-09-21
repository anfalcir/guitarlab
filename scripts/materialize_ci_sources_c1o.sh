#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1n.sh"
PATCH_B64="$ROOT/.source-parts/C1oExportContractClosure.patch.gz.b64"
PATCH_B64_SHA256="64f8c820f31bc66681a35c481cc538df6f46140709778c148c3c7188687617b7"
PATCH_GZ_SHA256="137113ba2f3673409b1e908c0d7678e53a355cc68d77eb603cb4b2ac127fbb84"
PATCH_SHA256="70ac3460d9148969bde11ceddf2c1c01810ee26469badef152adc0a30b598d66"

WORKSPACE="$ROOT/app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt"
STUDIO_VM="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt"
STUDIO_SHELL="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt"
TEST="$ROOT/app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"

WORKSPACE_PRE="f2711e34928c28c0c49762c2e0828ddf4a460e7d"
WORKSPACE_POST="1c83c859aff092da399eafbdd11bc487dbb7e666"
STUDIO_VM_PRE="eb49248319d8814748c617e3f55cef55daf47acc"
STUDIO_VM_POST="61f26c24cf4c999bcb17439274c43f307aef19d0"
STUDIO_SHELL_PRE="db253e899906a7ed0b3a98f72104eca4039cc784"
STUDIO_SHELL_POST="83a76c5aa0cfefb12f6d67b4f502aad9586e1943"
TEST_PRE="7559bb8909ee7290352bd3c6c2810e7e6983d4d5"
TEST_POST="f729638db0b038aee478debfd23111d8eeac0d02"

hash_file() { git -C "$ROOT" hash-object "$1"; }

if [[ -f "$WORKSPACE" && -f "$STUDIO_VM" && -f "$STUDIO_SHELL" && -f "$TEST" ]] &&
   [[ "$(hash_file "$WORKSPACE")" == "$WORKSPACE_POST" ]] &&
   [[ "$(hash_file "$STUDIO_VM")" == "$STUDIO_VM_POST" ]] &&
   [[ "$(hash_file "$STUDIO_SHELL")" == "$STUDIO_SHELL_POST" ]] &&
   [[ "$(hash_file "$TEST")" == "$TEST_POST" ]]; then
  echo "Source patch chain already materialized through C1o"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1n materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C1o patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"

[[ "$(hash_file "$WORKSPACE")" == "$WORKSPACE_PRE" ]] || { echo "C1o refuses to patch: export workspace blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$STUDIO_VM")" == "$STUDIO_VM_PRE" ]] || { echo "C1o refuses to patch: StudioViewModel blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$STUDIO_SHELL")" == "$STUDIO_SHELL_PRE" ]] || { echo "C1o refuses to patch: StudioShell blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$TEST")" == "$TEST_PRE" ]] || { echo "C1o refuses to patch: UnifiedProjectShell test blob mismatch" >&2; exit 1; }

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

[[ "$(hash_file "$WORKSPACE")" == "$WORKSPACE_POST" ]] || { echo "C1o final export workspace blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$STUDIO_VM")" == "$STUDIO_VM_POST" ]] || { echo "C1o final StudioViewModel blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$STUDIO_SHELL")" == "$STUDIO_SHELL_POST" ]] || { echo "C1o final StudioShell blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$TEST")" == "$TEST_POST" ]] || { echo "C1o final UnifiedProjectShell test blob mismatch" >&2; exit 1; }

if grep -q 'WAV • sem conversão' "$WORKSPACE"; then
  echo "C1o found implementation detail in primary export label" >&2
  exit 1
fi
grep -q 'WAV é publicado sem conversão' "$WORKSPACE" || { echo "C1o secondary zero-transcode explanation missing" >&2; exit 1; }
if grep -Eq '\bexporting\b|exportStatus|fun exportProjectPackage|fun exportMaster\(' "$STUDIO_VM"; then
  echo "C1o found obsolete Studio-local export state or entry point" >&2
  exit 1
fi
grep -q 'assertTextEquals("WAV")' "$TEST" || { echo "C1o primary WAV label contract test missing" >&2; exit 1; }

echo "Source patch chain materialized through C1o with exact blob verification"
