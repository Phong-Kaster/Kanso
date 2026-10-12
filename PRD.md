# Kanso — Run 7: the Influx layout, in Kanso's own colours

Run 6 gave Kanso Material 3 Expressive components, but I do not like how the screens are arranged.
I want Kanso laid out the way **InFlux v0.3.0** is laid out (HCM136, described in words below, because its
design files live outside this repository and stay there): big rounded cards on a soft background, a hero
carousel on Home, horizontal strips, one full-width action at the bottom of each screen, and a
floating pill navigation bar. **Only the layout comes from InFlux.** Kanso keeps its own colours:
Kanso blue by default, From wallpaper and Custom colour in Settings, light and dark everywhere.
InFlux's teal is not used anywhere.

**The goal is speed to the popular features.** From Home, a person reaches every tool, their recent
edits and the Camera in **one tap**, without scrolling past anything to find them (decided by me,
2026-10-12). The arrangement below exists to serve that.

## What this run delivers

1. **A floating navigation bar with three tabs and a round action button** (decided by me,
   2026-10-12):
   - a pill-shaped bar floating above the bottom edge, with **Home**, **My edits** and **Settings**,
     each an icon over a label, the selected tab drawn on a raised pill inside the bar;
   - a round button beside the bar that opens the **Camera**;
   - the bar is dark in light mode and light in dark mode (the scheme's `inverseSurface` roles, so it
     follows Kanso blue, wallpaper or custom colour);
   - the bar shows only on the three tab screens, not on the gallery, a tool, Saved or the Camera.
2. **Home**, top to bottom:
   - "Kanso" as a large title with a one-line greeting under it ("What will you edit today?");
   - a **hero carousel** of the four AI tools (Remove background, Enhance, Retouch, Artistic): each
     a large rounded banner with the tool's name, a one-line description, its icon drawn large, and a
     "Try now" pill button, with page dots under the carousel. No bitmap art: the banners are built
     from the scheme's container colours and the tool's icon;
   - a row of **two pill buttons**: "Edit" and "Remove background", the two tools people reach for
     most, so both are on screen on a fresh launch without scrolling the carousel;
   - every "Try now", pill and card opens the gallery for that tool straight away, with no screen in
     between;
   - a **"Recent edits"** strip (decided by me, 2026-10-12): Kanso's latest saved photos from
     `Pictures/Kanso` as tall rounded cards scrolling sideways, with a round arrow button that opens
     the My edits tab. Tapping a card opens it in the Edit tool. On a fresh install the strip shows a
     short empty message instead;
   - while the strip loads, **skeleton placeholders** in the shape of the cards, not a spinner.
3. **My edits** (new tab): every photo Kanso has saved, newest first, as a grid of rounded tiles, with
   the same skeleton while it loads and an empty message when there are none. Tapping a photo opens it
   full screen (the Saved layout, item 6) with Share and Edit.
4. **One screen per tool** (decided by me, 2026-10-12, reversing the one-editor decision of
   2026-10-09 in `docs/adr/ADR-001`). Each of the five tools gets its own screen:
   - a round Back button at the top left and the tool's name centred in the top bar;
   - the photo in a **large rounded card**, the space beside a narrow photo filled with a blurred
     copy of it, and a **"Replace"** chip floating at the bottom of the card that goes back to the
     gallery for the same tool;
   - under the card, the tool's **options strip**. Until a tool's own run builds it, the strip is the
     short "coming in a later run" message;
   - one **full-width "Save" button** at the bottom.

   The five-tool toolbar is removed. Switching tools happens from Home.
5. **Gallery**: a round Back button and a centred title ("<tool>: choose a photo"), album chips, and
   rounded tiles, matching the tool screens.
6. **Saved** (and a photo opened from My edits): the photo **fills the screen**, with a soft gradient
   behind a round close button (back to Home) and the title. At the bottom: a **round Share button**
   beside a **wide "Edit another" button**. The one-shot success mark stays, over the top of the photo.
7. **Settings**, now a tab with no Back button: the same grouped choices as today, drawn as rounded
   cards whose rows have the icon in a tinted rounded square, a title and a one-line description.
8. **Camera**: unchanged in behaviour; the round Back button and button shapes match the other screens.
9. **Fix: a hex colour typed in Settings is applied.** Today `HSVColorPicker` (`core/colorpicker`)
   reads the hex field with `toLong()`, which is base 10 and has no alpha: typing `2E7D32` saves
   `000002`, a transparent near-black, and any code with A to F is ignored (found on the emulator,
   2026-10-12). A complete six-digit code must apply exactly that opaque colour; a partial code must
   change nothing.

## Constraints

- `CLAUDE.md` applies in full: a light and a dark preview for every composable, one class per file, one
  UiState per screen, KDoc in plain English, the Compose animation APIs first.
- Material 3 Expressive stays the component set and motion (`MaterialExpressiveTheme`, the expressive
  `MotionScheme`, predictive back, "Remove animations" respected). Use a Material 3 component for each
  piece wherever one exists, the carousel included, before building one by hand.
- Every colour comes from the theme's colour scheme, never a fixed colour, so all three theme choices
  and both modes keep working. The colour picker's white card is still the one exception (A-006).
- Accessible: readable in both themes, touch targets of at least 48 dp, a content description on every
  icon that acts.
- **Every screen's look changes, so every Roborazzi baseline is re-recorded.** Ask me to approve that in
  the DoD, and keep old and new side by side in the run's evidence.
- **Say what is gone, not only what is there.** The DoD must check that the five-tool toolbar, the
  Settings row on Home and the big "Open a photo" button are gone, and that no test is left proving a
  screen that no longer exists.
- No new dependency, unless the DoD names it for my approval.

## Out of scope

The editing and AI tools themselves (their options strips stay placeholders), credits, a paywall,
rewards or anything else from InFlux beyond its layout, the speed problems on the Galaxy A13, and Run
6's checks waiting in `.harness/ISSUES.md`.
