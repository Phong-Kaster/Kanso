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

## D-001 - Approve the Definition of Done for Run 6 (Material 3 Expressive, Kanso blue), and the screenshot pictures it may re-record

- **Status:** answered (Iteration 2, 2026-10-09)
- **Type:** DoD approval
- **Iteration:** 1
- **Timestamp:** 2026-10-09
- **Blocks tasks:** T-001, T-002, T-003, T-004, T-005, T-006, T-007, T-008, T-009, T-010, T-011

### Question

Approve `.harness/run/DoD.md` (30 criteria and 6 removals), editing it first if you wish. Approval covers each criterion's Verification Class as well as its wording. The PRD also asks to be asked, in the DoD, about re-recording the Roborazzi baselines: that approval is the list below. Reply `## D-001` in `.harness/run/DECISIONS.md` with "approved" (or your edits) and, **in the same file, a separate `## Baselines` heading** listing the pictures the engine may re-record, exactly like this (this is the file the engine cannot write):

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
```

The first 16 already exist; the last 8 do not exist yet, and listing them lets a later phase re-record them if their screen changes again. Listing a picture does not mean it will change; it means the engine **may** re-record it, only with its comparison image (`_compare.png`: old, difference, new) kept in `.harness/run/evidence/baseline-diffs/phase<N>/`. Leave a picture out to forbid it. Without the list, the first re-recording is put back, `verifyRoborazziDebug` stays red, and the run ends `DONE_PARTIAL` with nothing built.

### Context

The PRD turns Kanso into a Material 3 Expressive app with blue as its colour: theme and motion scheme, icons, one editor for the five tools with a bottom toolbar, a new Home, Gallery with a loading indicator, Saved with a shape-morph mark and a button group, Camera with icon controls, grouped Settings, Navigation 3 transitions and predictive back. Material3 stays at 1.5.0-beta01 (Run 5 put it in the app), no new library, and the only new assets are Material Symbols drawables. Eleven tasks in four phases (the plan is in `PLAN.md`; each phase is one checkpoint with the whole suite, the screenshot check and lint green).

Three criteria need a person, so in Autonomous mode the run can end at best as `DONE_PARTIAL` until you sign them: **20** (look at every screen picture, old beside new), **21** (walk the new look on your Galaxy A13, light and dark, motion, the back gesture with swipe navigation, Remove animations) and **30** (read four files and judge the explanations). Criterion 13 (Remove animations) is closed by the phone walk. Everything else is a command or a test.

Where this DoD reads the PRD, each is an assumption in `ASSUMPTIONS.md` (A-001 to A-012; answer here to reverse one). The ones worth your eye:

- **Which tools are "AI tools"** (A-001): Remove background, Enhance, Retouch, Artistic; "Edit" holds the Edit card. (This is also how `README.md` groups them.)
- **The blue** (A-002): seed `#1A73E8` through the existing colour generator; it makes a calm blue. If you want a more vivid one, name the seed.
- **Your phone changes colour on update** (A-003): "blue by default" means a phone that never picked a colour (yours, on Android 14, was on wallpaper colours) opens in Kanso blue after the update; a saved "Default purple" and an unknown value also become blue; "From wallpaper" is still offered on Android 12 and later, and not below.
- **Home keeps visible "Camera" and "Settings" entries inside its scrolling page** (A-004), so about a dozen existing tests keep working; "Open a photo" opens the gallery and then the editor with Edit selected.
- **Edit another opens the tool you had selected when you saved** (A-005).
- **Every screen follows the theme** (A-006), the camera's black picture area included. One named exception: Settings' colour picker keeps its light card, because the engine's picker draws fixed black text and its source may not be edited.
- **Wavy progress indicators are not used** (A-007), because nothing in Kanso has measurable progress; `LoadingIndicator` replaces every spinner and every blank wait.
- **Predictive back and "Remove animations"** (A-011): a machine proves the transition specs and the manifest flag; you check the gesture preview (needs Settings → Display → Navigation bar → Swipe gestures on your phone) and "Remove animations" yourself.
- **Material Symbols** are fetched by the engine from github.com/google/material-design-icons (public, Apache 2.0; no installs); the DoD's Questions ask whether the app should show a licence notice.
- **Existing tests change only where the PRD forces it** (blue default, Home's new layout, the editor, the loading indicator); each edit is listed with its reason in `.harness/run/evidence/test-edits.md` and criterion 25 checks that no test was dropped without a replacement.

### Options Considered

1. Approve as written, with the `## Baselines` list - consequences: four phases and a Verifier pass; the run ends `DONE_PARTIAL` until you sign criteria 20, 21 and 30; every picture a screen change moves can be re-recorded and kept with its comparison image.
2. Approve with edits (for example another blue, another grouping of the tools, or a top-bar Settings icon) - consequences: the plan is re-cut by a Tier-3 amendment before any task starts.
3. Approve without the `## Baselines` list - consequences: the first screen change leaves a failing screenshot check; Phase 1 cannot checkpoint; the run ends `DONE_PARTIAL` with the comparison images as the only result.

### Engine Recommendation

Option 1. It is the smallest set of decisions that lets the run build the whole redesign unattended; the assumptions above are cheap to reverse one by one, and the pictures are judged by you in criterion 20 before anything is merged.

### Proposed Capabilities (if any)

None. Autonomous mode runs under the Deny List; the Material Symbols fetch uses `curl`, which the Deny List does not name.

### Decision

<!-- Answered in `.harness/run/DECISIONS.md`, under a heading `## D-001` - never here. -->
