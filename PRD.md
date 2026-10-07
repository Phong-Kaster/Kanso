# Kanso — Run 3: a preview for every composable

After Run 2 the app has 36 composables in 10 files and not one `@Preview`. I learn Compose by opening
a file and seeing what it draws; without previews I have to build, install and click through the app
on a slow phone to see any of it. This run adds the previews, and the small restructuring that some
screens need before they can have one. **It changes nothing the person using the app can see.**

The rule itself now lives in `CLAUDE.md` ("Every composable has a preview"); this run brings the
existing code up to it.

## What this run delivers

1. **Every composable in `app/` has a preview** in light and in dark, in the same file, built from
   sample data that looks like the real thing: a gallery with a few albums and a grid of photos, a
   tool screen with a photo on it, the access-refused panels, the camera's controls, the Saved
   screen, Settings with each theme choice, Home.
2. **Screens are split where a preview needs it.** A composable that reaches for Koin, a ViewModel,
   `MediaStore`, the camera or a permission launcher is split into a stateful part that gathers the
   state and a stateless `…Content` part that takes the state and callbacks. The preview shows the
   stateless part. Explain this split — state hoisting — where it is first used, for a reader who has
   never met it.
3. **Previews that matter show their interesting states**, not only the default: for example the
   gallery with photo access refused and with only some photos shared, the camera after a shot
   (Keep and Retake), Settings with a custom colour picked.
4. **Every preview renders.** A preview that throws when drawn is a defect, so prove on the host that
   all of them draw without an exception.
5. **Previews open in Android Studio**: the preview annotations are available to the main code and
   the tooling to debug builds only.

## Constraints

- `CLAUDE.md` applies: KDoc in plain English on every class, function and composable, previews
  included; no ads, analytics, accounts or network; `reference/` unchanged.
- **Behaviour and pixels stay the same.** The app's tests pass unchanged, and every existing Roborazzi
  baseline stays byte-for-byte as it is: this run restructures, so a baseline that moves means
  something the person sees has changed, which is a defect here, not an approval to ask for.
- Preview-only sample data never ships in a code path the app runs.
- Keep Navigation 3 at 1.1.7, AGP 8.x and the three debug ABIs.

## Out of scope for this run

The speed problems found on my Galaxy A13 (`.harness/ISSUES.md`, "Driven on a Galaxy A13"), the white
colour-picker card in dark mode, new features, and the human checks still waiting in `ISSUES.md`.
