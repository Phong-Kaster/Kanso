
### Iteration 2 - 2026-10-09 - D-001 applied (decision, not a plan change)

- **Answer:** "Approved as written (Option 1), 2026-10-09": the DoD's 30 criteria and 6 removals with their Verification Classes, and assumptions A-001 to A-012 as drafted (blue seed `#1A73E8` through the existing generator; the four AI tools; Camera as a Home action; the colour picker's light card as the one exception). The owner asked for the run to go on until the new look is built. The DoD's Questions stay open for a later run.
- **Baselines:** the 24 paths under `## Baselines` in `DECISIONS.md` (`app/src/test/screenshots/{1,2,3,4,5,6,10..27}_*.png`: the 16 that exist and the 8 planned new names) are the pictures the engine may re-record, under the DoD's conditions (criterion 20(b): `compareRoborazziDebug` first, each `_compare.png` kept in `.harness/run/evidence/baseline-diffs/phase<N>/`, new pictures only through `foreman-record-baselines.ps1`).
- **Effect:** T-001 to T-011 are unblocked; the plan is unchanged (no re-cut). A-001 to A-012 stand as taken.

### Iteration 2 - 2026-10-09 - Tier 1: three review follow-ups inside Phase 1 (no PRD, DoD or architecture change)

- **T-001 scope** gained `app/src/test/java/com/phongkaster/kanso/settings/ThemeColourRestartTest.kt` (a wallpaper-restart test, DoD 22) and its `SeedColorScheme.kt` declares the seed through a Long constant (A-013). Both are inside what T-001 is about (the colour model and its tests); the files simply were not on its list.
- **T-002 scope** was already `ToolScreenTest.kt`; it gained the whole-app walk DoD 3 names the class for, and a gesture-zone test.
- **T-003 scope** (`SavedScreen.kt`, `SavedScreenTest.kt`) gained three icon tags and `iconTag` parameters on `ActionButton` and `ActionLabel`, an icon test, and `ActionMenuItemPreview`. The three Workers were dispatched at the Capable tier (a review follow-up, not a retry of T-003's Fast attempt, which passed). Their attempts counters stay at 1.
- **Why:** the Fresh-Context Reviewer's two blocking findings (proofs absent from the classes DoD 3 and DoD 22 name) and four of its six advisory ones. Not done, with the reason: the eleven unused drawables stay (Phases 2 and 3 use them; DoD 28 checks `ic_settings` and the others at the end), and the DoD 16 line about the seed is answered by A-013, not by editing the DoD.

