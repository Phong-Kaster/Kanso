# HISTORY

> Append-only audit log. Never read during Orient.

### Iteration 1 - 2026-11-06 (bootstrap)
- **Phase:** none (bootstrap)
- Attempted: read the one-line PRD; four Capable analysts (survey, DoD proposal, decomposition, critique with tiers and conflicts); wrote DoD, PLAN, T-001..T-005, D-001; created `loop/kanso-curves` from `main` `069b5c3`.
- Learned: the Edit sheet is 220 dp (run11-facts says 136); highest picture is 94; curves cannot join the GPU colour matrix, so they go on the CPU path after HSL; `EditHsl.hslToRgb` is private (so Lightness copies the conversion); `EditPanel` scrolls vertically for every tool but Stickers and HSL (Curves must be exempt); the Galaxy A13 is attached (`foreman-device.ps1 -Op devices`); no ledger grants Gradle assemble/test/lint.
- Reconciled: scope ambiguity and build capability queued as D-001 (blocks all tasks); hue curves made an optional, deletable DoD section with T-005.

## Archived Decisions

### Iteration 2 - 2026-11-06 (Phase 1, T-001)
- Consumed D-001 (approved, option 1, Autonomous). Removed T-005. Wiring Worker wrote the icon and ADR-009; baseline verify was green before any change.
- T-001 in 5 slices; fixed 7 test failures then 1 more; recorded pictures 95/96; pictures 63/64 now red (D-002).
- Learned: `KdocCoverageTest` demands KDoc on local functions; the tool row is lazy so tests must `scrollToolRowTo`; the scope checker cannot parse `...` shorthand scopes; Read refuses files over about 10 KB to a Worker, so the Constraints were split into 4 temp files.

### Iteration 3 - 2026-11-06 (Phase 2, T-002)
- Consumed D-002 (approved): pictures 63/64 re-recorded through the wrapper.
- T-002 done: curves in the live preview (FilterPreviewRenderer, ToolViewModel), CurvesPreviewTest, CurveParityTest, README/edit.md. Build, 1 unit-test run, lint and screenshot check green.
- Reviewer: fixed a crash in `ToneCurve.moved` (empty coerceIn range) with a test, README Curves row corrected, test-edits.md completed. A change making diagonal-point curves neutral was tried and reverted (it would skip Undo and disable Reset).

### Iteration 4 - 2026-11-06 (Phase 3, T-003)
Red/Green/Blue chips: curveChannel state, CurveChips, per-channel graph and Reset, graph ignores gestures when disabled, tests, docs, pictures 97/98 recorded, 95/96 re-recorded (D-003 approved). Build, tests, lint green. Amendment: EditPanel.kt added to slice 2 scope.

## Iteration 4 (Phase 4, T-004)
Lightness curve (HSL L, greys follow, L 0..1), fifth chip, CurveGraph disabled fix, tests, README/docs. Tier-1 amendment: CurvesPanel.kt added to scope. Chip label wrapped in picture 99; fixed and re-recorded 99/100, 95/96 re-recorded with approval. 97/98 red, awaiting owner listing.

## Iteration 6 (Phase 5, T-006)
- Recovered: untracked `evidence/v-build.log` from a killed Verifier run (incomplete, "out of disk space?" in a test thread; D: and C: now have space) - removed as debris.
- Consumed D-004 (owner-raised; DoD 23 added by the owner): Lightness chip wrapped, Reset pushed off the sheet. Added T-006, cleared PARTIAL-candidate for the Phase.
- T-006: attempts 1 and 2 failed on an unsound test check (`didOverflowWidth` with `softWrap = false`), attempt 3 green. Reviewer: README row 102 and a KDoc untrue, test-edits.md missing, Reset size unchecked - all fixed in one review-fix dispatch; rebuilt green (`p5-build4.log`). Pictures 95-98 re-recorded through the wrapper; 99/100 red (not on `## Baselines`).
- Learned (Constraints): softWrap=false makes overflow flags meaningless; un-weighted horizontalScroll Row starves siblings.

## Iteration 7 (Verifier, DONE_PARTIAL)
- Recovered: clean apart from TELEMETRY.tsv. DECISIONS.md: no new ids; 99/100 are now on `## Baselines` (the owner added them 16:07).
- Re-recorded 99/100 through the wrapper (`v-record.log`), opened both: chips on one line, Reset visible, light and dark.
- Full Gradle run (`v-build.log`): assemble and lint passed; 1 failure in 1267 tests, `SettingsUiStateTest:175`, not on this branch. It passed alone (`v-settings-rerun.log`) and in a full `verifyRoborazziDebug --rerun-tasks` (`v-verify.log`: 1267 tests, 0 skipped, 0 failed, pictures green). Classified as a flaky test outside the feature: noted in ISSUES.
- 19-21: `git diff main` checks are empty; no manifest change; strings are prefixed. README paths exist; `docs/features/edit.md` has 7 stale paths that predate this run (noted).
- 16 not driven: the debug build installed on the A13 (no clear) and started on Home, but another app kept the screen; the wrapper refuses other apps.
- Result: every machine criterion holds; 15, 16, 17 and 22 await a person, written to ISSUES "Awaiting a person, Run 12". No Cleanup Commit (Autonomous partial).
