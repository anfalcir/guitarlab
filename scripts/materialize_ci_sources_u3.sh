#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u2d.sh"
PATCH_B64_PREFIX="$ROOT/.source-parts/U3SourceAcquisition.patch.gz.b64.part"
PATCH_B64_SHA256="8409b91e2ab88038f2e16c43a5921fb2f5124b54c0c01552a60e2129df5c1290"
PATCH_GZ_SHA256="e04a1cd5651d42a6fa0a9174d5b385c6630fbe8faf9af065fb659de42262a127"
PATCH_SHA256="6cf30c013a2a24a41bf6a0ad0fcd5ccd05ac452f01d7d1a1ec95519979e068df"

U3_CHECKS=(
    "app/build.gradle.kts|f6a219beedd50dc147415e7479e16ee93d2f6e01"
    "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt|4ae1ac682c2dfc407bc9572c8415a778826e121f"
    "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt|74d75b856190715fa2d8ebc53d4cd3d10d9debc7"
    "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt|50a509aa5e341e846520425f7c444d6ca124903a"
    "app/src/main/java/studio/guitarlab/app/ui/NewProjectScreen.kt|6fd9546f54e93d107e569e71d144a7374efcb75e"
    "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt|87314f58d632d3318a9daa0ee4be4b83d7131392"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/SourceAssetPublisher.kt|03401b3131f3812c913f959c6d685cf3a0ba4165"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/SourceAssetPublisherTest.kt|a28c67fb725026da65e3eadf726325d32b6f85a9"
    "core/source/build.gradle.kts|eed002171c777d2582dc6959de02b264cc0ece62"
    "core/source/src/main/kotlin/studio/guitarlab/core/source/SourceSearch.kt|1bf289578cd9e1f16e78024b79e8255e2080d9b9"
    "core/source/src/test/kotlin/studio/guitarlab/core/source/SourceSearchRulesTest.kt|13c879a13d81a68b36c99fe54b3c607a4b47f2c4"
    "gradle/libs.versions.toml|2b71b74fed5406a610f5ca4f1d0696badc6f8979"
    "platform/source-android/build.gradle.kts|5d0625871da8a35c35d69b13bced6debbc049769"
    "platform/source-android/src/main/AndroidManifest.xml|19d2638ebe5f6e89538ea336a8ea20b0b7915dc8"
    "platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/AndroidSourceMedia.kt|4ddc7c4297090bb4dd27805c080698e3bc034201"
    "platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceAcquisitionClient.kt|8ffb09a738a15837004db579f762c9125678ce65"
    "platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceAcquisitionWorker.kt|2cf408337c704a1d4c82c937db7b1a4e29335aae"
    "platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceDiscovery.kt|4df4131da5f0f47c32b727277af3684fe256a638"
    "platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/SourceOperationStore.kt|cdf76b039e26fbe863ab97fe55bbfc44ad888ecc"
    "platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/YtDlpRuntime.kt|dac7d0e8d740ccdafdb8d499defa7c64f8a2a465"
    "platform/source-android/src/main/kotlin/studio/guitarlab/platform/source/android/YtDlpSourceDownloader.kt|7122a460ebe4ff2c533ed83c6aac2ac347409d18"
    "settings.gradle.kts|a82db953d26c129f7a5e9b48c16e9c58e646d16e"
)

hash_file() { git -C "$ROOT" hash-object "$1"; }
u3_ready() {
    local entry relative expected
    for entry in "${U3_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

PATCH_PARTS=("${PATCH_B64_PREFIX}"*)
[[ ${#PATCH_PARTS[@]} -gt 0 && -f "${PATCH_PARTS[0]}" ]] || { echo "Missing U3 source patch payload parts" >&2; exit 1; }

if u3_ready; then
    echo "Source patch chain already materialized through U3"
    exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U2d materializer: $PREVIOUS" >&2; exit 1; }
bash "$PREVIOUS"

TMP_GZ="$(mktemp)"
TMP_PATCH="$(mktemp)"
TMP_B64="$(mktemp)"
trap 'rm -f "$TMP_B64" "$TMP_GZ" "$TMP_PATCH"' EXIT
cat "${PATCH_PARTS[@]}" > "$TMP_B64"
[[ "$(sha256sum "$TMP_B64" | awk '{print $1}')" == "$PATCH_B64_SHA256" ]] || {
    echo "U3 source patch payload SHA-256 mismatch" >&2
    exit 1
}
base64 --decode "$TMP_B64" > "$TMP_GZ"
[[ "$(sha256sum "$TMP_GZ" | awk '{print $1}')" == "$PATCH_GZ_SHA256" ]] || {
    echo "U3 compressed source patch SHA-256 mismatch" >&2
    exit 1
}
gzip -dc "$TMP_GZ" > "$TMP_PATCH"
[[ "$(sha256sum "$TMP_PATCH" | awk '{print $1}')" == "$PATCH_SHA256" ]] || {
    echo "U3 decompressed source patch SHA-256 mismatch" >&2
    exit 1
}
patch --dry-run -p1 -d "$ROOT" < "$TMP_PATCH" >/dev/null
patch --batch --forward -p1 -d "$ROOT" < "$TMP_PATCH"
u3_ready || { echo "U3 applied but final hashes do not match" >&2; exit 1; }
echo "Source patch chain materialized through U3 with verified final hashes"
