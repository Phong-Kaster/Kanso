# HISTORY

### Iteration 1 - 2026-10-05 (bootstrap)
- **Phase:** none (bootstrap)
- Attempted: read the PRD and engine modules; wrote DoD, PLAN and five tasks; one critique analyst (Capable) reviewed the draft.
- Learned: latest stable Navigation 3 is 1.2.0 and Koin 4.2.2; engine modules use the AGP 8 DSL; `core/common` uses Firebase; `HSVColorPicker` hard-codes black text and parses hex with `toLong()`.
- Reconciled: applied the critique's findings (KDoc moved into every task, picker driven through a ViewModel seam, ML Kit's transitive Firebase allowed, stronger persistence proof, material-kolor for seed colours).

## Archived Decisions

- D-001 (DoD approval): approved as written, 2026-10-05; assumptions A-001 to A-004 not overturned.

### Iteration 2 - 2026-10-05
- **Phase:** T-001
- Recovered: found uncommitted T-001 work from the previous invocation; build, tests and lint verified green before salvaging.
- Reviewer (Capable) found READ_PHONE_STATE merged in, undocumented Firebase infrastructure and version pins, and a grep scope trap. Fixed all; tried nav3 1.2.0 / lifecycle 2.11 / BOM 2026.09, all need AGP 9.1, reverted.
- Learned: AGP 8.13 caps versions; recorded in PROJECT.md.
