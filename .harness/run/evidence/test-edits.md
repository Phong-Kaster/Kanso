
## Run 8, Phase 1 (T-001)

- `tool/ToolScreenTest.each_tool_shows_its_own_panel_message` removed, replaced by `each_tool_shows_its_own_panel_and_no_later_run_message`: the five `editor_panel_*` strings are gone with the "coming in a later run" panel (DoD 14).
- `tool/ToolScreenTest.each_tool_has_its_own_screen_...`: the panel-message check is now a panel-tag check, and the strip-under-card / Save-under-strip position checks are skipped while the strip is empty (an empty slot is placed nowhere); they bite again once a tool's controls fill it.
- `saved/SavedScreenTest.share_opens_the_chooser_for_the_photo`: waits for onShare with `waitUntil` after the chooser appears, because Share now reads the file's MIME type through one more background hop (DoD 21).
- Baselines 16, 17, 22, 23, 40, 41, 42 re-recorded (the placeholder panel is gone, Undo added); permitted by D-001.
- New tests: ToolViewModelTest (undo, progress, failure, working size, save edited), SaveEditedTest, SharePhotoTest; `FakePhotoSaver`/`NoSavePhotoSaver` gained `save(bitmap)`.
