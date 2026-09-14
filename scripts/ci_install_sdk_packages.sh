#!/usr/bin/env bash
set -euo pipefail

if (( $# == 0 )); then
  echo "No Android SDK packages requested."
  exit 0
fi

max_attempts="${SDKMANAGER_MAX_ATTEMPTS:-3}"
if ! [[ "$max_attempts" =~ ^[1-9][0-9]*$ ]]; then
  echo "SDKMANAGER_MAX_ATTEMPTS must be a positive integer." >&2
  exit 2
fi

license_answers="$(printf 'y\n%.0s' {1..20})"
attempt=1

while (( attempt <= max_attempts )); do
  echo "sdkmanager attempt ${attempt}/${max_attempts}: $*"
  if sdkmanager "$@" <<<"$license_answers"; then
    exit 0
  fi

  if (( attempt == max_attempts )); then
    echo "sdkmanager failed after ${max_attempts} attempts." >&2
    exit 1
  fi

  sleep_seconds=$((attempt * 3))
  echo "sdkmanager failed; retrying in ${sleep_seconds}s..." >&2
  sleep "$sleep_seconds"
  attempt=$((attempt + 1))
done
