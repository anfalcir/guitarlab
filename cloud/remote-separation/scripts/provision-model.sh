#!/usr/bin/env bash
set -euo pipefail
: "${GBW_BUCKET:?Set GBW_BUCKET}"
: "${GBW_MODEL_FILE:?Set GBW_MODEL_FILE to the controlled checkpoint}"
EXPECTED_BYTES=54855129
EXPECTED_SHA=09704f4ceae204e56e77d5eefd6ac71d7275be81fd507e6913371d59abcee856
test "$(stat -c%s "$GBW_MODEL_FILE")" = "$EXPECTED_BYTES"
test "$(sha256sum "$GBW_MODEL_FILE" | awk '{print $1}')" = "$EXPECTED_SHA"
gcloud storage cp "$GBW_MODEL_FILE" "gs://${GBW_BUCKET}/ggml-model-htdemucs-6s-f16.bin" --no-clobber
gcloud storage objects describe "gs://${GBW_BUCKET}/ggml-model-htdemucs-6s-f16.bin" --format='value(size)'
