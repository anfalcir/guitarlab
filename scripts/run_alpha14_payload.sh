#!/usr/bin/env bash
set -euo pipefail
python3 - <<'PY'
from pathlib import Path
source = Path('.github/workflows/alpha14-stereo-import-integration.yml').read_text()
start_marker = "          python3 - <<'PY'\n"
end_marker = "\n          PY\n      - name: Materialize split source"
start = source.index(start_marker) + len(start_marker)
end = source.index(end_marker, start)
raw = source[start:end]
out = []
in_triple = False
for line in raw.splitlines():
    if not in_triple and line.startswith("          "):
        line = line[10:]
    out.append(line)
    if line.count("'''") % 2 == 1:
        in_triple = not in_triple
code = "\n".join(out)
old = "t = rep(t, '                                onSplit = onSplitClip,\\n                                onReorderTrack', '                                onSplit = onSplitClip,\\n                                onSplitStereo = onSplitStereo,\\n                                onReorderTrack', 'ui lane split call')"
new = "t = rep(t, '                                onSplit = onSplitClip,\\n                                trackIndex = trackIndex,', '                                onSplit = onSplitClip,\\n                                onSplitStereo = onSplitStereo,\\n                                trackIndex = trackIndex,', 'ui lane split call')"
if old not in code:
    raise SystemExit('alpha14 executor could not locate stale lane anchor')
code = code.replace(old, new, 1)
compile(code, 'alpha14_payload.py', 'exec')
exec(code, {'__name__': '__main__'})
PY
chmod +x scripts/materialize_ci_sources.sh
scripts/materialize_ci_sources.sh
git diff --check
gradle --no-daemon --console=plain test
gradle --no-daemon --console=plain lint
gradle --no-daemon --console=plain assembleDebug
git config user.name "GuitarLab CI Integrator"
git config user.email "actions@users.noreply.github.com"
git rm -f \
  .github/workflows/alpha14-stereo-import-integration.yml \
  .github/workflows/alpha14-run-payload.yml \
  .github/workflows/alpha14-integrator.yml \
  scripts/run_alpha14_payload.sh \
  scripts/alpha14-trigger.txt
git add -A app core docs .github scripts
git commit -m "release: promote M5 alpha14 stereo ingress candidate [sign-homologation]"
git pull --rebase origin dev/parallel-m3-m5
git push origin HEAD:dev/parallel-m3-m5
