# HISTORY

## Iteration 1 — Bootstrap (2026-11-06)

- PRD: Run 13, a Colour group (RGB, HSL, Curves) on the Edit tool row. Repository class: product.
- Fan-out: survey + DoD proposal, decomposition + conflict analysis, critique (all Capable analysts).
  Critique changes adopted: one task (pictures re-recorded in Phase 1 so it ends green), closing via the
  `onOpen` toggle (no new callback), shared pieces in `ui/tool/component/`, Compare/Back disabled while
  comparing, a rotation criterion, a human-only README read, named unedited tests for DoD 9.
- Created loop branch `loop/kanso-colour-group`, DoD (19 criteria + R1, R2), PLAN, T-001, D-001.
- Knowledge: `reference/run13-facts.md` added to the index; the three Constraints that sat under the
  Reference index now sit under a "Constraints (continued)" heading.

## Iteration 2 - Phase 1 (T-001), 2026-11-06

- Consumed D-001 (approved): AMENDMENTS, ESCALATION marked answered, mode now Autonomous. Fetched `instant_mix` as `ic_instant_mix.xml` (tint removed).
- Workers (Fast): slice A model + shared pieces, B Edit row + tests, C other tests + pictures tests, D docs. Review (Capable) found one blocking item (test-edits log, written by the Iteration) and nine others; fixed N1-N7 in two more Worker slices (split ToolRowEntry/CompareEntry into files, real one-line test, README/docs wording); N8/N9 recorded.
- Evidence: build + 1282 unit tests + lint + verifyRoborazzi green (`evidence/p1-final.log`); 43 listed pictures re-recorded, 101-104 added, none unlisted changed. ADR-010 written.
- PARTIAL-candidate set: only criterion 11 (people look at pictures) awaits a signature.
