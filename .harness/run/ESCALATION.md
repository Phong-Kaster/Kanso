# DECISION QUEUE

> Questions the engine could not answer within its authority. Queueing one does **not** stop the run:
> the engine marks the tasks that entry blocks and keeps working on everything else. The Runtime
> stops only when no executable task remains.
>
> This file is the engine's own log. **Answer in `.harness/run/DECISIONS.md`, never here** - copy an
> entry's id as a heading there and write your decision underneath it, then re-run. The engine is
> denied write access to `DECISIONS.md`, mechanically, so nothing it does here can ever race what you
> write there. Answer any number of entries; unanswered ones stay queued and the tasks they name stay
> unselectable.

---

## D-001 - Approve the Definition of Done for Run 10 (stickers), the picture re-recordings and the toolchain

- **Status:** pending
- **Type:** DoD approval
- **Iteration:** 1 (bootstrap)
- **Timestamp:** 2026-10-26
- **Blocks tasks:** T-001, T-002, T-003, T-004, T-005, T-006

### Question

1. Approve `.harness/run/DoD.md` (40 criteria, no removals). Edit it freely first, including each criterion's
   class.
2. Answer the four questions listed under "Questions" in the DoD (each tied to a criterion you can keep, edit or
   delete):
   - **Q1** - Pinity's `ic_rotate_circle` stays unported.
   - **Q2 (criterion 20)** - show the stickers while Crop is open (today the cropper shows the plain original, so
     stickers would vanish exactly while you crop through one).
   - **Q3 (criterion 2)** - narrow the tool entries so all five fit on a 411 dp phone without scrolling (today
     Filters is already partly off screen there).
   - **Q4 (criterion 18)** - stickers are drawn at the resolution Done already saves at (the photo as loaded for
     editing, at most 2,000 px on its long side), not the file's full size.
3. Approve re-recording the existing pictures that show the tool row. The PRD allows it, but the recording
   script keeps an existing picture only if you list it under a `## Baselines` heading in
   `.harness/run/DECISIONS.md`. If you keep criterion 2, all sixteen change in Phase 2. To approve, add this
   to `DECISIONS.md`:

   ```
   ## Baselines
   - app/src/test/screenshots/51_tool_edit_free_crop_light.png
   - app/src/test/screenshots/52_tool_edit_free_crop_dark.png
   - app/src/test/screenshots/55_edit_tool_row_light.png
   - app/src/test/screenshots/56_edit_tool_row_dark.png
   - app/src/test/screenshots/57_edit_crop_sheet_light.png
   - app/src/test/screenshots/58_edit_crop_sheet_dark.png
   - app/src/test/screenshots/59_edit_rotate_sheet_light.png
   - app/src/test/screenshots/60_edit_rotate_sheet_dark.png
   - app/src/test/screenshots/61_edit_adjust_brightness_light.png
   - app/src/test/screenshots/62_edit_adjust_brightness_dark.png
   - app/src/test/screenshots/63_edit_filters_1977_light.png
   - app/src/test/screenshots/64_edit_filters_1977_dark.png
   - app/src/test/screenshots/65_edit_compare_on_light.png
   - app/src/test/screenshots/66_edit_compare_on_dark.png
   - app/src/test/screenshots/67_edit_pill_light.png
   - app/src/test/screenshots/68_edit_pill_dark.png
   ```

   Each old version is copied to `.harness/run/evidence/baseline-diffs/` before it is replaced (criteria 30, 31).
4. Approve the capabilities proposed below.

### Context

Bootstrap of Run 10 from `PRD.md` (stickers on the Edit screen, ported from `reference/pinity-stickers/`). Five
analysts surveyed the code and critiqued the plan. They found four places where the PRD and the code as it stands
leave a choice open (Q1-Q4). Each one is now an editable criterion, so you decide it here, at the one gate, and
not later as a guess. The plan follows the PRD's five commits: the model and Done (Phase 1), the catalogue and
panel (2), the sticker on the photo (3), the live preview (4), and the docs and pictures (5).

### Options Considered

1. Approve as written: the plan runs as described.
2. Edit criteria 2, 18 or 20 before approving. Deleting 2 keeps the scrolling row, so only pictures 55 and 56
   might change. Deleting 20 accepts stickers hidden while cropping. Changing 18 to the file's full size adds a
   full-size decode for Done.

### Engine Recommendation

Approve with criteria 2, 18 and 20 as written. Criterion 20 is what makes decision 1 ("the crop can cut the
sticker off") visible while you crop. Criterion 2 makes the "row with five tools" picture actually show five
tools. Criterion 18 keeps Done's memory use on the A13 as it is today.

### Proposed Capabilities (if any)

```json
{
  "intent": "build, test, lint and check the screenshot pictures of Kanso in every Phase (this repository's toolchain)",
  "command": "./gradlew :app:assembleDebug | :app:testDebugUnitTest | :app:lintDebug | :app:verifyRoborazziDebug, each under timeout, from the repository root",
  "scope": "the repository's build outputs (app/build, build/), no source files",
  "lifetime": "permanent",
  "allow": [
    "Bash(timeout * ./gradlew :app:assembleDebug*)",
    "Bash(timeout * ./gradlew :app:testDebugUnitTest*)",
    "Bash(timeout * ./gradlew :app:lintDebug*)",
    "Bash(timeout * ./gradlew :app:verifyRoborazziDebug*)"
  ],
  "target_ledger": ".harness/knowledge/capabilities.json"
}
```

```json
{
  "intent": "record the new pictures 69-82 (and, with your Baselines list, the 16 row pictures) through Foreman's recorder, which puts back any existing picture you did not list",
  "command": "powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app [-Tests <filter>]",
  "scope": "app/src/test/screenshots/",
  "lifetime": "goal",
  "allow": [
    "Bash(powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1*)"
  ],
  "target_ledger": ".harness/run/capabilities.json"
}
```

```json
{
  "intent": "copy Pinity's 60 sticker pictures into Kanso (Phase 2) and prove they are byte-identical (criterion 32); keep old row pictures beside the new ones (criterion 30)",
  "command": "cp reference/pinity-stickers/res/drawable/img_sticker_*.webp app/src/main/res/drawable/ ; sha256sum ; cp app/src/test/screenshots/<n>_*.png .harness/run/evidence/baseline-diffs/",
  "scope": "reads reference/pinity-stickers/res/drawable/ (never writes it); writes app/src/main/res/drawable/img_sticker_*.webp and .harness/run/evidence/baseline-diffs/",
  "lifetime": "goal",
  "allow": [
    "Bash(cp reference/pinity-stickers/res/drawable/img_sticker_*.webp app/src/main/res/drawable/)",
    "Bash(sha256sum *)",
    "Bash(cp app/src/test/screenshots/* .harness/run/evidence/baseline-diffs/*)"
  ],
  "target_ledger": ".harness/run/capabilities.json"
}
```

### Decision

<!-- Answered in `.harness/run/DECISIONS.md`, under a heading `## D-001` - never here. -->
