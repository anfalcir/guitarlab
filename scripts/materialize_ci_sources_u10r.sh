#!/usr/bin/env bash
# U10r corrective seal: make the U10q patch diff-clean while preserving fail-closed materialization.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PREVIOUS="$ROOT/scripts/materialize_ci_sources_u10q.sh"
PATCH="$ROOT/.source-parts/U10rPatchWhitespaceCorrective.patch"
PATCH_BLOB="6d4aa24747bc50e9ff96747273445cb3e052a170"

declare -a FILES=(
  ".source-parts/U10qScreenshotMediaStoreAndExecutionCoverage.patch"
  "scripts/materialize_ci_sources_u10q.sh"
)

declare -a HASHES=(
  "2cefcd881bf04e7295c78aeeae3c4e7f4e7a0e29"
  "c931667cb52fbdd0648348622cc2b23fe35cad2f"
)

verify_patch() {
  [[ -f "$PATCH" ]] || { echo "Missing U10r patch whitespace corrective" >&2; exit 1; }
  [[ "$(git -C "$ROOT" hash-object "$PATCH")" == "$PATCH_BLOB" ]] || {
    echo "U10r patch blob mismatch" >&2
    exit 1
  }
}

ready() {
  for i in "${!FILES[@]}"; do
    local file="${FILES[$i]}"
    [[ -f "$ROOT/$file" ]] || return 1
    [[ "$(git -C "$ROOT" hash-object "$ROOT/$file")" == "${HASHES[$i]}" ]] || return 1
  done
}

verify_semantics() {
  local u10q="$ROOT/scripts/materialize_ci_sources_u10q.sh"
  git -C "$ROOT" diff --check
  grep -q 'apply --unidiff-zero --check --reverse' "$u10q" || {
    echo "U10r zero-context reverse check missing" >&2; exit 1;
  }
  grep -q 'PATCH_BLOB="2cefcd881bf04e7295c78aeeae3c4e7f4e7a0e29"' "$u10q" || {
    echo "U10r corrected U10q patch seal missing" >&2; exit 1;
  }
}

if ready; then
  verify_patch
  verify_semantics
  git -C "$ROOT" apply --unidiff-zero --check --reverse "$PATCH"
  echo "Source patch chain already materialized through U10r patch whitespace corrective"
  exit 0
fi

[[ -f "$PREVIOUS" ]] || { echo "Missing U10q materializer" >&2; exit 1; }
verify_patch
bash "$PREVIOUS"
git -C "$ROOT" apply --unidiff-zero --check "$PATCH"
git -C "$ROOT" apply --unidiff-zero "$PATCH"
git -C "$ROOT" diff --check
ready || { echo "U10r final blob mismatch" >&2; exit 1; }
verify_semantics
git -C "$ROOT" apply --unidiff-zero --check --reverse "$PATCH"
echo "Source patch chain materialized through U10r patch whitespace corrective"
