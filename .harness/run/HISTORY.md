# HISTORY

> Append-only audit log. Written every iteration, **never read during Orient**.

<!-- Newest first. One entry per iteration. -->

### Iteration 1 - 2026-10-26 (bootstrap)
- **Phase:** none (bootstrap)
- Attempted: read `PRD.md` (Run 10, stickers); created `loop/kanso-stickers` from `main` at `fadd7c4`; five
  Capable analysts (survey, DoD proposal, decomposition, critique, conflict/tier analysis); wrote `DoD.md`
  (40 criteria, no removals, Q1-Q4), `PLAN.md` (5 Phases matching the PRD's five commits, 6 tasks, all Fast with
  fixed contracts), `TASKS/T-001..T-006`, and queued D-001.
- Learned: Done renders the screen-sized decode capped at 2,000 px, not the file's full size (Q4); the cropper
  shows the plain original (Q2); at 411 dp four tool entries already overflow the row (Q3); Done keeps the last
  `processorFor` it was given, so stickers need one processor factory for the panel and the cropper; Pinity's
  pointer code halves zoom, pan and rotation and cannot guarantee newest-first hit testing, so it is rewritten,
  not ported; Pinity's handle icons are two-colour and vanish under `Icon` tinting, so only their glyphs come
  across; no test pins the 84 dp entry width; existing pictures can be re-recorded only when listed under
  `## Baselines` in `DECISIONS.md`.
- Reconciled: Q1-Q4 into the DoD as editable criteria; the re-recording list and capabilities into D-001;
  PROJECT.md's `reference/` Constraint extended to `reference/pinity-stickers/`. The working tree's modified
  `.claude/agents/loop-analyst.md` and `loop-worker.md` predate this run (Foreman's own sync on `main`); left
  uncommitted and untouched.

## Archived Decisions

### Iteration 2 - 2026-10-26
- Consumed D-001 (approved, owner DoD edits kept; see AMENDMENTS.md). Unblocked T-001..T-006. Phase 1 dispatched.
- Phase 1 (T-001): two Workers (model+session+processor; tests+docs), tests 10+17 green, lint green, reviewer found a missing
  test-edits.md (written), over-sized-sticker decode (capped), weak tests (strengthened); one fix pass. Reviewer's
  "launcher-icon files in the diff" was `git add -N` line-ending noise (git status shows them unmodified). Rejected:
  removing `StickerPictures` default (T-002's factory wires it).

### Iteration 4 - 2026-10-26
- Phase 2 (T-002): copied the 60 sticker pictures (byte-identical to reference), wrote `strings_stickers.xml`; Wiring Worker wrote `ic_stickers.xml` (a lookalike, not the official path); 6 sequential Worker slices (data, mapping/top bar/chips, row/panel/bar, screen state, EditPanel factory, tests, tests+docs); tests all green, lint and assemble green. 16 row baselines re-recorded (D-001). Found: the screenshot verify fails 33 tests at HEAD already (stale baselines older than Run 10; same count with and without this Phase), and `SettingsUiStateTest.state_follows_the_theme_mode_set_through_the_view_model` failed once and passed on rerun (flaky).
- Reviewer found previews inside `KansoTheme` unwrapped, a missing test-edits.md, a misplaced file, cancel allowed while Done renders, stale docs; fixed in one pass.

### Iteration 5 - 2026-10-26
- Phase 3 (T-003): the Iteration wrote the four handle icons; 4 sequential Worker slices (overlay, screen wiring, tests, docs), then one review-fix pass. Tests (all, incl. 5 new sticker test files), lint and assemble green; verifyRoborazziDebug 15 stale-picture failures, none on Edit.
- Reviewer found: test-edits.md missing (fixed), a flip or Undo mid-drag reverted by the next move, consumed pointers not filtered between overlay and handle, an unreachable sticker dragged off the photo, an undrawn sticker still hit-testable, weak tests. All fixed. Recorded, not fixed: folder layout (StickerFrame and StickerCornerButton should sit in `stickeroverlay/stickerframe/`), `rememberStickerPainter` second composable in `StickerImage.kt`, drawable names `ic_close_circle`/`ic_resize_circle` show no circle, `DUPLICATE_OFFSET` naming, README item 6 wording says tap where touch-down selects.

### Iteration 6 - 2026-10-26
- Phase 4 (T-004): 3 sequential Worker slices (renderer/ViewModel/live preview/overlay; cropper + 4 new tests; docs), then a fix for `StickerOverlayTest` (its pixel reads raced the async preview; now poll) and one review-fix pass (stuck touched key on tick/cross/Compare/tool change, dead `StickerImage`, `StickerPlace` own file, cropper OOM catch). All unit tests, lint, assemble green. Recorded not fixed: ghost/blink at drag start and lift, main-thread decode. Amendments AM-T004-1/2 logged.

### Iteration 7 - 2026-10-26
- Phase 5 (T-005): Worker wrote StickerScreenshotTest (14 pictures); recorded and opened each. 69/70 first duplicated 71/72, so a second pass made them the tool row after a sticker was added (dot on Stickers). verifyRoborazziDebug for the class and OneTypePerFile/KdocCoverage green. No Reviewer (test-only).

### Iteration 8 - 2026-10-26
- Phase 6 (T-006): Worker wrote docs and the missing ADR-004; Reviewer found 3 false sentences (cropper shows stickers, preview renders stickers too, picture decode size) and 6 minor; one fix pass. Scope check green. All tasks complete: DONE-candidate recorded.

### Iteration 9 - 2026-10-26 (Verifier)
- Fresh assemble, test and lint: exit 0. verifyRoborazziDebug: 15 failures, all older pictures outside D-001's list (A-001, Tier 3; criteria 27 and 34 assumed). Static criteria 26, 29, 30, 32, 33, 35, 37 and 39 hold. 38 and 40: a fresh reader found a dead `EditPill.kt` path, 8 false or imprecise doc sentences and 2 stale KDocs. Filed as T-007, DONE-candidate cleared. Evidence: evidence/v-verifier-2026-10-26.md. Created ASSUMPTIONS.md and RECOVERY.md, which were missing since the switch to Autonomous at D-001.

### Iteration 10 - 2026-10-26
- Phase 7 (T-007): one Fast Worker fixed the 9 doc and KDoc items (Undo in the top bar, Compare in the tool row, no pill, EditPill link, renderer call, touched-key clearing, touch-down selects). Scope check and app compile green; no code behaviour changed. All tasks complete; PARTIAL-candidate recorded (A-001).

### Iteration 11 - 2026-10-26 (Verifier)
- Fresh run (cleanTestDebugUnitTest, assemble, test, lint, verifyRoborazzi): assemble and lint pass, 941 tests with only A-001's 15 stale pictures failing. Criteria 26, 29, 30, 32, 33, 35, 37 hold; 38's structure holds. 40: a fresh Capable reader found two false sentences in `docs/features/edit.md` (preview mermaid "filter only"; pictures 69-82 credited to DiscardDialogTest and EditToolTest) and three over-claims (EditSettings list omits stickers; cropper "always" shows stickers despite the OOM fallback; requestPreview caller list). Filed T-008; PARTIAL-candidate cleared. Device criteria not driven while a gap is open. Evidence: evidence/v2-verifier-2026-10-26.md.

### Iteration 12 - 2026-10-26
- Phase 8 (T-008): one Fast Worker fixed the 5 doc items (preview diagram, pictures 69-82 credit, EditSettings list, cropper OOM fallback, requestPreview callers). Scope check green; docs only. All tasks complete; PARTIAL-candidate recorded (A-001).
