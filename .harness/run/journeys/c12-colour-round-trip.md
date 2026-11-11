# Journey report: criterion 12 (Colour round trip), Iteration 3 (Verifier), 2026-11-11

Driven on `R58W1120QVA` (Galaxy A13) only, through `foreman-device.ps1`. **Start state:** debug build
from this checkpoint freshly installed (`-Op uninstall`, `-Op install`), photo permission not yet
granted, granted with "Allow all" when asked. Photo: `qc_cat.jpg`. Journey: `journeys/c12-colour-round-trip.xml`.

| Step | Result | Evidence |
|---|---|---|
| (a) Colour visible without swiping | did not fail: `edit-group-COLOUR` at x 551-720 of 1080, Filters partly visible, Compare pinned | `c12-04-edit.xml` |
| (b) tap Colour: Back, RGB, HSL, Curves, Compare | did not fail: `edit-group-back` [11-214] < RGB < HSL < Curves < `edit-compare` [878-1069], no sheet | `c12-05-colour.xml` |
| (c) open HSL, change, Back: Colour dot | did not fail: Saturation (Red) 0 → 26, `edit-tool-dot-HSL` shown; after Back, layer 1 with `edit-group-dot-COLOUR` | `c12-07-hsl-changed.xml`, `c12-08-back.xml`, `c12-08-layer1-dot.png` |
| (d) Compare shows the original | partly driven: Compare is a toggle (no hold op exists in the wrapper); with it on, `edit-compare` checked and enabled, every tool entry disabled. **Whether the picture is the original was not judged** (a +26 red saturation is barely visible on this photo). | `c12-09-compare.xml`, `c12-09-compare.png` |
| (e) system Back asks to discard | did not fail: "Discard your changes?" with Discard / Keep editing | `c12-10-discard.xml` |
| (f) Done, Save: saved photo has the change | partly driven: Save reported "Your photo is saved in Pictures/Kanso.", no crash in the app's logcat. **The saved file's pixels were not read** (the wrapper cannot read the media store). | `c12-11-done.xml`, `c12-12-saved.xml` |

Not committed: the photo-picker dump, which listed the owner's own file names.

Unlooked-at: whether the saved photo really carries the change, whether Compare's picture is the
original, and how the row looks to a person. The owner repeats the journey and signs.
