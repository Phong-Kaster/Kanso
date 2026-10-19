
## D-001 consumed (Iteration 2, 2026-10-15)
Human approved the DoD as written; A-001..A-008 accepted; criterion 22 re-recording approved for the 21 baselines listed under `## Baselines` in DECISIONS.md (keep old pictures in `evidence/baseline-diffs/`). No plan change.

- Run 9 Iteration 4 (Tier 1): T-003 scope extended with ToolScreen.kt (wiring), EditToolTest, EditUndoTest, CompareTest (tests broken by the live preview, rewritten per task note). FilterPreviewRenderer takes a processorFor instead of a lut source.

- Run 9 Verifier, 2026-10-19 (Tier 1): DONE-candidate cleared. Added Phase 7: T-010 (R6's greps still find `saved_*` string ids and "Saved screen" prose; read by Tier-3 A-009) and T-011 (criterion 34: four imprecise sentences in README/edit.md and three stale KDocs). PRD, DoD and architecture unchanged.

- Run 9 second Verifier, 2026-10-19 (Tier 1): the PARTIAL-candidate is cleared. Added Phase 8, T-012: criterion 34 found `edit.md:229` "Done with no change" (the code means "no control used") and three stale KDocs (EditPanel.kt:110-113, EditToolRow.kt:74, ToolViewModel.kt:55-57). PRD, DoD and architecture are unchanged.

- Run 9 third Verifier, 2026-10-19 (Tier 1): the PARTIAL-candidate is cleared. Added Phase 9, T-013: criterion 34 found README/edit.md sentences that say less than the code (failed Done with Crop open, Retouch/Artistic Undo, untouched non-JPEG save name, `*Map.png` glob, name suffix, `hasAlpha()`) and four stale KDocs (ToolScreen.kt:189-190, ToolUiState.kt:46, ToolViewModel.kt:563, 659). PRD, DoD and architecture are unchanged.
