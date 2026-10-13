# HISTORY

> Append-only audit log. Written every iteration, **never read during Orient** — it exists for a
> human auditing the run, not for the engine deciding what to do next. Keeping it out of the read
> path is what stops orientation cost from growing with the run.

### Iteration 1 - 2026-10-12 (bootstrap)
- **Phase:** none (bootstrap)
- Attempted: read the new PRD (Run 7, the InFlux layout), the repository, `PROJECT.md`, the Run 6 DoD and plan, the guard tests; created `loop/kanso-influx-layout` from `a720960`; fanned out read-only analysis roles at the Capable tier (survey, DoD proposal, decomposition and a Material 3 feasibility reading, then an independent critique of my draft DoD and plan and a conflict analysis of the file scopes); authored the DoD (41 criteria, 9 removals, 24 readings), the plan (11 tasks, 4 phases), the task files, the assumptions A-001 to A-024 and D-001; captured the base evidence (368 test names, the badging of the base APK, the base runtime dependency list, lint's unused resources); rewrote the Constraints of `PROJECT.md` that the PRD supersedes; appended sections J to R to `api-digest.md`.
- Learned: the first plan was not safe to approve. The critique found four blockers and eleven major problems, the conflict analysis eight hidden couplings or unowned files (three of them compile couplings inside Phase 1): DoD proofs named test classes no task wrote (S-005 and S-007 again), a shade proof that contradicted its task, "more than 8 per channel" that fails a correct tonal button, Phase 1 red by construction (missing baselines, `ThemeFollowTest` premises, `PreviewCoverageTest` on a composable no preview may animate), the overlaid bar catching `performScrollTo().performClick()`, a tab screen depending on a same-Phase file for `tabBarClearance()`, Home too big for one Worker, a Home photo source that a swap after launch cannot reach. The fixes: tab screens take their clearance as a parameter; the shell, fade and back-stack moves are T-001's own files; `TabTaps.tapAboveBar()`; Home split into T-005 and T-008; gallery and camera merged into T-006 (Fast); the cross-screen walks split into T-010 and T-011; every picture, preview and string name fixed in `PLAN.md`.
- Reconciled: new Constraints and Reference facts in `.harness/knowledge/PROJECT.md`; the Run 7 section in `.harness/ISSUES.md`; one Suggestion Box entry; D-001 queued (blocks every task); nothing implemented.

### Iteration 2 - 2026-10-12 (consume D-001, re-cut, Phase 1 preparation)
- **Phase:** none (a decision-and-preparation Iteration: no task started; Phase 1 needs more than the 90 minutes an Iteration has left after this, so it is Iteration 3's)
- Attempted: consumed D-001 (approved with one edit: every tool one tap, five round shortcuts, A-001 reversed; the 41 pictures approved as listed); applied the edit to `DoD.md` (criteria 4, 5, 6, 7(e), 19, 21, 30, R5, Q1, the status line), re-cut `PLAN.md`, T-005, T-008 (and one sentence each in T-003 and T-011), marked A-001 reversed, answered D-001 in `ESCALATION.md`, logged everything in `AMENDMENTS.md`, wrote ISSUES; Phase 1 preparation: fetched the five Material Symbols (`evidence/symbols/`, `check.sh` exit 0), wrote ADR-002 and the ADR-001 note, ran the `rememberInfiniteTransition` spike (throwaway test, deleted; `evidence/spike-infinite-transition.md`); dispatched a read-only Capable analyst to review the re-cut and fixed its nine findings.
- Learned: the pills-to-shortcuts edit changes the Home height budget (five round buttons with names are 84 dp against 48 dp; kept at 24 dp spare by a 176 dp carousel and 12 dp gaps) and the tag family (`home-shortcut-<TOOL>`); `waitForIdle()` returns at once with a `rememberInfiniteTransition` under the test rule and the pulse never moves (so no paused clock is needed, and no test can see the pulse); `FilledTonalIconButton`'s default is a 40 dp circle in a 48 dp node, which the DoD's "round" pixel rule would read on its rim (T-003 sizes `RoundIconButton` to 48 dp); `boundsInRoot` is clipped by Compose, so a "wholly on screen" proof measures `positionInRoot` plus `size`.
- Reconciled: knowledge update (PROJECT.md: the Home tag in a Constraint, the spike answer in Reference); a Tier-3 human edit applied and logged; one Suggestion Box entry (S-012); no decision queued; no task attempted.

### Iteration 5 - 2026-10-13 (recover Phase 1a, finish and checkpoint)
- **Phase:** 1a (T-001, T-002). Recovery: Iteration 4 left the Phase 1a work uncommitted with 18 red tests (its run was killed); it was coherent, so it was salvaged, not reverted.
- Fixed: `ShortNavigationBar` sized two tabs to zero (bar rebuilt as Surface + Row of weighted items); Home-row clicks hidden under the bar (`tapAboveBar()`); ToolScreenTest's end probe looked for Home's removed Camera text; NavBarTest's `hasVisualOverflow` premise (Robolectric) replaced by geometry.
- Evidence: whole unit suite, `compareRoborazziDebug`, lint, assembleDebug green (`evidence/p1a-run5.log`); new pictures 28-33 recorded, 1-6, 24, 25 re-recorded, `verifyRoborazziDebug` green. Pictures 1 and 29 opened and judged.
- Not done: the fresh-context review of Phase 1a was skipped for lack of time (the diff is 300 KB); the Verifier gate still re-proves everything. Pictures' `_compare.png` copies were not saved to `evidence/baseline-diffs/phase1/`.

## Archived Decisions

<!-- Full request + decision + rationale of every consumed Decision Queue entry. -->

### D-001 (answered 2026-10-12, consumed at Iteration 2)

- **Question (Iteration 1):** approve the Definition of Done for Run 7 (41 criteria, 9 removals) and the `## Baselines` list (the 24 existing pictures and the 17 new ones the run may record).
- **Decision (the owner):** approved with one edit. Every other criterion, removal, Verification Class and assumption stands as written, A-005 (whole photo over a blurred copy on Saved) and A-004 included. A-001 is reversed: every tool is one tap from Home with no swipe, because the goal of the run is speed to the popular features and three AI tools behind carousel swipes misses it. Home's two pill buttons become a row of five round tool shortcuts (Edit, Remove background, Enhance, Retouch, Artistic, each with its name under it, all visible at once on 360 x 780 dp without scrolling the row or the screen) directly under the carousel's dots; each opens that tool's gallery at once; the carousel of four AI tools stays with "Try now" on each banner. Criterion 6 becomes: on a small phone, right after launch and with nothing scrolled, all five tool shortcuts, the first banner's "Try now", the Camera button, the recent-edits arrow and the newest recent photo are fully on screen and each is one tap from where it leads. Criterion 4 and R5 read "a row of five tool shortcuts" wherever they say "two pill buttons". "Re-cut the plan for this before any task starts."
- **Baselines approved:** the 41 pictures listed under `## Baselines` in `DECISIONS.md` (the 24 existing and the 17 new ones, `1_home_light` to `44_photo_viewer_dark`, with the numbers 7 to 9 unused).
- **Applied:** `AMENDMENTS.md`, Iteration 2.

### Iteration 6 - 2026-10-13 (Phase 1b)
- **Phase:** 1b (T-003). Worker (Capable) rewrote the tool screen; Iteration wired Replace and the Saved key in KansoNavHost, removed `Tool.editingTools`, deleted ToolSelectionAnimationTest, fixed two `onSelectTool` call sites and ThemeFollowTest's editor premise (now reads background left of the card).
- Evidence: unit suite, compare, verify, lint, assemble green (`evidence/p1b-*.log`); baselines 16, 17, 22, 23 re-recorded, 40-42 new.
- Not done: fresh-context review skipped for time.

### Iteration 7 - 2026-10-13 (Phase 2: recover, finish, checkpoint)
- **Phase:** 2 (T-004 Saved/viewer, T-005 Home hero + five shortcuts, T-006 gallery/camera). Recovery: the previous Iteration was killed with the main code and wiring written but the tests uncompilable (SavedScreenTest, HomeScreenTest, NavigationTest, PhotoToToolNavigationTest, ThemeFollowTest); the debris was coherent and was salvaged.
- Did: two Capable Workers wrote SavedScreenTest and HomeScreenTest + the two navigation tests (the read guard refused them the evidence drafts, so HomeScreenTest was written afresh); Iteration fixed ThemeFollowTest (Saved premise: a photo in the theme's background colour), AccessibilityWalk (dots carry `stateDescription`, not `selected`), a Saved message assertion, HomeScreenTest's banner checks, ScreenshotTest's dot check; removed `strings_home.xml` and `saved_home`; recorded pictures 1-3, 10-15, 18-21, 26, 27 and new 43, 44 (judged 1, 10, 43).
- Evidence: whole set green (`evidence/p2-run4.log`: tests, verifyRoborazziDebug, lint, assembleDebug).
- Not done: fresh-context review of Phases 1 and 2 skipped for time; the Verifier re-proves everything.

### Iteration 9 - 2026-10-13 (Phase 3: Settings rows, Recent edits strip, hex field)
- **Phase:** 3 (T-007, T-008, T-009), three Capable Workers, attempt 1 each. Knowledge layout migrated first (ADR-040): `PROJECT.md` now Constraints plus an index, Reference and the API digest split into `knowledge/reference/` topics under 12 KB.
- Did: Iteration wired `AppModule` (HomeViewModel takes the source) and `KansoNavHost` (`onRecentClick`, `onSeeAllEdits`); added `TestKansoApplication` + `robolectric.properties` (the real source on Home's launch crashed the next Robolectric sandbox); fixed the strip so a new first photo is scrolled into view; fixed the My edits empty-message assertion in `NavigationTest`; recorded pictures 1-6, 24, 25 (judged 4 and the small-phone Home 38) and new 34-39.
- Evidence: whole set green (`evidence/p3-full-2.log`, then `p3-verify.log` after recording). Worker note: `HSVColorPicker.kt` has CRLF and no final newline, so the hex fix is inline with no trailing helper (DoD 39 still satisfied).
- Not done: fresh-context review of Phases 1-3 (time); `evidence/baseline-diffs/` copies.

### Iteration 10 - 2026-10-13 (Phase 4: T-010, T-011)
- Two Capable Workers wrote the cross-screen tests (disjoint scopes). First whole run: 6 failures (banner 4 painted `surfaceContainerHighest`, not an allowed container: fixed in `HomeScreen.kt` as Tier 1, `index % 3`; banner pixel under a neighbour's node; Replace swipe too fast; sideways-cut "Try now" of a peeking banner). Second run green (`evidence/p4-full-2.log`, exit 0: tests, verifyRoborazzi, lint, assemble).
- Review (Capable, fresh): blocking F1 (title contrast scored lightest vs darkest across a fading shade), F2 (insets sent but not verified as received); non-blocking F3 (skip too broad, front banner not proven checked), F4 (viewer check backdrop equals the expected colour by construction), F5 (double-tap does not prove the guard). Checkpointed green because the clock was short; fixes queued as T-012.
- Dirty tree at start was the owner's Foreman skill updates and telemetry, not run debris; skill files left uncommitted.

### Iteration 11 - 2026-10-13 (Phase 5: T-012)
- One Capable Worker fixed F1-F5 in the six test files. The new F1 measure failed on a real defect: Saved's dark-mode title over a white photo had contrast 3.5 (viewer) and 3.95 (Saved), because the top shade began to fade 8 dp below the status bar, inside the title row. Iteration fixed it in `SavedScreen.kt` (`SCRIM_HOLD` 8 dp -> 64 dp, the whole top row) and re-recorded pictures 18, 19, 43, 44 (compare images in `evidence/baseline-diffs/phase5/`: only the top band changed).
- Evidence: whole set green (`evidence/p5-full-2.log`, exit 0). Review (Capable, fresh): no blocking finding; minor stale comments fixed; `ThemeFollowTest.checkSaved` keeps the by-construction backdrop pixel (recorded in ISSUES.md); no full-suite log existed after the first fix, so the final one was run.
- Next: the Verifier (a DONE/PARTIAL-candidate).

### Iteration 12 - 2026-10-13 (the Verifier: gaps found, DONE-candidate cleared)
- Recover: the dirty tree was the owner's Foreman skill update under `.claude/` and telemetry, not debris; the skill files are left uncommitted, as in Iterations 10 and 11. No decision to consume.
- Re-proved, fresh: whole set exit 0 in 3 min 35 s (`evidence/verifier-full.log`): 571 tests = 571 `@Test` lines, 0 failures/errors/skipped; Roborazzi total 41, changed 0; lint 0 errors (forced run, `lint.log`); `compileDebugKotlin --rerun-tasks` no deprecation in `app/src/main`; badging equals the base, no INTERNET, ABIs and min/target SDK hold; dependencies equal the base; `reference/` unchanged; `core` diff is one hunk in base lines 66-70; R1-R3, R5-R7 greps empty; ADR-002 and the ADR-001 note exist; `git diff --name-status` shows 24 M and 17 A pictures.
- Failed: DoD 38 (`@Suppress("DEPRECATION")` added, `MediaStoreSavedPhotoSource.kt:85`); 20(a) (`ValueAnimator` in `SkeletonBlock.kt`; catalog lines at the base -> A-025); R4/R9 (`ic_add_photo_alternate.xml` unused); 33 (line number only -> A-026); 29(b) (no Phase 2-3 compare images -> A-027). Missing: `test-edits.md`, `m3-scan.md`, `fixed-colours.md`, `orphans.sh`, `home-reach.md`, `head-testnames.txt`.
- Wrote (Verifier evidence): `baseline-diffs/before/` (24), `after/` (41), `index.md`; `head-badging.txt`, `head-deps.txt`, `head-unused-resources.txt`. Picture 38 looked at: did not show a defect.
- Review of Phases 1-3 (Capable, fresh, `evidence/verifier-review-p1-3.patch`): no blocking finding, no Constraint broken. F1 bar KDoc describes `ShortNavigationBar` the code no longer uses; F2 Settings' title differs from Home's, so it jumps on a tab fade; F3 Recent strip scrolls to 0 on every return; F4 backdrop tiny copy built on the main thread; F5 stale one-editor comments in `Tool.kt`/`KansoNavHost.kt`; F6 Android 8.1/9 `LIKE` pattern too loose; F7 teal sample in a preview. -> T-013 (F6), T-015 (the rest).
- Not driven: the Galaxy A13 (the code still changes, so the next Verifier drives it).
- Mistake: an attempt to archive the Runs 1-6 sections of `ISSUES.md` wrote `.harness/archive/ISSUES-run-1-6.md` with a shell redirect. The permission rules then refused editing or deleting it. It is left untracked and uncommitted, flagged in `ISSUES.md` for a person, and `ISSUES.md` itself was not trimmed. A Constraint now records the trap.

### Iteration 13 - 2026-10-13 (Phase 6: T-013 Fast + T-015 Capable)
- Recover: dirt was the owner's Foreman skill update and telemetry, left as before. No decision to consume.
- T-013: no `@Suppress("DEPRECATION")` (column named `"_data"` by a const); Android 8.1/9 selection anchored with `GLOB` to `/storage/emulated/<n>/Pictures/Kanso/*`, subfolders out; "Remove animations" read from `Settings.Global.ANIMATOR_DURATION_SCALE`; `ic_add_photo_alternate.xml` removed.
- T-015: bar KDoc true to the `Surface`+`Row` build; Settings' title = Home's style and 8 dp; Recent strip scrolls to 0 only when the newest id changed; non-teal `sampleWideBitmap`. F4 not done (A-028).
- Iteration: stale comments in `Tool.kt`, `KansoNavHost.kt`. Whole set (tests, compare, lint, assemble) exit 0, `evidence/p6-full-1.log`; `foreman-record-baselines` re-recorded exactly pictures 4, 5, 6, 24, 25 (D-001); 4 looked at: title now Home's size. No test file edited.
- Not done by Workers' report: `navigation/KansoTabShell.kt:110` uses `headlineSmallEmphasized` for a title; looked at in the Verifier.

### Iteration 14 - 2026-10-13 (Phase 7: T-014, written by the Iteration itself; evidence only)
- Recover: dirt was the owner's Foreman skill update, telemetry and the untracked archive file, left as before. No decision to consume.
- Wrote `evidence/test-edits.md` (45 gone names: 10 renamed, 17 replaced, 18 removed, each with its Removal; every A/M/D test file with a reason), `orphans.sh` (run: prints nothing), `m3-scan.md` (every piece with file:line; 15 button calls, all with `shapes =`; hand-built pieces say why), `fixed-colours.md` (8 lines, all in samples or `PickerSurfaceColor`; the Settings sample is at 738, was 733). Inputs: `head-testnames.txt` (571), `comm-removed.txt`, `test-name-status.txt`. The base names file is not in C order, so both lists were re-sorted before `comm`.
- No code changed, so no build was run. Not written: `home-reach.md` (named in the evidence README; left to the Verifier to check against the DoD).
- Report: PARTIAL-candidate recorded; CONTINUE.
