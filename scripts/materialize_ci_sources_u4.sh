#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
bash "$ROOT/scripts/materialize_ci_sources_through_h28.sh"
bash "$ROOT/scripts/materialize_ci_sources_u3.sh"
U4_PATCH="$ROOT/.source-parts/U4SeparationIntegration.patch.gz"
echo "d6af8e0e2e9ad440654e50729a949a40e3ad58d8a0c454fda2a3dae5279dfc66  $U4_PATCH" | sha256sum -c -
if gzip -dc "$U4_PATCH" | patch -p1 -R --dry-run -d "$ROOT" >/dev/null 2>&1; then
  : # already materialized
else
  gzip -dc "$U4_PATCH" | patch -p1 --forward -d "$ROOT"
fi
grep -q 'include(":core:separation")' "$ROOT/settings.gradle.kts" || printf '\ninclude(":core:separation")\ninclude(":platform:separation")\n' >> "$ROOT/settings.gradle.kts"
grep -q 'project(":platform:separation")' "$ROOT/app/build.gradle.kts" || python3 - "$ROOT/app/build.gradle.kts" <<'PY'
from pathlib import Path
import sys
p=Path(sys.argv[1]); s=p.read_text(); needle="dependencies {"
if needle not in s: raise SystemExit("app dependencies block not found")
p.write_text(s.replace(needle, needle+'\n    implementation(project(":core:separation"))\n    implementation(project(":platform:separation"))',1))
PY
echo "Source chain materialized through U4 remote separation"
