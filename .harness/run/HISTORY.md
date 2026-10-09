# HISTORY

> Append-only audit log. Written every iteration, **never read during Orient** — it exists for a
> human auditing the run, not for the engine deciding what to do next. Keeping it out of the read
> path is what stops orientation cost from growing with the run.

<!-- Newest first. One entry per iteration. -->

### Iteration 5 - 2026-10-09 (Verifier)
- **Phase:** none; the Verifier wrote no implementation, so no Worker or Reviewer was dispatched.
- Attempted: re-proved every `machine` criterion on `9fa9d89` with fresh evidence: the combined run `cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` exit 0 (`evidence/v-verify.log`, 5 min 10 s cold) and the greps and diffs of criteria 2, 3 (a to d), 7, 9, 10, 11, 15 (pre-check), 16, 17 and R1 to R6, all by hand under Bash. Then drove criterion 6's machine pre-check on the owner's Galaxy A13 (`-Serial R58W1120QVA`; two real phones attached, no emulator) from a fresh install: Home, permission dialog, gallery, tool, Save, Saved, Home, Settings.
- Verified: 231 tests, 0 failed, 0 errors, 0 skipped, the same 231 `@Test` at HEAD; the 22 test classes the DoD names each ran green; `verifyRoborazziDebug` 16 of 16 unchanged; the 198 base test names all exist at HEAD; no `@Ignore`; previews and KDoc tests byte-identical to base; the 6 removed assertion lines all come back as added lines; manifest, build files and `reference/` unchanged; no INTERNET or ACCESS_NETWORK_STATE in the merged manifest; the app's log on the phone shows no `FATAL EXCEPTION`.
- Not driven: after the Settings screen opened, a person pulled down the notification shade on the A13, so the Dark choice, Camera, Share and the picker drag were not driven and the drive stopped there. Criteria 6 and 15 stay unsigned.
- Learned: `Assert-DumpIsOurs` lets a dump of the notification shade through, because the shade is `com.android.systemui`, which it allows for the system bars; one dump captured the owner's notifications and was deleted before any commit (Constraint in `knowledge/PROJECT.md`, suggestion S-002). A gallery dump lists the owner's photo names, so raw device output is summarised, not kept. A `/tmp` directory was removed through `foreman-trash.ps1` (R-001; the denied plain `rm -rf` was not retried another way).
- Reconciled: A-002, A-003 and A-012 now name `9fa9d89`; every task complete, nothing abandoned; PARTIAL-candidate consumed: `DONE_PARTIAL`, no Cleanup Commit, `.harness/run/` stays.

### Iteration 4 - 2026-10-09 (Phase 3)
- **Phase:** 3 - T-006, T-007 (two Fast-tier Workers in parallel, scopes disjoint; `git status` showed exactly the declared new and edited files, 36 paths, nothing outside either scope)
- Attempted: 13 new and 7 edited main files (`Photo`, `Album`, `LoadedPhoto`, `SaveResult`, `FullSizeDecoder`, `RetryingMoveFileSystem` now `internal`, `PermissionAccess`, six navigation keys; the NavKey explanation moved to `Home.kt`), 9 new and 7 edited test files (`FakePhotoSource`, `RecordingRegistry`, `FakePhotoSaver`, `SinglePermissionRegistry`, `PickerDefaultAlphaTest`, `PickerMatchers`, `ScreenSample`, `FakePhotoLoader`, `SolidColourFetcherFactory`). The Iteration `git rm -f`'d the stub `Destinations.kt` (a Worker cannot delete) and emptied `ALLOWED` in `OneTypePerFileTest.kt` (`emptySet()`, KDoc rewritten); `PROJECT.md` updated for the new fake locations.
- Verified: combined run `cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` exit 0 (`evidence/p3-verify.log`): 231 tests, 0 failures, 0 skipped; the 231 test names are identical before and after (`evidence/p3-testnames-*.txt`); 16 baselines untouched. A multiset comparison of every old line against the new files found only the allowed deviations (private -> internal, KDoc wording). Rerun after the review's two comment fixes: `evidence/p3-verify-2.log`.
- Review (Capable, fresh context): nothing blocking. Two non-blocking comments that said something untrue (the `PERCENTAGE` KDoc gave a wrong reason for `internal`; the KDoc of `every_allowed_file_still_breaks_the_rule` still claimed a proof the empty list no longer gives). Both fixed by the Iteration.
- Learned: `git rm` refuses a file the Worker already edited (use `-f`); the review of a Phase that adds untracked files needs them staged first (`git add -A app`, then `git diff --cached`).
- Reconciled: A-001 and A-011 now name `e50a0a5`; A-002, A-003 and A-012 depend on this checkpoint. Every task is complete: PARTIAL-candidate recorded (DoD 6 and 15 need a person), next invocation is the Verifier.

