# AMENDMENTS

> Every plan mutation (Tier 1) and every applied decision, with its rationale. Newest first.

## Iteration 2 — D-001 applied (2026-11-04)

Owner's answer: DoD approved as written (37 criteria, verification classes included); A-001..A-004 stand;
the 35 pictures under `## Baselines` in DECISIONS.md may be re-recorded, old versions kept in
`.harness/run/evidence/baseline-diffs/`. T-001..T-005 unblocked.

## Iteration 2 — Tier 1: task scopes written with full paths

`foreman-check-scope.ps1` does not expand `.../` in a Declared File Scope; T-001 and T-002 now name full paths. No scope changed.

## Iteration 5 — Tier 1: Phase 3 recovered and closed (2026-11-05)

Iteration 4 was stopped by the owner mid-Phase 3. Its tree was coherent; the hanging `performScrollTo` on a circle was already removed from `HslSheetTest`, which now ends in 47 s. Review fixes (HSL controls obey `enabled`, README Filters claim, stale test comments) made in two slices. D-001's `## Baselines` lists bare file names, but `foreman-record-baselines.ps1` matches repo-relative paths (`app/src/test/screenshots/<name>`), so it put all 25 changed pictures back unchanged (`evidence/record-p3.log`). Not routed around: see ISSUES.md.
