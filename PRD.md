# Kanso — Curves that handle like Meitu's

Kanso's Curves tool works, but it does not feel like the curve editors people already know. I
compared it with Meitu's Curves on my Galaxy A13 (2026-11-11). **This run changes how Kanso's
Curves is touched, how its line bends, and how it looks, to match Meitu's behaviour**, while every
colour stays in Kanso's own theme. Screenshots of each Meitu step:
`C:\Users\MAC_PC\AppData\Local\Temp\claude\e211d288b1631b2f\d132edb7-d9d0-44b3-b268-10fe6949f509\scratchpad\meitu\`.

## What Meitu does (observed on the A13), and what Kanso does today

| | Meitu | Kanso today |
|---|---|---|
| Add a point | Tap **on the line**: a point appears on the line at that spot (its height comes from the line, not from the finger) and becomes the selected point. A drag that starts on the line away from any point adds a point there and drags it. A tap away from the line only deselects. | A tap on any empty spot adds a point at the finger, and the line jumps to it. |
| Select | One point at a time is selected: white fill with a coloured ring. The others are dark with a white ring. Tapping or dragging a point selects it. | No selection; every point looks the same. |
| Delete | A **trash button** deletes the selected point, then the neighbour to its left (or right, if none) becomes selected. Any point can be deleted, end points included, while more than two remain. The button is disabled with nothing selected or with only two points left. | A long press on a middle point removes it. End points cannot be removed. |
| End points | Move **sideways as well as up and down**. Left of the first point and right of the last, the line is flat at that point's height (a black point / white point). | The first point stays at x = 0 and the last at x = 1; both move only up and down. |
| Line shape | A smooth spline through every point that **may overshoot** between points; the result is clipped to black and white (seen as a flat top or bottom where it overshoots). | Monotone cubic (Fritsch-Carlson): never overshoots, so one point bends the line less. |
| Graph | Wide, about 2 : 1. Only the frame and three vertical lines (four columns). No diagonal guide. | Square-ish card, a 4 x 4 grid and the diagonal guide. |
| Channels | `RGB · R · G · B`, each with a colour dot (RGB's is multicoloured). The line takes the channel's colour: neutral for RGB, red, green, blue. | RGB, Red, Green, Blue, Lightness chips; the line is red for Red and so on. |
| Reset | An icon button beside the graph, under the trash button. It resets **only the open channel**, and is disabled while that channel is untouched. | A text "Reset" button. |

What is already the same and must stay so: the photo follows the finger live while dragging; a
point cannot pass its neighbour; dragging past the frame holds the point at the edge.

## The decisions (mine)

1. **Touch model as Meitu's**: tap on the line adds a point on the line and selects it; tap away
   from the line deselects; a drag from the line adds and drags; tap or drag a point selects it;
   a trash button deletes the selected point (end points too, never below two points). The long
   press that removes a point today goes away.
2. **End points move freely** in both directions, and the line is flat beyond the first and last
   point.
3. **Line shape as Meitu's**: a spline through every point that may overshoot, clipped to 0..1,
   replacing the monotone curve.
4. **Look as Meitu's, colours as Kanso's**: a wide graph, frame plus vertical lines only, no
   diagonal; channel entries `RGB · R · G · B` with colour dots; the line coloured per channel;
   the selected point drawn differently from the others; the trash and Reset buttons as icons
   beside the graph, Reset acting on the open channel only. Every colour, font, shape and spacing
   comes from Kanso's theme in light and dark — Meitu's dark-only panel is not copied.
5. **Remove the Lightness channel** (chip, curve, the Lightness pass in `EditCurves`). Curves are
   not stored anywhere but in the saved photo, so no saved data needs migrating.
6. **Not in this run**: Meitu's preset row (Custom, Highlight, Cool, Dark, Purple…) and its
   Hide/Show button.

## What must keep working

- A drag is one Undo step; adding and deleting a point is one Undo step each; Undo/Redo across
  channels as today.
- Curves still runs on the CPU after HSL (ADR-009), and the saved photo matches the preview.
- The Colour group from Run 13 (Colour → Back, RGB, HSL, Curves) and its "changed" dots.
- RGB and HSL do not change.

## Open for the DoD (propose, I decide)

- The exact spline (natural cubic or Catmull-Rom) — whichever bends closest to Meitu's for one
  and two points; show me the comparison.
- How near the line a tap must be to count as "on the line".
- The selected point's ring colour and the RGB line colour in light and dark, from the theme.
- Which screenshots move (95–100 at least; 99/100 are Lightness pictures and go away) and which
  new ones are added.

Test on my Galaxy A13 (serial `R58W1120QVA`) at the end, as before.
