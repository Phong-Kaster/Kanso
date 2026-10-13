# Home reach: gestures per target (DoD 6)

Copied by the Verifier (Iteration 15, 2026-10-13) from the log of `HomeReachTest.each_target_is_one_tap_from_a_fresh_launch_on_the_small_phone`, in `app/build/test-results/testDebugUnitTest/TEST-com.phongkaster.kanso.home.HomeReachTest.xml`, run by `./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest` (`evidence/test.log`, `BUILD SUCCESSFUL`, 571 tests, 0 failed, 0 skipped). All five `HomeReachTest` tests passed.

Home reach, gestures per target (small phone, 48 dp navigation bar, five edits):

| Target | Gestures used |
|---|---|
| Edit shortcut (`home-shortcut-EDIT`) | 1 tap |
| Remove background shortcut (`home-shortcut-REMOVE_BACKGROUND`) | 1 tap |
| Enhance shortcut (`home-shortcut-ENHANCE`) | 1 tap |
| Retouch shortcut (`home-shortcut-RETOUCH`) | 1 tap |
| Artistic shortcut (`home-shortcut-ARTISTIC`) | 1 tap |
| Remove background's Try now (`home-try-REMOVE_BACKGROUND`) | 1 tap |
| Camera (`nav-camera`) | 1 tap |
| Recent edits arrow (`home-recent-more`) | 1 tap |
| Home tab (`nav-tab-HOME`) | 1 tap |
| My edits tab (`nav-tab-MY_EDITS`) | 1 tap |
| Settings tab (`nav-tab-SETTINGS`) | 1 tap |
| Newest recent photo (`home-recent-0`) | 1 tap |

Every row for a tool shortcut reads "1 tap": no swipe, no scroll.

The same log's inset lines (bar top against the window height, in px at xxhdpi):

- small phone, 48 dp navigation bar: bar top 1956, window 2340; 32 dp gesture area: bar top 2004, window 2340
- phone, 48 dp navigation bar: bar top 2289, window 2673; 32 dp gesture area: bar top 2337, window 2673
