#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u1b.sh"
U2_PATCH="$ROOT/.source-parts/U2UnifiedProjectShell.patch"
U2_PATCH_SHA256="3f0430452c096a4666b7ccf07c3ef79598508140f22e9e1afc40739cf5c629ed"

U2_CHECKS=(
    "app/src/main/java/studio/guitarlab/app/ui/AppScreen.kt|870854eb873c648f40a3e31e125bd9a87fdbff04"
    "app/src/main/java/studio/guitarlab/app/ui/GuitarLabApp.kt|a559ead4d34ee2432f9875d85a0eaa948586a24a"
    "app/src/main/java/studio/guitarlab/app/ui/HomeScreen.kt|71263db275adb2322cca682baba8f8f92dbf40e4"
    "app/src/main/java/studio/guitarlab/app/ui/HomeViewModel.kt|90ec08c0a410fe9ffb9c2538093774895d716e14"
    "app/src/main/java/studio/guitarlab/app/ui/NewProjectScreen.kt|c34855666ccaf921c809676e03c062ae8b7c4b13"
    "app/src/test/java/studio/guitarlab/app/ui/AppRouteCodecTest.kt|fa7bdd12fea1e73d4a7c132934b5f456948f2cd5"
    "app/src/main/java/studio/guitarlab/app/ui/UnifiedProjectWorkspaceScreen.kt|a0e2ccfb1858978db206a175e397eca45638c133"
    "app/src/test/java/studio/guitarlab/app/ui/UnifiedProjectShellPolicyTest.kt|2955c26db08e6ea0a47ac29bfb742f6c16d69b46"
    "app/src/androidTest/java/studio/guitarlab/app/UnifiedProjectShellInstrumentedTest.kt|882d896e9168cdf6877cb4833b57c8c6f54b6ff0"
)

hash_file() { git -C "$ROOT" hash-object "$1"; }
u2_ready() {
    local entry relative expected
    for entry in "${U2_CHECKS[@]}"; do
        relative="${entry%%|*}"
        expected="${entry#*|}"
        [[ -f "$ROOT/$relative" ]] || return 1
        [[ "$(hash_file "$ROOT/$relative")" == "$expected" ]] || return 1
    done
}

[[ -f "$U2_PATCH" ]] || { echo "Missing U2 source patch: $U2_PATCH" >&2; exit 1; }
[[ "$(sha256sum "$U2_PATCH" | awk '{print $1}')" == "$U2_PATCH_SHA256" ]] || {
    echo "U2 source patch SHA-256 mismatch" >&2
    exit 1
}

if u2_ready; then
    echo "Source patch chain already materialized through U2"
    exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U1b materializer: $PREVIOUS" >&2; exit 1; }
bash "$PREVIOUS"
patch --dry-run -p1 -d "$ROOT" < "$U2_PATCH" >/dev/null
patch --batch --forward -p1 -d "$ROOT" < "$U2_PATCH"
u2_ready || { echo "U2 applied but final hashes do not match" >&2; exit 1; }
echo "Source patch chain materialized through U2 with verified final hashes"
