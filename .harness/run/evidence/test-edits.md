# Test edits (DoD 16, 35, 36, R8)

Written in Iteration 14 from `comm-removed.txt` (`comm -23` of `base-testnames.txt` and `head-testnames.txt`, both re-sorted with `LC_ALL=C` first, because the committed base file is not in C order) and `test-name-status.txt` (`git diff --name-status --no-renames a720960 HEAD -- app/src/test/java`).
Base has 368 names, HEAD 571, and 45 base names are gone. Each is listed below as renamed, replaced or removed.

## Renamed (the subject lives on; the new name is given)

- `AccessibilityWalkTest:every_tool_has_its_own_editor_state` -> `every_tool_has_its_own_tool_screen_states`
- `AccessibilityWalkTest:editor_with_edit_selected` -> `tool_screen_edit_ready`
- `AccessibilityWalkTest:editor_with_enhance_selected` -> `tool_screen_enhance_ready`
- `AccessibilityWalkTest:editor_with_artistic_selected` -> `tool_screen_artistic_ready`
- `AccessibilityWalkTest:editor_with_remove_background_selected` -> `tool_screen_remove_background_ready`
- `AccessibilityWalkTest:editor_with_retouch_selected` -> `tool_screen_retouch_ready`
- `AccessibilityWalkTest:editor_while_saving` -> `tool_screen_<tool>_saving`, one per tool
- `AccessibilityWalkTest:editor_with_the_photo_unreadable` -> `tool_screen_<tool>_unreadable`, one per tool
- `HomeViewModelTest:home_state_holds_the_ai_tools_and_the_edit_group_in_order` -> `home_state_holds_the_carousel_tools_and_the_five_shortcuts_in_order`
- `ScreenStateTest:the_screen_packages_hold_exactly_the_six_view_models` -> `the_screen_packages_hold_exactly_the_seven_view_models` (My edits added a screen)

## Replaced (a new test proves the new form)

