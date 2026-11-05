# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.
> Execution history lives in HISTORY.md, not here — this file must not grow with the run.

## Current

- **Run:** 11 — HSL on the Edit screen. Run Mode: Autonomous. Base commit `8b2b0f2`.
- **Stage:** build — Phase 4 next
- **Loop Branch:** loop/kanso-hsl
- **Next Phase:** Phase 4 — T-004
- **DONE-candidate:** no
- **PARTIAL-candidate:** no

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | complete (Phase 1) | `data/edit/edit/{ColourRange,HslChannel,HslAdjustments,EditHsl,EditSettings}.kt`, tests `HslRangeWeightTest`, `HslFormulaTest` | tests 31 pass, lint 0 |
| T-002 | complete (Phase 2) | `EditProcessor`, `FilterPreviewRenderer`, `ToolViewModel` (preview), `LivePreviewImage` (KDoc), `README.md`, tests `EditProcessorTest`, `FilterPreviewRendererTest`, `HslParityTest` | full tests + lint green (`evidence/p2-full.log`) |
| T-003 | complete (Phase 3), row pictures not re-recorded | 27 files, see `TASKS/T-003.md` | full tests + lint green (`evidence/p3-full-7.log`); pictures red, see ISSUES |
| T-004 | pending | `README.md`, `docs/features/{edit,tool-screen}.md`, `docs/adr/ADR-008-…`, `CONTEXT.md` | - |
| T-005 | pending | `screenshots/ToolScreenshotTest.kt` | - |

## Assumptions

- A-001 to A-004 (Tier 2), see `ASSUMPTIONS.md`. No Tier-3 assumption; no assumed criterion.

## Signed criteria

- none
