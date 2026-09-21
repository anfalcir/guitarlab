#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c2b.sh"
PART_PREFIX="$ROOT/.source-parts/C3aLifecycleCoherence.part"
PATCH_B64_SHA256="1e475e3201da260408f4d956d1fef4468eb037ffcfdb04ae696a66b11a85a308"
PATCH_GZ_SHA256="4604084fa968ccbb6020f3c1acdba4618998fd13585179c4892d4276f8c62f8b"
PATCH_SHA256="6603b92a8f1d59c17240c793954ec7259444e87322dd9d349bb7b8823a09613b"
PRE_AGG="5122094466c2fc6845395149f8a3fda1c8206efa7ef413eaf5e6f7fb17e5712d"
POST_AGG="370e3d0d87c473b5391795042de12a70ab6581d6dad404c25578b477d2377c50"
FILES=(
"app/src/androidTest/java/studio/guitarlab/app/ProjectDeleteConfirmationInstrumentedTest.kt"
"app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
"app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt"
"app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt"
"app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
"app/src/main/java/studio/guitarlab/app/ui/NewProjectScreen.kt"
"app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt"
"core/project/src/main/kotlin/studio/guitarlab/core/project/FileProjectRepository.kt"
"core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectLifecyclePolicy.kt"
"core/project/src/main/kotlin/studio/guitarlab/core/project/SourceAssetPublisher.kt"
"core/project/src/test/kotlin/studio/guitarlab/core/project/FileProjectRepositoryTest.kt"
"core/project/src/test/kotlin/studio/guitarlab/core/project/SourceAssetPublisherTest.kt"
"platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FileRemoteJobStore.kt"
"platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
"platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceAcquisitionClient.kt"
"platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceOperationStore.kt"
)
aggregate() {
  local f
  for f in "${FILES[@]}"; do
    if [[ -f "$ROOT/$f" ]]; then printf '%s|%s\n' "$f" "$(git -C "$ROOT" hash-object "$ROOT/$f")"; else printf '%s|ABSENT\n' "$f"; fi
  done | sha256sum | awk '{print $1}'
}
if [[ "$(aggregate)" == "$POST_AGG" ]]; then echo "Source patch chain already materialized through C3a"; exit 0; fi
[[ -f "$PREVIOUS" ]] || { echo "Missing C2b materializer" >&2; exit 1; }
bash "$PREVIOUS"
[[ "$(aggregate)" == "$PRE_AGG" ]] || { echo "C3a pre-image aggregate mismatch" >&2; exit 1; }
TMP_B64="$(mktemp)"; TMP_GZ="$(mktemp)"; TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_B64" "$TMP_GZ" "$TMP_PATCH"' EXIT
cat "${PART_PREFIX}"*.b64 > "$TMP_B64"
echo "$PATCH_B64_SHA256  $TMP_B64" | sha256sum -c -
base64 --decode "$TMP_B64" > "$TMP_GZ"
echo "$PATCH_GZ_SHA256  $TMP_GZ" | sha256sum -c -
gzip -dc "$TMP_GZ" > "$TMP_PATCH"
echo "$PATCH_SHA256  $TMP_PATCH" | sha256sum -c -
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check
[[ "$(aggregate)" == "$POST_AGG" ]] || { echo "C3a post-image aggregate mismatch" >&2; exit 1; }
grep -q 'title = "Começar no Studio"' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/NewProjectScreen.kt"
grep -q 'fun beginSourceReplacement' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
grep -q 'ProjectLifecyclePolicy.withPublishedSource' "$ROOT/core/project/src/main/kotlin/studio/guitarlab/core/project/SourceAssetPublisher.kt"
grep -q 'fun closeProject(projectId:String)' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
grep -q 'sourceReplacementStartsFreshPrepareGenerationAndPreservesCreativeStateAndOldAssets' "$ROOT/core/project/src/test/kotlin/studio/guitarlab/core/project/SourceAssetPublisherTest.kt"
echo "Source patch chain materialized through C3a with exact aggregate verification"
