# Kanso has one screen per tool and a floating tab bar, and every tool is one tap from Home

On 2026-10-12 the owner decided to lay Kanso out the way **InFlux v0.3.0** is laid out: big rounded
cards on a soft background, a hero carousel on Home, sideways strips, one full-width action at the
bottom of each screen, and a floating pill navigation bar. Only the layout comes from InFlux. Kanso
keeps its own colours: Kanso blue by default, "From wallpaper" and "Custom colour" in Settings, light
and dark everywhere. InFlux's teal is used nowhere, and none of InFlux's other features (credits, a
paywall, rewards) come with it.

**This reverses one decision of ADR-001 and keeps the rest.** ADR-001 (2026-10-09) chose one editor
for the five editing tools, with a five-tool toolbar at the bottom that switched tools without leaving
the screen. The owner reversed that on 2026-10-12 (the PRD of Run 7, item 4): **each of the five
tools gets its own screen**, and the toolbar is removed. ADR-001 carries a note saying it is superseded
in part by this record.

## What was decided, all by the owner on 2026-10-12

- **One screen per tool.** Edit, Remove background, Enhance, Retouch and Artistic each open their own
  screen from their own navigation key: a round Back button at the top left, the tool's name centred,
  the photo in a large rounded card over a blurred copy of itself, a "Replace" chip floating at the
  bottom of the card that goes back to the same tool's gallery, the tool's options strip under the
  card (until a tool's own run builds it, the short "coming in a later run" message), and one
  full-width "Save" button. There is no way to switch tools on the screen. The code keeps one shared
  layout, ViewModel and UiState, parameterised by the tool, so a later run can split one tool out
  without touching the others.
- **A floating tab bar with three tabs and a round Camera button.** Home, My edits and Settings, each
  an icon over a label, the selected one on a raised pill; the Camera is a round button beside the
  bar. The bar is the inverse of the screen (the scheme's `inverseSurface` roles: dark in light mode,
  light in dark mode), so it follows whichever colour choice is active. It shows only on the three tab
  screens. On the Navigation 3 back stack the three tabs are ordinary keys that never pile up: a tab
  tap leaves `[Home]` or `[Home, <tab>]`, and Back from a tab goes to Home.
- **Every tool is one tap from Home, with no swipe (D-001, the owner's edit at the approval gate).**
  The goal of the run is speed to the popular features. Home carries a hero carousel of the four AI
  tools, each banner with "Try now", **and a row of five round tool shortcuts directly under the
  carousel's dots** (Edit, Remove background, Enhance, Retouch, Artistic, each with its name under it,
  all visible at once on a 360 x 780 dp phone without scrolling). Each shortcut opens that tool's
  gallery at once. The draft plan had put two pill buttons there and left three tools behind carousel
  swipes; the owner reversed that.
- **My edits**, a new tab: every photo Kanso has saved to `Pictures/Kanso`, newest first, as a grid of
  rounded tiles. Home shows the newest ten as a "Recent edits" strip. Both show soft skeleton
  placeholders while they load, not a spinner.
- **Saved, the gallery, the camera and Settings** take the same arrangement: round Back and close
  buttons, a centred title in the gallery, a full-width action at the bottom, Settings as rounded cards
  whose rows have an icon in a tinted rounded square.
- **One change to the engine**, authorised by the PRD (item 9): the colour picker reads a typed code
  such as `2E7D32` as that colour, fully opaque. Everything else in `core/` is untouched.

## What stays from ADR-001

Material 3 Expressive (`MaterialExpressiveTheme`, the expressive `MotionScheme`, predictive back, the
system's "Remove animations" respected); Kanso blue as the default with the two other colour choices;
light and dark on every screen; Material Symbols Rounded icons stored as vector drawables; motion built
with Jetpack Compose's own animation APIs first. The engine modules keep their own Material3 1.3.2; the
app keeps 1.5.0-beta01, and Run 7 adds no dependency.

## Considered Options

- **Keep one editor with the toolbar (ADR-001).** Rejected by the owner on 2026-10-12.
- **Two pill buttons for the two most used tools, and swipes for the others** (the engine's first
  reading of the PRD). Rejected by the owner in D-001: three AI tools behind carousel swipes misses the
  goal.
- **A bottom navigation bar that fills the width.** Not chosen: the PRD asks for a pill that floats
  above the bottom edge with a separate round Camera button.
- **InFlux's own colours.** Rejected by the PRD: only the layout is borrowed.

## Consequences

- Every Roborazzi baseline changes. The owner's D-001 lists the 24 existing pictures that may be
  re-recorded and the 17 new ones that may be recorded, each kept beside its old version in
  `.harness/run/evidence/baseline-diffs/` while the run lasts.
- The five-tool toolbar, the editor's tool switching (`selectTool`, `Tool.editingTools`), Home's long
  lists of tool cards, Home's "Open a photo" button and its Settings and Camera rows, Settings' Back
  button and Saved's Home button are gone, and the tests that proved them are replaced or removed, each
  listed with its reason.
- Kanso's own saved photos are read by Home and My edits without a permission prompt. On Android 8.1
  and 9, and after a reinstall on Android 10 and later, they show the empty message until photo access
  was granted in the gallery.
- A later run that builds a tool's real options strip changes that tool's screen only.

## Open

- **Open:** the blur behind a narrow photo is Compose's `Modifier.blur` on Android 12 and later and a
  stretched, scaled-down copy on older phones, where `blur` draws nothing. How it looks on the owner's
  Galaxy A13 (Android 14) is judged by the owner, not by a machine.
- **Open:** the Material Symbols are Apache 2.0 and the app still has no licence notice (Run 6's
  question, still open).
