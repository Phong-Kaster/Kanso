# HISTORY

> Append-only audit log. Written every iteration, **never read during Orient**.

<!-- Newest first. One entry per iteration. -->

### Iteration 1 (Bootstrap) - 2026-11-04
- **Phase:** none (bootstrap)
- Attempted: read PRD (HSL tool, uncommitted edit to `PRD.md` at `8b2b0f2`, committed here); classed the repository a product (PROJECT.md); created `loop/kanso-hsl` from `feat/autumn-filter` at `8b2b0f2`. Fan-out at Capable: conventions survey, DoD proposal, decomposition, conflict/tier analysis, then an independent critique of the draft.
- Learned: the live preview already runs `EditProcessor` on a ≤ 640 px CPU copy (filter + layers) with the colour matrix on the GPU on top, so HSL slots between the two with one formula; `EditTool` has four exhaustive `when`s, so `EditTool.HSL` waits for the UI task; three 48 dp rows do not fit the 136 dp sheet; the row pictures 16, 17, 40-42, 51, 52, 55-82 move with a ninth tool and the recording script needs the owner's `## Baselines` list.
- Reconciled: critique B1 (row baselines) → asked in D-001; B2 (sheet height) → A-001; B3 (four preview code spots) → T-002 description; B4 (`EditLayoutTest`, screenshot taps) → T-003 scope. README timing → A-002. Knowledge: `reference/run11-facts.md` added and indexed.
- ISSUES.md: the settled Run 10 section stays (the archive cannot be written from here, PROJECT.md Constraint on `.harness/archive/`); a Run 11 section added on top.

## Archived Decisions