### Iteration 3 - 2026-10-09 (Phase 2)
- **Phase:** 2 - T-004, T-005 (two Capable Workers in parallel, scopes disjoint; `git status` showed exactly the two new files)
- Attempted: before dispatch, a rough scan of both source trees found exactly the 14 files `ALLOWED` lists, so Phase 1 left no stray offender. `OneTypePerFileTest` (rule, 14-entry allow-list, stale-entry check, self-checks) and `ScreenStateTest` (six ViewModels by Java reflection, `…Content` takes the UiState, no `var`/`Mutable…` in a UiState, self-checks) written; neither Worker could build or run, both desk-checked.
- Verified: the two classes alone first (10 + 13 green), then a fresh review fix round and the full combined run `cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` exit 0 (`evidence/p2-verify.log`): 231 tests, 0 failures, 0 skipped; no baseline changed. `every_allowed_file_still_breaks_the_rule` passing is the proof that the scanner reports each of the 14 real files. Three mutation checks (templates off, backticks off, `declaredMethods` instead of `methods`) each turned a self-check red and were restored byte for byte.
- Failed run, not a defect: the first combined run failed only on `:app:cleanTestDebugUnitTest` (`Unable to delete directory …\test-results\testDebugUnitTest`) because the Iteration's own shell was standing in that directory; recorded as a Constraint and as suggestion S-001; the rerun from the repository root was green. No attempt was charged to any task.
- Review (Capable, fresh context): nothing blocking, three should-fix, five nits. Fixed by the Iteration: a KDoc that claimed Unicode escapes the code does not use; two self-check traps that could not fail; `declaredMethods` missed an inherited StateFlow (and a self-check for it was added); the new `parentFile` warning; a lambda type with a qualified UiState name passed for the parameter; a character literal could run past its line. Recorded, not fixed: ISSUES.md.
- Learned: `PROJECT.md` Constraints (no Gradle from inside `app/build`; nested block comments) and Reference (the two guards, test count 231).
- Reconciled: assumptions A-004..A-010 now name `adbdbe0`; A-001 and A-011 depend on this checkpoint; T-006 and T-007 unblocked.

### Iteration 2 - 2026-10-09 (Phase 1)
- **Phase:** 1 - T-001, T-002, T-003 (three Capable Workers in parallel, scopes disjoint; reported file sets matched `git status`)
- Attempted: D-001 consumed (DoD approved as written); each of the six screens now has one `…UiState`, one `state` StateFlow and a `…Content` that takes it; Saved got a ViewModel; the Iteration wired `KansoNavHost.kt` (Home preview, `entry<Saved>`) and `AppModule.kt` (Saved recipe).
- Verified: one combined run `cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` exit 0 (`evidence/p1-verify.log`); 207 tests, 0 failures; baselines unchanged; the R1-R5, criterion 11 and 15 greps pass.
- Review (Capable, fresh context): no Critical. Minor 1 (Tool's `tool`/`photoUri` still public) and Minor 3 (first `init` block unexplained) fixed by the Iteration; Minor 2 (`CameraContent` decides "a photo waits" from the `shotPicture` slot, not `state.shot`) kept: identical output on the real screen, already explained in its KDoc; recorded in ISSUES.md.
- Learned: `PreviewHygieneTest` scans top-level `sample…` bodies with their comments (PROJECT.md); the Saved fake now goes through the constructor, so Koin swaps are gone there.
- Reconciled: knowledge updated; assumptions A-004..A-010 marked as applied in this checkpoint.


### Iteration 1 - 2026-10-09 (Bootstrap)
- **Phase:** none (no implementation)
- Attempted: surveyed the repository; four read-only analysts at the Capable tier (survey and inventory, DoD proposal, decomposition with file scopes and tiers, then a critique of the draft plan and DoD); authored `DoD.md`, `PLAN.md`, seven task files, `ASSUMPTIONS.md`, `ESCALATION.md`.
- Learned: the PRD's count (8 main, 6 test files holding more than one type) is right, and five more files break "named after it" (`PermissionGate.kt`, `UprightJpeg.kt`, `SettingsStore.kt`, `SavedScreen.kt`, test `ScreenPixels.kt`); Camera and Tool also expose two StateFlows, which the PRD does not mention. The critique forced three changes: the guard test ships first with an allow-list so it can be shown red on the real tree (it cannot run before the splits otherwise); the mechanical splits go to the Fast tier in their own Phase; and the test-name proof needs `tr -d '\r'` and a next-`fun` match (29 tests have `@Config` between `@Test` and `fun`, six test files use CRLF).
- Reconciled: DoD written (17 criteria, 6 removals, 2 needing a person); 12 assumptions recorded; D-001 queued and blocks every task.

## Archived Decisions

<!-- Full request + decision + rationale of every consumed Decision Queue entry. -->

### D-001 - Approve the Definition of Done for Run 4 (answered 2026-10-09)
- **Question:** approve `DoD.md` (17 criteria, 6 removals) with each criterion's Verification Class; recommendation: approve as written.
- **Decision (human):** "Approved as written." All 17 criteria and R1-R6 with their classes unchanged; A-001 to A-012 not overturned; Q1-Q4 out of scope.
- **Rationale (human):** "the human read the DoD and approved it verbatim. The run continues in Autonomous mode."
