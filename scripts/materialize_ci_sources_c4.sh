#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_c3g.sh"
PATCH_PARTS=(
  "$ROOT/.source-parts/C4ProgressivePrepare.patch.gz.b64.part00"
  "$ROOT/.source-parts/C4ProgressivePrepare.patch.gz.b64.part01"
  "$ROOT/.source-parts/C4ProgressivePrepare.patch.gz.b64.part02"
  "$ROOT/.source-parts/C4ProgressivePrepare.patch.gz.b64.part03"
)
PATCH_PART_SHA256=(
  "7a00c80b7124b67fdeaff0bbe64775ac82d8a8275c184b820f5396f9b7e9a8a3"
  "fdb89bcd95c4b2631b99ae7081e84f46c099d6bd4d7ed41099f77cd38b063ea3"
  "d5bc4f5c56dac1b28729851ef1894e2846cb5a0e16bf33783171be525199ca66"
  "1494a54b55f73ff8167a042d5d5b59d7bea6af6fb8e5565c331503e98532e87b"
)
PATCH_B64_SHA256="c67d03c83bb060b26195c6845de14ddad1b8052f0ca84c369fde27a30a696256"
PATCH_GZ_SHA256="b7851dfdd0040c20381d2778512636bcee9db10bf513315b5bb284dcc358edd6"
PATCH_SHA256="96728749151da4ac24ecace8e315b97331b0317eb6d43ed45125cab260af5e7a"

TARGETS=(
  "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt"
  "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt"
  "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt"
  "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt"
  "app/src/main/java/studio/guitarlab/app/ui/PrepareJourneyPolicy.kt"
  "app/src/test/java/studio/guitarlab/app/ui/PrepareJourneyPolicyTest.kt"
  "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FileRemoteJobStore.kt"
  "platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt"
  "platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceAcquisitionClient.kt"
  "platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceOperationStore.kt"
)
PRE_HASHES=(
  "549f0c5e1989b18a1beb496e7b8a6270d23bc54d"
  "201b21ea9c4a3865513bbecb5010a35b8297a7f3"
  "46d9066d329eee6ce9ce73f777b13e23e14bae41"
  "9a4e1fd7e61ea15c4045bfcdbac4c5f0cbe91960"
  "ABSENT"
  "ABSENT"
  "33e303ef02cf594192e5d995e7dfd11fac75d9fc"
  "f90a94233c47812dc337a7d3020d452363e7de06"
  "96879c56551b4a889b773fd46278265e7e3307a4"
  "3e5e2ba9cd7d397e831ddcc4a11a4f1681ef2663"
  "e67613045acd7c4c0a09b601eeda83d4cc9d5b18"
)
POST_HASHES=(
  "345672cbe467ae573778099adf2c8c68e9d791de"
  "2dacee255023efc1938b13ee316093b7c4b8ad45"
  "2a4c66581658320a69ba099a9d90e6b5bd153f4f"
  "ba79da3430acbdec72818ed609fc1b88b5dd6d35"
  "2e1d412c3479fedf840788676eb3d232d0b8fac4"
  "9df8287d36818773ca60ad3dd2d7c6eb2d441771"
  "b7189cbb20ffcb5ca272848c8835c137613ec625"
  "a6eab7b4ae9d65cc96385409e4d5af695b080574"
  "a46f5d624e4cf52bbc7c24b13f1bae12f3109ca7"
  "ddda8ea9f0b38a298f97f862214449ffc7262aab"
  "0f329207295881621ad18ba3aeecc12751199e29"
)

hash_target() { git -C "$ROOT" hash-object "$ROOT/$1"; }
all_post=true
for i in "${!TARGETS[@]}"; do
  target="${TARGETS[$i]}"
  if [[ ! -f "$ROOT/$target" ]] || [[ "$(hash_target "$target")" != "${POST_HASHES[$i]}" ]]; then
    all_post=false
    break
  fi
done
if [[ "$all_post" == true ]]; then
  echo "Source patch chain already materialized through C4"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing C3g materializer" >&2; exit 1; }
for i in "${!PATCH_PARTS[@]}"; do
  [[ -f "${PATCH_PARTS[$i]}" ]] || { echo "Missing C4 patch payload part: ${PATCH_PARTS[$i]}" >&2; exit 1; }
  echo "${PATCH_PART_SHA256[$i]}  ${PATCH_PARTS[$i]}" | sha256sum -c -
done
bash "$PREVIOUS"

for i in "${!TARGETS[@]}"; do
  target="${TARGETS[$i]}"
  expected="${PRE_HASHES[$i]}"
  if [[ "$expected" == "ABSENT" ]]; then
    [[ ! -e "$ROOT/$target" ]] || { echo "C4 refuses to patch: expected absent target exists: $target" >&2; exit 1; }
  else
    [[ -f "$ROOT/$target" ]] || { echo "C4 refuses to patch: missing target: $target" >&2; exit 1; }
    [[ "$(hash_target "$target")" == "$expected" ]] || { echo "C4 refuses to patch: pre-blob mismatch: $target" >&2; exit 1; }
  fi
done

TMP_B64="$(mktemp)"; TMP_GZ="$(mktemp)"; TMP_PATCH="$(mktemp)"
trap 'rm -f "$TMP_B64" "$TMP_GZ" "$TMP_PATCH"' EXIT
cat "${PATCH_PARTS[@]}" > "$TMP_B64"
echo "$PATCH_B64_SHA256  $TMP_B64" | sha256sum -c -
base64 --decode "$TMP_B64" > "$TMP_GZ"
echo "$PATCH_GZ_SHA256  $TMP_GZ" | sha256sum -c -
gzip -dc "$TMP_GZ" > "$TMP_PATCH"
echo "$PATCH_SHA256  $TMP_PATCH" | sha256sum -c -
git -C "$ROOT" apply --check "$TMP_PATCH"
git -C "$ROOT" apply "$TMP_PATCH"
git -C "$ROOT" diff --check

for i in "${!TARGETS[@]}"; do
  target="${TARGETS[$i]}"
  [[ -f "$ROOT/$target" ]] || { echo "C4 final target missing: $target" >&2; exit 1; }
  [[ "$(hash_target "$target")" == "${POST_HASHES[$i]}" ]] || { echo "C4 final blob mismatch: $target" >&2; exit 1; }
done

grep -q 'startPrepareObservation' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt" || { echo "C4 observable Prepare ownership missing" >&2; exit 1; }
! grep -q 'delay(600)' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt" || { echo "C4 Compose polling remains" >&2; exit 1; }
! grep -q 'Criar base e referência' "$ROOT/app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt" || { echo "C4 redundant reference confirmation remains" >&2; exit 1; }
grep -q 'PreparedReferenceService(repository, mediaStore, applicationContext.cacheDir).prepare' "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteRuntime.kt" || { echo "C4 automatic references missing" >&2; exit 1; }
grep -q 'Excelente · 88/100' "$ROOT/app/src/test/java/studio/guitarlab/app/ui/PrepareJourneyPolicyTest.kt" || { echo "C4 accessible score regression test missing" >&2; exit 1; }

echo "Source patch chain materialized through C4 with exact blob verification"
