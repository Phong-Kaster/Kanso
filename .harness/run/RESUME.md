# RESUME BLOCK

> Regenerated every iteration. A derived cache, never a source of truth.

- **Stage:** The Verifier (Iteration 9) cleared the DONE-candidate because the docs are wrong in places. T-007 fixes them. After T-007, the next Iteration records a **PARTIAL-candidate**, not a DONE-candidate, because A-001 (Tier 3) assumes criteria 27 and 34.
- **Next Phase:** 7. T-007 (Fast), scope `README.md`, `docs/features/edit.md`, `docs/features/tool-screen.md`, `app/src/main/java/com/phongkaster/kanso/ui/tool/components/EditPhotoArea.kt` (KDoc only), `app/src/main/java/com/phongkaster/kanso/ui/tool/components/topbar/ToolTopBar.kt` (KDoc only). The 9 items are in `TASKS/T-007.md`.
- **Queued decisions:** 0 | **Abandoned:** none | **Unreachable:** none | **Assumptions:** A-001 (Tier 3; criteria 27, 34)
- **Verified commands:** build `timeout 3600 ./gradlew :app:assembleDebug` | test `timeout 3600 ./gradlew :app:testDebugUnitTest` | lint `timeout 3600 ./gradlew :app:lintDebug`. `verifyRoborazziDebug` fails 15 stale pictures (A-001); compare the count, it should stay 15.
- **Criteria T-007 cites (quoted):**
  - 38. [machine] README.md and the Edit and tool-screen docs describe the Stickers tool as built, with its flows drawn. Proof: README's "Editing features" table has a Stickers row with its real state; "How each editing feature works" traces Stickers from the panel to Done (screen, ViewModel, `EditSession`, `EditProcessor` order, threading, input and output, failure, file paths); README's folder table lists `reference/pinity-stickers/` as read-only; `docs/features/edit.md` and `docs/features/tool-screen.md` describe Stickers, each with a mermaid block covering it; every file path these files name exists at HEAD.
  - 40. [machine-then-human] Everything the docs say about Stickers is true of the app. Proof: pre-check: a fresh-context reader compares README, `docs/features/edit.md`, `docs/features/tool-screen.md` and ADR-004 with the code and finds no false sentence. Then a person reads README's Stickers section, uses the tool (add, move, crop, filter, Compare, Undo, Done) and notes any sentence that is untrue.
- **Open issues in scope:** `.harness/ISSUES.md` Run 10: "README Stickers step 6 says a tap selects but touch-down selects" (T-007 item 8 covers it).
- **Model tiers:** fast: `sonnet` | capable: `opus`
