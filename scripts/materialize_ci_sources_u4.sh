#!/usr/bin/env bash
set -euxo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
bash -x "$ROOT/scripts/materialize_ci_sources_u3.sh"
grep -q 'include(":core:separation")' "$ROOT/settings.gradle.kts" || printf '\ninclude(":core:separation")\ninclude(":platform:separation")\n' >> "$ROOT/settings.gradle.kts"
grep -q 'project(":platform:separation")' "$ROOT/app/build.gradle.kts" || python3 - "$ROOT/app/build.gradle.kts" <<'PY'
from pathlib import Path
import sys
p=Path(sys.argv[1]); s=p.read_text(); needle="dependencies {"
if needle not in s: raise SystemExit("app dependencies block not found")
p.write_text(s.replace(needle, needle+'\n    implementation(project(":core:separation"))\n    implementation(project(":platform:separation"))',1))
PY
echo "Source chain materialized through U4 remote separation"
