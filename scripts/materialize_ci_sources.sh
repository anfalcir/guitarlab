#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
bash "$ROOT/scripts/materialize_ci_sources_through_h28.sh"
bash "$ROOT/scripts/materialize_ci_sources_u3.sh"
echo "Source patch chain materialized through U3 with verified final hashes"
