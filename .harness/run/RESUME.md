# RESUME BLOCK

> Regenerated every iteration. A derived cache, never a source of truth: on any disagreement with the task
> files or git, this file is wrong. Keep it small.

- **Stage:** escalated - waiting on D-001 (DoD approval, Q1-Q4, the `## Baselines` list, capabilities)
- **Next Phase:** Phase 1 - T-001 (2 slices), once D-001 is answered. Consume D-001 first: if the human edited
  criteria 2, 18 or 20, amend T-002 / T-004 / PLAN.md before dispatching anything.
  - T-001 - slice 1: `main/data/edit/edit/` `Sticker.kt` (new), `StickerPictures.kt` (new), `EditSettings.kt`,
    `EditSession.kt`, `EditProcessor.kt`; slice 2: `test/tool/edit/` `StickerSessionTest.kt`,
    `EditProcessorStickerTest.kt`, `SolidStickerPictures.kt` (all new), `README.md`, `docs/features/edit.md`
- **Wiring for Phase 1:** the Iteration writes `.harness/run/evidence/test-edits.md` (create it).
- **Queued decisions:** 1 - D-001 blocks T-001..T-006
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** (from `reference/build-and-environment.md`, verified 2026-10-05; not re-run at this
  bootstrap) build: `cd /d/Valve/Personal/Kanso && timeout 3600 ./gradlew :app:assembleDebug` | test:
  `timeout 3600 ./gradlew :app:testDebugUnitTest` (+ `:app:verifyRoborazziDebug`) | lint: `timeout 3600 ./gradlew :app:lintDebug`
- **Criteria the next Phase cites (quoted from DoD.md):**
  - **DoD 12:** Undo takes back sticker actions one at a time, in the same history as the other edits: an add, a delete, a duplicate, a flip, and one whole drag, pinch or handle pull each count as one action; selecting a sticker is not an action. Proof: `EditSession` JVM tests: add, delete, duplicate and flip each push exactly one entry; a gesture of 50 moves between finger-down and finger-up pushes exactly one, and its undo restores the settings from before the gesture; a mixed sequence (crop, add, filter, drag) undoes in reverse order.
  - **DoD 13 (session half):** A photo holds at most 20 stickers: adding a 21st, from the panel or by duplicating, adds nothing. Proof: an `EditSession` test driven from 20 stickers: an add and a duplicate each leave settings and undo history unchanged.
  - **DoD 15:** A sticker is part of the photo: a crop can cut it off, and turning or flipping the photo turns or flips the sticker with it. Proof: `EditProcessor` JVM tests with a solid-colour sticker from a fake picture source: a crop through the sticker's middle keeps only the kept part; a crop that excludes it leaves no sticker pixel; with one quarter turn and a horizontal flip the sticker's pixels sit at the turned and flipped place. The order in `EditProcessor` is stickers, crop, turns, flips, filter, sliders.
  - **DoD 16:** Filters and the colour sliders colour the stickers too. Proof: with a filter at full strength each sticker pixel equals the filtered sticker colour; with brightness raised each sticker pixel equals the sticker colour through the colour matrix.
  - **DoD 18 (processor half):** Done puts the stickers into the saved picture at the same resolution as the rest of the saved photo, drawn from the sticker pictures themselves. Proof: the output size with stickers equals the output size without them; a 1-px checkerboard sticker placed 1:1 with no turn appears pixel for pixel (± 1 per channel).
  - **DoD 21 (data half):** `EditSettings` and the sticker value type hold no selection field.
  - (Re-quote from DoD.md with Grep by number if D-001 changed any of these.)
- **Open issues touching the next Phase's files (quoted from ISSUES.md):** the Run 8 item "README matches the app" (`README.md`, "Editing features" / "How each editing feature works") is open and awaits a person; do not drop or rewrite it.
- **Reference topics the next Phase touches:** `reference/run9-facts.md` (Edit, EditProcessor, EditSession), `reference/engine-edit.md` (FilterLut)
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
