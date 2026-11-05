# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.
> Execution history lives in HISTORY.md, not here — this file must not grow with the run.

## Current

- **Run:** 11 — HSL on the Edit screen. Run Mode: Autonomous. Base commit `8b2b0f2`.
- **Stage:** build — Phase 6 (T-006) complete; criteria 23-26 need a person
- **Loop Branch:** loop/kanso-hsl
- **Next Phase:** none executable
- **DONE-candidate:** no
- **PARTIAL-candidate:** yes - criteria 23-26 await a person's signature (cleared earlier by the Verifier, Iteration 7: the 4 Sticker pictures are on the owner's Baselines list; T-006 filed)

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | complete (Phase 1) | `data/edit/edit/{ColourRange,HslChannel,HslAdjustments,EditHsl,EditSettings}.kt`, tests `HslRangeWeightTest`, `HslFormulaTest` | tests 31 pass, lint 0 |
| T-002 | complete (Phase 2) | `EditProcessor`, `FilterPreviewRenderer`, `ToolViewModel` (preview), `LivePreviewImage` (KDoc), `README.md`, tests `EditProcessorTest`, `FilterPreviewRendererTest`, `HslParityTest` | full tests + lint green (`evidence/p2-full.log`) |
| T-003 | complete (Phase 3), row pictures not re-recorded | 27 files, see `TASKS/T-003.md` | full tests + lint green (`evidence/p3-full-7.log`); pictures red, see ISSUES |
| T-004 | complete (Phase 4) | `README.md`, `docs/features/{edit,tool-screen}.md`, `docs/adr/ADR-008-…`, `CONTEXT.md` | scope check passed; docs only |
| T-006 | complete (Phase 6) | `screenshots/StickerScreenshotTest.kt`; baselines 69, 70, 81, 82 via the wrapper | full tests, lint, verifyRoborazzi all exit 0 (`evidence/t6-full.log`, `t6-lint.log`, `t6-verify.log`) |
| T-005 | complete (Phase 5) | `screenshots/ToolScreenshotTest.kt`; 12 new pictures 83-94, listed baselines re-recorded (old ones in `evidence/baseline-diffs/`) | pictures recorded, 85, 87, 94 looked at; full suite not re-run |

## Assumptions

- A-001 to A-004 (Tier 2), see `ASSUMPTIONS.md`. No Tier-3 assumption; no assumed criterion.

## Signed criteria

- none
