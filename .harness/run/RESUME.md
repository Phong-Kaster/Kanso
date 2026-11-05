# RESUME BLOCK

> Derived cache, never a source of truth. On disagreement with task files or git, this is wrong.

- **Stage:** build — Phase 3 done (T-003 complete, row pictures not re-recorded); next Phase 4.
- **Next Phase:** Phase 4
  - T-004 (Fast; `TASKS/T-004.md`): README, `docs/features/edit.md`, `docs/features/tool-screen.md`, new ADR-008, `CONTEXT.md`. Carry-over: stale `ToolViewModel.kt` KDocs and `FilterPreviewRenderer.kt:63` comment (outside T-004's scope: leave in ISSUES.md unless a scope amendment is made).
  - Then Phase 5: T-005 (`screenshots/ToolScreenshotTest.kt`, HSL pictures).
  - T-003 left: HSL tool, sheet, ViewModel, README row "Works" and pipeline; full tests + lint green (`evidence/p3-full-7.log`).
- **Pictures:** `verifyRoborazziDebug` fails 25 pictures (the row moved). The record script refuses them: `## Baselines` in DECISIONS.md lists bare names, the script wants `app/src/test/screenshots/<name>`. Only the owner can fix the list; do not route around. Re-run `foreman-record-baselines.ps1` once DECISIONS.md is fixed.
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** (run from the repo root, under `timeout`) build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` (about 6 min; run `HslSheetTest` alone first when touching the sheet) | lint: `./gradlew :app:lintDebug` | pictures: `./gradlew :app:verifyRoborazziDebug` | record: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app` | Worker compile (never piped): `powershell -NoProfile -File D:/Valve/Personal/Kanso/.harness/loop/bin/foreman-compile.ps1` (module `app`). Never run `python -`/stdin-reading commands in Bash: they hang.
- **Criteria the next Phase cites (quoted from DoD.md):**
  - **DoD 26:** [human-only] The README, the two feature pages and the ADR tell the truth about HSL and are readable.
  - **DoD 31:** [machine] README describes HSL as the code is: its row in "Editing features" with its real state, and its pipeline traced end to end in "How each editing feature works".
  - **DoD 32:** [machine] The Edit and tool-screen feature pages describe HSL, with flow diagrams.
  - **DoD 33:** [machine] A new decision record says how HSL is computed and why that way, how the preview and the saved photo stay identical, and what it costs on the Galaxy A13.
  - **DoD 34:** [machine] The glossary defines "colour range" and says the Edit screen now has nine tools.
- **Open issues touching the next Phase's files:** none beyond the carry-overs above.
- **Reference topics the next Phase touches:** `.harness/knowledge/reference/run11-facts.md`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
