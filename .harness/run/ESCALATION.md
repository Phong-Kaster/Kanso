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

## D-001 - Approve the Definition of Done for Run 7 (the InFlux layout, in Kanso's own colours), and the screenshot pictures it may re-record

- **Status:** answered (2026-10-12, Iteration 2): approved with one edit - every tool one tap from Home, five round shortcuts (A-001 reversed); the 41 pictures under `## Baselines` approved as listed. Applied in `DoD.md`, `PLAN.md` and the task files; see `AMENDMENTS.md`.
- **Type:** DoD approval
- **Iteration:** 1
- **Timestamp:** 2026-10-12
- **Blocks tasks:** T-001, T-002, T-003, T-004, T-005, T-006, T-007, T-008, T-009, T-010, T-011

### Question

Approve `.harness/run/DoD.md` (41 criteria and 9 removals), editing it first if you wish. Approval covers each criterion's Verification Class as well as its wording. The PRD also asks to be asked, in the DoD, about re-recording the Roborazzi baselines: that approval is the list below. Reply `## D-001` in `.harness/run/DECISIONS.md` with "approved" (or your edits) and, **in the same file, a separate `## Baselines` heading** listing the pictures the engine may re-record, exactly like this (this is the file the engine cannot write):

```
## Baselines
- app/src/test/screenshots/1_home_light.png
- app/src/test/screenshots/2_home_dark.png
- app/src/test/screenshots/3_home_custom_colour.png
- app/src/test/screenshots/4_settings_light.png
- app/src/test/screenshots/5_settings_dark.png
- app/src/test/screenshots/6_settings_custom_colour.png
- app/src/test/screenshots/10_gallery_light.png
- app/src/test/screenshots/11_gallery_dark.png
- app/src/test/screenshots/12_gallery_refused_light.png
- app/src/test/screenshots/13_gallery_refused_dark.png
- app/src/test/screenshots/14_camera_refused_light.png
- app/src/test/screenshots/15_camera_refused_dark.png
- app/src/test/screenshots/16_tool_photo_light.png
- app/src/test/screenshots/17_tool_photo_dark.png
- app/src/test/screenshots/18_saved_light.png
- app/src/test/screenshots/19_saved_dark.png
- app/src/test/screenshots/20_camera_live_light.png
- app/src/test/screenshots/21_camera_live_dark.png
- app/src/test/screenshots/22_editor_enhance_light.png
- app/src/test/screenshots/23_editor_enhance_dark.png
- app/src/test/screenshots/24_settings_swatches_light.png
- app/src/test/screenshots/25_settings_swatches_dark.png
- app/src/test/screenshots/26_gallery_loading_light.png
- app/src/test/screenshots/27_gallery_loading_dark.png
- app/src/test/screenshots/28_my_edits_light.png
- app/src/test/screenshots/29_my_edits_dark.png
- app/src/test/screenshots/30_my_edits_empty_light.png
- app/src/test/screenshots/31_my_edits_empty_dark.png
- app/src/test/screenshots/32_my_edits_loading_light.png
- app/src/test/screenshots/33_my_edits_loading_dark.png
- app/src/test/screenshots/34_home_empty_light.png
- app/src/test/screenshots/35_home_empty_dark.png
- app/src/test/screenshots/36_home_loading_light.png
- app/src/test/screenshots/37_home_loading_dark.png
- app/src/test/screenshots/38_home_small_phone_light.png
- app/src/test/screenshots/39_home_small_phone_dark.png
- app/src/test/screenshots/40_tool_narrow_photo_light.png
- app/src/test/screenshots/41_tool_narrow_photo_dark.png
- app/src/test/screenshots/42_tool_narrow_photo_sdk30_light.png
- app/src/test/screenshots/43_photo_viewer_light.png
- app/src/test/screenshots/44_photo_viewer_dark.png
```

