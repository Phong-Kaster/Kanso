# AMENDMENTS

> Every plan mutation (Tier 1, logged) and every consumed decision, with the human's rationale. Newest first.

### 2026-10-26 - D-001 consumed (DoD approved with owner edits)
- Human rationale: approved; owner edited DoD criteria 3, 11, 12, 27, 28 and added 41-43 (Stickers panel is a
  sheet confirmed with a check mark or cancelled with an X; Back = X; Undo one action per step); kept 2, 18, 20; Q1
  `ic_rotate_circle` unported; three capabilities approved; 16 row pictures may be re-recorded (Baselines list).
- Tier-1 plan amendment: the "tabs over a grid" panel in PLAN.md / T-002 / T-003 reads as the sheet in DoD 3, 11, 12,
  41, 42, 43. T-002 gains: category chips (LazyRow) + 3-column LazyVerticalGrid, X at top-left / check at top-right, the sheet
  replaces the tool row, the ViewModel snapshots settings + undo history on open and restores them on X / Back, drops
  selection on close. T-003 selection/gestures act only while the sheet is open. T-001 is unchanged.

## Iteration 4 (Phase 2) - Tier 1
- T-002 slice 2 split into 2 and 2b (6-file cap): top bar and chips became their own files (one composable per file, CLAUDE.md hierarchy rule) and `EditBottomBar.kt` joined the scope because the sheet replaces the tool row there. PRD, DoD and architecture unchanged.

## Iteration 5 - Tier 1: StickerOverlayTest joins T-004 slice 3 scope
Since slice 1 sticker pixels arrive via the asynchronously rendered filtered copy, so `StickerOverlayTest.compare_hides...` (line 105) read the pixel too early and failed. The premise of the check changed; the test must poll for the pixel. PRD, DoD, architecture unchanged.

## Iteration 5 - Tier 1: review-fix files join T-004
Reviewer found dead `StickerImage` and a second composable `StickerPlace` in `StickerOverlay.kt`; both files join slice 3's scope (StickerPlace.kt new). PRD, DoD, architecture unchanged.

## Iteration 8 - Tier 1: ADR-004 joins T-006
The planned Wiring Worker never wrote `docs/adr/ADR-004-sticker-is-part-of-the-photo.md`; it joins T-006's scope. PRD, DoD, architecture unchanged.

## Iteration 9 (Verifier) - Tier 1: T-007 added
The Verifier's docs check (DoD 38 and 40's pre-check) found a dead `EditPill.kt` path and 8 false or imprecise sentences, plus 2 stale KDocs. They are filed as T-007, and the DONE-candidate is cleared. PRD, DoD and architecture are unchanged. Also A-001 (Tier 3, see ASSUMPTIONS.md): the 15 stale screenshot references are not re-recorded, so criteria 27 and 34 are assumed, not met.
