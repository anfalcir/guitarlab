#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
bash "$ROOT/scripts/materialize_ci_sources_u3.sh"
grep -q 'include(":core:separation")' "$ROOT/settings.gradle.kts" || cat >> "$ROOT/settings.gradle.kts" <<'EOF'

include(":core:separation")
include(":platform:separation")
EOF
grep -q 'project(":platform:separation")' "$ROOT/app/build.gradle.kts" || python3 - "$ROOT/app/build.gradle.kts" <<'PY'
from pathlib import Path
import sys
p=Path(sys.argv[1]); s=p.read_text()
needle="dependencies {"
if needle not in s: raise SystemExit("app dependencies block not found")
s=s.replace(needle, needle+'\n    implementation(project(":core:separation"))\n    implementation(project(":platform:separation"))',1)
p.write_text(s)
PY
test -f "$ROOT/core/separation/src/main/kotlin/studio/guitarlab/core/separation/RemoteSeparation.kt"
test -f "$ROOT/platform/separation/src/main/kotlin/studio/guitarlab/platform/separation/FirebaseRemoteBackend.kt"
echo "Source chain materialized through U4 remote separation"
