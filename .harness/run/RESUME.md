# RESUME BLOCK

> Derived cache, never a source of truth. On disagreement with task files or git, this file is wrong.

- **Stage:** build - Verifier (Iteration 5) found a DoD 26 doc gap and filed T-005; DONE-candidate cleared.
- **Next Phase:** 4 - T-005 (Fast). Declared File Scope: `README.md`, `docs/features/edit.md`. No Wiring Worker. Code untouched, so only a doc check, a Reviewer, and (as nothing compiles) no Gradle run are needed; then record DONE-candidate again.
- **Queued decisions:** 0. Mode: Autonomous. **Abandoned / Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && timeout 2400 ./gradlew :app:assembleDebug` | test: `... :app:testDebugUnitTest` (OOM on StickerScreenshotTest possible; rerun alone; delete `app/hs_err_pid*.log`) | lint: `... :app:lintDebug` | pictures: `... :app:verifyRoborazziDebug`. Iteration 5: all four exit 0, 1297 tests.
- **DoD criterion T-005 cites (quoted):** 26. [machine-then-human] "The README, the Edit feature docs and a new design record describe Curves as built — touch model, natural spline, free end points, two-point minimum, no Lightness — and the old record says which of its parts are replaced." Proof: README "Editing features" Curves row and "Curves, end to end"; `docs/features/edit.md`; `docs/features/tool-screen.md` Curves lines; new `docs/adr/ADR-011-…md`; `docs/adr/ADR-009-…md` marks its Fritsch-Carlson, x = 0/1 and Lightness lines superseded by ADR-011; README ADR table lists 011; grep finds no "Fritsch", "monotone", "long press" or "Lightness curve" about Curves in README or `docs/features`; every `app/src/…` path the README names exists. A person reads "Curves, end to end" against the phone.
- **Open issues on T-005's files:** none.
- **Reference topics:** `reference/run14-facts.md`, `reference/run12-facts.md`; Constraints: `reference/constraints-verbatim.md`
- **Model tiers:** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
