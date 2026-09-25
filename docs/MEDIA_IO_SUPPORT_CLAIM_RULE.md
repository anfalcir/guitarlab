# Media I/O support-claim rule

Updated: 2026-09-24

Codec support claims follow `CODEC_SUPPORT_MATRIX.md` plus the current exact-source automated gate. A code path alone never establishes Android support.

Where capability depends on the physical device/codec stack, the support level remains device-gated until representative evidence exists. Repeat that evidence only when the codec path/device changes or the capability is deliberately included in a new frozen baseline. Implemented formats do not imply universal-input support or a duty to expand the matrix.
