# ASSUMPTIONS

> Autonomous mode only (ENGINE.md §14.1, ADR-027). Every decision the engine made alone, where a
> Collaborative run would have stopped to ask. The Runtime renders this file into the This run tab of
> `FOREMAN.html` after every iteration, so each entry must make sense to someone who has read nothing else.
>
> To overturn one: answer its id in `.harness/run/DECISIONS.md` and re-run. A human answer always
> outranks an assumption — the engine reverts what depended on it. Or run the revert command below
> yourself, on the Loop Branch, after the run has stopped.

---

## A-001 - Which tools are "AI tools" and which are "Edit"

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** `9e2535a` (the bootstrap commit, which only recorded it; the code that depends on it comes in the phase named under Revert)
- **Revert:** `git revert <sha>` of the Phase 2 checkpoint that built Home (T-004)

### Question

The PRD groups Home's five tool cards as "AI tools" and "Edit" without saying which card goes where.

### Options Considered

1. AI tools = Remove background, Enhance, Retouch, Artistic; Edit = Edit - consequences: matches `README.md` (AI tools: remove a background, enhance, erase an object, turn a photo into art, retouch a face; Editing: crop, colour, filters); the Edit group has one card.
2. AI tools = Remove background, Enhance, Artistic; Edit = Edit, Retouch - consequences: follows Retica's naming of its retouch tool (an object eraser); Kanso's own `Tool.kt` calls Retouch "face and skin retouching", an AI job.

### Taken, and why

Option 1: Kanso's own README makes the same split, and DoD 5 states it.

---

## A-002 - The Kanso blue seed and palette style

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** `5d5ac91` (the Phase 1 checkpoint, Iteration 2)
- **Revert:** `git revert 5d5ac91` (the Phase 1 checkpoint) (T-001)

### Question

The PRD asks for "a Kanso blue scheme generated from one blue seed" and names no colour.

### Options Considered

