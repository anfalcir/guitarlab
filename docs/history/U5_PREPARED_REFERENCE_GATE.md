# U5 prepared-reference / zero-copy Studio gate

U5 makes the six authoritative Demucs stems directly usable by the Studio without a user-visible export/reimport round-trip.

The implementation renders the deterministic five-stem backing (all authoritative stems except guitar), binds the isolated guitar as the reference, derives managed Guitar L/R references under the existing stereo policy, and publishes all references inside the project-managed media store. A recipe fingerprint makes retry/reopen idempotent. A newly prepared reference set never silently replaces an already edited Studio session: the user explicitly chooses whether to keep the currently bound references or update the Studio.

The final coverage closure also proves save/reopen, project-history Undo/Redo of the reference rebind, master-render input resolution after rebind, preservation of an existing recording, and the Prepare → Studio handoff in API 36 instrumentation.

## Digital closure — 2026-09-21

U5 is closed as **DIGITAL PASS** on exact source `4bada624e68f8a55ff22830e1dc276c8d51af223`.

Canonical evidence:
- GuitarLab Android CI #690 / run `35625349001`: PASS;
- deterministic U5c source materialization: PASS;
- JVM/unit suite: **378 tests, 0 failures, 0 errors, 0 skipped**;
- Android Lint: PASS with **0 errors**;
- debug APK assembly: PASS;
- standard API 36 instrumented regression: **38/38 PASS**;
- isolated target-tablet geometry regression: **1/1 PASS**;
- the new API 36 coverage explicitly exercised `preparedReferencesAreVisibleAndStudioHandoffRemainsAvailable`;
- the exact-source artifacts are retained by the canonical run (source snapshot, software gate, Android integration reports).

Roadmap U5 requirements covered by this gate:
- deterministic backing/reference render with shared gain and alignment checks;
- managed zero-copy Studio binding plus optional derived Guitar L/R;
- explicit keep/update reference flow;
- idempotent retry/reopen and fail-closed corrupt-stem handling;
- reference rebind Undo/Redo;
- recordings/takes remain unchanged by reference updates;
- save/reopen persistence;
- master/export render request after rebind resolves the new prepared reference while retaining recorded material;
- Prepare → Studio surface/handoff.

No U6 implementation is included in this closure.
