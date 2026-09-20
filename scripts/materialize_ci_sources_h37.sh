#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_h29_h33.sh"
H37_PARTS=(
    "$ROOT/.source-parts/H37DriveV3Backup.patch.gz.b64.part00"
    "$ROOT/.source-parts/H37DriveV3Backup.patch.gz.b64.part01"
    "$ROOT/.source-parts/H37DriveV3Backup.patch.gz.b64.part02"
    "$ROOT/.source-parts/H37DriveV3Backup.patch.gz.b64.part03"
    "$ROOT/.source-parts/H37DriveV3Backup.patch.gz.b64.part04"
)
H37_ARCHIVE_SHA256="253953752a421a5b2299a040024c7f897940cfaa7de17aec914bcc2793ce9000"
H37_PATCH_SHA256="d4d7da1d1a097c9451644d78b92cc10dae6faf35b523b0339cce906ab01007a2"

H37_CHECKS=(
    "app/build.gradle.kts|e725ebf5dc8f225422b0b9616e3c2082ab11ede5"
    "app/src/androidTest/java/studio/guitarlab/app/BackupScreenInstrumentedTest.kt|6b6c0ef5fe11e3c796dcd32bfca833783f390699"
    "app/src/main/AndroidManifest.xml|790f061d61ba77306e8c5f41bc18611b49959de8"
    "app/src/main/java/studio/guitarlab/app/backup/AutomaticBackupWorker.kt|7df2deabe332b7aacc947174d33bfef95ec6b333"
    "app/src/main/java/studio/guitarlab/app/backup/BackupScheduler.kt|c840e4ed2cd5dd2442377d30137d657dd186a4d8"
    "app/src/main/java/studio/guitarlab/app/backup/BackupScreen.kt|f52417280bc8aef5c1733ee7509d037a57b90229"
    "app/src/main/java/studio/guitarlab/app/backup/BackupSettingsStore.kt|706e2e34b19b01b9543394626236756fe1636d54"
    "app/src/main/java/studio/guitarlab/app/backup/BackupViewModel.kt|e19efc16b57ebc1938dca9852e06b92952fc5d12"
    "app/src/main/java/studio/guitarlab/app/backup/DriveAuthorization.kt|e52728ea9c177327177a370b7792f46f40299d7f"
    "app/src/main/java/studio/guitarlab/app/backup/DriveBackupStateStore.kt|a6530240235b1aa90e616f499fbff040bb5ac6ab"
    "app/src/main/java/studio/guitarlab/app/backup/DriveV3BackupRemoteStore.kt|7fc287421c15a1c9f25ec81784f618409db93272"
    "app/src/main/java/studio/guitarlab/app/backup/DriveV3Protocol.kt|4743991632f29e95d1c9c4230a4cbbbbf9de82e7"
    "app/src/main/java/studio/guitarlab/app/backup/LegacySafBackupMigrator.kt|69f313ff9a1d6d39207ae9e1c46dc28bb143b921"
    "app/src/main/res/xml/backup_rules.xml|27f920406bc5933c4ea83265b8b38acd1b5c75cd"
    "app/src/main/res/xml/data_extraction_rules.xml|c17a585891d9fc3067ec41e72863dfce53b6ac0b"
    "app/src/test/java/studio/guitarlab/app/backup/DriveV3BackupRemoteStoreTest.kt|6fc055d9ea5777f790a5203ba85edc010009d492"
    "app/src/test/java/studio/guitarlab/app/backup/DriveV3ProtocolTest.kt|c43b11f3c550ff9f9431ea3be256e067ca4ce162"
    "core/project/src/main/kotlin/studio/guitarlab/core/project/ProjectBundleWriter.kt|923b3e4e7cb089793281b8851af64b363670534f"
    "core/project/src/test/kotlin/studio/guitarlab/core/project/ProjectBundleWriterTest.kt|a27d06d3f2002901bb9a6e8023c83d3ec6bdebef"
    "gradle/libs.versions.toml|ffb4a90fa2b00de4c96cbbd18ef2e3f94c1904df"
)

hash_file() { git -C "$ROOT" hash-object "$1"; }

h37_ready() {
    local entry relative expected
    for entry in "${H37_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

decode_h37_patch() {
    local output="$1" encoded archive actual_archive part
    encoded="$(mktemp)"
    archive="$(mktemp)"
    trap 'rm -f "$encoded" "$archive"' RETURN
    : > "$encoded"
    for part in "${H37_PARTS[@]}"; do
        [[ -f "$part" ]] || { echo "Missing H37 source archive part: $part" >&2; return 1; }
        cat "$part" >> "$encoded"
    done
    base64 -d "$encoded" > "$archive"
    gzip -t "$archive"
    actual_archive="$(sha256sum "$archive" | awk '{print $1}')"
    [[ "$actual_archive" == "$H37_ARCHIVE_SHA256" ]] || {
        echo "H37 source archive SHA-256 mismatch: $actual_archive" >&2
        return 1
    }
    gzip -dc "$archive" > "$output"
    rm -f "$encoded" "$archive"
    trap - RETURN
}

verify_h37_patch() {
    local decoded actual
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_h37_patch "$decoded"
    actual="$(sha256sum "$decoded" | awk '{print $1}')"
    [[ "$actual" == "$H37_PATCH_SHA256" ]] || {
        echo "H37 source patch SHA-256 mismatch: $actual" >&2
        return 1
    }
    rm -f "$decoded"
    trap - RETURN
}

apply_h37() {
    local decoded
    decoded="$(mktemp)"
    trap 'rm -f "$decoded"' RETURN
    decode_h37_patch "$decoded"
    [[ "$(sha256sum "$decoded" | awk '{print $1}')" == "$H37_PATCH_SHA256" ]] || {
        echo "H37 source patch changed between verify and apply." >&2
        return 1
    }
    patch --dry-run -p1 -d "$ROOT" < "$decoded" >/dev/null
    patch --batch --forward -p1 -d "$ROOT" < "$decoded"
    rm -f "$decoded"
    trap - RETURN
}

verify_h37_patch

if h37_ready; then
    echo "Source patch chain already materialized through H37"
    exit 0
fi

[[ -x "$PREVIOUS" ]] || { echo "Missing H36c materializer: $PREVIOUS" >&2; exit 1; }
bash "$PREVIOUS"
apply_h37
h37_ready || { echo "H37 applied but final H37 hashes do not match." >&2; exit 1; }
echo "Source patch chain materialized through H37 with verified final hashes"
