# U4 real-cloud gate

The canonical U4 cloud integration gate is `.github/workflows/u4-cloud-integration-smoke.yml`.

It deliberately uses the production `gbw-demucs` Cloud Run Job and the same keyless Workload Identity Federation plane as the proven GBW RC5 workflow. It creates a unique synthetic namespace, executes a real Demucs separation, validates the exact six-stem manifest/checksum/model contract, proves worker source cleanup, then validates post-publication remote purge idempotency and cleans its namespace.

The WIF provider must explicitly trust the immutable GuitarLab repository id `1361533070`, owner id `216095257`, branch `main`, and this exact workflow ref. Run `scripts/bootstrap_u4_github_wif.sh` once from an authenticated owner Cloud Shell if the provider still trusts GBW only. The script preserves the existing GBW workflow trust and adds only this GuitarLab workflow.

A green ordinary Android CI is not a substitute for this gate. U4 becomes DIGITAL PASS only after both CI #681-equivalent software/API36 evidence and this real-cloud workflow are green on the applicable U4 source.

## Digital closure — 2026-09-21

U4 is closed as **DIGITAL PASS** on exact source `55ae7a14d99b710367ea7650cbf9f48298b90eba`.

Canonical evidence:
- GuitarLab Android CI #686 / run `35620101527`: PASS;
- U4 Cloud Integration Smoke #8 / run `35620101684`: PASS;
- both runs were produced from the same exact U4 source commit;
- the final U4 commit hardens lost-response and process-death recovery while preserving fail-closed ownership/integrity publication semantics.

No U5 implementation is included in this closure.
