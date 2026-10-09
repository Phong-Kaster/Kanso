# Kanso — Run 6: Material 3 Expressive, with blue as Kanso's colour

Kanso looks bare: plain text rows, no icons, no motion. I want it to be a **Material 3 Expressive**
app: Google's own design language, built from Material3 1.5.0 (made available by Run 5), in light and
dark, with blue as Kanso's colour. The design follows the Material 3 Expressive guidance and its
components, not another app. In particular, Kanso is **not** a copy of Adobe Lightroom or any other
photo editor.

## What this run delivers

1. **Material 3 Expressive as the design system.**
   - `MaterialExpressiveTheme` and the expressive `MotionScheme` (spring motion).
   - Expressive typography, shapes and components wherever Material 3 Expressive has one for the job,
     before anything hand-made: `ButtonGroup`, the floating toolbars, `LoadingIndicator` /
     `ContainedLoadingIndicator`, the wavy progress indicators, toggle buttons and icon buttons that
     change shape when pressed or selected, and `MaterialShapes` with shape morphing.
2. **Blue by default, in light and dark.**
   - A fresh install uses a "Kanso blue" scheme generated from one blue seed.
   - Settings keeps "From wallpaper" and "Custom colour" as its two other choices, with blue first.
   - Every screen, the editor included, follows the app's theme: light in light mode, dark in dark mode.
3. **An icon for every feature**: Edit, Remove background, Enhance, Retouch, Artistic, Camera, Settings,
   Save, Share, Back. Use Material Symbols (Rounded), imported as vector drawables rather than through
   a large icon library.
4. **One editor screen for the five editing tools** (Edit, Remove background, Enhance, Retouch,
   Artistic: every `Tool` except Camera; decided by me, 2026-10-09):
   - the photo fills the space above the controls;
   - a Material 3 Expressive bottom toolbar holds the five tools as icons, so a person can switch tools
     without going back to Home. The selected tool is shown the Expressive way (a selected,
     shape-changed toggle);
   - a top bar with Back and Save;
   - each tool's panel stays a short "coming in a later run" message until its run builds it, with room
     above the toolbar for the controls those runs add.
5. **Home:**
   - one large, clear "Open a photo" action and a camera shortcut (Camera stays a Home action, not an
     editor tool);
   - the five editing tools as cards with an icon and a label, grouped "AI tools" and "Edit". Each card opens the
     gallery and then the editor with that tool selected.
6. **Gallery:**
   - rounded thumbnails and album chips;
   - a `LoadingIndicator` while photos load.
7. **Saved:**
   - a short shape-morph success mark;
   - Share, Edit another and Home as a `ButtonGroup`.
8. **Camera:**
   - icon controls in Material 3 Expressive form;
   - a shutter that responds to the press.
9. **Settings:** grouped rows with icons, and colour swatches for the three theme choices.
10. **Motion with meaning** (`CLAUDE.md`: the Compose animation APIs first):
    - screen transitions through Navigation 3's `NavDisplay`, with predictive back;
    - press feedback and shape changes from the expressive motion scheme;
    - the selected tool's change animating in the editor's toolbar.

## Constraints

- `CLAUDE.md` applies in full: a light and a dark preview for every composable, one class per file, one
  UiState per screen, KDoc in plain English.
- Accessible:
  - readable text and icons in both themes;
  - touch targets of at least 48 dp;
  - a content description on every icon that acts;
  - motion that respects the system's "remove animations" setting.
- **Every screen's look changes, so every Roborazzi baseline is re-recorded.** Ask me to approve that in
  the DoD, and keep old and new side by side in the run's evidence.
- Material3 1.5.0-beta01 is already in the app since Run 5, with `ExpressiveApiTest` showing the
  Expressive components work behind `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`. Use that
  version; a newer pre-release only if the DoD names it for my approval. The engine modules in `core/*`
  keep their own Material3.
- No new dependency beyond Material Symbols drawables, unless the DoD names it for my approval.

## Out of scope

The editing and AI tools themselves, a before/after comparison (there is nothing to compare until a tool
edits), the speed problems on the Galaxy A13, and the human checks waiting in `.harness/ISSUES.md`.
