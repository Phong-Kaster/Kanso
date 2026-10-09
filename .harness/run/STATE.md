# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.
> Execution history lives in HISTORY.md, not here — this file must not grow with the run.

## Current

- **Stage:** Phase 6 (Iteration 8) closed the three Proof clauses the Verifier of Iteration 7 found unmet: DoD 29's `reference/` search prints nothing (T-019), a tapped colour row is proved by the choice's seven-role scheme (T-020), and the 600x400 photo is proved to fill its area (T-021). Test files only. The whole set is green (`evidence/p8-full-2.log`). The Verifier runs next.
- **Loop Branch:** loop/kanso-m3-expressive-blue (started from `76522d7`)
- **Next Phase:** none. The next invocation is the Verifier (PARTIAL-candidate below): it re-proves every `machine` criterion fresh, drives what it can, and writes the "Awaiting a person, Run 6" items.
- **DONE-candidate:** no
- **PARTIAL-candidate:** yes (recorded by Iteration 8). Nothing executable remains; criteria 13, 20, 21 and 30 await a signature and criterion 14 is assumed (A-015), so the run cannot be `DONE`.
- **Run Mode:** Autonomous

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | complete | theme, colour model, settings colour rows, theme tests | `evidence/p1-full-3.log`: 286 tests green, 18/18 pictures, lint 0 errors |
| T-002 | complete | `ui/tool/*`, tool tests | same run; pictures 16, 17 re-recorded, 22, 23 new |
| T-003 | complete | `SavedScreen.kt`, `SavedScreenTest.kt` | same run; pictures 18, 19 re-recorded |
| T-004 | complete | `ui/home/*`, Home-facing tests | `evidence/p2-full-2.log`: only the 9 expected picture failures; pictures 1, 2, 3 re-recorded |
| T-005 | complete | `GalleryScreen.kt`, gallery tests | same run; pictures 10-13 re-recorded, 26 and 27 new |
| T-009 | complete | `ToolScreen.kt`, `SaveButtonTest.kt` | same run; no picture changed |
| T-006 | complete | `CameraScreen.kt`, camera tests | `evidence/p3-full-3.log`: 318 tests green, 24/24 pictures, lint 0 errors; pictures 14, 15 re-recorded, 20, 21 new |
| T-007 | complete (the Custom swatch's last seed was split off to T-012) | `SettingsScreen.kt`, settings tests | same run; pictures 4, 5, 6 re-recorded, 24, 25 new |
| T-010 | complete | `SavedScreen.kt`, saved tests | same run; pictures 18, 19 re-recorded |
| T-008 | complete | `navigation/KansoTransitions.kt`, `NavTransitionTest.kt` (+ the Iteration's wiring) | `evidence/p4-full-2.log` to `-4.log`: 363 tests green, 24/24 pictures, lint 0 errors |
| T-011 | complete | new `a11y/*`, `theme/ThemeFollowTest.kt` | same runs; 19 states walked, no screen defect |
| T-012 | complete | `SettingsUiState.kt`, `SettingsViewModel.kt`, `SettingsRepository.kt`, the Custom swatch line, settings tests | same runs |
| T-013 | complete | `tool/ToolSelectionAnimationTest.kt` | `evidence/p6-verify-1.log` and `p6-verify-2.log` (the finished tree): exit 0, 368 tests (0 failed, 0 errors, 0 skipped), 24/24 pictures, lint 0 errors |
| T-014 | complete (attempt 2) | `navigation/KansoTransitions.kt` (the predictive function) | same run; the straight-line slide is proved by T-016 and T-017 |
| T-015 | complete | `a11y/AccessibilityWalkTest.kt` (camera refused) | same run |
| T-016 | complete | new `navigation/PredictiveBackGestureTest.kt` (+ two KDoc passages) | same run; 3 tests |
| T-017 | complete | new `navigation/PredictiveBackOpacityTest.kt`, the commit test of `PredictiveBackGestureTest.kt` | same run; the mutation (fade-out put back) fails exactly this test: `evidence/p5-mutation-fadeout.log` |
| T-018 | complete | `AccessRefusedPanel.kt`, `KansoTheme.kt`, `PermissionGate.kt` | same run; pictures 12-15 unchanged |
| T-019 | complete | `OneTypePerFileTest.kt` (one KDoc sentence) | `evidence/p8-full-2.log`; the `reference/` search prints nothing |
| T-020 | complete | `settings/ThemeSwatchTest.kt` (`choose`) | same run; 4 of 4 green |
| T-021 | complete | `tool/ToolScreenTest.kt` (the gradient layout test) | same run; 13 of 13 green |

## Signed off

None yet. Iteration 7 (the Verifier) drove nothing on the phone, because a person was using it, and wrote no unsigned item.

## Assumed criteria (Tier 3)

**Criterion 14 (assumption A-015).** DoD 14's statement says "the expressive spring"; the back gesture's preview slides the top screen on a one-second straight line instead (the Proof still holds as written). Every other assumption, A-001 to A-014 and A-016, is Tier 2.

## Assumptions

- A-001 to A-016 in `ASSUMPTIONS.md` (A-015 is also a Tier-3 reading of criterion 14) (grouping of the tools, the blue seed, colour migration, Home's design, the editor's design, the colour-picker exception, no wavy indicators, the icon set, typography, `Tool.CAMERA`, predictive back and Remove animations, Material3 pinned, the seed written through a Long constant, where a proof's wording and its test differ slightly, the back gesture's straight-line slide; A-011 is updated: Robolectric does carry a gesture's progress; A-016: DoD 24's `sdkVersion:'27'` is `aapt2`'s `minSdkVersion:'27'`).
