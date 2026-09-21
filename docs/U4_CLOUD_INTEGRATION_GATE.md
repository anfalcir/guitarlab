# U4 real-cloud gate

The canonical U4 cloud integration gate is `.github/workflows/u4-cloud-integration-smoke.yml`.

It deliberately uses the production `gbw-demucs` Cloud Run Job and the same keyless Workload Identity Federation plane as the proven GBW RC5 workflow. It creates a unique synthetic namespace, executes a real Demucs separation, validates the exact six-stem manifest/checksum/model contract, proves worker source cleanup, then validates post-publication remote purge idempotency and cleans its namespace.

The WIF provider must explicitly trust the immutable GuitarLab repository id `1361533070`, owner id `216095257`, branch `main`, and this exact workflow ref. Run `scripts/bootstrap_u4_github_wif.sh` once from an authenticated owner Cloud Shell if the provider still trusts GBW only. The script preserves the existing GBW workflow trust and adds only this GuitarLab workflow.

A green ordinary Android CI is not a substitute for this gate. U4 becomes DIGITAL PASS only after both CI #681-equivalent software/API36 evidence and this real-cloud workflow are green on the applicable U4 source.
