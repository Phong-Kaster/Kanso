# Kanso — conventions

Standing rules for anyone writing code here, human or agent. Each one is the owner's decision; a run's
`PRD.md` may add to them, never relax them.

## Every composable has a preview

- Every `@Composable` in `app/` has a preview in light and in dark (`@PreviewLightDark`, or two
  `@Preview`s), in the same file, built from sample data.
- A preview never touches Koin, a ViewModel, `MediaStore`, the camera or the network. A screen that
  needs any of those is split in two: a stateful composable that gathers the state, and a stateless
  `…Content` composable that takes the state and the callbacks as parameters. The preview shows the
  stateless one.
- A new composable arrives with its preview in the same change. A composable without one is unfinished.

## One class per file, one UiState per screen

- Every top-level class, interface, object and enum lives in its own file, named after it — never two
  in one file. Types nested inside one (the cases of a sealed interface) stay with it. Top-level
  functions, composables included, are not classes: a screen's composables share its `…Screen.kt`.
- A screen's package holds at least three files, for example `ui/home/`: `HomeScreen.kt` (the
  composables), `HomeViewModel.kt` and `HomeUiState.kt`.
- Every screen has a ViewModel and a UiState, even one whose content is fixed, so every screen reads
  the same way. The ViewModel exposes exactly one `StateFlow<…UiState>`; the stateless `…Content`
  composable takes that UiState and its callbacks.
- The rule covers `app/src/test` too: a fake, a helper or a test's own data class gets its own file.

## Motion uses the Compose Animation APIs

Transitions, loading indicators and press feedback are built with Jetpack Compose's own animation APIs
first — `AnimatedVisibility`, `AnimatedContent`, `animate*AsState`, `updateTransition`,
`rememberInfiniteTransition`, `Animatable` — and Navigation 3's `NavDisplay` transition specs for moving
between screens. A third-party animation library needs the owner's approval in the DoD.

## The code teaches

The owner is learning Navigation 3 and full-Compose apps from this code. Every class, function and
composable has a KDoc comment in plain English saying what it does and why; a concept is explained
where it is first used; a non-obvious line gets a short comment.

## Fixed constraints

- No ads, no analytics, no accounts, no network permission.
- `reference/` is Retica's original app, read-only: never edit, build or import it.
- The owner's phone is a Galaxy A13 running 32-bit Android (`armeabi-v7a`); the debug build keeps
  `x86_64` (emulator), `arm64-v8a` and `armeabi-v7a`.
