# Kanso — Run 1: the app shell

Kanso is a minimalist, offline photo editor for Android, for my own use. It reuses Retica's image
engine (already in this repository) and puts a new app on top of it: written from scratch, in full
Jetpack Compose, with Navigation 3. Later runs add the gallery and camera, the editing tools and the
AI tools. **This run builds the shell every later run grows into.**

## What is already here

- `core/` — the engine's eight modules, `libs/` — their prebuilt AARs, `opencv/` — the OpenCV module
  `core/editimage` and `core/localml` depend on. Reuse them as they are; adapt their build scripts
  only as far as the build requires.
- `gradle/libs.versions.toml` — Retica's version catalog, a starting point to trim.
- `reference/retica-app/` — Retica's original app code, **read-only**. Read it to see how the engine's
  use cases are called. Never edit it, never build it, never import from it.
- There is no `settings.gradle.kts` and no app module yet. This run writes both.

## What this run delivers

1. **The project builds.** `settings.gradle.kts`, the root build and an `app` module, with every
   `core` module and `opencv` wired in. Package and applicationId `com.phongkaster.kanso`, app name
   `Kanso`, minSdk 27 like the engine.
2. **One Activity, all Compose, Navigation 3.** A back stack of typed navigation keys
   (`rememberNavBackStack`), `NavDisplay`, ViewModels scoped to their screen
   (`lifecycle-viewmodel-navigation3`), and Koin for dependency injection (`koin-compose-navigation3`).
   Use the latest stable releases.
3. **Splash** — only the system SplashScreen API, nothing more. No onboarding, no intro screens.
4. **Home** — a minimal screen listing the tools: Edit, Remove background, Enhance, Retouch,
   Artistic, Camera, and a way into Settings. Each tool opens a simple placeholder screen for now;
   later runs replace them. Back always works the way a person expects.
5. **Settings → appearance**, applied to the whole app immediately and kept after a restart:
   - theme mode: follow the system, light, or dark;
   - theme colour: from the wallpaper (dynamic colour, Android 12 and later), or a colour I pick
     myself, from which a full Material 3 colour scheme is generated. Reuse `core/colorpicker` for
     picking it.
6. **Material 3 everywhere**, minimal and calm: no decoration a screen does not need.

## Constraints

- **No ads, no analytics, no accounts, no network.** Nothing from Apero (ads, monetization, first
  open), no Firebase, no Play in-app update, review or billing, and no private Maven repository —
  so no credentials in any file. The app declares no network permission; if a reused library merges
  one in, remove it unless that breaks the engine, and say so.
- **The code teaches.** I am using this app to learn Navigation 3 and full-Compose apps. Every
  class, function and composable gets a KDoc comment in plain English saying what it does and why;
  a Navigation 3 or Compose concept is explained where it is first used; a non-obvious line gets a
  short comment. Write for a reader who has never used Navigation 3.
- **Roborazzi** checks the screens this run builds (Home, Settings, a placeholder) in light and dark
  mode, and with a custom theme colour applied.
- `reference/` stays byte-for-byte unchanged.

## Out of scope for this run

Gallery, camera, export, the editing tools and the AI tools themselves — later runs.
