#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_through_h25.sh"
H26_ARCHIVE_SHA256="20e58a470a2a3a720261699b0d0c4106617cf374455aaa1a6326e56615733f7e"
H26A_PATCH_PART="$ROOT/.source-parts/H26aSafCopyPackageContract.patch.b64"
H26A_PATCH_SHA256="ceb3c686f6bb85d2bbeaa47b7cee24650c64ce0cdcc1cc728a253ddfc7ab78ad"
H26A_SAF_HASH="caedc69c794a041653fae906b12e532e3e1b0498"
H26B_PATCH_PART="$ROOT/.source-parts/H26bBackupScreenTestCompat.patch.b64"
H26B_PATCH_SHA256="5a625e9cf034f39868d0a1db94d937e81b7187a73911b8c74fee8287fbf4ec67"
H26B_TEST_HASH="a4ef2e752d27d7a9d7e80324029f3f3044b8241c"
H26C_PATCH_PART="$ROOT/.source-parts/H26cBackupScreenScroll.patch.b64"
H26C_PATCH_SHA256="0a5668d61e8e1f1a95a1ae424511e79506cd987347bf4701690e180eb18b2c51"
H26C_TEST_HASH="953ec6dd18bb71354bbf9c72c16f8c28bbc14eee"

hash_file() { git -C "$ROOT" hash-object "$1"; }

H26_CHECKS=(
    "app/build.gradle.kts|a1ced47c3dfa807daeb5c0bd9fd8c8fca230478a"
    "gradle/libs.versions.toml|48fcd02b43f98d48a1516c1b4f92cce75a98e5ca"
    "app/src/main/AndroidManifest.xml|d5078963c606cb23df8decb673b2d6f7c80c9135"
    "app/src/main/java/studio/guitarlab/app/GuitarLabApplication.kt|98a43d965bf5653c2f8c43122e427006ec21005c"
    "app/src/main/java/studio/guitarlab/app/backup/AutomaticBackupWorker.kt|92c63ffa423f5ce061f0e1fb74a101574c44d7d7"
    "app/src/main/java/studio/guitarlab/app/backup/BackupOperationLock.kt|d1c5f2f57d045bc792143fa875145f13fd653ea6"
    "app/src/main/java/studio/guitarlab/app/backup/BackupScheduler.kt|b2b9bc13523f5162ef25292c29da2472ac766da5"
    "app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt|c32a1ab589765b7651806b2322df2a9efc91580c"
    "app/src/main/java/studio/guitarlab/app/backup/BackupSettingsStore.kt|96471013c70a8a0effc5d470181f899d136c0318"
    "app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt|27cbfa75eaa3a23f18ccc08b95da90ac44a0dbd8"
    "app/src/main/java/studio/guitarlab/app/backup/SafBackupRemoteStore.kt|bf22cb619a5ad3f06e9b98ed2b90d7cfac5b7a67"
    "app/src/main/java/studio/guitarlab/app/ui/AppScreen.kt|cba54660ce053ce3fc7c26128ea5331dbc0aa048"
    "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt|75ea1b1b7738a07056578c0e3c274a257a14ab69"
    "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt|3e46d960ebd30ba5a71dc1af9f5b141cbdd9f471"
    "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt|1e7b66fc53cea59b3df610f791bdb3a06563422d"
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt|4fa38b1c4a4472bbefd546cf4973489272a3a795"
    "app/src/main/java/studio/guitarlab/app/ui/StudioUserGuideDialog.kt|e0446da2346648496569811fa9a8c2b95b456012"
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt|4867f0d506e797bf05740502711d40c594d295e9"
    "app/src/test/java/studio/guitarlab/app/ui/AppRouteCodecTest.kt|0d6641a25738fe017cd1cee8904b4151cd8cf460"
    "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt|edebaf27a7b0c712485bec6eda10a90377234bec"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/BackupDomain.kt|9dcda4799cea4bd3a620909cf8d306c5f681923d"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectBackupCoordinator.kt|89565858077c9d6ff5f0e0c67d8cf4fddd87b6f9"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/BackupDomainTest.kt|79a01d2fa4f76c4cbbf7539e1228ccd6c65d0bb3"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/ProjectBackupCoordinatorTest.kt|7b5db25e8c29903b64656299f670ade8f05cb613"
)

