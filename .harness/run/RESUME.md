# RESUME BLOCK

- **Stage:** phase 5. The Verifier (Iteration 7) found the README stale and filed T-008; PARTIAL-candidate cleared. After T-008 passes, record PARTIAL-candidate again (17 pictures still unrecorded, record command denied).
- **Next Phase:** T-008 (Capable). Scope: `README.md`, `.harness/run/evidence/readme-paths.sh`. The task file lists the six stale parts with line numbers. No Worker is strictly needed for wiring; one Worker writes the README from the code.
- **Queued decisions:** 0
- **Abandoned / unreachable:** none
- **Open red:** 17 `ToolScreenshotTest` pictures (45-54 new without baseline; 16,17,22,23,40,41,42 differ), `recordRoborazziDebug` denied; listed in ISSUES.md.
- **Verified commands:** build `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test `./gradlew :app:testDebugUnitTest :app:verifyRoborazziDebug` | lint `./gradlew :app:lintDebug` (run under `timeout`). T-008 touches no code: its proof is the greps below plus `bash .harness/run/evidence/readme-paths.sh`.
- **Criteria the Phase cites (quoted from DoD):**
  - 31. [machine] The README says, for each of the five tools, exactly what the code does. Proof: the "Editing features" table has no row marked "UI only" or "Not wired" for the five tools (`git grep -n -E 'UI only|Not wired|coming in a later run' -- README.md` prints nothing); the section "How each editing feature works" has one subsection per tool naming its screen, ViewModel, every processing step in order, the engine module, library or model, the thread, input and output size and format, where it is saved and what happens on failure; and every path in backticks in that section exists (`git ls-files` check by script `.harness/run/evidence/readme-paths.sh`, which the Verifier writes and runs).
  - R2. [machine] The README no longer says the image engine is "built, but not called". Proof: `git grep -n -i 'built, but not called' -- README.md` prints nothing; the section describes what each engine module is called for.
- **Open ISSUES naming README.md:** "Phase 2 review, not fixed: ... README "How each editing feature works" traces still to write (T-007)."
- **Model tiers:** fast: `sonnet` | capable: `opus`
- **Notes:** Workers read `.harness/` only under `knowledge/reference/`; copy PROJECT.md Constraints (lines 3-67) into the Brief verbatim. Device criteria 7, 9, 11, 13: A13 was in use at Iteration 7; check `-Op focus` first.
