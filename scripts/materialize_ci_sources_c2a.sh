#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c1o.sh"
PATCH_B64="$ROOT/.source-parts/C2aUnifiedProjectShell.patch.gz.b64"
PATCH_B64_SHA256="e1d5cf07dce18a2e4b6827eb9d7be672dfcebcb95f507bf2c872afbf749fb7cf"
PATCH_GZ_SHA256="fdf1fcfd9cc11f94506efaafd5f91fba2ca7cd85a6f8871421f823bea5168de8"
PATCH_SHA256="e3ebcf66fa8573582458ff8878d1b657dca5ebcab332cd658e118e2b7de9f58c"

NAV="$ROOT/app/src/main/java/studio/guitarlab/app/ui/AppNavigationViewModel.kt"
APP="$ROOT/app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt"
WORKSPACE="$ROOT/app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt"
STUDIO="$ROOT/app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt"
SHELL="$ROOT/app/src/main/java/studio/guitarlab/app/ui/ProjectShellScaffold.kt"
SHELL_TEST="$ROOT/app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
LIFECYCLE_TEST="$ROOT/app/src/androidTest/java/studio/guitarlab/app/GuitarLabLifecycleInstrumentedTest.kt"
NAV_TEST="$ROOT/app/src/test/java/studio/guitarlab/app/ui/AppNavigationViewModelTest.kt"
POLICY_TEST="$ROOT/app/src/test/java/studio/guitarlab/app/ui/UnifiedProjectShellPolicyTest.kt"

hash_file() { git -C "$ROOT" hash-object "$1"; }

post_materialized() {
  [[ -f "$SHELL" ]] || return 1
  [[ "$(hash_file "$NAV")" == "bb50fb3bfb2b13505442c19c524e7b29e0a5d213" ]] || return 1
  [[ "$(hash_file "$APP")" == "db428587f6aafd3c968838f5704cf15e976ac99f" ]] || return 1
  [[ "$(hash_file "$WORKSPACE")" == "ac2de3d2337db55ad1619971ea9a3e378ad608fe" ]] || return 1
  [[ "$(hash_file "$STUDIO")" == "eea467a1b56d45fdb71f280fe882c43c88410752" ]] || return 1
  [[ "$(hash_file "$SHELL")" == "0840cdd4b2729907b2df335011914c550b955edf" ]] || return 1
  [[ "$(hash_file "$SHELL_TEST")" == "8dfa71f963cdb33d82acd86aca081082adc2b58c" ]] || return 1
  [[ "$(hash_file "$LIFECYCLE_TEST")" == "eb5bd523062c3ee1ee4c316163838b58edd99153" ]] || return 1
  [[ "$(hash_file "$NAV_TEST")" == "498310a72d61156567a6ddc970e9287f76aae2f3" ]] || return 1
  [[ "$(hash_file "$POLICY_TEST")" == "eab492787dee3892871f74eedf9b9bbfb9e7e84c" ]] || return 1
}

if post_materialized; then
  echo "Source patch chain already materialized through C2a"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C1o materializer: $PREVIOUS" >&2; exit 1; }
[[ -f "$PATCH_B64" ]] || { echo "Missing C2a patch payload: $PATCH_B64" >&2; exit 1; }

bash "$PREVIOUS"

[[ "$(hash_file "$NAV")" == "83802594e5704369e66810323213d5c819639207" ]] || { echo "C2a navigation blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$APP")" == "6466e47a1a5a44ed3c6e11b5de767bee927505d9" ]] || { echo "C2a app router blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$WORKSPACE")" == "1c83c859aff092da399eafbdd11bc487dbb7e666" ]] || { echo "C2a workspace blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$STUDIO")" == "83a76c5aa0cfefb12f6d67b4f502aad9586e1943" ]] || { echo "C2a Studio shell blob mismatch" >&2; exit 1; }
[[ ! -e "$SHELL" ]] || { echo "C2a refuses to overwrite pre-existing ProjectShellScaffold" >&2; exit 1; }
[[ "$(hash_file "$SHELL_TEST")" == "f729638db0b038aee478debfd23111d8eeac0d02" ]] || { echo "C2a shell test blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$LIFECYCLE_TEST")" == "9ae0aa1bccee7978022e7ba831f40d65f46b0d94" ]] || { echo "C2a lifecycle test blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$NAV_TEST")" == "e5f9b4610422a46ee9397fd6198f9fb7e5aba27a" ]] || { echo "C2a navigation test blob mismatch" >&2; exit 1; }
[[ "$(hash_file "$POLICY_TEST")" == "757adc3ca6aeb25497f616655ce8eadb2ef13164" ]] || { echo "C2a policy test blob mismatch" >&2; exit 1; }

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

post_materialized || { echo "C2a final source blob mismatch" >&2; exit 1; }

grep -q 'fun ProjectShellScaffold' "$SHELL" || { echo "C2a shared shell missing" >&2; exit 1; }
grep -q 'currentWorkspace = ProjectWorkspace.PREPARE' "$WORKSPACE" || { echo "C2a Prepare shell integration missing" >&2; exit 1; }
grep -q 'currentWorkspace = ProjectWorkspace.EXPORT' "$WORKSPACE" || { echo "C2a Export shell integration missing" >&2; exit 1; }
grep -q 'currentWorkspace = ProjectWorkspace.STUDIO' "$STUDIO" || { echo "C2a Studio shell integration missing" >&2; exit 1; }
if grep -q 'private fun StudioTopBar' "$STUDIO"; then
  echo "C2a found obsolete bespoke Studio project top bar" >&2
  exit 1
fi
grep -q 'rememberSaveableStateHolder' "$APP" || { echo "C2a workspace saveable-state holder missing" >&2; exit 1; }
grep -q 'destinationForProject' "$NAV" || { echo "C2a remembered project workspace routing missing" >&2; exit 1; }
grep -q 'returnFromSettings' "$NAV" || { echo "C2a Settings-origin routing missing" >&2; exit 1; }
grep -q 'project-route-missing' "$APP" || { echo "C2a missing-project fallback missing" >&2; exit 1; }
grep -q 'projectSettingsReturnsToExactOriginWorkspaceAfterRecreation' "$LIFECYCLE_TEST" || { echo "C2a Settings recreation regression missing" >&2; exit 1; }
grep -q 'sharedProjectShellUsesOneNavigationGrammar' "$SHELL_TEST" || { echo "C2a shell navigation regression missing" >&2; exit 1; }

echo "Source patch chain materialized through C2a with exact blob verification"
