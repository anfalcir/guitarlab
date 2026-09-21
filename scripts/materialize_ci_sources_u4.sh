#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
bash "$ROOT/scripts/materialize_ci_sources_through_h28.sh"
bash "$ROOT/scripts/materialize_ci_sources_u3.sh"
U4_PATCH="$ROOT/.source-parts/U4SeparationIntegration.patch.gz"
echo "0753676239a38fbe22a21655eecc2416964fd2aa52c7c4e8f19058be063d978d  $U4_PATCH" | sha256sum -c -
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
