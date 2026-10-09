# AMENDMENTS

> Every plan mutation and every consumed decision, newest last. Audit only: never read during Orient.

### Iteration 2 - 2026-10-09 - D-001 applied (decision, not a plan change)

- **Answer:** "Approved as written (Option 1), 2026-10-09": the DoD's 17 criteria and 3 removals with their Verification Classes; the stable Compose BOM 2026.09.00 with Material3 1.5.0-beta01 pinned in the app above it (not the beta BOM); the release builds, the Galaxy A13 check and AGP 9 on its defaults stay in. Q1-Q3 stay as the DoD says, left for a later run.
- **Baselines:** the 16 paths under `## Baselines` in `DECISIONS.md` (`app/src/test/screenshots/{1,2,3,4,5,6,10,11,12,13,14,15,16,17,18,19}_*.png`) are the pictures the engine may re-record, under the DoD's conditions (after a library move only, never in Phase 1; the `_compare.png` kept in `evidence/baseline-diffs/` with a README line naming the library change that moved it).
- **Effect:** T-001, T-002 and T-003 are unblocked; the plan is unchanged (no re-cut: the beta BOM and the dropped-criteria options were not chosen). A-001 to A-006 stand as taken.

### Iteration 2 - 2026-10-09 - Phase 1 findings (plan notes, no scope change)

- **Tier 3 (assumption A-007), no DoD text changed:** the four empty wrapper modules `core/editimage`, `overlaybrush`, `imageprocessor`, `localml` got their own `namespace` in their build scripts (`com.phongkaster.kanso.engine.<module>`). DoD 11 expected a namespace clash to make criterion 11 "unmet, reported", on the premise that fixing it changes an engine source file; the wrappers have no `src/`, so no source changed, and the app could not build without it. Criterion 11 is listed as assumed in `STATE.md`; `DoD.md` is untouched.
- **Tier 2 (assumption A-008):** `core/localml` declares `litert-support-api` (same version) instead of the empty `litert-support`.
- **Tier 1:** PLAN "Known Risks" corrected (the wrappers have no source). T-001's Declared File Scope held; every file written is inside it (nine module scripts by the Worker, the catalog, root, app and wrapper files by the Iteration, `opencv/build.gradle` also by the Iteration for the NDK pin and the Kotlin source directory after review). The wrapper was regenerated after the AGP bump and the module fixes, not before as the task text said (a `wrapper` run configures every project, and AGP 8.13 cannot run on Gradle 9.6.0): `PROJECT.md` records the order that works.
