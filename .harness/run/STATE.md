# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.
> Execution history lives in HISTORY.md, not here — this file must not grow with the run.

## Current

- **Run:** 11 — HSL on the Edit screen. Run Mode: Autonomous. Base commit `8b2b0f2`.
- **Stage:** escalated (DoD approval, D-001)
- **Loop Branch:** loop/kanso-hsl
- **Next Phase:** Phase 1 — T-001 (after D-001)
- **DONE-candidate:** no
- **PARTIAL-candidate:** no

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | blocked (D-001) | `data/edit/edit/{ColourRange,HslChannel,HslAdjustments,EditHsl,EditSettings}.kt`, tests `HslRangeWeightTest`, `HslFormulaTest` | - |
| T-002 | blocked (D-001) | `EditProcessor`, `FilterPreviewRenderer`, `ToolViewModel` (preview), `LivePreviewImage` (KDoc), `README.md`, tests `EditProcessorTest`, `FilterPreviewRendererTest`, `HslParityTest` | - |
| T-003 | blocked (D-001) | 27 files, see `TASKS/T-003.md` | - |
| T-004 | blocked (D-001) | `README.md`, `docs/features/{edit,tool-screen}.md`, `docs/adr/ADR-008-…`, `CONTEXT.md` | - |
| T-005 | blocked (D-001) | `screenshots/ToolScreenshotTest.kt` | - |

## Assumptions

- A-001 to A-004 (Tier 2), see `ASSUMPTIONS.md`. No Tier-3 assumption; no assumed criterion.

## Signed criteria

- none