h26_ready() {
    local entry relative expected
    for entry in "${H26_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

h26a_ready() {
    local entry relative expected
    for entry in "${H26_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        if [[ "$relative" == "app/src/main/java/studio/guitarlab/app/backup/SafBackupRemoteStore.kt" ]]; then
            expected="$H26A_SAF_HASH"
        fi
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

h26b_ready() {
    local entry relative expected
    for entry in "${H26_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        if [[ "$relative" == "app/src/main/java/studio/guitarlab/app/backup/SafBackupRemoteStore.kt" ]]; then
            expected="$H26A_SAF_HASH"
        elif [[ "$relative" == "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt" ]]; then
            expected="$H26B_TEST_HASH"
        fi
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

h26c_ready() {
    local entry relative expected
    for entry in "${H26_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        if [[ "$relative" == "app/src/main/java/studio/guitarlab/app/backup/SafBackupRemoteStore.kt" ]]; then
            expected="$H26A_SAF_HASH"
        elif [[ "$relative" == "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt" ]]; then
            expected="$H26C_TEST_HASH"
        fi
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

decode_h26c_patch() {
    local output="$1"
    [[ -f "$H26C_PATCH_PART" ]] || { echo "Missing H26c patch part: $H26C_PATCH_PART" >&2; return 1; }
    base64 -d "$H26C_PATCH_PART" > "$output"
}

verify_h26c_patch() {
    local decoded actual
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_h26c_patch "$decoded"
    actual="$(sha256sum "$decoded" | awk '{print $1}')"
    [[ "$actual" == "$H26C_PATCH_SHA256" ]] || {
        echo "H26c source patch SHA-256 mismatch: $actual" >&2
        return 1
    }
    rm -f "$decoded"
    trap - RETURN
}

apply_h26c() {
    local decoded
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_h26c_patch "$decoded"
    patch --dry-run -p1 -d "$ROOT" < "$decoded" >/dev/null
    patch --batch --forward -p1 -d "$ROOT" < "$decoded"
    rm -f "$decoded"
    trap - RETURN
}

decode_h26b_patch() {
    local output="$1"
    [[ -f "$H26B_PATCH_PART" ]] || { echo "Missing H26b patch part: $H26B_PATCH_PART" >&2; return 1; }
    base64 -d "$H26B_PATCH_PART" > "$output"
}

verify_h26b_patch() {
    local decoded actual
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_h26b_patch "$decoded"
    actual="$(sha256sum "$decoded" | awk '{print $1}')"
    [[ "$actual" == "$H26B_PATCH_SHA256" ]] || {
        echo "H26b source patch SHA-256 mismatch: $actual" >&2
        return 1
    }
    rm -f "$decoded"
    trap - RETURN
}

apply_h26b() {
    local decoded
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_h26b_patch "$decoded"
    patch --dry-run -p1 -d "$ROOT" < "$decoded" >/dev/null
    patch --batch --forward -p1 -d "$ROOT" < "$decoded"
    rm -f "$decoded"
    trap - RETURN
}

decode_h26a_patch() {
    local output="$1"
    [[ -f "$H26A_PATCH_PART" ]] || { echo "Missing H26a patch part: $H26A_PATCH_PART" >&2; return 1; }
    base64 -d "$H26A_PATCH_PART" > "$output"
}

verify_h26a_patch() {
    local decoded actual
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_h26a_patch "$decoded"
    actual="$(sha256sum "$decoded" | awk '{print $1}')"
    [[ "$actual" == "$H26A_PATCH_SHA256" ]] || {
        echo "H26a source patch SHA-256 mismatch: $actual" >&2
        return 1
    }
    rm -f "$decoded"
    trap - RETURN
}

apply_h26a() {
    local decoded
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_h26a_patch "$decoded"
    patch --dry-run -p1 -d "$ROOT" < "$decoded" >/dev/null
    patch --batch --forward -p1 -d "$ROOT" < "$decoded"
    rm -f "$decoded"
    trap - RETURN
}

build_h26_archive() {
    local encoded="$1" archive="$2"
    cat \
        "$ROOT/.source-parts/H26SafCloudBackup.patch.gz.part00" \
        "$ROOT/.source-parts/H26SafCloudBackup.patch.gz.part01" \
        "$ROOT/.source-parts/H26SafCloudBackup.patch.gz.part02" \
        "$ROOT/.source-parts/H26SafCloudBackup.patch.gz.part03" \
        "$ROOT/.source-parts/H26SafCloudBackup.patch.gz.part04" \
        > "$encoded"
    base64 -d "$encoded" > "$archive"
    gzip -t "$archive"
    local actual
    actual="$(sha256sum "$archive" | awk '{print $1}')"
    [[ "$actual" == "$H26_ARCHIVE_SHA256" ]] || {
        echo "H26 source archive SHA-256 mismatch: $actual" >&2
        return 1
    }
}

verify_h26_archive() {
    local encoded archive
    encoded="$(mktemp)"
    archive="$(mktemp)"
    trap 'rm -f "$encoded" "$archive"' RETURN
    build_h26_archive "$encoded" "$archive"
    rm -f "$encoded" "$archive"
    trap - RETURN
}

apply_h26() {
    local encoded archive patch_file
    encoded="$(mktemp)"
    archive="$(mktemp)"
    patch_file="$(mktemp)"
    trap 'rm -f "$encoded" "$archive" "$patch_file"' EXIT
    build_h26_archive "$encoded" "$archive"
    gzip -dc "$archive" > "$patch_file"
    patch --dry-run -p1 -d "$ROOT" < "$patch_file" >/dev/null
    patch --batch --forward -p1 -d "$ROOT" < "$patch_file"
    rm -f "$encoded" "$archive" "$patch_file"
    trap - EXIT
}

verify_h26_archive
verify_h26a_patch
verify_h26b_patch
verify_h26c_patch

if h26c_ready; then
    echo "Source patch chain already materialized through H26c"
    exit 0
fi

if ! h26b_ready; then
    if ! h26a_ready; then
        if ! h26_ready; then
            [[ -x "$PREVIOUS" ]] || { echo "Missing H25 materializer: $PREVIOUS" >&2; exit 1; }
            bash "$PREVIOUS"
            apply_h26
            h26_ready || { echo "H26 applied but final H26 hashes do not match." >&2; exit 1; }
        fi
        apply_h26a
        h26a_ready || { echo "H26a applied but final H26a hashes do not match." >&2; exit 1; }
    fi
    apply_h26b
    h26b_ready || { echo "H26b applied but final H26b hashes do not match." >&2; exit 1; }
fi

apply_h26c
h26c_ready || { echo "H26c applied but final H26c hashes do not match." >&2; exit 1; }
echo "Source patch chain materialized through H26c with verified final hashes"
