# HISTORY

> Append-only audit log. Written every iteration, **never read during Orient** — it exists for a
> human auditing the run, not for the engine deciding what to do next. Keeping it out of the read
> path is what stops orientation cost from growing with the run.

<!-- Newest first. One entry per iteration. -->

### Iteration 1 - 2026-10-09 (Bootstrap)
- **Phase:** none (nothing implemented)
- Attempted: read `PRD.md` (Run 5), the repository's build files, `PROJECT.md` and the Foreman rules; classified the repository (product); checked what is published (Google Maven and Maven Central metadata, the AGP 9.4 and built-in-Kotlin pages); captured base evidence (`aapt2 dump badging` of the debug APK built from the base app code, the 231 test names, the catalog's 68 coordinates and plugins); created `loop/kanso-agp9-toolchain` from `76ecb97`; wrote the DoD (17 criteria, 3 removals), the plan (3 tasks, 3 Phases), the task files, six assumptions and D-001; had one independent read-only critique of the draft.
- Critique: one Capable read-only analyst reviewed the first draft (decomposition, proofs, classes, AGP traps). Adopted: greps exclude `.harness` (the run's own evidence quotes the old build files and would match forever); the KSP grep matches a comment in `core/localml`, so it looks for plugin and `ksp(` uses; Kotlin 2.3.20 is a recorded pin, so DoD 9 and R3 move it to the newest stable in Phase 2 and check that the compiling Kotlin is the catalog's; the nine module scripts go to a Capable Worker because the Iteration runs at Fast; release builds also at Phase 1; NDK 27.0.12077973 and the native library list are compared with base; per-phase comparison images only from this run's results; the recorder is limited to the screenshot tests; the beta BOM question went to D-001. Not adopted: dropping the device criterion (kept, but without the Run 3 and 4 items, which the PRD excludes).
- Learned: the stable Compose BOMs map Material3 to 1.4.0, but `compose-bom-beta:2026.10.00` maps 1.5.0-beta01 (and all of Compose to 1.13.0-beta01), a fact found only because the critique asked for the check; Material3 1.5.0-beta01 itself needs only Compose 1.12.0; AGP 9.4 needs Gradle 9.6.0; the wrapper at base is 8.14.3, not 8.13 as PROJECT.md said; nothing in the build uses KSP; the base debug APK used NDK 27.0.12077973.
- Reconciled: knowledge update (PROJECT.md corrections and one Constraint), assumptions A-001 to A-006, queued decision D-001 (DoD approval, the beta-BOM choice and the `## Baselines` list).
- Working-tree note: `PRD.md` was modified and uncommitted when the run started (the human's edit for Run 5); committed unchanged with the bootstrap checkpoint.

## Archived Decisions

<!-- Full request + decision + rationale of every consumed Decision Queue entry. -->
