# Media I/O support-claim rule

Updated: 2026-09-20

Codec support claims follow `CODEC_SUPPORT_MATRIX.md` plus the current exact-source automated gate. A code path alone never establishes Android support.

Where capability depends on the physical device/codec stack (for example an encoder exposed by the target Android build), the support level remains device-gated until representative target-device evidence exists. Historical alpha13 checklists are evidence only and are not current authority.