- `AccessibilityWalkTest:home` -> `home_empty`, `home_loading`, `home_with_recent_photos_on_banner_1` to `_4`: the walk of Home in every state, banner by banner.
- `NavigationTest:each_tool_opens_its_screen_and_back_returns_home` -> `each_shortcut_opens_its_gallery_and_back_returns_home`: a shortcut opens the tool's gallery and Back goes Home.
- `NavigationTest:home_lists_open_a_photo_camera_five_tool_cards_and_settings` -> `home_shows_the_carousel_and_five_shortcuts_while_settings_and_the_camera_are_on_the_bar` (R3, R4, R5 are gone; this proves the new Home).
- `NavigationTest:launch_asks_for_nothing_and_the_gallery_asks_for_photo_access` -> `each_tab_shows_its_screen_and_asks_for_nothing` and the three `each_shortcut_and_try_now_asks_only_for_photo_access_on_android_11`, `_13`, `_15`.
- `NavigationTest:open_a_photo_asks_only_for_photo_access` -> the same three `each_shortcut_and_try_now_asks_only_for_photo_access_on_android_*` (R4: the "Open a photo" button is gone).
- `NavigationTest:settings_opens_and_back_returns_home` -> `the_settings_tab_opens_settings_without_a_back_button_and_espresso_back_returns_home` and `the_settings_tab_then_dispatcher_back_returns_home` (R6).
- `NavigationTest:the_camera_shortcut_asks_only_for_camera` -> `from_home_the_camera_button_asks_only_for_camera_and_back_returns_home` and `from_settings_the_camera_button_asks_only_for_camera_and_back_returns_to_settings` (R4: the round button replaces the shortcut).
- `NavigationTest:the_camera_shortcut_opens_the_camera_and_keep_opens_the_editor_on_edit` -> `from_home_keep_opens_the_edit_tool_and_one_back_returns_home` and `from_settings_keep_opens_the_edit_tool_and_one_back_returns_to_settings`.
- `PhotoToToolNavigationTest:each_tool_card_opens_its_gallery_then_the_editor_with_that_tool_selected` -> `each_shortcut_on_home_opens_its_gallery_then_its_own_tool_screen`, `each_try_now_opens_its_gallery_then_its_own_tool_screen`, `each_banner_opens_its_gallery_then_its_own_tool_screen` (R5: cards replaced by shortcuts and the carousel).
- `SavedNavigationTest:home_leaves_only_home` -> `close_leaves_only_home` (R7: the round close button takes the person Home).
- `SavedScreenTest:home_calls_back` -> `the_close_button_calls_on_close_and_nothing_else` (R7).
- `SavedScreenTest:each_action_shows_an_icon_inside_its_button_left_of_its_label` -> `the_wide_button_shows_an_icon_left_of_its_label` (Saved has Share and one wide button now).
- `SavedScreenTest:the_three_actions_are_one_button_group_in_a_single_row` -> `share_and_the_wide_button_share_one_row_across_the_width_on_android_11` and `_15`.
- `SavedScreenTest:the_three_actions_are_shown_and_large_enough_to_tap` -> the two row tests above (both actions shown) and `AccessibilityWalkTest:saved` (touch size, through `TouchTargetCheck`).
- `SavedScreenTest:the_success_mark_plays_once_and_then_rests` -> `the_success_mark_plays_once_and_then_rests_on_android_11` and `_15`.
- `ToolScreenTest:the_toolbar_and_the_panel_clear_the_bottom_gesture_zone_when_the_navigation_bar_is_zero_tall` -> `save_clears_the_bottom_gesture_zone_when_the_navigation_bar_is_zero_tall` (no toolbar any more; Save is the control at the bottom edge).
- `ToolScreenTest:back_save_and_every_toggle_are_at_least_48_dp` -> `AccessibilityWalkTest:tool_screen_<tool>_ready` for each tool (every control's size through `TouchTargetCheck`); Back's 48 dp is still asserted inside `ToolScreenTest`. The toggles are gone (R1).

## Removed (the behaviour is gone; the Removal says which)

- `ToolSelectionAnimationTest`: `selecting_a_tool_animates_its_toggle_and_then_settles`, `selecting_a_tool_without_a_touch_animates_its_toggle_and_then_settles`, `with_animations_removed_a_tool_switch_settles_at_once_if_the_host_carries_the_setting`. R1: the toolbar and its toggles are gone, and the file is deleted.
- `ToolScreenTest:the_toolbar_switches_between_the_five_editing_tools`, `the_toolbar_switches_the_five_tools_in_place_without_reloading_the_photo`: R1 and R2.
- `ToolScreenTest:the_toggles_are_switched_off_while_a_save_runs`: R1, no toggles to switch off.
- `ToolScreenTest:the_photo_the_panel_and_the_toolbar_stack_from_top_to_bottom`: R1, no toolbar in the stack. The photo's place is proved by `ToolBackdropTest` and `each_tool_has_its_own_screen_with_back_its_name_the_photo_card_replace_its_strip_and_save`.
- `ToolViewModelTest:select_tool_changes_only_the_tool`, `select_tool_is_ignored_while_a_save_runs`, `select_tool_keeps_the_decoded_photo_and_does_not_load_again`, `select_tool_keeps_where_saving_stands`, `selecting_the_selected_tool_changes_nothing`: R2, `selectTool` is gone. "A save keeps the decoded photo" remains as `a_save_keeps_the_decoded_photo_and_does_not_load_again`.
- `ToolPhotoKeyTest:process_death_keeps_a_switched_tool_and_the_photo`, `recreate_keeps_a_switched_tool_and_the_photo`: R2. Their un-switched twins `process_death_keeps_the_tool_and_the_photo` and `recreate_keeps_the_tool_and_the_photo` stay.
- `SavedNavigationTest:the_editor_switches_tools_in_place_on_the_chosen_photo`, `edit_another_after_switching_tools_opens_the_switched_tools_gallery`, `home_after_switching_tools_leaves_only_home`: R2 (and R7 for the last). `edit_another_leaves_home_and_the_tools_gallery` stays.
- `PhotoToToolNavigationTest:open_a_photo_opens_the_edit_gallery_then_the_editor_on_edit`: R4, the "Open a photo" button is gone. The Edit tool is reached by its shortcut and its banner, tested above.

Count: 10 renamed, 17 replaced, 18 removed entries; the fanned names add up to all 45 lines of `comm-removed.txt`.

## Files changed or deleted (`test-name-status.txt`)

`D` (1): `tool/ToolSelectionAnimationTest.kt`: R1.

`A` (28, new): `TestKansoApplication.kt` (the test application for the tab shell); `a11y/BottomClearanceTest.kt`, `a11y/OverPhotoContrastTest.kt`; `gallery/GalleryTopBarTest.kt`; `home/{EditsLoadingTest,HomeReachTest,HomeRecentTest,HomeScreenTest}.kt` (Home and the recent strip); `myedits/{EditsSourceTest,MyEditsTest,MyEditsViewModelTest}.kt` and the helpers `FakeSavedPhotoSource.kt`, `RecordingMediaProvider.kt` (My edits); `navigation/{BackStackActionsTest,MyEditsViewerNavigationTest,NavBarColourTest,NavBarTest,NavStateTest,ReplaceNavigationTest}.kt` and the helpers `HomeTaps.kt`, `TabTaps.kt` (tab bar, back stack, Replace); `screenshots/{HomeScreenshotTest,MyEditsScreenshotTest}.kt`; `settings/{PickerHexInputTest,SettingsRowsTest,ThemeColourHexTest}.kt` (Settings cards, the hex field); `tool/ToolBackdropTest.kt`.

`M` (30), one reason each:
- `LaunchTest.kt`, `navigation/NavTransitionTest.kt`, `previews/PreviewsRenderTest.kt`: start from the tab shell, new previews.
- `ScreenStateTest.kt`: seven view models.
- `a11y/AccessibilityWalkTest.kt`: walks the new states, tool screens renamed; `a11y/TouchTargetCheck.kt`: helper extended for the new walks.
- `camera/CameraControlsTest.kt`, `CameraFlowTest.kt`, `CameraPermissionTest.kt`, `CameraScreenTest.kt`: the Camera's Back button and button shapes (DoD 16), and the shell around it.
- `home/HomeViewModelTest.kt`: carousel, the five shortcuts, the recent strip.
- `navigation/NavigationTest.kt`, `PhotoToToolNavigationTest.kt`, `SavedNavigationTest.kt`: renamed, replaced or removed as listed above.
- `saved/SavedScreenTest.kt`, `saved/SavedViewModelTest.kt`: the full-bleed Saved screen and its viewer mode; `screenshots/{SavedScreenshotTest,ScreenshotTest,ToolScreenshotTest}.kt`: pictures of the changed screens.
- `settings/{PickerNoAlphaTest,ThemeColourRestartTest,ThemeColourSettingsTest,ThemeModeRestartTest,ThemeModeSettingsTest,ThemeSwatchTest}.kt`: Settings is a tab now (no Back button, reached from the bar), and the picker has its hex field.
- `theme/{KansoBlueContrastTest,ThemeFollowTest}.kt`: the new bar and screen pairs (DoD 19, 23).
- `tool/{ToolFixtures,ToolPhotoKeyTest,ToolScreenTest,ToolViewModelTest}.kt`: R1 and R2 as listed above.

No base test whose subject still exists in the app was only removed. `orphans.sh` prints nothing (R8, helpers).
