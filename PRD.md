# Kanso — Run 4: one class per file, one UiState per screen

`CLAUDE.md` now says every class lives in its own file and every screen has a ViewModel and a UiState
in their own files ("One class per file, one UiState per screen"). The code from Runs 1-3 predates
that rule. This run brings it up to the rule. **It changes nothing the person using the app can see.**

## What this run delivers

1. **One top-level type per file**, in `app/src/main` and in `app/src/test`. Today 8 main files and 6
   test files hold more than one, for example:
   - `PhotoSource.kt`: `Photo`, `Album`, `PhotoSource`;
   - `CameraController.kt`: five types;
   - `ToolViewModel.kt`: `ToolUiState`, `SaveUiState`, `ToolViewModel`;
   - `GalleryGridTest.kt`: the test and its `FakePhotoSource`.

   Types nested inside another stay with it; the navigation keys may live as cases of one sealed
   type.
2. **Every screen has `…Screen.kt`, `…ViewModel.kt` and `…UiState.kt`.** That covers Home, Gallery,
   Camera, Tool, Saved and Settings. Specifically:
   - the UiStates now inside ViewModel files move out to their own files;
   - Settings gets one `SettingsUiState` in place of its two separate flows;
   - Home gets a `HomeUiState` even though its content is fixed;
   - Saved gets a `SavedViewModel` and `SavedUiState`, and the loading it does in the composable
     today moves into the ViewModel.
3. **Each ViewModel exposes exactly one `StateFlow<…UiState>`**, and each screen's stateless
   `…Content` takes that UiState and its callbacks. The previews from Run 3 keep working: they build
   a UiState from sample data.
4. **The rule is checked by a test**, so a later run that puts two classes in one file fails the
   build instead of slipping through.

## Constraints

- `CLAUDE.md` applies in full: a preview for every composable, KDoc in plain English (moved and new
  files included), no ads, analytics, accounts or network, `reference/` unchanged, and the three
  debug ABIs.
- **Behaviour and pixels stay the same.** Every existing Roborazzi baseline stays byte for byte, and
  every test keeps asserting what it asserts today. A test may move to a new file or follow a type to
  its new package, but none is weakened or deleted.
- Explain the UiState pattern where it is first used — one immutable snapshot of what a screen shows,
  produced by its ViewModel — for a reader who has never met it.
- Keep Navigation 3 at 1.1.7 and AGP at 8.x.

## Out of scope for this run

Any new look or motion (that is the next run), the speed problems found on the Galaxy A13, and the
human checks waiting in `.harness/ISSUES.md`.
