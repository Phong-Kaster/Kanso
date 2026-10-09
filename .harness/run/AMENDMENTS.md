
### Iteration 2 - 2026-10-09 - D-001 applied (decision, not a plan change)

- **Answer:** "Approved as written (Option 1), 2026-10-09": the DoD's 30 criteria and 6 removals with their Verification Classes, and assumptions A-001 to A-012 as drafted (blue seed `#1A73E8` through the existing generator; the four AI tools; Camera as a Home action; the colour picker's light card as the one exception). The owner asked for the run to go on until the new look is built. The DoD's Questions stay open for a later run.
- **Baselines:** the 24 paths under `## Baselines` in `DECISIONS.md` (`app/src/test/screenshots/{1,2,3,4,5,6,10..27}_*.png`: the 16 that exist and the 8 planned new names) are the pictures the engine may re-record, under the DoD's conditions (criterion 20(b): `compareRoborazziDebug` first, each `_compare.png` kept in `.harness/run/evidence/baseline-diffs/phase<N>/`, new pictures only through `foreman-record-baselines.ps1`).
- **Effect:** T-001 to T-011 are unblocked; the plan is unchanged (no re-cut). A-001 to A-012 stand as taken.

### Iteration 2 - 2026-10-09 - Tier 1: three review follow-ups inside Phase 1 (no PRD, DoD or architecture change)

- **T-001 scope** gained `app/src/test/java/com/phongkaster/kanso/settings/ThemeColourRestartTest.kt` (a wallpaper-restart test, DoD 22) and its `SeedColorScheme.kt` declares the seed through a Long constant (A-013). Both are inside what T-001 is about (the colour model and its tests); the files simply were not on its list.
- **T-002 scope** was already `ToolScreenTest.kt`; it gained the whole-app walk DoD 3 names the class for, and a gesture-zone test.
- **T-003 scope** (`SavedScreen.kt`, `SavedScreenTest.kt`) gained three icon tags and `iconTag` parameters on `ActionButton` and `ActionLabel`, an icon test, and `ActionMenuItemPreview`. The three Workers were dispatched at the Capable tier (a review follow-up, not a retry of T-003's Fast attempt, which passed). Their attempts counters stay at 1.
- **Why:** the Fresh-Context Reviewer's two blocking findings (proofs absent from the classes DoD 3 and DoD 22 name) and four of its six advisory ones. Not done, with the reason: the eleven unused drawables stay (Phases 2 and 3 use them; DoD 28 checks `ic_settings` and the others at the end), and the DoD 16 line about the seed is answered by A-013, not by editing the DoD.


### Iteration 3 - 2026-10-10 - Tier 1: Phase 2 wiring and one added proof (no PRD, DoD or architecture change)

- **Recovery, not a plan change:** the first process of this Iteration dispatched the three Phase 2 Workers (T-004, T-005, T-009; each counts as attempt 1) and was killed before wiring or checkpointing. The second process found the Workers' files coherent and finished the Phase by salvage, with no re-dispatch.
- **The Iteration's wiring (shared files no Worker may touch):** `KansoNavHost.kt` (Home's `onOpenPhoto`, `onCameraClick`, `onToolClick`, `onSettingsClick`, and the preview's `HomeUiState`), `Tool.kt` (`CAMERA` removed, A-010 applied; `editingTools` is now `entries`). `PreviewsRenderTest.requiredPreviewNames` needed no change: no preview name was added, renamed or removed.
- **T-004 scope** gained one test, `HomeViewModelTest.camera_is_not_a_tool`, because R3 requires "a test asserts `Tool.entries.size == 5`" and no task listed it; it belongs beside the `Tool.kt` change, which is the Iteration's.
- **T-002's `ToolScreenTest.kt`** (Phase 1) was edited by the Iteration: its two `Tool.CAMERA` references now read `R.string.tool_camera` through a `cameraLabel()` helper, because the enum entry is gone. Not a Worker scope question: the enum edit is the Iteration's wiring and its consequence is the Iteration's too.
- **Review follow-ups (Tier 1, no PRD, DoD or architecture change):** Home's title moved into the scrolling column (T-004's `HomeScreen.kt`, `NavigationTest.kt`, `LaunchTest.kt`, pictures 1, 2, 3 re-recorded a second time); `ToolScreen.kt`'s `panelMessageRes` lost its `else` (T-009's file, a consequence of the enum change); `GalleryScreen.kt`'s title `maxLines` 1 -> 2 (T-005's file). All three files are inside the Phase's scopes except `ToolScreen.kt`'s `panelMessageRes`, which the Iteration edited because the enum edit is the Iteration's.
- **A Worker's test did not compile** (`NavigationTest.kt` called `SemanticsConfiguration.getOrNull` without its import); the Iteration added the import. Not an attempt against T-004: the Worker had no build capability, and the error is the kind the Iteration's first combined run exists to find.
