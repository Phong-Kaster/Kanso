# Test file edits forced by a library change

none

Through Phase 2 no file under `app/src/test` was edited: the 231 tests compiled and passed unchanged on
Compose 1.12.1, Navigation 3 1.2.0, lifecycle-viewmodel-navigation3 2.11.0 and Kotlin 2.4.21
(`evidence/phase2-combined-1.log`). The compiler now prints warnings for the tests, none of them an error,
and none edited because an edit is not forced and the guard tests are the Definition of Done's own checks:

- 28 deprecation warnings in 28 test files for `createComposeRule`, `createAndroidComposeRule` and
  `createEmptyComposeRule` (Compose 1.12 deprecates them for the `androidx.compose.ui.test.junit4.v2` versions, which use `StandardTestDispatcher` instead of `UnconfinedTestDispatcher`: a change in how the tests run, so not a mechanical edit).
- 7 "Java type mismatch: inferred type is `String?`, but `String` was expected" warnings, new with Kotlin
  2.4.21, at `KdocCoverageTest.kt:109`, `OneTypePerFileTest.kt:552`, `ScreenStateTest.kt:323`,
  `PreviewCoverageTest.kt:315`, `PreviewHygieneTest.kt:393`, `PreviewsRenderTest.kt:223` and
  `ThemeColourLabelsTest.kt:74`. Six of those files are guard tests of criterion 17: if a later Kotlin
  turns this warning into an error they stop compiling, and the fix is a null-safe argument there.
- 1 "Unnecessary non-null assertion" warning at `KansoThemeColourTest.kt:102`, new with Kotlin 2.4.21.
