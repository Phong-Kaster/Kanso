# Journey report: DoD 22, Curves handling

- **Journey:** `journeys/dod22-curves-handling.xml`
- **Device:** Galaxy A13 `R58W1120QVA`, 2026-11-11, debug build at `47c43d2` installed with `-Op install`
  (an update over the existing app, data kept). **Landscape (2408 x 1080)**: the engine cannot rotate the phone.
- **Photo:** a full-size camera photo from the owner's gallery (`20250524_…jpg`).
- **Input:** `adb shell input tap` / `swipe` (900 ms drags), so it is synthetic, not a finger.
- **Result:** every check did not fail when driven from that state. **Not looked at, for the person to
  judge:** the feel (strictness of the 24 dp reach, stutter), and whether the photo follows the drag.
  In landscape the sheet hides the photo.

| Step | Read from the screen | Screenshot |
|---|---|---|
| Tap the line at x 0.4 | "RGB curve, 3 points", trash enabled, Undo enabled | `shots/c22-01-tap-line.png` |
| Tap ~1 cm (160 px) below the line at x 0.7 | still 3 points, trash disabled (selection cleared) | - |
| Drag from the line at x 0.7 up 70 px | 4 points, new one selected; the line already clips flat at the top right | `shots/c22-02-drag-from-line.png` |
| Tap the point at x 0.4 | still 4 points, trash enabled | - |
| Drag the first point right ~300 px | moved sideways, selected, line flat to its left | `shots/c22-03-first-point-right.png` |
| Trash | 3 points; the right neighbour selected; flat to its left | `shots/c22-04-end-point-deleted.png` |
| Drag that point to the top | line pinned at white across the left half (clipped) | `shots/c22-05-pulled-high.png` |
| R, then RGB | "Red curve, 2 points", nothing selected; back to "RGB curve, 3 points", nothing selected | `shots/c21-02-red-light.png` |
| Undo x4 | 3, 4, 4, 3 points; Redo enabled | `shots/c22-06-after-4-undos.png` |
| Redo x4 | 4, 4, 3, 3 points; Redo disabled after the fourth | `shots/c22-07-after-4-redos.png` |
| Done, Save | "Your photo is saved in Pictures/Kanso."; the result is strongly brightened | not kept (shows a person) |

The saved photo is now in the owner's `Pictures/Kanso`.
