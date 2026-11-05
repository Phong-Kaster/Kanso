# ASSUMPTIONS

> Autonomous mode only (ENGINE.md §14.1, ADR-027). Every decision the engine made alone, where a
> Collaborative run would have stopped to ask. The Runtime renders this file into the This run tab of
> `FOREMAN.html` after every iteration, so each entry must make sense to someone who has read nothing else.
>
> To overturn one: answer its id in `.harness/run/DECISIONS.md` and re-run. A human answer always
> outranks an assumption — the engine reverts what depended on it. Or run the revert command below
> yourself, on the Loop Branch, after the run has stopped.

---

## A-001 - The HSL sheet fits 136 dp with three 40 dp rows and 48 dp touch areas

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1 (bootstrap), commit 77f828b
- **First dependent checkpoint:** Phase 3's commit (T-003) — filled in after it lands
- **Revert:** `git revert <sha>` of Phase 3
- **Assumed DoD criteria:** -

### Question

The PRD keeps the sheet at 136 dp and puts three rows in it (nine circles, three chips, one slider).
Kanso's controls are 48 dp tall to touch, and three 48 dp rows need 144 dp before any spacing. Which
gives way?

### Options Considered

1. Three rows of 40 dp on screen, each control's touch area still 48 dp (Compose extends a small
   control's touch area past its edges) - consequences: 136 dp kept, nothing scrolls, touches near a
   row's edge are shared with the next row by closeness.
2. The sheet scrolls vertically - consequences: the slider can be hidden below the chips.
3. A taller sheet - consequences: the photo changes size, which the PRD forbids.

### Taken, and why

Option 1: the only one that keeps both of the PRD's promises (136 dp, photo never changes size) and
the 48 dp touch rule. The nine circles scroll sideways on a screen narrower than the row.

---

## A-002 - README is updated in each commit that changes the app, not only in Phase 4

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1 (bootstrap), commit 77f828b
- **First dependent checkpoint:** Phase 2's commit (T-002)
- **Revert:** `git revert <sha>` of each Phase
- **Assumed DoD criteria:** -

### Question

The PRD lists README in Phase 4 ("the docs"), but CLAUDE.md says a behaviour change without its
README update is unfinished.

### Options Considered

1. README in Phase 4 only - consequences: Phases 2 and 3 ship code the README does not describe.
2. README in every Phase that changes the app - consequences: three small README edits.

### Taken, and why

Option 2: the standing rule in CLAUDE.md is the owner's and the PRD says it holds. Phase 2 adds the
render step (HSL row "not wired"), Phase 3 the working tool, Phase 4 the ADR link and the A13 cost.

---

## A-003 - The 27 values are stored as one HSL group inside the edit settings, from -1 to 1

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1 (bootstrap), commit 77f828b
- **First dependent checkpoint:** Phase 1's commit (T-001)
- **Revert:** `git revert <sha>` of Phase 1 and every later Phase
- **Assumed DoD criteria:** -

### Question

"27 values in `EditSettings`": as 27 separate fields, or grouped?

### Options Considered

1. 27 flat fields (`redHue`, `redSaturation`, …) - consequences: closest to RGB, but 27 names to
   keep in step everywhere.
2. One `HslAdjustments` value inside `EditSettings` holding three lists of nine (Hue, Saturation,
   Lightness per colour range) - consequences: one field, indexed by colour range.

### Taken, and why

Option 2. Stored from -1 to 1 like RGB's values; the slider shows -100..+100.

---

## A-004 - The HSL formula and its thresholds

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1 (bootstrap), commit 77f828b
- **First dependent checkpoint:** Phase 1's commit (T-001)
- **Revert:** `git revert <sha>` of Phase 1 and every later Phase
- **Assumed DoD criteria:** -

### Question

The PRD describes soft edges, a ±20° hue limit, grey-to-double saturation and "never pure black or
white", and leaves the engine to the run. Which exact curve and limits?

### Options Considered

1. CPU, one formula used by both the live preview copy and the saved photo - consequences: identical
   by construction; cost is a CPU pass over a ≤ 640 px copy per drag step.
2. A GPU shader - consequences: needs Android 13 (Kanso supports Android 8.1 up), and the saved
   photo would need a second copy of the formula.
3. A colour lookup table - consequences: not pixel-identical to the formula.

### Taken, and why

Option 1. Range share = cos²(π·d/80) of the hue distance d (1 at the centre, ½ + ½ halfway, 0 at the
next centre); colourfulness gate smoothstep(0.03, 0.25) on max − min, so greys are hardly touched;
Hue ±20° at ±100; Saturation × (1 + value), clamped; Lightness moves at most to 5 % or 95 %, never
pure black or white. ADR-008 records it.

## A-005 — Tier 2 (Iteration 5)

**Question:** DoD 21 wants every circle, chip and the slider to be at least 48 dp to touch, but circles are 40 dp apart and the sheet's rows 36 dp apart, so 48 dp touch areas overlap their neighbours (review B3).
**Options:** (1) keep 32 dp cells with 48 dp touch boxes that overlap by 8 dp / 4 dp; (2) 48 dp pitch, which does not fit the 136 dp sheet (A-001) or 411 dp width.
**Taken:** option 1, the same pattern as the existing `ColourCircle`; each control's own touch box measures >= 48 dp (tested). The owner should judge feel on a phone (DoD 24).
**Depends on:** Iteration 5 (phase-3 checkpoint).