The first 24 already exist; the last 17 do not exist yet, and listing them lets a later phase re-record them if their screen changes again (Home's pictures move in two phases). Listing a picture does not mean it will change; it means the engine **may** re-record it, only with its comparison image (`_compare.png`: old, difference, new) kept in `.harness/run/evidence/baseline-diffs/phase<N>/`. Leave a picture out to forbid it. Without the list, the first re-recording is put back, `verifyRoborazziDebug` stays red, and the run ends `DONE_PARTIAL` with nothing built.

### Context

The PRD lays Kanso out the way InFlux v0.3.0 is laid out, in Kanso's own colours: a floating pill bar with three tabs (Home, My edits, Settings) and a round Camera button; a new Home (large title, greeting, a hero carousel of the four AI tools, two pill buttons, a "Recent edits" strip of Kanso's saved photos with skeletons); a new My edits tab and a full-screen viewer; one screen per tool (round Back, a rounded photo card over a blurred copy, a Replace chip, an options placeholder, a full-width Save) with the five-tool toolbar removed; restyled Gallery, Saved, Settings and Camera; and the fix for a hex colour typed in Settings. Eleven tasks in four phases (the plan is in `PLAN.md`; each phase is one checkpoint with the whole suite, the screenshot check and lint green). Two read-only reviews of the first plan (a critique and a conflict analysis, both at the Capable tier) found four blockers and eleven major problems (the critique) and eight hidden couplings or unowned files (the conflict analysis); every one is fixed in this DoD and plan before you see them (the shade proof, the "differs" rule for colours, unowned test classes, the order of the phases, the Home split).

Seven criteria need a person, so in Autonomous mode the run can end at best as `DONE_PARTIAL` until you sign them: **20** (motion and "Remove animations" on the phone), **27** (the blur on the phone), **28** (the carousel's feel), **29** (look at every picture, old beside new), **30** (walk the new layout on your Galaxy A13, light and dark, three colour choices), **31** (compare with the InFlux designs, human-only) and **37** (read five files and judge the explanations). Everything else is a command or a test.

Where this DoD reads the PRD, each is an assumption in `ASSUMPTIONS.md` (A-001 to A-024, numbered like the readings Q1 to Q24 at the end of the DoD; answer here to reverse one). The ones worth your eye:

- **A-001, "every tool in one tap" against a carousel (DoD 6).** Edit, Remove background, the Camera, the recent-edits arrow and the newest recent photo are one tap on a fresh launch of a 360 x 780 dp phone with nothing scrolled. **Enhance, Retouch and Artistic take 1, 2 and 3 swipes of the carousel plus a tap**, because the PRD's own sentence says the two pill buttons exist so Edit and Remove background are visible "without scrolling the carousel". If you want all four AI tools one tap with no swipe, say so here: DoD 6 and the carousel change.
- **A-005, "the photo fills the screen" on Saved.** Read as the whole photo, as large as fits, over a blurred copy of itself that fills the rest (nothing cropped), the same look as the tool card. The other reading crops the photo to fill edge to edge; it is a one-line change.
- **A-004, close on a photo opened from My edits** goes Home, as PRD item 6 says; system Back returns to My edits.
- **A-011, no permission prompts from Home or My edits.** On Android 8.1/9 (needs the photo-read permission) and after a reinstall on Android 10+ (the old photos are no longer the app's own) the Recent edits strip and My edits show their empty message until photo access has been granted in the gallery.
- **A-012, the carousel** is Material 3's uncontained carousel, one banner per swipe with the next one peeking; **A-022** the bar's selected pill uses the scheme's `inversePrimary`; **A-013** the bar hides while the keyboard is open.
- **A-019, the engine edit.** PRD item 9 asks to fix the hex field inside `core/colorpicker`; the rule "never edit engine source" in the project knowledge dates from Run 1 and was rewritten with one exception, limited to that field's two lines (DoD 39).
- **Existing tests change only where the PRD forces it** (the new Home, the tab bar, the per-tool screen, Saved's layout, Settings without Back); every edit is listed with its reason in `.harness/run/evidence/test-edits.md` and criterion 35 checks that no test was dropped without a replacement and that none is left proving a screen that no longer exists (R8).

### Options Considered

1. Approve as written, with the `## Baselines` list - consequences: four phases and a Verifier pass; the run ends `DONE_PARTIAL` until you sign criteria 20, 27, 28, 29, 30, 31 and 37; every picture a screen change moves can be re-recorded and kept with its comparison image.
2. Approve with edits (for example all four AI tools one tap, a cropped full-bleed Saved photo, close returning to My edits) - consequences: the plan is re-cut by a Tier-3 amendment before any task starts.
3. Approve without the `## Baselines` list - consequences: the first screen change leaves a failing screenshot check; Phase 1 cannot checkpoint; the run ends `DONE_PARTIAL` with the comparison images as the only result.

### Engine Recommendation

Option 1. It is the smallest set of decisions that lets the run build the whole layout unattended; the assumptions above are cheap to reverse one by one, and the pictures are judged by you in criterion 29 before anything is merged.

### Proposed Capabilities (if any)

None. Autonomous mode runs under the Deny List; the Material Symbols fetch uses `curl`, which the Deny List does not name.

### Decision

Answered in `.harness/run/DECISIONS.md` (`## D-001`): see the Status line above and `AMENDMENTS.md`, Iteration 2.
