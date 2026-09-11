#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

command -v java >/dev/null || { echo "Java 17 não encontrado." >&2; exit 1; }
command -v gradle >/dev/null || { echo "Gradle 9.6.1 não encontrado." >&2; exit 1; }

GRADLE_VERSION="$(gradle --version | sed -n 's/^Gradle //p' | head -n1)"
[[ "$GRADLE_VERSION" == "9.6.1" ]] || { echo "Gradle 9.6.1 obrigatório; encontrado: $GRADLE_VERSION" >&2; exit 1; }

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
[[ -n "$ANDROID_SDK_ROOT" && -f "$ANDROID_SDK_ROOT/platforms/android-36/android.jar" ]] || {
  echo "Android SDK Platform 36 não encontrado em ANDROID_SDK_ROOT/ANDROID_HOME." >&2
  exit 1
}

chmod +x scripts/materialize_ci_sources.sh
scripts/materialize_ci_sources.sh

gradle --no-daemon --console=plain test lint assembleDebug

if [[ "${SIGNED_HOMOLOGATION:-false}" == "true" ]]; then
  : "${GUITARLAB_KEYSTORE_PATH:?Defina GUITARLAB_KEYSTORE_PATH}"
  : "${GUITARLAB_KEYSTORE_PASSWORD:?Defina GUITARLAB_KEYSTORE_PASSWORD}"
  : "${GUITARLAB_KEY_ALIAS:?Defina GUITARLAB_KEY_ALIAS}"
  : "${GUITARLAB_KEY_PASSWORD:?Defina GUITARLAB_KEY_PASSWORD}"
  gradle --no-daemon --console=plain assembleRelease
fi
