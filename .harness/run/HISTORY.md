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
