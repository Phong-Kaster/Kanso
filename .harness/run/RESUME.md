# RESUME BLOCK

> Regenerated every iteration. A derived cache, never a source of truth.

- **Stage:** The Verifier (Iteration 11) found a docs gap for DoD 40 and filed T-008; the PARTIAL-candidate is cleared. After T-008, record the PARTIAL-candidate again (A-001, Tier 3: criteria 27 and 34 assumed; criterion 40 awaits a signature) so the next Verifier can end `DONE_PARTIAL`.
- **Next Phase:** 8 - T-008 (Fast). Declared File Scope: `README.md`, `docs/features/edit.md`. Docs only: no Wiring Worker, no Reviewer needed beyond a scope check; the compile check is enough.
- **Queued decisions:** 0 | **Abandoned:** none | **Unreachable:** none | **Assumptions:** A-001
- **Verified commands:** build `timeout 3600 ./gradlew :app:assembleDebug` | test `timeout 3600 ./gradlew :app:testDebugUnitTest` | lint `timeout 3600 ./gradlew :app:lintDebug`. With `verifyRoborazziDebug` in the task graph, `testDebugUnitTest` also fails the 15 stale pictures (A-001); the count should stay 15.
- **DoD criteria T-008 cites (quoted):**
  - 38. [machine] README.md and the Edit and tool-screen docs describe the Stickers tool as built, with its flows drawn. Proof: README's "Editing features" table has a Stickers row with its real state; "How each editing feature works" traces Stickers from the panel to Done (screen, ViewModel, `EditSession`, `EditProcessor` order, threading, input and output, failure, file paths); README's folder table lists `reference/pinity-stickers/` as read-only; `docs/features/edit.md` and `docs/features/tool-screen.md` describe Stickers, each with a mermaid block covering it; every file path these files name exists at HEAD.
  - 40. [machine-then-human] Everything the docs say about Stickers is true of the app. Proof: pre-check: a fresh-context reader compares README, `docs/features/edit.md`, `docs/features/tool-screen.md` and ADR-004 with the code and finds no false sentence. Then a person reads README's Stickers section, uses the tool (add, move, crop, filter, Compare, Undo, Done) and notes any sentence that is untrue.
- **Open issues in scope:** the Verifier, Iteration 11 bullet in `.harness/ISSUES.md` (the five items are in T-008's Description).
- **Model tiers:** fast: `sonnet` | capable: `opus`