1. Seed `#1A73E8` (Google blue) through the existing `seedColorScheme` (MaterialKolor's default TonalSpot style) - consequences: no new dependency, a calm blue (the style mutes the seed); hue band checked by a test.
2. A more saturated seed or style (Vibrant) - consequences: livelier blue; a different generated palette, and `Expressive`-style palettes rotate hues away from blue.

### Taken, and why

Option 1: smallest change, the existing function already does it, and the pictures of DoD 20 let a person judge the result; a later run can change the seed in one line.

---

## A-003 - Colour defaults and old saved colour choices

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** `5d5ac91` (the Phase 1 checkpoint, Iteration 2)
- **Revert:** `git revert 5d5ac91` (the Phase 1 checkpoint) (T-001)

### Question

Blue becomes the default colour. What happens to installs that never chose a colour, to a saved "Default purple", and to "From wallpaper" on phones without wallpaper colours?

### Options Considered

1. Every unset install, a saved "Baseline" or an unknown value, and a saved "Wallpaper" below Android 12 read as Kanso blue; a saved "Wallpaper" at Android 12+ stays; Custom colour starts from the blue seed; "From wallpaper" is hidden below Android 12 as before this run - consequences: the owner's Galaxy A13 (Android 14, no colour ever saved) turns from wallpaper colours to blue on update, which is what "blue by default" asks; below Android 12 only two choices appear.
2. Keep wallpaper as the default on Android 12+ - consequences: contradicts "a fresh install uses a Kanso blue scheme".
3. Show "From wallpaper" on every version - consequences: on Android 11 and older it would silently show blue.

### Taken, and why

Option 1. The stored key names and the file never change, a new stored value `"KansoBlue"` is added and the old values are only read. DoD 1, 2 and 22 pin it.

---

## A-004 - Home's design details

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** `9e2535a` (the bootstrap commit, which only recorded it; the code that depends on it comes in the phase named under Revert)
- **Revert:** `git revert <sha>` of the Phase 2 checkpoint (T-004)

### Question

The PRD describes Home's content but not where "Open a photo" leads, how Settings and Camera are reached, or how the page scrolls.

### Options Considered

1. "Open a photo" opens the gallery (still titled "Edit: choose a photo") and then the editor with Edit selected; the camera shortcut is a labelled "Camera" action that opens the camera (Keep then opens the editor with Edit selected); Settings stays a visible "Settings" entry (icon and text) inside the scrolling column; all three texts appear once - consequences: about a dozen tests outside the Home task keep working (they tap those texts); a top-bar Settings icon would break seven settings tests and a pixel-sampling test.
2. A top-bar Settings icon and a camera FAB - consequences: cleaner Material look; the tests that scroll to "Settings" and "Camera" fail with no scrollable parent.

### Taken, and why

Option 1, to keep existing behaviour and tests stable (PRD: existing tests keep asserting what they assert). The title "Kanso" is the first item of the same scrolling column and Home has no top bar, because DoD 5 lists the title among the things "inside its one scrolling column" (the fresh-context review of Phase 2 found the first build kept it in a top bar, outside the column).

---

## A-005 - The editor's design and how the selected tool is kept

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** `5d5ac91` (the Phase 1 checkpoint, Iteration 2)
- **Revert:** `git revert 5d5ac91` (the Phase 1 checkpoint) (T-002)

### Question

The editor can switch tools in place. Where does the selected tool live so rotation and process death keep it, and what does "Edit another" on the Saved screen open?

### Options Considered

1. Editor stays in `ui/tool`; title = the selected tool's name; floating toolbar of five icon-only toggles with content descriptions; the selection lives in `ToolUiState`, restored after process death by the cheapest mechanism a test proves (a saveable value in the stateful screen, else `SavedStateHandle`); the navigation host builds `Saved(selectedTool, uri)` from the ViewModel, so "Edit another" opens the tool selected when the person saved - consequences: no change to `ToolScreen`'s `onSaved` signature or the `ToolPhoto` key; four tests that count the tool's name as text stay green.
2. Rewrite the top `ToolPhoto` key on every switch - consequences: a new entry decodes the photo again and plays a transition unless its content key is stable.
3. Rename the package to `ui/editor` - consequences: edits the `ScreenStateTest` guard and about ten tests, for no PRD gain.

### Taken, and why

Option 1; DoD 3, 7 and 23 pin the behaviour and leave the mechanism to the test.

---

## A-006 - "Every screen follows the theme" against the engine's colour picker

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** `9e2535a` (the bootstrap commit, which only recorded it; the code that depends on it comes in the phase named under Revert)
- **Revert:** `git revert <sha>` of the Phase 3 checkpoint (T-006, T-007)

### Question

The PRD says every screen follows the theme, the editor included. The camera paints a fixed black picture area, and Settings' colour picker sits on a fixed white card with black text because the engine's picker (`core/colorpicker`, whose source may not be edited) hard-codes black text.

### Options Considered

1. The camera's picture area and every other surface use theme colours (R6); the picker's white card stays as the one named exception in DoD 16 and 18 - consequences: the literal PRD sentence holds everywhere except one engine widget the PRD also forbids touching.
2. Keep the black viewfinder - consequences: contradicts the PRD sentence; an earlier review already flagged it.
3. Edit the engine picker - consequences: forbidden by the PRD ("the engine modules keep their own...") and by PROJECT.md.

### Taken, and why

Option 1, written into the DoD itself so the person approving it sees the exception.

---

## A-007 - Wavy progress indicators and where the loading indicator is used

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** `9e2535a` (the bootstrap commit, which only recorded it; the code that depends on it comes in the phase named under Revert)
- **Revert:** `git revert <sha>` of the Phase 2 checkpoint (T-005, T-009)

### Question

The PRD lists the wavy progress indicators among the Expressive components to use "wherever it has one for the job". Nothing in Kanso has a measurable progress.

### Options Considered

1. Do not use the wavy indicators; use `LoadingIndicator` for every wait (gallery loading, editor photo decoding, Save, Saved photo decoding, camera starting) and remove the stock spinners (R5) - consequences: nothing invented to justify a component; more tests need the paused-clock discipline.
2. Invent determinate progress (for example around Save) - consequences: false progress, extra code.

### Taken, and why

Option 1; the DoD lists it under Questions so the person sees it.

---

## A-008 - Icons: which symbols, how many, how fetched

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** `5d5ac91` (the Phase 1 checkpoint, Iteration 2)
- **Revert:** `git revert 5d5ac91` (the Phase 1 checkpoint) that added the drawables

### Question

The PRD names ten features needing icons and the source (Material Symbols Rounded, as vector drawables, no large icon library). Which symbol per feature, are more than ten allowed, and how are they obtained?

### Options Considered

1. Edit = `edit`, Remove background = `background_replace`, Enhance = `auto_fix_high`, Retouch = `face_retouching_natural`, Artistic = `brush`, Camera = `photo_camera`, Settings = `settings`, Save = `save`, Share = `share`, Back = `arrow_back`; plus the extras the screens need (`home`, `check`, `refresh`, `add_photo_alternate`, `flash_on`, `flash_off`, `flash_auto`, `cameraswitch`, `dark_mode`, `palette`, `contrast`); all outlined (FILL 0, weight 400, optical size 24), selection shown by shape and colour; fetched by the Iteration with `curl` from github.com/google/material-design-icons (public, Apache 2.0; every name answered 200 on 2026-10-09), only `android:tint` removed - consequences: the hand-drawn `BackArrowIcon.kt` and `CameraIcons.kt` become dead and are removed (R4).
2. Draw look-alikes - consequences: not Material Symbols.

### Taken, and why

Option 1. In Autonomous mode network access is not gated; provenance (URL and SHA-256) goes into the evidence.

---

## A-009 - What "expressive typography and shapes" means in code

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** `5d5ac91` (the Phase 1 checkpoint, Iteration 2)
- **Revert:** `git revert 5d5ac91` (the Phase 1 checkpoint) (T-001)

### Question

Does "Expressive typography" mean a bundled font?

### Options Considered

1. Material 3's own `…Emphasized` text styles and `MaterialExpressiveTheme`'s default shapes, plus `MaterialShapes` where the PRD asks for morphing - consequences: no font file, no new asset beyond the drawables.
2. A custom font - consequences: a new asset and licence the PRD did not ask for.

### Taken, and why

Option 1; DoD 10 checks the styles are used, DoD 20 and 21 let a person judge them.

---

## A-010 - `Tool.CAMERA` goes; the `Camera` navigation key stays

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** `9e2535a` (the bootstrap commit, which only recorded it; the code that depends on it comes in the phase named under Revert)
- **Revert:** `git revert <sha>` of the Phase 2 checkpoint (Home, T-004)

### Question

The PRD says Camera is a Home action, not an editor tool. Should the `Tool` enum keep its `CAMERA` entry?

### Options Considered

1. Remove it in Phase 2, when Home stops listing it (R3); until then `Tool.editingTools` hides it from the editor - consequences: the enum names exactly the five editing tools; saved back stacks never carry `CAMERA` (the camera tile always opened `Camera(EDIT)`).
2. Keep it - consequences: dead code the editor must filter forever.

### Taken, and why

Option 1; it is an enum entry, not a feature, and the dead-code rule applies.

---

## A-011 - What a machine can prove about predictive back and "Remove animations"

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** `9e2535a` (the bootstrap commit, which only recorded it; the code that depends on it comes in the phase named under Revert)
- **Revert:** `git revert <sha>` of the Phase 4 checkpoint (T-008)

### Question

Robolectric may not deliver back-gesture progress to Navigation 3, and may not carry the system animation scale into Compose.

### Options Considered

1. The machine proves the transition specs, the manifest flag and finished moves (criterion 14), repeats them with the animation scale at 0 only if a control shows the host carries the setting (criterion 13), and a person checks the gesture preview and "Remove animations" on the phone (criterion 21); the owner's Galaxy A13 uses three-button navigation by default, so the gesture preview needs the person to switch to swipe gestures - consequences: honest "not driven" outlets instead of an unprovable machine claim.
2. Class both as machine - consequences: either abandoned tasks or a claim nothing proves.

### Taken, and why

Option 1 (POLICIES: classify for a signature when a command cannot drive it).

---

## A-012 - Material3 version and how the Expressive APIs are opted into

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** `5d5ac91` (the Phase 1 checkpoint, Iteration 2)
- **Revert:** `git revert 5d5ac91` (the Phase 1 checkpoint) (T-001)

### Question

The PRD pins Material3 1.5.0-beta01 and allows a newer pre-release only if the DoD names it.

### Options Considered

1. Stay on 1.5.0-beta01 (no newer pre-release named); each Expressive call site opts in with `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`, none at module level - consequences: matches `ExpressiveApiTest`; nothing to approve.
2. Move to a newer pre-release or a module-wide opt-in flag - consequences: needs the person's approval in the DoD, which names none.

### Taken, and why

Option 1.

---

## A-013 - The Kanso blue seed is declared through a Long constant

- **Tier:** 2 (plan/architecture)
- **Iteration:** 2 (review finding of Phase 1)
- **First dependent checkpoint:** `5d5ac91` (the Phase 1 checkpoint, Iteration 2)
- **Revert:** `git revert 5d5ac91` (the Phase 1 checkpoint) (`SeedColorScheme.kt` only: put `Color(0xFF1A73E8)` back on the declaration)

### Question

DoD 16 greps `app/src/main` for `Color(0x` and allows only the colour picker's card, `sample...` declarations and `@Preview` functions, yet the DoD's own header requires the constant `KANSO_BLUE_SEED = 0xFF1A73E8`, which that grep also finds if it is written `Color(0xFF1A73E8)`. The seed is the input of the colour generator, not a colour a screen paints.

### Options Considered

1. Write the hex as `private const val KANSO_BLUE_SEED_ARGB = 0xFF1A73E8` and define `val KANSO_BLUE_SEED: Color = Color(KANSO_BLUE_SEED_ARGB)` - consequences: same name, type and value for every user and test; the grep of DoD 16 reports only real paint calls; costs one extra declaration.
2. Keep `Color(0xFF1A73E8)` and list the line in `fixed-colours.md` as an exception - consequences: the DoD's wording "only that one line" is then literally false, so criterion 16 would have to be read as assumed (Tier 3) and could never end `DONE`.

### Taken, and why

Option 1. It keeps the criterion provable by its own command and changes nothing a user or a test can observe. `fixed-colours.md` (the Verifier's) names the constant anyway.

