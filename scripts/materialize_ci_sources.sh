#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
bash "$ROOT/scripts/materialize_ci_sources_through_h28.sh"
bash "$ROOT/scripts/materialize_ci_sources_h37b.sh"
echo "Source patch chain materialized through H37b with verified final hashes"
