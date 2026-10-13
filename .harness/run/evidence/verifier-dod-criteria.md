## Acceptance Criteria

### Behaviour

1. [machine] A floating, rounded bar near the bottom of Home, My edits and Settings holds three tabs: Home, My edits and Settings, each an icon over its name, the current one on a raised pill. Beside the bar is a round Camera button. Tapping a tab shows that screen, and the bar appears on no other screen.
   Proof: `NavBarTest` (new, the bar and its shell hosted), `NavigationTest` (the whole app; its My edits clauses are written with the My edits tab's own tests) and `MyEditsViewerNavigationTest` (the viewer clause) at SDK 35 on the phone. Start: fresh install, photo access granted, with `FakePhotoSource`, `FakePhotoLoader`, `FakeCameraController` and an edits source of 3 photos swapped in before launch.
   - On Home, `nav-bar` is rounded and holds exactly three nodes with `Role.Tab`, left to right: `nav-tab-HOME`, `nav-tab-MY_EDITS`, `nav-tab-SETTINGS`, with the texts "Home", "My edits" and "Settings".
   - Each tab holds an icon node `<tab>-icon` (unmerged tree). The icon's horizontal centre is within 1 dp of its label's, and its bottom is at or above the label's top.
   - Home `assertIsSelected()`; the other two `assertIsNotSelected()`.
   - `nav-camera` lies outside `nav-bar`, to its right, with vertical centres within 4 dp. It is round, has the content description "Camera" and no visible text.
   - With a 0 dp inset sent, the bottoms of `nav-bar` and `nav-camera` are at least 8 dp above the window's bottom.
   - Tapping each tab shows its screen (`home-screen`, `my-edits-screen`, `settings-screen`), selects that tab and deselects the others. Tapping the selected tab again changes nothing.
   - After transitions settle, `nav-bar` exists exactly once on Home, My edits and Settings, and does not exist on: the gallery of each tool, each of the five tool screens, Saved, a photo opened from My edits, or the camera (live and refused).
   - While the keyboard is open (an IME inset sent), `nav-bar` and `nav-camera` are not displayed (Q13).

2. [machine] Back from My edits or Settings goes to Home, and Back on Home closes the app. Moving between the tabs never piles them up. (Q2)
   Proof: `NavigationTest` at SDK 35, fresh install. Each step is driven once with `Espresso.pressBack()` and once with `onBackPressedDispatcher.onBackPressed()`.
   - Home, then My edits, then Back: Home is shown and selected. Home, then Settings, then Back: Home.
   - Home → My edits → Settings → My edits → Back: Home. One more Back leaves the activity finishing (`isFinishing`).
   - Back on Home at launch finishes it.

3. [machine] The round Camera button opens the camera from any of the three tabs. A kept photo opens in the Edit tool, and Back from there returns to the tab the person started on. (Q3)
   Proof: `NavigationTest` and `CameraFlowTest` at SDK 35, `FakeCameraController`.
   - From each of Home, My edits and Settings, a tap on `nav-camera` shows `camera-screen`. With camera access not yet granted, it records only CAMERA (`RecordingRegistry`).
   - Back returns to that tab, still selected.
   - Shutter then Keep shows the Edit tool screen on the kept photo (title "Edit", `TOOL_PHOTO_URI_KEY` the kept Uri); one Back returns to the starting tab.
   - A double tap on `nav-camera` opens one camera: one Back leaves it.

4. [machine] Home shows, from top to bottom: "Kanso" as a large title with the greeting "What will you edit today?" under it; a carousel of the four AI tools, each banner with the tool's name, a one-line description, its icon drawn large and a "Try now" button, with page dots under it; a row of five round tool shortcuts directly under the dots, in this order: Edit, Remove background, Enhance, Retouch, Artistic, each a round icon button with its name under it; and the "Recent edits" strip with a round arrow button.
   Proof: `HomeScreenTest` (new) at SDK 35 on the phone, hosting `HomeContent` with a sample state of 3 recent photos, plus `HomeViewModelTest`.
   - The `positionInRoot` tops increase strictly in this order: (1) the text "Kanso", with heading semantics; (2) the text "What will you edit today?" (exact); (3) `home-carousel`; (4) `home-shortcuts`, the row of the five `home-shortcut-<TOOL>` buttons; (5) the text "Recent edits", with `home-recent-more` on its row (round, with a content description, centres within 4 dp); (6) `home-recent`.
   - The five shortcuts, left to right in the order `home-shortcut-EDIT`, `-REMOVE_BACKGROUND`, `-ENHANCE`, `-RETOUCH`, `-ARTISTIC`, have their tops within 1 dp of one another and are all in view at once (no scrolling of the row: it is not a scrolling container). Each is round, has a click action and the tool's name as its content description, and has its name as visible text (`home-shortcut-<TOOL>-label`, tagged text equal to `Tool.labelRes`) under it, the label's horizontal centre within 1 dp of the button's and its top at or below the button's bottom. The five centres are evenly spaced within 1 dp.
   - `home-carousel` holds four banners in this order: `home-banner-REMOVE_BACKGROUND`, `-ENHANCE`, `-RETOUCH`, `-ARTISTIC`. Each banner, reached by swiping, shows the tool's label, a description of its own (non-empty, different for each tool, `maxLines = 1`), an icon node `<banner>-icon` with the tool's `iconRes` and at least 64 dp square, and a button with the text "Try now" (`home-try-<TOOL>`).
   - `home-carousel-dots` holds four dots, with the first marked current (`stateDescription` or selected) at launch.
   - Home's state lists the carousel tools in that order and the shortcut tools `[EDIT, REMOVE_BACKGROUND, ENHANCE, RETOUCH, ARTISTIC]`.

5. [machine] Every "Try now", every banner and every tool shortcut open the photo gallery for their tool at once, with no screen in between, and the photo picked there opens in that tool.
   Proof: `PhotoToToolNavigationTest` at SDK 35, photo access granted, `FakePhotoSource`.
   - For each AI tool, each of these puts the gallery titled "<tool>: choose a photo" on top: a tap on its "Try now" after swiping to it, and separately a tap on the banner outside the button (Q12). One Back then reaches Home, so the stack was `[Home, Gallery(tool)]`. Choosing photo 1 then shows that tool's screen on photo 1.
   - The same holds for each of the five `home-shortcut-<TOOL>`: one tap on it, with no swipe and nothing else, puts the gallery titled "<tool>: choose a photo" on top; one Back reaches Home; choosing photo 1 shows that tool's screen on photo 1.
   - A double tap opens one gallery.

6. [machine] On a small phone, right after launch and with nothing scrolled, all five tool shortcuts, the first banner's "Try now", the Camera button, the recent-edits arrow and the newest recent photo are fully on screen, and each is one tap from where it leads. (Q1, as the owner decided it: every tool is one tap, no swipe.)
   Proof: `HomeReachTest` (new) at SDK 35, portrait, font scale 1.0, Kanso blue light.
   - It runs on the small phone and on the phone, each with a 24 dp status bar and a 48 dp three-button navigation bar sent as insets, and again with a 0 dp navigation bar and a 32 dp bottom gesture inset. Two starting states: (i) fresh install with an empty edits source; (ii) an edits source with 5 photos.
   - Right after Home is idle, with no scroll and no swipe (Home's scroll value 0, carousel on banner 1), each of these has a click action, a touch target of at least 48 dp, `boundsInRoot` wholly inside the window and below the status bar, and (if outside the bar) a bottom at or above the tops of `nav-bar` and `nav-camera`: the five `home-shortcut-<TOOL>` (`EDIT`, `REMOVE_BACKGROUND`, `ENHANCE`, `RETOUCH`, `ARTISTIC`), `home-try-REMOVE_BACKGROUND`, `nav-camera`, `home-recent-more`, the three tabs, and in state (ii) `home-recent-0`. In state (i) `home-recent-empty` is likewise wholly on screen and above the bar. The five shortcuts are also wholly inside the window's width and not clipped by a scrolling container.
   - From a fresh launch for each target, exactly one `performClick()` and nothing else gives: each of the five shortcuts → that tool's gallery (titled "<tool>: choose a photo"); Remove background's "Try now" → its gallery; Camera → `camera-screen`; arrow → My edits selected; `home-recent-0` → the Edit tool screen on the newest photo's Uri; each tab → its screen.
   - The test prints a table of the gestures used per target to its log, one row each, and every row for a tool shortcut reads "1 tap" (no swipe, no scroll); the Verifier copies it into `.harness/run/evidence/home-reach.md`. The banners of Enhance, Retouch and Artistic stay reachable by swiping the carousel (criteria 5 and 28), but no promise of this criterion rests on that swipe.

7. [machine] The "Recent edits" strip shows Kanso's saved photos, newest first, as tall rounded cards that scroll sideways. Tapping a card opens that photo in the Edit tool, and the arrow opens My edits. A fresh install shows a short message instead. A photo just saved is first in the strip when the person comes back to Home.
   Proof: `HomeRecentTest` (new) at SDK 35, MainActivity, edits source swapped.
   - (a) With 12 photos of distinct dates, `home-recent-0`, `-1`, ... follow the source's newest-first order, up to the strip's limit of 10 (Q10). Each card is taller than it is wide, rounded, and has a content description (Q9). A left swipe on `home-recent` moves the first card's left edge left while Home's scroll value stays 0.
   - (b) Tap `home-recent-0`: the Edit tool screen on that photo's Uri; one Back gives Home.
   - (c) `home-recent-more` selects My edits.
   - (d) With an empty source, `home-recent-empty` shows the empty message string (exact text). No `home-recent-<i>` and no placeholder exists.
   - (e) `home-shortcut-EDIT` → photo 1 → Save, where `FakePhotoSaver` returns Uri X and the fake source adds X as newest → Saved → `saved-close`. Within 10 s of real time, `home-recent-0` carries X.

8. [machine] While the recent photos and My edits load, soft placeholders in the shape of the cards and tiles show instead of a spinner. They give way to the photos once loaded.
   Proof: `EditsLoadingTest` (new) at SDK 35 on the phone, MainActivity, with an edits source whose read waits on a gate. The clock is paused, or the test waits with `waitUntil` while stepping frames (PROJECT.md, loading-indicator trap).
   - Gate closed: Home shows at least two `home-recent-skeleton-<i>`, and My edits at least six `my-edits-skeleton-<i>`. Each placeholder's width and height equal a real card's or tile's within 1 dp (measured after the gate opens), and each is rounded. The strip and the grid each carry one content description saying photos are loading; the placeholders have none of their own. No node under `home-recent` or `my-edits-screen` has `ProgressBarRangeInfo`, and the empty message is absent.
   - Gate opened: within 10 s no placeholder remains, and the cards and tiles show.
   - `git grep -n -E '(LoadingIndicator|ContainedLoadingIndicator|CircularProgressIndicator|LinearProgressIndicator)\(' -- app/src/main/java/com/phongkaster/kanso/ui/home app/src/main/java/com/phongkaster/kanso/ui/myedits` prints nothing.
   - Pictures 32, 33, 36 and 37 (criterion 29).

9. [machine] My edits shows every photo Kanso has saved, newest first, as a grid of rounded tiles, with a message when there are none. Tapping a photo opens it full screen with Share and Edit; closing it goes Home and Back returns to My edits. (Q4, Q6, Q7)
   Proof: `MyEditsTest` (new, the screen hosted), `MyEditsViewModelTest` (new) and `MyEditsViewerNavigationTest` (new, the whole app, MainActivity) at SDK 35, edits source swapped.
   - With 40 photos, `my-edits-grid` tiles `my-edits-tile-<i>` in reading order (rows top to bottom, then left to right, by `positionInRoot`) follow the source's newest-first order. Tiles are rounded, each has a content description (Q9), and scrolling reaches the 40th tile.
   - With an empty source, `my-edits-empty` shows its message (exact text) and no tile exists.
   - A tap on tile 3 shows the full-screen photo screen, with: photo 3's Uri on `saved-photo`; a round `saved-share` (description "Share", no visible text); a wide button with the text "Edit" (`saved-edit-another`); a round `saved-close`; the title "My edits"; no `saved-mark` and no `nav-bar`.
   - Share starts the chooser with photo 3's `content://` Uri, checked as in `SavedScreenTest.share_opens_the_chooser_for_the_photo`. Edit shows the Edit tool screen on photo 3.
   - `saved-close` shows Home. System Back shows My edits with tile 3 still on screen.

10. [machine] Each of the five tools has its own screen. It has a round Back button at the top left and the tool's name centred at the top. The photo sits in a large rounded card with a "Replace" chip floating at its bottom; the tool's "coming in a later run" strip is under it; one full-width Save button is at the bottom. There is no way to switch tools on it. (Q15)
    Proof: `ToolScreenTest` (rewritten) and `ToolViewModelTest` at SDK 35 on the phone, once per tool, with the 600x400 `ToolFixtures.gradient` photo. Each tool screen is reached as `PhotoToToolNavigationTest` does (photo access granted, fakes swapped).
    - `tool-back` is round with the description "Back", its left within 24 dp of the window's left, and calls Back.
    - The tool's name is the only text on the screen equal to any tool label, and it is centred.
    - `tool-card` is full-width and rounded and holds `tool-image`. The image lies inside the card and fills it in one direction: its width equals the card's inner width, or its height the card's inner height, within 1 px, and the image's width-to-height ratio equals the photo's within 1 %.
    - `tool-replace` (text "Replace") lies inside the card, its bottom within 24 dp of the card's bottom.
    - `editor-panel` lies below the card and shows that tool's own string, ending "coming in a later run.".
    - `tool-save` (text "Save") is full-width and below the panel.
    - No node on the screen is toggleable, and no `editor-toolbar` exists. `ToolUiState.tool` equals the key's tool for the ViewModel's whole life.
    - `FakePhotoLoader` is called exactly once per screen. An unreadable photo shows `tool-unreadable`, with Save disabled. While decoding, `tool-loading` shows (paused clock).

11. [machine] "Replace" goes back to the photo gallery of the same tool, whichever way the tool screen was opened, and the photo picked there opens in the same tool. (Q16)
    Proof: `ReplaceNavigationTest` (new) at SDK 35, photo and camera access granted, fakes swapped.
    - (a) Retouch "Try now" → photo 1 → Replace: the gallery titled "Retouch: choose a photo" is on top, and one Back reaches Home. Photo 2 then gives the Retouch screen on photo 2.
    - (b) `home-recent-0` → Edit screen → Replace: the Edit gallery is on top, and one Back reaches Home.
    - (c) Camera → Keep → Edit screen → Replace: the Edit gallery, and one Back reaches Home.
    - (d) My edits → tile → Edit → Replace: the Edit gallery. Back reaches the photo screen, and another Back reaches My edits.
    - A double tap on Replace opens one gallery.

12. [machine] Saving works as before on every tool screen. After a save, "Edit another" opens the gallery of the tool the photo was saved from, and the close button returns to Home.
    Proof: `SaveButtonTest` and `SavedNavigationTest` at SDK 35, plus the SDK 27 and 28 write-permission cases of `SaveButtonTest`.
    - Save is disabled until the photo is ready. One tap makes one save (`FakePhotoSaver` counts 1). While saving, Save shows "Saving…" and a progress node and is disabled. The failed, refused and kept-orientation messages show as today.
    - From the Retouch screen, Save shows Saved. "Edit another" then shows the gallery titled "Retouch: choose a photo", and one Back reaches Home.
    - `saved-close` leaves Home alone: one further Back finishes the activity. System Back on Saved shows Home.

13. [machine] The photo gallery has a round Back button, the centred title "<tool>: choose a photo", album chips and rounded tiles, like the tool screens. (Q8)
    Proof: `GalleryTopBarTest` (new), `GalleryGridTest`, `GalleryPermissionTest` and `GalleryLoadingScreenshotTest` at SDK 35.
    - `gallery-back` is round, has the description "Back", and calls Back. The title "<tool>: choose a photo" is centred for each tool.
    - The chip tests (`gallery-album-...`, filtering, reading order, scroll kept) and the rounded-cell pixel test pass. The refused, partial, empty and loading states keep their tags and behaviour.
    - `gallery-camera` keeps its behaviour (`SavedNavigationTest.gallery_camera_keep_opens_the_galleries_own_tool`).
    - Pictures 10 to 13, 26 and 27.

14. [machine] After a save, the whole photo is shown as large as fits, over a blurred copy of itself that fills the rest of the screen, with a soft shade behind a round close button and the title at the top. A round Share button sits beside a wide "Edit another" button at the bottom, and the success mark plays once over the top of the photo. (Q5, Q7)
    Proof: `SavedScreenTest` (rewritten) at SDK 35 on the phone, paused clock, `SavedContent` with a 600x800 photo and a 600x400 photo.
    - `saved-backdrop` covers the whole window. `saved-photo` lies inside the window, is centred, and fills it in one direction (its width equals the window's, or its height does), and its width-to-height ratio equals the photo's within 1 %.
    - The backdrop beside or above the photo is filled (it differs from the window's plain background), comes from the photo (the upper half is reddish and the lower half bluish for the red-and-blue test photo) and is soft (horizontally adjacent pixels differ by at most 12 per channel): the same pixel checks as criterion 27, here on Saved.
    - `saved-close` (round, with a description, its left within 24 dp of the window's left) and the title "Saved" lie in the top 20 % of the window, over `saved-scrim`.
    - The shade is the scheme's `surface` role fading from alpha 0.85 at the top edge to alpha 0 at its bottom. With a photo that contrasts with the mode (all black in light mode, all white in dark mode), the pixel 4 dp below the window's top equals `surface` blended over the photo's pixel at alpha 0.85 within ±3 (the test computes it from both), and it differs from the photo's own pixel at the shade's bottom edge by at least 0.2 in relative luminance.
    - At the bottom, `saved-share` (round, description "Share", no visible text) sits on the left of `saved-edit-another` (text "Edit another", at least twice Share's width). Their vertical centres are within 1 dp, and together they span the full width.
    - `saved-mark` has a description and its centre lies in the top quarter of the window, centred; it is drawn differently at 0, 250 and 1 000 ms, and identically at 1 500 and 3 000 ms; it does not play again after `recreate()`.
    - The share tests (chooser; content Uri on Android 9) pass. "Edit another" and close call their callbacks.
    - Pictures 18, 19, 43 and 44 pin the layout (criterion 29); how the blur and the shade look is judged under criteria 27 and 29.

15. [machine] Settings is a tab with no Back button. Its choices are the same as today, grouped in rounded cards whose rows each have an icon in a tinted rounded square, a title and a one-line description.
    Proof: `ThemeModeSettingsTest`, `ThemeColourSettingsTest`, `ThemeSwatchTest` and `SettingsRowsTest` (new) at SDK 35, with Settings reached by `nav-tab-SETTINGS` from a fresh install.
    - No node with the content description "Back" exists on `settings-screen`.
    - `settings-group-appearance` and `settings-group-colour` are rounded.
    - Every row holds `<row>-icon` inside `<row>-icon-badge`. The rows are `theme-mode-system`, `-light` and `-dark`; `theme-colour-kanso-blue`; `-wallpaper` at SDK 35; and `-custom`.
    - Each badge is a rounded square (width equals height within 1 dp). Its pixel equals one scheme container role within ±2 and differs from the group's colour by more than 8 per channel.
    - Each row also holds its existing title and `<row>-description`, which is non-empty, drawn on one line and different for each row.
    - The existing selection, swatch, picker and restart assertions pass. At SDK 30 there is no wallpaper row.
    - Pictures 4, 5, 6, 24 and 25 pin how the cards and tinted squares look (criterion 29).

16. [machine] The camera works exactly as before; only its Back button and its buttons' shapes change to match the other screens.
    Proof: `CameraControlsTest`, `CameraScreenTest`, `CameraFlowTest`, `CameraPermissionTest` and `CameraControlsScreenshotTest` pass. Their behaviour assertions are unchanged: shutter, flash cycle, lens switch, zoom, Keep, Retake, refused, unavailable and starting.
    - `camera-back` is round with the description "Back".
    - Every button call in `ui/camera/CameraScreen.kt` that has a `shapes` overload passes `shapes =` (listed in criterion 19's scan).
    - Edits to these tests concern only how the camera is reached (now `nav-camera`) and the new `camera-back` assertions, each listed in `test-edits.md`; no behaviour assertion is loosened.

17. [machine] The colour picker reads a typed colour code as the colour it names. A complete six-character code gives exactly that colour, fully opaque; a shorter one gives nothing; the letters A to F work. (Q19)
    Proof: `PickerHexInputTest` (new) at SDK 35 on the phone. It hosts the engine's `HSVColorPicker` alone, as `PickerDefaultAlphaTest` does: once with `showAlpha = false` and once with the default `showAlpha = true`. Each picker starts at `Color.White`, and the test records every colour passed to `onColorChanged`. The field is the node with `EditableText` under the host's tag (unmerged tree).
    - (a) `performTextReplacement("2E7D32")`: the last recorded colour's `toArgb()` is exactly `0xFF2E7D32.toInt()`, alpha 1.0, no tolerance; the field reads `2E7D32`; in the `showAlpha = true` picker the percentage reads `100%`.
    - (b) Typing `2`, `E`, `7`, `D`, `3`, `2` with one `performTextInput` each: nothing is recorded after the first five characters, and exactly one colour, `0xFF2E7D32`, after the sixth.
    - (c) From a fresh picker, `performTextReplacement` of each length from 1 to 5 (for example `2E7`): nothing is recorded, and the field reads what was typed.
    - (d) `ABCDEF` gives `0xFFABCDEF`; `1A73E8` gives `0xFF1A73E8`; `000000` gives `0xFF000000`; `FFFFFF` gives `0xFFFFFFFF`.
    - (e) Lowercase `2e7d32` gives `0xFF2E7D32`, and the field shows `2E7D32`.
    - (f) Characters that are not hex digits are dropped: `#2E-7D 32` gives `0xFF2E7D32`.
    - Evidence of the bug, taken once at `<base>` by running (a) there: nothing is recorded, and typing `000002` records `0x00000002`.

18. [machine] In Settings, typing 2E7D32 into the colour code box turns the whole app that green and keeps it after a restart; an unfinished code changes nothing.
    Proof: `ThemeColourHexTest` (new). Start: SDK 35, `notnight`, fresh install (empty settings file); Settings tab; Custom colour chosen (`theme-colour-custom`); the picker shown.
    - The field (the editable node under `custom-colour-picker`) is replaced with `2E7D32`. One deadline is taken at that keystroke (PROJECT.md: one budget from the tap).
    - Within 10 s of real time: the stored colour, polled in real time with `awaitSavedThemeColour`, is `ThemeColour.Custom(0xFF2E7D32.toInt())`; the scheme on the `kanso-theme` node equals `seedColorScheme(Color(0xFF2E7D32), dark = false)` in the seven roles of `rolesOf`; the Custom swatch's centre pixel is `#2E7D32` within ±2.
    - The field is then replaced with `2E7`. After 2 s of real time (more than six times the 300 ms settle time), the stored colour is still `Custom(0xFF2E7D32)` and the scheme is unchanged.
    - After `recreate()`, and with a fresh store opened over the same file (`writeThenReopen`), the colour reads `Custom(0xFF2E7D32)`. At SDK 30 the first step passes too.

19. [machine] Every piece of the new layout is a Material 3 component wherever one exists, the carousel included; anything built by hand says why.
    Proof: `.harness/run/evidence/m3-scan.md`, from a scan of `app/src/main` only, names the call that draws each piece:
    - the floating bar, its tabs and the Camera button;
    - the carousel (a Material 3 carousel from material3 1.5.0-beta01: `HorizontalMultiBrowseCarousel`, `HorizontalUncontainedCarousel` or `HorizontalCenteredHeroCarousel`);
    - the "Try now" buttons and the five round tool shortcuts on Home (round icon buttons: `FilledTonalIconButton` or the nearest Material 3 icon button, with `shapes =`);
    - the round Back, close, Share and arrow buttons, and the "Replace" chip;
    - the full-width Save and "Edit another" buttons, and the album chips;
    - the Settings cards and rows (no `ListItem(headlineContent =` call);
    - the recent strip and the My edits grid (lazy layouts);
    - the shade on Saved, the placeholders and the page dots.
    The scan has these rules:
    - Each piece drawn without a Material 3 component carries a one-line reason, such as "no Material 3 page indicator in 1.5.0-beta01".
    - Every call in `app/src/main` to `Button`, `OutlinedButton`, `FilledTonalButton`, `TextButton`, `IconButton`, `FilledIconButton`, `FilledTonalIconButton`, `IconToggleButton` or `ToggleButton` that has a `shapes` overload passes `shapes =`, and the scan lists each one.
    - `MaterialExpressiveTheme(` is still called in `KansoTheme.kt` (`KansoBlueThemeTest` passes). A silent gap fails the criterion.

20. [machine-then-human] Motion stays Material 3 Expressive, built with Compose's own animation tools. Tab changes, screen changes, the carousel, the placeholders and button presses move with purpose, and the phone's "Remove animations" setting stills them.
    Proof: the machine pre-check is reported as "did not fail when driven from X", never as "works":
    - (a) `git grep -n -E 'ObjectAnimator|ValueAnimator|AnimatorSet|android\.view\.animation|ViewPropertyAnimator|\.animate\(\)' -- app/src/main` prints nothing, and `git grep -n -i -E 'lottie|airbnb|shimmer' -- app/src/main gradle/libs.versions.toml app/build.gradle.kts` prints nothing (a placeholder's pulse is built with `rememberInfiniteTransition` or `Animatable`; the word "shimmer" is not used in `app/src/main`).
    - (b) `NavTransitionTest` passes and gains a tab case. Driven at SDK 35 with the clock paused, on a `NavDisplay` built with the tab fade's entry metadata (Home and Settings entries): a few frames after the move both screens are composed; 2 000 ms later only the new one is. Between two tabs the move is a fade (the incoming screen's left stays at 0), not a slide, and a push to a non-tab key still slides (Q17). `NavigationTest` shows the same on the real app: after a tap on `nav-tab-MY_EDITS`, only My edits is composed within 2 000 ms.
    - (c) `PredictiveBackGestureTest` and `PredictiveBackOpacityTest` pass.
    - (d) No Compose test with an auto-advancing clock shows an animation that never settles, and the placeholders' preview is a still picture (`PreviewsRenderTest` passes inside its 60 s watchdog); criterion 14's play-once check passes.
    - (e) The "Remove animations" passes print "not driven by machine" on this host and assert the animation still runs (PROJECT.md: the setting does not reach Compose under Robolectric).
    The person, on the Galaxy A13 (criterion 30): with Remove animations off, tab changes, pushes, the carousel's snap, the placeholders and press feedback feel deliberate, and nothing jitters. With it on (step 12), screen and tab changes are instant, the placeholders do not move, and the success mark appears at rest.

### Permissions the user is asked for

21. [machine] The redesign asks for nothing new. Launching, Home, the recent strip and My edits ask for no permission; the gallery and the camera ask as before; and there is still no internet access. (Q11)
    Proof: after criterion 34's `assembleDebug`, run `aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk | tr -d '\r' | grep -E '^(uses-permission|uses-feature)' > .harness/run/evidence/head-badging.txt`.
    - `diff <(tr -d '\r' < .harness/run/evidence/base-badging.txt) <(tr -d '\r' < .harness/run/evidence/head-badging.txt)` prints nothing (a checkout may turn line ends into CRLF), and `grep -c -E 'INTERNET|ACCESS_NETWORK_STATE' .harness/run/evidence/head-badging.txt` prints 0.
    - `NavigationTest`, at SDK 35 and at SDK 27, with no permission granted (`RecordingRegistry`): launching, showing Home with its strip, and tapping `home-recent-more`, `nav-tab-MY_EDITS` and `nav-tab-SETTINGS` record no request; at SDK 27 Home and My edits show their empty messages and nothing throws; each tool shortcut and each "Try now" record only the photo set; `nav-camera` records only CAMERA.
    - `GalleryPermissionTest`, `CameraPermissionTest` and the SDK 27 and 28 cases of `SaveButtonTest` pass.

### Appearance and reachability

22. [machine] The floating bar is dark in light mode and light in dark mode, in the app's own colours, for Kanso blue, From wallpaper and Custom colour. The selected tab's pill, and every label and icon on the bar, stand out clearly. (Q22)
    Proof: `NavBarColourTest` (new) at SDK 35 on the phone, `@GraphicsMode(NATIVE)`. Home is shown inside `KansoTheme` with each of the six schemes, first with Home selected and then with My edits selected.
    - A pixel of `nav-bar` between two tabs equals the scheme's `inverseSurface` within ±2. Its relative luminance is below 0.15 in light and above 0.6 in dark.
    - The pixel inside the selected tab's pill, beside its icon, equals the scheme's `inversePrimary` within ±2, and its contrast with `inverseSurface` is at least 3.0.
    - The selected tab's icon colour has contrast at least 4.5 with the pill; its label and the unselected tabs' icons and labels have contrast at least 4.5 with `inverseSurface`. Inside each tab icon's bounds, the most frequent colour other than the ground equals its role within ±8.
    - `nav-camera`'s container pixel equals its container role within ±2, and its icon role has contrast at least 3.0 with that container.
    - Kanso blue's pairs are also added to `KansoBlueContrastTest`.
    - Pictures 1, 2, 28, 29, 34 and 35 show the bar in the light and dark pictures (criterion 29).

23. [machine] Every screen, new and old, follows the app's light or dark choice and its colour choice. Nothing paints a colour of its own, InFlux's teal appears nowhere, and the carousel banners are made from the theme's colours and the tools' icons, with no pictures.
    Proof: `ThemeFollowTest` is extended. It keeps its fourteen checks and adds, at SDK 35 with Kanso blue under `ThemeMode.Light` and `ThemeMode.Dark`: Home with recent photos, and Home empty; My edits with photos, and My edits empty; each of the five tool screens with a ready photo; the photo opened from My edits.
    - In each screen, a pixel at the top and one in the background (outside photos) equal the mode's `surface`, `background` or top-bar container within ±2, with luminance above 0.6 in light and below 0.15 in dark.
    - Each banner's background pixel equals one of `primaryContainer`, `secondaryContainer` or `tertiaryContainer` within ±2, for Kanso blue and Custom `#2E7D32`, light and dark.
    - `git grep -n -E 'Color\(0x|Color\.(Black|White)' -- app/src/main` finds only `PickerSurfaceColor = Color.White` and lines inside `sample...` declarations or `@Preview` functions, each listed in `.harness/run/evidence/fixed-colours.md`.
    - `git diff --exit-code <base> HEAD -- app/src/main/res/values/colors.xml app/src/main/res/values-night/colors.xml` exits 0.
    - `git diff --name-only --diff-filter=A <base> HEAD -- app/src/main/res | grep -i -E '\.(png|jpe?g|webp|gif)$'` prints nothing (no bitmap art).

24. [machine] Text and buttons drawn over a photo stay readable on a white photo and on a black one, in light and in dark.
    Proof: `OverPhotoContrastTest` (new) at SDK 35 on the phone, `@GraphicsMode(NATIVE)`, Kanso blue light and dark. It draws Saved, the photo opened from My edits and each tool screen, each with an all-white and an all-black 600x800 photo. Contrast is WCAG contrast from pixels:
    - Saved's title, between the lightest and the darkest pixel inside its text bounds: at least 4.5.
    - `saved-close`'s and `saved-share`'s icon against their container: at least 3.0.
    - The "Replace" text against the chip's container: at least 4.5.
    - The success mark's check against the mark: at least 3.0.

25. [machine] Every control that does something has a spoken name and is at least 48 dp in both directions, on every new screen and state.
    Proof: `AccessibilityWalkTest` is extended, at SDK 35 on the phone, with `TouchTargetCheck`.
    - Every node with a click action, toggle state or selected state has non-empty text or a content description. Its `max(boundsInRoot, touchBoundsInRoot)` and its `size` are at least 48 dp both ways. The test prints the node count per state, each above 0, and its 40 dp nameless control still fails.
    - States kept from today: the gallery (full, partly shared, refused, empty, loading); the camera (live, after a shot, refused, unavailable); Settings with Custom chosen (nodes under `custom-colour-picker` still excluded).
    - States new or changed: Home with recent photos, with the carousel on each of its four banners, Home empty and Home loading; My edits with photos, empty and loading; Saved, and the photo opened from My edits; each of the five tool screens ready, with the photo unreadable, and while saving (replacing "editor with X selected"); Settings with Kanso blue.
    - The bar's tabs and `nav-camera` are walked on every tab state. If the page dots are tappable, they are named and 48 dp.

26. [machine] Nothing sits under the floating bar or in the phone's gesture area. Every screen can be scrolled until its last item rests above the bar, and the bottom buttons sit above the gesture area.
    Proof: `BottomClearanceTest` (new) at SDK 35, on the phone and on the small phone. Each runs with (i) a 0 dp navigation bar and a 32 dp bottom gesture inset, and (ii) a 48 dp navigation bar.
    - These bottoms are at least the larger of the two insets above the window's bottom: on Home, My edits and Settings, `nav-bar` and `nav-camera`; on each tool screen, `tool-save`; on Saved and the photo opened from My edits, `saved-share` and `saved-edit-another`; on the live camera, its shutter.
    - These items, scrolled to their end, have their bottoms at or above the top of `nav-bar`: Home's last recent card; My edits' 40th tile (40 photos); the picker card in Settings, with Custom chosen.
    - The gesture-zone tests of `SavedScreenTest` and `ToolScreenTest` pass in their new form.

27. [machine-then-human] Beside a narrow photo on a tool screen, the space is filled with a soft, blurred copy of the photo, on Android 12 and later and also on older phones. (Q14)
    Proof: machine pre-check, reported as "did not fail when driven from X": `ToolBackdropTest` (new) on the phone, `@GraphicsMode(NATIVE)`, Kanso blue light and dark. It draws the Edit screen with a 300x800 photo whose upper half is red-and-white vertical stripes 8 px wide and whose lower half is blue-and-white stripes, at SDK 30 and at SDK 35. In the space between the card's left edge and the photo's:
    - at least 90 % of sampled pixels differ from the card's container colour by more than 8 per channel (the space is filled);
    - the mean colour of the upper half is reddish and of the lower half bluish (it comes from the photo);
    - horizontally adjacent pixels differ by at most 12 per channel (it is soft);
    - inside the photo, adjacent stripe pixels differ by at least 100 in some channel (the photo stays sharp).
    The SDK 30 pass (the fallback) is always asserted. A control first draws a striped box with the same blur at SDK 35; if the host does not soften it, the SDK 35 pass prints "blur not driven by machine on this host" and asserts only that the space is filled. Pictures 40 to 42.
    The person (criterion 30, step 5): on the Galaxy A13 (Android 14) a portrait photo shows a soft blurred copy at its sides in each tool, not a sharp duplicate and not a flat colour, in light and dark. The same copy fills the empty space around the photo on Saved (its machine pre-check is in criterion 14).

28. [machine-then-human] The carousel swipes smoothly from banner to banner and settles on each one; its dots follow, and it never moves by itself.
    Proof: machine pre-check in `HomeScreenTest` at SDK 35.
    - A left swipe on `home-carousel` settles (clock stepped frame by frame until nothing changes) with banner 2's "Try now" wholly on screen and `home-carousel-dots` marking 2. A right swipe returns to 1.
    - After 10 s of clock time with no input, it still shows banner 1.
    - Criterion 19 names the Material 3 carousel used.
    The person, on the Galaxy A13: swiping snaps to each banner, the next banner hints that it is there, the dots follow, and "Try now" is obvious. Each banner's text is readable in light and dark and with each colour choice.

29. [machine-then-human] All 24 existing screen pictures are re-recorded in the new layout, each kept beside its old version; 17 new pictures pin the new screens and states; and a person looks at every one.
    Proof: machine pre-check.
    - (a) D-001 asks the owner to list under `## Baselines` in `DECISIONS.md` the 24 existing pictures and the 17 new ones of list (a) in `ESCALATION.md` D-001.
    - (b) Before every re-recording, the Iteration runs `./gradlew :app:compareRoborazziDebug` and copies each changed `app/build/outputs/roborazzi/*_compare.png` into `.harness/run/evidence/baseline-diffs/phase<N>/`. The commit that re-records a picture also adds its compare file. New pictures are recorded only with `.harness/loop/bin/foreman-record-baselines.ps1 -Module app`.
    - (c) At the end the Verifier writes into `baseline-diffs/`: `before/<file>` for each of the 24, from `git show <base>:app/src/test/screenshots/<file>` with `MSYS_NO_PATHCONV=1`; `after/<file>` for every picture at HEAD; and `index.md`, one row per picture.
    - (d) `./gradlew :app:verifyRoborazziDebug` exits 0, and `app/build/test-results/roborazzi/debug/results-summary.json` reports total 41 and changed 0. `git diff --name-status <base> HEAD -- app/src/test/screenshots` shows `M` for exactly the 24, `A` for exactly the new ones recorded, and no `D`.
    - (e) `.harness/ISSUES.md` carries one unsigned item per group of pictures, naming the folder.
    The person opens each `after/` picture beside its `before/` and expects: Kanso's colours (blue, the custom green, wallpaper colours) and no teal; the InFlux arrangement (big rounded cards on a soft background, the carousel, the sideways strips, and a pill bar dark in the light pictures and light in the dark ones); readable text and icons, and nothing under the bar; a soft blur beside the narrow photo, and placeholders that look like cards; in the small-phone pictures, every way in visible without scrolling. The Verifier records per picture "did not show a defect in ...", never "correct".

30. [machine-then-human] On the owner's Galaxy A13, in light and dark and with each of the three colour choices, every screen can be seen, found and reached. The layout, the blur, the floating bar, the carousel, the placeholders and the motion look right.
    Proof: the starting state is the update path. Whatever Kanso build is on the Galaxy A13 `R58W1120QVA` (Android 14, `armeabi-v7a`, photos allowed) stays, and this run's debug APK is installed over it with `foreman-device.ps1 -Op install`. Theme mode System; the phone first in light, then in dark. At least one photo is already in Pictures/Kanso; if none, step 6 saves one first.
    The machine pre-check runs only while `-Op focus` names Kanso or its permission dialog (a person using the phone means "not driven"):
    - `-Op start` reaches Home, and the crash buffer has no `FATAL EXCEPTION`.
    - One `-Op dump` of Home is checked to name only `com.phongkaster.kanso` and read outside the repository (PROJECT.md: never commit a raw dump). It holds "Home", "My edits", "Settings" and "What will you edit today?", and not "Open a photo". Only a written summary goes into `evidence/`.
    A person then walks:
    1. Home in light: the title, greeting, carousel, the row of five round tool shortcuts, Recent edits with real photos and the arrow, the dark bar with Home on its pill, and the round Camera button are all visible without scrolling, and none sits under the bar or the phone's navigation.
    2. Tap each of the five tool shortcuts: that tool's gallery opens at once, with no swipe. Swipe the carousel and tap each "Try now": the right gallery opens at once.
    3. Tap a recent photo: the Edit screen opens. Tap the arrow: My edits opens.
    4. My edits: the grid is newest first, with placeholders while it loads after a cold start. Open a photo: it shows whole over a blurred copy; Share opens the share sheet; Edit opens the Edit screen; close goes Home.
    5. Each tool screen: a round Back, the name centred, the rounded card, a soft blur beside a portrait photo, Replace returning to the gallery, and a full-width Save above the phone's navigation.
    6. Save: the photo shows whole with the mark playing once, the title and close are readable over the photo, and Share and "Edit another" are easy to hit. Close goes Home, and the new photo is first in Recent edits.
    7. Settings tab: no Back, rounded cards, rows with tinted icon squares and descriptions. Choose Custom colour, tap the code box and type 2E7D32 on the phone's keyboard: the box stays visible above the keyboard, the bar does not cover it, and the app turns green. Type 2E7: nothing changes. (Q13)
    8. Tap the Camera button from each tab: Back returns to that tab. The camera's round Back works.
    9. Repeat steps 1, 4, 5 and 7 in dark mode (the bar light, its pill clear), then with From wallpaper and with Custom colour.
    10. Back gesture from My edits and from Settings reaches Home; from Home it closes the app.
    11. Landscape on Home, a tool screen and Settings: nothing is unreachable. (Q13)
    12. Settings → Accessibility → Remove animations on: tab changes, screen changes, the carousel and the placeholders are still.
    Not judged: speed or dropped frames (out of scope), and the items in `.harness/ISSUES.md`.

31. [human-only] Side by side with InFlux v0.3.0's designs, Kanso is laid out the InFlux way: big rounded cards on a soft background, a hero carousel on Home, sideways strips, one full-width action at the bottom of each screen, and a floating pill bar. It keeps only Kanso's own colours, and none of InFlux's other features.
    Proof: nothing can drive it. The InFlux v0.3.0 designs (HCM136) are outside the repository and stay there. The owner compares `baseline-diffs/after/` (or the phone) with those designs and expects the arrangement above. They also expect no teal anywhere, and no credits, paywall or rewards.

### Data and storage

32. [machine] Recent edits and My edits show only the photos in Kanso's own album in Pictures, newest first, on Android 8.1, 11 and 15. Looking at them never changes or deletes anything. (Q10, Q11)
    Proof: `EditsSourceTest` (new) at SDK 27, 30 and 35. A `media` provider is registered through Robolectric that applies the selection it is given: a new one-type-per-file test helper, or `FakeMediaProvider` extended. It holds 5 photos with distinct dates in `Pictures/Kanso` and 3 elsewhere (`DCIM/Camera`, `Pictures/Other`).
    - The source returns exactly the 5, newest first, each as a `content://` Uri.
    - At SDK 27 the folder is matched by its file path (no `RELATIVE_PATH` there). With read access missing at SDK 27, it returns an empty list without throwing.
    - The provider records no insert, update or delete during the reads.
    - A photo then saved by `MediaStorePhotoSaver` with `FakeMediaProvider` at SDK 35 is returned first.

33. [machine] The open tab, a tool screen's tool and photo, and the colour and mode choices all survive turning the phone and Android closing the app in the background.
    Proof: `ToolPhotoKeyTest` at SDK 35: the key round-trips through JSON; `recreate()` and the process-death harness each reopen the same tool and photo; the two "switched tool" tests are gone (R8). `NavStateTest` (new): with My edits selected, `recreate()` and the process-death harness each restore My edits, selected, with its first tile; likewise with Settings; every new navigation key is `@Serializable` and round-trips through JSON.
    Settings storage: `ThemeColourStoreTest`, `ThemeColourRestartTest` and `ThemeModeRestartTest` pass; and the output of `git grep -n -E '"theme_mode"|"theme_colour_source"|"theme_colour_seed"|settings\.preferences_pb' -- app/src/main` at HEAD equals its output at `<base>`.

### Build, start and quality

34. [machine] The app builds, keeps its three phone architectures and its minimum Android version, and opens on Home with the floating bar on Android 8.1, 11 and 15.
    Proof:
    - `cd /d/Valve/Personal/Kanso && set -o pipefail; timeout 2400 ./gradlew :app:assembleDebug > .harness/run/evidence/assemble.log 2>&1` exits 0, and the log ends `BUILD SUCCESSFUL`.
    - `aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk | tr -d '\r' | grep -E "^native-code|minSdkVersion|^targetSdkVersion|compileSdkVersion='37'"` prints `native-code: 'arm64-v8a' 'armeabi-v7a' 'x86_64'`, `minSdkVersion:'27'`, `targetSdkVersion:'36'` and the compile line.
    - `LaunchTest` at SDK 27, 30 and 35 shows `home-screen`, the text "Kanso", and `nav-bar` with Home selected.
    - The start on the real phone is part of criterion 30.

35. [machine] Every test passes. Every test that was changed or removed is explained. No test is left proving a screen or behaviour that no longer exists, and nothing still in the app lost its test.
    Proof: `cd /d/Valve/Personal/Kanso && set -o pipefail; timeout 2400 ./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest > .harness/run/evidence/test.log 2>&1` exits 0, and the log ends `BUILD SUCCESSFUL`. Read the XML only after seeing that line.
    - `cat app/build/test-results/testDebugUnitTest/TEST-*.xml | grep -o '<testsuite [^>]*>' | grep -o -E '(tests|skipped|failures|errors)="[0-9]+"' | awk -F'[="]' '{s[$1]+=$3} END{for (k in s) print k, s[k]}'` prints `failures 0`, `errors 0` and `skipped 0`. It also prints `tests` equal to `git grep -h -I -E '^\s*@Test' HEAD -- app/src/test/java | wc -l`.
    - `git grep -n -E '@Ignore|assumeTrue|assumeFalse|Assume\.' HEAD -- app/src/test` exits 1.
    - `.harness/run/evidence/base-testnames.txt` holds the 368 names at `<base>` (`File.kt:name`), and `head-testnames.txt` the names at HEAD, both made with the script in `.harness/run/evidence/testnames.sh`. `comm -23 base-testnames.txt head-testnames.txt` prints only names that `.harness/run/evidence/test-edits.md` lists as one of: renamed, with the new name; replaced, with the replacement's name and what it proves now; removed, with the Removal (R1 to R8) or PRD clause that removes what it proved.
    - Every base test whose subject still exists in the app is renamed or replaced, never only removed. `test-edits.md` also lists every `M` or `D` file of `git diff --name-status --no-renames <base> HEAD -- app/src/test/java`, with its reason. R8 holds.

36. [machine] The project's standing rules hold for all new and changed code: a light and a dark preview for every composable, one type per file, one state holder and one state per screen, and plain-English documentation on everything.
    Proof: `PreviewCoverageTest`, `PreviewsRenderTest`, `PreviewHygieneTest`, `OneTypePerFileTest`, `ScreenStateTest`, `KdocCoverageTest` and `ExpressiveApiTest` pass (criterion 35). None of them is loosened:
    - `OneTypePerFileTest`'s allow-list is still `emptySet()`. `PreviewCoverageTest.EXEMPT` still holds exactly its three entries (Q18). `PreviewHygieneTest`'s forbidden words are unchanged.
    - `ScreenStateTest`'s list of ViewModels is the six at `<base>` plus `myedits/MyEditsViewModel`; each new screen package holds its `...Screen.kt`, `...ViewModel.kt` and `...UiState.kt`, and its `...Content` takes its UiState.
    - `PreviewsRenderTest.requiredPreviewNames` holds every base name except those `test-edits.md` lists as removed with an R-number, plus one preview per new screen state: `KansoTabBarPreview`, `KansoTabShellPreview`, `MyEditsPreview`, `MyEditsEmptyPreview`, `MyEditsLoadingPreview`, `ToolNarrowPhotoPreview`, `PhotoViewerPreview`, `HomeEmptyPreview`, `HomeLoadingPreview`.
    - No preview shows `LoadingIndicator` or a wavy indicator. `git grep -n -E 'MaterialTheme\(' -- app/src/main` finds no call, and `git grep -n 'androidx.compose.material\.' -- app/src/main` prints nothing.

37. [machine-then-human] The new ideas are explained in plain English where they first appear: the carousel, tabs in Navigation 3, the floating bar, the blur, the placeholders and the shade on Saved.
    Proof: machine pre-check, a grep of comments and KDoc:
    - `ui/home/HomeScreen.kt` mentions "carousel" and the carousel function's name, and says why placeholders are used instead of a spinner.
    - The bar's file mentions "tab" and `inverseSurface`.
    - `navigation/KansoNavHost.kt` explains how the tabs live on the Navigation 3 back stack.
    - The blurred-backdrop file mentions `blur` and "Android 12"; `ui/saved/SavedScreen.kt` mentions `Brush` and "gradient".
    - The My edits screen mentions `LazyVerticalGrid`.
    - The hex fix in `HSVColorPicker.kt` has a comment saying the code is read in base 16 and made opaque.
    The person reads `HomeScreen.kt`, the bar's file, `KansoNavHost.kt`, the blurred-backdrop file and `SavedScreen.kt`, in that order. They expect each idea explained where it is first used, in words a learner of Navigation 3 and Compose can follow.

38. [machine] The screenshot check, lint and the compiler are clean.
    Proof:
    - `./gradlew :app:verifyRoborazziDebug > .harness/run/evidence/verify-roborazzi.log 2>&1` exits 0 (totals as in criterion 29).
    - `./gradlew :app:lintAnalyzeDebug --rerun :app:lintReportDebug --rerun :app:lintDebug > .harness/run/evidence/lint.log 2>&1` exits 0 with 0 errors.
    - `./gradlew :app:compileDebugKotlin --rerun-tasks > .harness/run/evidence/compile.log 2>&1` exits 0, and `grep -i 'deprecat' .harness/run/evidence/compile.log | grep 'app/src/main'` prints nothing.
    - No `@Suppress("DEPRECATION")` or `@Suppress("DEPRECATION_ERROR")` is added under `app/src/main`.

39. [machine] The only change to the engine is how its colour picker reads a typed colour code.
    Proof:
    - `git diff --name-only <base> HEAD -- core opencv libs` prints exactly `core/colorpicker/src/main/java/com/library/colorpicker/HSVColorPicker.kt`.
    - In `git diff -U0 <base> HEAD -- core/colorpicker/src/main/java/com/library/colorpicker/HSVColorPicker.kt`, every hunk's base range lies within base lines 63-72 (the hex `BasicTextField`'s `value` and `onValueChange`), or the hunk only adds lines after base line 168 (at most one new private helper). Any other hunk fails.
    - `./gradlew :core:colorpicker:assembleDebug :core:colorpicker:assembleRelease` exits 0. `PickerNoAlphaTest` and `PickerDefaultAlphaTest` pass; their only edit is how Settings is reached (the bar).

40. [machine] No new library is added, the reference app is untouched, and the new icons are Material Symbols Rounded stored in the app.
    Proof:
    - `git diff --exit-code <base> HEAD -- gradle/libs.versions.toml settings.gradle.kts build.gradle.kts gradle.properties app/build.gradle.kts opencv libs` exits 0 (`core` as criterion 39 says).
    - `./gradlew :app:dependencies --configuration debugRuntimeClasspath > .harness/run/evidence/head-deps.txt 2>&1` is normalised with `sed -E 's/^[|+\\ -]+//; s/ \([*cn]\)$//' <file> | grep -E '^[A-Za-z0-9_.-]+:' | sort -u`. The result equals the same normalisation of `.harness/run/evidence/base-deps-debugRuntime.txt`. `dependencyInsight --dependency androidx.compose.material3:material3 --configuration debugRuntimeClasspath` selects `1.5.0-beta01`.
    - `git diff --quiet a0d9a79 HEAD -- reference` exits 0, and `git grep -n 'reference/' -- settings.gradle.kts app` prints nothing.
    - Every file added under `app/src/main/res/drawable/` has a `<vector` root and is listed in `.harness/run/evidence/symbols/SOURCES.md`, with its upstream URL (`https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android/<name>/materialsymbolsrounded/<name>_24px.xml`), the SHA-256 of the upstream and the stored file, and removing `android:tint` as the only change, shown by a check script that compares `pathData`. Each new `R.drawable.ic_...` is referenced in `app/src/main/java`.

41. [machine] The decision to change from one editor to one screen per tool is written down for the next run.
    Proof: `docs/adr/ADR-002-*.md` exists, names the 2026-10-12 decision, says it reverses the one-editor decision of ADR-001, and `docs/adr/ADR-001-*.md` carries a one-line "superseded in part by ADR-002" note (Q21).

## Removals

Repository class (from PROJECT.md): **product**. Only what the PRD replaces or leaves unreachable is removed. Anything else that looks unused is listed under "Questions".

R1. [machine] The five-tool toolbar is gone from the tool screens.
    Proof: `git grep -n -E 'HorizontalFloatingToolbar|IconToggleButton|FloatingToolbarDefaults|EditorToolbar|ToolToggle|DISABLED_ICON_ALPHA' -- app/src/main/java/com/phongkaster/kanso/ui/tool` prints nothing. `git grep -n -E 'EDITOR_TOOLBAR_TAG|editorToolTag|editor-toolbar|editor-tool-' -- app/src` prints nothing. (`HorizontalFloatingToolbar` may draw the new floating bar outside `ui/tool`.)

R2. [machine] Switching tools inside a tool screen is gone: a tool screen shows only the tool it was opened for.
    Proof: `git grep -n -w -E 'selectTool|editingTools|keptToolName|onSelectTool' -- app/src` prints nothing. `git grep -n -i -E 'switch(es|ing)? tools?|tools? (can )?switch|toolbar toggle' -- app/src/main/java/com/phongkaster/kanso/ui/tool app/src/main/java/com/phongkaster/kanso/navigation` prints nothing, so no KDoc still describes it. Criterion 10's `ToolUiState.tool` assertion holds.

R3. [machine] Home's Settings row is gone; Settings is reached only from the bar.
    Proof: `git grep -n -w -E 'SettingsEntry|SETTINGS_MIN_HEIGHT|onSettingsClick' -- app/src` prints nothing. In `NavigationTest`, the only node on Home with the text "Settings" and a click action lies inside `nav-bar`.

R4. [machine] The big "Open a photo" button is gone, with its words, and with its icon if nothing else uses it. Home's own Camera shortcut is gone too (the round Camera button replaces it).
    Proof: `git grep -n -w -E 'OpenPhotoButton|OPEN_PHOTO_HEIGHT|HOME_OPEN_PHOTO_TAG|onOpenPhoto|home_open_photo|CameraButton|onCameraClick' -- app/src/main/java/com/phongkaster/kanso/ui/home app/src/test` prints nothing, and `git grep -n -E 'home-open-photo|Open a photo' -- app/src` prints nothing. `app/src/main/res/drawable/ic_add_photo_alternate.xml` is either absent or referenced as `R.drawable.ic_add_photo_alternate` in `app/src/main/java`.

R5. [machine] Home's two lists of tool cards, under "AI tools" and "Edit", are gone, replaced by the carousel and a row of five tool shortcuts.
    Proof: `git grep -n -w -E 'ToolGroup|GroupHeader|ToolCard|CARD_MIN_HEIGHT|PRESSED_CARD_SCALE' -- app/src/main/java/com/phongkaster/kanso/ui/home` prints nothing. `git grep -n -w -E 'aiTools|editTools|homeToolTag|home_header_ai_tools|home_header_edit' -- app/src` prints nothing, and `git grep -n 'home-tool-' -- app/src` prints nothing.

R6. [machine] Settings has no Back button and no back callback any more.
    Proof: `git grep -n -w 'onBack' -- app/src/main/java/com/phongkaster/kanso/ui/settings` prints nothing. `git grep -n -E 'R\.string\.back|ic_arrow_back' -- app/src/main/java/com/phongkaster/kanso/ui/settings` prints nothing. Criterion 15 finds no "Back" node.

R7. [machine] Saved's Home button in the bottom row is gone; the round close button at the top takes the person Home.
    Proof: `git grep -n -E 'SAVED_HOME_TAG|SAVED_HOME_ICON_TAG|SAVED_ACTIONS_TAG|saved-home|saved-actions' -- app/src` prints nothing. `SavedScreenTest` asserts that the bottom holds exactly two actions, `saved-share` and `saved-edit-another`.

R8. [machine] No test is left proving a screen or behaviour that is gone, and no test helper is left without a user.
    Proof: `git ls-files app/src/test/java/com/phongkaster/kanso/tool/ToolSelectionAnimationTest.kt` prints nothing, and R1 to R7's greps over `app/src` (tests included) print nothing. None of these base test names exists at HEAD; where the subject lives on, the test that proves its new form has a new name, given in `test-edits.md`:
    - all three tests of `ToolSelectionAnimationTest`;
    - `ToolScreenTest`: `the_toolbar_switches_between_the_five_editing_tools`, `the_toggles_are_switched_off_while_a_save_runs`, `the_toolbar_switches_the_five_tools_in_place_without_reloading_the_photo`, `the_photo_the_panel_and_the_toolbar_stack_from_top_to_bottom`, `back_save_and_every_toggle_are_at_least_48_dp`, `the_toolbar_and_the_panel_clear_the_bottom_gesture_zone_when_the_navigation_bar_is_zero_tall`;
    - `ToolViewModelTest`: the five `select_tool_*` tests and `selecting_the_selected_tool_changes_nothing`;
    - `ToolPhotoKeyTest`: `recreate_keeps_a_switched_tool_and_the_photo`, `process_death_keeps_a_switched_tool_and_the_photo`;
    - `SavedNavigationTest`: `edit_another_after_switching_tools_opens_the_switched_tools_gallery`, `home_after_switching_tools_leaves_only_home`, `the_editor_switches_tools_in_place_on_the_chosen_photo`, `home_leaves_only_home`;
    - `SavedScreenTest`: `home_calls_back`, `the_three_actions_are_shown_and_large_enough_to_tap`, `the_three_actions_are_one_button_group_in_a_single_row`;
    - `NavigationTest`: `home_lists_open_a_photo_camera_five_tool_cards_and_settings`, `open_a_photo_asks_only_for_photo_access`;
    - `PhotoToToolNavigationTest`: `open_a_photo_opens_the_edit_gallery_then_the_editor_on_edit`;
    - `HomeViewModelTest`: `home_state_holds_the_ai_tools_and_the_edit_group_in_order`;
    - `AccessibilityWalkTest`: `editor_with_<tool>_selected` for the five tools.
    Orphan check: `.harness/run/evidence/orphans.sh` prints nothing. For every file under `app/src/test/java` that is not a `*Test.kt`, it checks that at least one of its declarations is used by another test file.

R9. [machine] Words and pictures the app no longer uses after the redesign are gone.
    Proof: criterion 38's forced lint run is used. The `UnusedResources` entries of `app/build/reports/lint-results-debug.xml`, saved as `.harness/run/evidence/head-unused-resources.txt`, are a subset of those in `.harness/run/evidence/base-unused-resources.txt` (made at bootstrap; it lists `gallery_album_label` and `gallery_body_placeholder`, see Questions). Each of `home_open_photo`, `home_header_ai_tools`, `home_header_edit`, `saved_home` and `ic_add_photo_alternate` is either absent from `app/src/main/res` or referenced as `R.string.`/`R.drawable.` in `app/src/main/java` (`git grep -w`). Strings are removed only by the Iteration (PROJECT.md).

Questions (product repository: a person decides, none is a removal):
- Already unused at `<base>` and left alone by Run 6: the strings `gallery_body_placeholder` and `gallery_album_label`, the do-nothing `onShare = {}` in the navigation host, `GalleryScreen`'s `onOpenCamera` default `{}`, and `ViewModelClearedProbe` (test-only code living in the app). This run leaves them too.
- The Material Symbols are Apache 2.0, and the app still has no licence notice (Run 6's question, still open).
