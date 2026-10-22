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
  functions, composables included, are not classes (see "Screen packages show the hierarchy" below
  for where each composable lives).
- A screen's package holds at least three files, for example `ui/home/`: `HomeScreen.kt` (the
  parent layout), `HomeViewModel.kt` and `HomeUiState.kt`.
- Every screen has a ViewModel and a UiState, even one whose content is fixed, so every screen reads
  the same way. The ViewModel exposes exactly one `StateFlow<…UiState>`; the stateless `…Content`
  composable takes that UiState and its callbacks.
- The rule covers `app/src/test` too: a fake, a helper or a test's own data class gets its own file.

## Screen packages show the hierarchy

The folder tree of a screen shows which composable is the parent layout, which are its children and
which are their sub-components (owner's decision, 2026-10-21). One composable per file, named after it; a
stateful composable and its stateless `…Content` twin (and a preview's sample helper) share the file.

```
ui/tool/
  ToolScreen.kt        the parent layout: the screen's one stateful + stateless pair
  ToolViewModel.kt
  ToolUiState.kt
  components/          every child composable the screen draws, one level below it
    topbar/            a child that has its own sub-components gets a folder named after it
      ToolTopBar.kt    the child itself, with its preview
      DoneButton.kt    its sub-components, each with a preview
      UndoButton.kt
    photocard/ …
```

- The screen file stays at the top of the screen's package; it never holds a child composable.
- A child with no sub-components is a single file straight in `components/`.
- A child that has sub-components gets a folder `components/<child>/` holding the child and its
  sub-components. A sub-component with children of its own repeats the rule one level further down.
- Parts shared by several children of one screen stay in `components/`; parts shared by several
  screens go in `ui/common/`.
- Exception: a composable that cannot be drawn in a preview (an endless animation such as Material's
  `LoadingIndicator`, which hangs the preview renderer) stays `private` in its parent's file, so the
  parent's preview covers it (`PhotoLoadingIndicator` in `PhotoCard.kt`).
- A composable that moved out of the screen file becomes `internal` (it was `private`). Previews of the
  whole screen stay in the screen file; each component's own previews sit in its own file.

## Motion uses the Compose Animation APIs

Transitions, loading indicators and press feedback are built with Jetpack Compose's own animation APIs
first — `AnimatedVisibility`, `AnimatedContent`, `animate*AsState`, `updateTransition`,
`rememberInfiniteTransition`, `Animatable` — and Navigation 3's `NavDisplay` transition specs for moving
between screens. A third-party animation library needs the owner's approval in the DoD.

## The code teaches

The owner is learning Navigation 3 and full-Compose apps from this code. Every class, function and
composable has a KDoc comment in plain English saying what it does and why; a concept is explained
where it is first used; a non-obvious line gets a short comment.

## README.md follows the code, in the same change

`README.md` describes Kanso as the code is, not as it is planned (decided by the owner, 2026-10-14).
Every change to what the app does — a feature added, removed or changed, business logic, a pipeline
step, a default, a limit, a file format, a permission — updates `README.md` in the same commit. A
behaviour change without its README update is unfinished.

The **photo-editing features get the owner's closest attention**. For each one, the README's
"Editing features" table states its real state (works, partial, UI only, not wired), and "How each
editing feature works" traces its pipeline end to end: the screen and ViewModel, every processing
step in order, the engine module, library or model that does it (bundled or downloaded), the
threading, the input and output (size, format, quality, where it is saved), and what happens on
failure, with file paths. A claim the code does not back is a defect.

## Fixed constraints

- No ads, no analytics, no accounts, no network permission.
- `reference/` is Retica's original app, read-only: never edit, build or import it.
- The owner's phone is a Galaxy A13 running 32-bit Android (`armeabi-v7a`); the debug build keeps
  `x86_64` (emulator), `arm64-v8a` and `armeabi-v7a`.
