# Journey report: DoD 21, Curves look, light and dark

- **Journey:** `journeys/dod21-curves-look.xml`
- **Device:** Galaxy A13 `R58W1120QVA`, 2026-11-11, debug build of this branch at `47c43d2` installed with
  `foreman-device.ps1 -Op install` (an update over the existing app; the owner's data kept, not a wiped install).
- **State it was driven from:** theme "System default" (phone in light), photo access already granted.
  **The phone was in landscape (2408 x 1080)** and the engine has no way to rotate it, so every step ran in
  landscape. Light: a full-size camera photo. Dark: `qc_cat.jpg`.
- **Result:** did not fail when driven from that state. **Not looked at:** the portrait layout the DoD's
  360/411 dp tests cover, which is the one the owner uses.

| Step | Seen | Screenshot |
|---|---|---|
| Curves open, light | Wide graph, frame and three vertical lines only; RGB, R, G, B with dots (RGB's dot three-coloured), RGB chosen; black line; trash above Reset right of the graph, both disabled; nothing cut off | `shots/c21-01-curves-light-open.png` |
| Tap on the line, light | Third point, hollow with a thick dark ring; trash enabled | `shots/c22-01-tap-line.png` |
| R, light | Red line and points, nothing selected, both icons disabled | `shots/c21-02-red-light.png` |
| Curves, dark, after a tap on the line | Same arrangement; light line; selected point hollow with a light ring; icons visible | `shots/c21-03-curves-dark-tap-line.png` |
| R, dark | Light red line, R chosen, icons disabled | `shots/c21-04-red-dark.png` |

Noticed, not against the criterion's portrait wording:
- In landscape the Curves sheet covers the whole photo, so the photo cannot be seen while editing a curve,
  and the graph is about 5 : 1 (1947 x 382 px) rather than about 2 : 1.
- Afterwards the dark-theme edit was discarded and Theme mode set back to "System default".
- Two screenshots were deleted and are not in the commit: the saved result (it shows a person in the
  owner's photo) and a photo-picker frame (the owner's gallery).
